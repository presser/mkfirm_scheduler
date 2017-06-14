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

    /**
     * Gets the distance to a dinamic fault
     * @return how many activations can be missed before the
     * stream goes faulty. Negative if already in dynamic fault
     */
    public int getDistance()
    {
        //first count how many met requests we have
        int met = 0;
        for (int i = 0; i < k; i++)
            met += history[i];

        //Returns negative if in dynamic fault
        if (met < m)
            return met - m;

        //walk back history array counting how many
        //requests can we miss before going dynamic
        //fault. Each met request we pass decreases the
        //met-request-count, until we have m met requests
        //in the history.
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

    /**
     * Adds a missed request to the history
     * @return missed requests count
     */
    public int addMissedRequest()
    {
        addRequestToHistory(0);
        return missedCount.incrementAndGet();
    }

    /**
     * Adds a met request to the history
     * @return met request count
     */
    public int addMetRequest()
    {
        addRequestToHistory(1);
        return metCount.incrementAndGet();
    }

    /**
     * Gets the met requests count
     * @return met requests count
     */
    public int getMetCount()
    {
        return metCount.get();
    }

    /**
     * Gets the missed requests count
     * @return missed requests count
     */
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
