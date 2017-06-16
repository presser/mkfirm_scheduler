import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * A simple request processor thread that randomly misses deadlines
 */
public class RandomRequestProcessor extends Thread
{
    private static Logger logger = LogManager.getLogger(RandomRequestProcessor.class);

    private DBPScheduler scheduler;
    private Clock clock;
    private double percentOfRequestsToMiss;
    private Random random;
    private int processed;
    private int misses;
    private boolean waiting;
    private Request currentRequest;

    public RandomRequestProcessor(DBPScheduler scheduler, Clock clock, double percentOfRequestsToMiss)
    {
        this.scheduler = scheduler;
        this.random = new Random();
        this.clock = clock;
        this.percentOfRequestsToMiss = percentOfRequestsToMiss / 100.0;
    }

    @Override
    public void run()
    {
        processed = 0;
        misses = 0;
        while (true)
        {
            currentRequest = null;
            waiting = true;
            try
            {
                currentRequest = scheduler.getNextRequestToExecute();
            }
            catch (InterruptedException e)
            {
                logger.error("Processor thread was interrupted", e);
                break;
            }

            waiting = false;
            try
            {
                execute(currentRequest);
            }
            catch (InterruptedException e)
            {
                logger.error("Processor thread was interrupted", e);
                break;
            }
            catch (Throwable t)
            {
                logger.error("Error processing request", t);
            }
            scheduler.requestFinished(currentRequest);
            processed += 1;
        }
    }

    private void execute(Request request) throws InterruptedException
    {
        long remaining = request.getDeadline() - clock.getTick();
        if (remaining < 0)
        {
            misses += 1;
            return;
        }

        double shouldMiss = random.nextDouble();

        long delay;
        if (shouldMiss <= percentOfRequestsToMiss)
        {
            delay = remaining + (long) Math.floor((remaining / 2) * random.nextDouble());
            misses += 1;
        }
        else
            delay = (long)Math.floor(remaining * random.nextDouble());
        Thread.sleep(delay);
    }

    @Override
    public String toString()
    {
        return String.format("Processed: %d, missed: %d (%s)", processed, misses,
                (waiting ? "waiting" : "executing " + currentRequest.getStream().getName()));
    }
}
