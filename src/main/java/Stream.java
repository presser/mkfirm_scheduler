import java.util.concurrent.atomic.AtomicInteger;

/**
 * A stream of requests that follow a (m,k)-firm model
 */
public class Stream
{
    private int m;
    private int k;
    private int c;
    private int[] history;

    private int metCount;
    private int missedCount;
    private String name;

    private int dynamicFaultsChances;
    private int dynamicFaultCount;

    public Stream(int m, int k, int dynamicFaultsChances)
    {
        this(null, m, k, dynamicFaultsChances);
    }

    public Stream(String name, int m, int k, int dynamicFaultsChances)
    {
        this.name = name != null ? name : String.format("[%d,%d]", m, k);
        this.m = m;
        this.k = k;
        this.history = new int[k];
        this.dynamicFaultsChances = dynamicFaultsChances;
        this.metCount = 0;
        this.missedCount = 0;
        this.dynamicFaultCount = 0;
    }

    /**
     * Gets the distance to a dinamic fault
     * @return how many activations can be missed before the
     * stream goes faulty. Negative if already in dynamic fault
     */
    public synchronized int getDistance()
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
    public synchronized int addMissedRequest()
    {
        addRequestToHistory(0);
        ++missedCount;
        if (isInDynamicFault())
        {
            ++c;
            ++dynamicFaultCount;
        }
        return missedCount;
    }

    private boolean isInDynamicFault()
    {
        return getDistance() < 0;
    }

    /**
     * Adds a met request to the history
     * @return met request count
     */
    public synchronized int addMetRequest()
    {
        addRequestToHistory(1);
        if (!isInDynamicFault())
            c = 0;
        return ++metCount;
    }

    /**
     * Gets the met requests count
     * @return met requests count
     */
    public int getMetCount()
    {
        return metCount;
    }

    /**
     * Gets the missed requests count
     * @return missed requests count
     */
    public int getMissedCount()
    {
        return missedCount;
    }

    /**
     * Each time the stream enters in dynamic fault, each new fault is counted in this variable.
     * Once the dynamic fault condition ceases, the counter is reset to zero.
     * The maximum allowed value for this counter is getDynamicFaultsChances().
     */
    public int getChancesUsed() {
        return c;
    }
    public int getDynamicFaultsChances()
    {
        return dynamicFaultsChances;
    }

    public synchronized boolean isBlacklisted()
    {
        return getChancesUsed() >= getDynamicFaultsChances();
    }

    public synchronized void setDynamicFaultsChances(int dynamicFaultsChances)
    {
        this.dynamicFaultsChances = dynamicFaultsChances;
    }

    private void addRequestToHistory(int status)
    {
        for (int i = (k-2); i >= 0; i--)
            history[i+1] = history[i];
        history[0] = status;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(",d:");
        sb.append(getDistance());
        sb.append("[");
        for (int i = 0; i < history.length; i++)
        {
            sb.append(history[i]);
            sb.append(",");
        }
        sb.replace(sb.length()-1, sb.length(), "]");
        sb.append("missed:");
        sb.append(missedCount);
        sb.append(",met:");
        sb.append(metCount);

        return sb.toString();
    }

    public String getName()
    {
        return name;
    }

    /**
     * Gets the number of activations this stream has had in dynamic fault
     * @return
     */
    public int getDynamicFaultCount()
    {
        return dynamicFaultCount;
    }
}
