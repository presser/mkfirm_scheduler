import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Random;

/**
 * A random request producer, produces requests to random streams with randomly bounded
 * deadlines within randomly bounded intervals.
 */
public class RandomRequestProducer extends Thread
{
    private static Logger logger = LogManager.getLogger(RandomRequestProducer.class);

    private DBPScheduler scheduler;
    private List<Stream> streams;
    private int numOfRequests;
    private int streamIndex;
    private RandomIntervalGenerator intervalGenerator;
    private RandomIntervalGenerator deadlineGenerator;
    private Random random;
    private int produced;

    public RandomRequestProducer(DBPScheduler scheduler, List<Stream> streams,
                                 int streamIndex, int numOfRequests,
                                 RandomIntervalGenerator intervalGenerator,
                                 RandomIntervalGenerator deadlineGenerator)
    {
        this.scheduler = scheduler;
        this.streams = streams;
        this.streamIndex = streamIndex;
        this.intervalGenerator = intervalGenerator;
        this.deadlineGenerator = deadlineGenerator;
        this.numOfRequests = numOfRequests;
        random = new Random();
    }

    @Override
    public void run()
    {
        produced = 0;
        for (int i = 0; i < numOfRequests; i++)
        {
            Request request = produceRequest();
            scheduler.addRequest(request);
            produced += 1;
            try
            {
                waitInterval();
            } catch (InterruptedException e)
            {
                logger.error("Producer was interrupted", e);
                break;
            }
        }
    }

    private void waitInterval() throws InterruptedException
    {
        int interval = intervalGenerator.nextInterval();
        Thread.sleep(interval);
    }

    private Request produceRequest()
    {
        Stream stream = streamIndex >= 0 ? streams.get(streamIndex)
                : streams.get(random.nextInt(streams.size()));
        long deadline = scheduler.getClock().getTick() + deadlineGenerator.nextInterval();
        DummyRequest request = new DummyRequest("Dummy", stream, deadline);
        return request;
    }

    @Override
    public String toString()
    {
        return String.format("Produced: %d", produced);
    }
}
