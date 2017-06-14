import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * A request that misses the deadline randomly
 */
public class RandomMissRequest implements Request
{
    private Stream stream;
    private long deadline;
    private Random random;
    private double missPercent;

    private static Logger logger = LogManager.getLogger(RandomMissRequest.class);

    public RandomMissRequest(Stream stream, long deadline, double missPercent)
    {
        this.stream = stream;
        this.deadline = deadline;
        this.random = new Random();
        this.missPercent = missPercent;
    }

    @Override
    public long getDeadline()
    {
        return deadline;
    }

    @Override
    public Stream getStream()
    {
        return stream;
    }

    @Override
    public void run()
    {
        double shouldMiss = random.nextDouble();
        long remaining = deadline - System.currentTimeMillis();

        //A random percent between 30 and 50%
        double percent = ((30 + random.nextInt(20)) / 100.0);

        long delay;
        if (shouldMiss <= missPercent)
        {
            //if should miss, calculate the delay as the remaining time until deadline plus
            //30-50%~ish of the deadline
            delay = remaining + (long)Math.floor(deadline * percent);
        }
        else
        {
            //if not, calculate the delay as 30-50%~ish of the remaining time until deadline
            delay = (long)(remaining * percent);
        }

        try
        {
            Thread.sleep(delay);
        } catch (InterruptedException e)
        {
            logger.error(e);
        }
    }
}
