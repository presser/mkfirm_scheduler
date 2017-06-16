import com.esotericsoftware.yamlbeans.YamlReader;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.*;

import java.nio.charset.Charset;
import java.util.*;

/**
 * Runs a set of random requests
 */
public class RandomRunner
{
    private String configFileName;
    private String resultsFileName;

    private List<Stream> streams;
    private List<RandomRequestProducer> producers;
    private List<RandomRequestProcessor> processors;

    private int consoleOutputInterval;
    private Terminal terminal;
    private TextGraphics terminalText;

    private Clock clock;
    private DBPScheduler scheduler;

    public RandomRunner(String configFileName, String resultsFileName)
    {
        this.configFileName = configFileName;
        this.resultsFileName = resultsFileName;
        this.clock = new CurrentTimeMillisClock();
        this.scheduler = new DBPScheduler(clock);
    }

    private void loadConfig() throws IOException
    {
        YamlReader yaml = new YamlReader(new FileReader(configFileName));
        Map config = (Map) yaml.read();

        consoleOutputInterval = Integer.valueOf((String)config.get("consoleOutputInterval"));

        streams = new ArrayList<>();
        List streamList = (List)config.get("streams");
        for (int i = 0; i < streamList.size(); i++)
        {
            Map streamConfig = (Map)streamList.get(i);
            streams.add(new Stream(Integer.valueOf((String)streamConfig.get("m")),
                    Integer.valueOf((String)streamConfig.get("k")),
                    Integer.valueOf((String)streamConfig.get("dynamicFaultChances"))));
        }

        producers = new ArrayList<>();
        List prodList = (List)config.get("producers");
        for (int i = 0; i < prodList.size(); i++)
        {
            Map prodConfig = (Map)prodList.get(i);
            RandomIntervalGenerator interval = new RandomIntervalGenerator(
                    Integer.valueOf((String)prodConfig.get("intervalBetweenRequestsMin")),
                    Integer.valueOf((String)prodConfig.get("intervalBetweenRequestsMax")));
            RandomIntervalGenerator deadline = new RandomIntervalGenerator(
                    Integer.valueOf((String)prodConfig.get("deadlineMin")),
                    Integer.valueOf((String)prodConfig.get("deadlineMax")));
            producers.add(new RandomRequestProducer(scheduler, streams,
                    Integer.valueOf((String)prodConfig.get("numOfRequests")),
                    interval, deadline));
        }

        processors = new ArrayList<>();
        List procList = (List)config.get("processors");
        for (int i = 0; i < procList.size(); i++)
        {
            Map procConfig = (Map)procList.get(i);
            processors.add(new RandomRequestProcessor(scheduler, clock,
                    Double.valueOf((String)procConfig.get("percendOfRequestsToMiss"))));
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        String configFileName = "randomRunner.yaml";
        String resultsFileName = "results.csv";
        if (args.length > 1)
            configFileName = args[1];
        if (args.length > 2)
            resultsFileName = args[2];

        RandomRunner runner = new RandomRunner(configFileName, resultsFileName);
        runner.run();
    }

    private void run() throws IOException
    {
        loadConfig();

        processors.forEach(proc -> proc.start());
        producers.forEach(prod -> prod.start());

        Timer consoleTimer = startConsoleTimer();

        producers.forEach(producer ->
        {
            try
            {
                producer.join();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });
        processors.forEach(processor ->
        {
            try
            {
                processor.interrupt();
                processor.join();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });
        consoleTimer.cancel();
        saveResults();
        terminal.exitPrivateMode();
        terminal.close();
    }

    private Timer startConsoleTimer() throws IOException
    {
        DefaultTerminalFactory factory = new DefaultTerminalFactory(System.out, System.in, Charset.defaultCharset());
        terminal = factory.createTerminalEmulator();
        terminal.enterPrivateMode();
        terminal.clearScreen();
        terminal.setCursorVisible(false);
        terminalText = terminal.newTextGraphics();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                updateConsole();
            }
        }, 0, consoleOutputInterval);
        return timer;
    }

    private void updateConsole()
    {
        try
        {
            terminal.clearScreen();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        terminalText.putString(0, 0, "Streams:");
        int line = 1;
        for (int i = 0; i < streams.size(); i++)
            terminalText.putString(2, line++, String.format("%d: %s", i, streams.get(i)));

        terminalText.putString(0, ++line, "Producers:");
        line++;
        for (int i = 0; i < producers.size(); i++)
            terminalText.putString(2, line++, String.format("%d: %s", i, producers.get(i)));

        terminalText.putString(0, ++line, "Processors:");
        line++;
        for (int i = 0; i < processors.size(); i++)
            terminalText.putString(2, line++, String.format("%d: %s", i, processors.get(i)));

        try
        {
            terminal.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void saveResults() throws IOException
    {
        File f = new File(resultsFileName);
        if (f.exists())
            f.delete();


        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(resultsFileName))))
        {
            writer.write("stream;total_requests;missed;met;is_blacklisted;dynamic_fault_count\r\n");
            for (Stream stream : streams)
            {
                writer.write(String.format("\"%s\";%d;%d;%d;%s;%d\r\n", stream.getName(), stream.getMetCount() +
                    stream.getMissedCount(), stream.getMissedCount(), stream.getMetCount(),
                    (stream.isBlacklisted() ? "yes" : "no"), stream.getDynamicFaultCount()));
            }
        }
    }

}
