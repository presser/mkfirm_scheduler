import java.util.concurrent.atomic.AtomicInteger;

/**
 * A stream of requests that follow a (m,k)-firm model
 */
public class Stream
{
    private int m;
    private int k;
    private int[] history;

    private AtomicInteger metCount;
    private AtomicInteger missedCount;

    public Stream(int m, int k)
    {
        this.m = m;
        this.k = k;
        this.history = new int[k];
        this.metCount = new AtomicInteger(0);
        this.missedCount = new AtomicInteger(0);
    }

    public int getDistance()
    {
        int met = 0;
        for (int i = 0; i < k; i++)
            met += history[i];

        //Returns negative if in dynamic fault
        if (met < m)
            return met - m;

        int distance = 0;
        int pos = k - 1;
        while (met >= m)
        {
            if (history[pos--] == 1)
                met -= 1;
            if (met >= m)
                distance += 1;
        }

        return distance;
    }

    public int addMissedRequest()
    {
        addRequestToHistory(0);
        return missedCount.incrementAndGet();
    }

    public int addMetRequest()
    {
        addRequestToHistory(1);
        return metCount.incrementAndGet();
    }

    public int getMetCount()
    {
        return metCount.get();
    }

    public int getMissedCount()
    {
        return missedCount.get();
    }

    private void addRequestToHistory(int status)
    {
        for (int i = (k-2); i >= 0; i--)
            history[i+1] = history[i];
        history[0] = status;
    }
}
