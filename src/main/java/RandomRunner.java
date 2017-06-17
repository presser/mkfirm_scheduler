import com.esotericsoftware.yamlbeans.YamlReader;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Runs a set of random requests
 */
@SuppressWarnings("unchecked")
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
    private int iterSpec, iterSpecCount, iter, iterCount;

    private SortedMap<String, String> iterPathValues;

    public RandomRunner(String configFileName, String resultsFileName)
    {
        this.configFileName = configFileName;
        this.resultsFileName = resultsFileName;
        this.clock = new CurrentTimeMillisClock();
        this.scheduler = new DBPScheduler(clock);
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        String configFileName = "randomRunner.yaml";
        String resultsFileName = "results.csv";
        if (args.length >= 1)
            configFileName = args[0];
        if (args.length >= 2)
            resultsFileName = args[1];

        RandomRunner runner = new RandomRunner(configFileName, resultsFileName);
        runner.run();
    }

    private Map loadConfig() throws IOException
    {
        YamlReader yaml = new YamlReader(new FileReader(configFileName));
        return (Map) yaml.read();
    }

    private List getIterationSpecs(Map config)
    {
        return (List) config.getOrDefault("iterations", null);
    }

    private int getIterationCount(Map config, int iterationSpec)
    {
        String value = (String) ((Map) getIterationSpecs(config).get(iterationSpec)).get("count");
        return Integer.parseInt(value);
    }

    @SuppressWarnings("ConstantConditions") // we want NoSuchElementException to be thrown
    private synchronized void applyConfig(Map cfg, Map iterSpec, int iter)
    {
        consoleOutputInterval = Integer.valueOf((String) cfg.get("consoleOutputInterval"));

        streams = new ArrayList<>();
        List streamList = (List) cfg.get("streams");
        for (int i = 0; i < streamList.size(); i++)
        {
            String name = (String)readPath(cfg, "streams", i, "name").orElse(null);
            streams.add(new Stream(name, readInt(cfg, iterSpec, iter, "streams", i, "m").get(),
                    readInt(cfg, iterSpec, iter, "streams", i, "k").get(),
                    readInt(cfg, iterSpec, iter, "streams", i, "dynamicFaultChances").get()));
        }

        producers = new ArrayList<>();
        List prodList = (List) cfg.get("producers");
        for (int i = 0; i < prodList.size(); i++)
        {
            RandomIntervalGenerator interval = new RandomIntervalGenerator(
                    readInt(cfg, iterSpec, iter, "producers", i, "intervalBetweenRequestsMin").get(),
                    readInt(cfg, iterSpec, iter, "producers", i, "intervalBetweenRequestsMax").get());
            RandomIntervalGenerator deadline = new RandomIntervalGenerator(
                    readInt(cfg, iterSpec, iter, "producers", i, "deadlineMin").get(),
                    readInt(cfg, iterSpec, iter, "producers", i, "deadlineMax").get());
            producers.add(new RandomRequestProducer(scheduler, streams,
                    readInt(cfg, iterSpec, iter, "producers", i, "streamIndex").get(),
                    readInt(cfg, iterSpec, iter, "producers", i, "numOfRequests").get(),
                    interval, deadline));
        }

        processors = new ArrayList<>();
        List procList = (List) cfg.get("processors");
        for (int i = 0; i < procList.size(); i++)
        {
            RandomRequestProcessor p = new RandomRequestProcessor(scheduler, clock);
            processors.add(p);
            double value = readDouble(cfg, iterSpec, iter,
                    "processors", i, "percentOfRequestsToMiss").orElse(-100.0)/100.0;
            p.setPercentOfRequestsToMiss(value);
            int iValue = readInt(cfg, iterSpec, iter, "processors", i, "fixedDelay").orElse(0);
            p.setFixedProcessDelay(iValue);
        }

        updateIterPathsValues(cfg, iterSpec, iter);
    }

    private void updateIterPathsValues(Map cfg, Map iterSpec, int iter)
    {
        for (String iterPath : iterPathValues.keySet())
        {
            String[] strings = iterPath.split("/");
            Object pathArray[] = new Object[strings.length];
            for (int i = 0; i < strings.length; i++)
            {
                if (Pattern.matches("\\d+", strings[i]))
                    pathArray[i] = Integer.parseInt(strings[i]);
                else
                    pathArray[i] = strings[i];
            }
            readDouble(cfg, iterSpec, iter, pathArray).ifPresent(v -> iterPathValues.put(iterPath, String.valueOf(v)));
        }
    }

    private void initIterPaths(List<Map> specs)
    {
        iterPathValues = new TreeMap<>();
        specs.forEach(s -> initIterPathsVisit(s, ""));
        iterPathValues.remove("count"); //configuration, not iterable
    }

    private void initIterPathsVisit(Object node, String path)
    {
        if (node instanceof Map)
        {
            for (Object k : ((Map)node).keySet())
                initIterPathsVisit(((Map)node).get(k), path + "/" + k.toString());
        }
        else if (node instanceof List)
        {
            for (int i = 0; i < ((List) node).size(); i++)
                initIterPathsVisit(((List) node).get(i), path + "/" + i);
        } else
        {
            if (path.startsWith("/"))
                path = path.substring(1, path.length());
            iterPathValues.put(path, "<<UNKNOWN>>");
        }
    }

    private Optional<Object> readPath(Map config, Object... path)
    {
        Object node = config;
        for (Object element : path)
        {
            if (node == null)
                return Optional.empty();
            if (element instanceof Integer)
            {
                List list = (List) node;
                if (list.size() <= (Integer) element)
                    return Optional.empty();
                node = list.get((Integer) element);
            } else if (element instanceof String)
            {
                if (!((Map) node).containsKey(element))
                    return Optional.empty();
                node = ((Map) node).get(element);
            }
        }
        return Optional.of(node);
    }

    private Object readRequiredPath(Map config, Object... path) {
        return readPath(config, path)
                .orElseThrow(() -> new NoSuchElementException("Path " + pathToString(path) + " not found"));
    }

    private String pathToString(Object... path)
    {
        StringBuilder b = new StringBuilder();
        for (Object e : path)
            b.append("/").append(e.toString());
        return b.toString();
    }

    private Optional<Integer> readInt(Map config, Map spec, int iteration, Object... path)
    {
        Optional<Object> opt = readPath(config, path);
        if (!opt.isPresent()) return Optional.empty();
        String value = (String) opt.get();
        int step = Integer.parseInt((String) readPath(spec, path).orElse("0"));
        return Optional.of(Integer.parseInt(value) + iteration * step) ;
    }

    private Optional<Double> readDouble(Map config, Map spec, int iteration, Object... path)
    {
        Optional<Object> opt = readPath(config, path);
        if (!opt.isPresent()) return Optional.empty();
        String value = (String) opt.get();
        double step = Double.parseDouble((String) readPath(spec, path).orElse("0"));
        return Optional.of(Double.parseDouble(value) + iteration * step);
    }

    private void run() throws IOException
    {
        Timer consoleTimer = null;

        Map config = loadConfig();
        List<Map> specs = new ArrayList<>();
        specs.add(null);
        ((List) config.getOrDefault("iterations", Collections.emptyList())).forEach(o -> specs.add((Map) o));
        iterSpecCount = specs.size()-1;
        initIterPaths(specs.subList(1, specs.size()));
        initResult();
        for (int i = 0; i < specs.size(); i++)
        {
            iterSpec = i;
            int count = specs.get(i) == null ? 1 : Integer.parseInt((String) specs.get(i).get("count"));
            iterCount = count;
            /* All iteration specs share the 0-th iteration that is done under the hard-coded 0-th spec */
            for (int j = 1; j <= count; j++)
            {
                iter = j;
                applyConfig(config, specs.get(i), j);
                // only start consoleTimer after first aplyConfig()
                if (consoleTimer == null) consoleTimer = startConsoleTimer();
                runIteration();
            }
        }

        assert consoleTimer != null;
        consoleTimer.cancel();
        terminal.exitPrivateMode();
        terminal.close();
    }

    private void runIteration() throws IOException
    {
        processors.forEach(proc -> proc.start());
        producers.forEach(prod -> prod.start());

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
        saveResults();
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

    private synchronized void updateConsole()
    {
        try
        {
            terminal.clearScreen();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        int line = 0;
        terminalText.putString(0, line++, String.format("Iter. spec: %d/%d  Iter. count: %d/%d",
                iterSpec, iterSpecCount, iter, iterCount));
        terminalText.putString(0, line++, "Streams:");

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

        line++;
        terminalText.putString(0, line++, "Iterated Values:");
        for (Map.Entry<String, String> e : iterPathValues.entrySet())
            terminalText.putString(0, line++, e.getKey() + ": " + e.getValue());

        try
        {
            terminal.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void initResult() throws IOException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(resultsFileName))))
        {
            writer.write("stream;total_requests;missed;met;is_blacklisted;dynamic_fault_count;iter_spec;iter");
            for (String key : iterPathValues.keySet())
                writer.write(";" + key);
            writer.write("\r\n");
        }

    }

    private void saveResults() throws IOException
    {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(resultsFileName, true))))
        {
            for (Stream stream : streams)
            {
                writer.write(String.format("\"%s\";%d;%d;%d;%s;%d;%d;%d", stream.getName(), stream.getMetCount() +
                                stream.getMissedCount(), stream.getMissedCount(), stream.getMetCount(),
                        (stream.isBlacklisted() ? "yes" : "no"), stream.getDynamicFaultCount(),
                        iterSpec, iter));
                for (String key : iterPathValues.keySet())
                    writer.write(";" + iterPathValues.get(key));
                writer.write("\r\n");
            }
        }
    }

}
