/**
 * A clock based on System.currentTimeMillis
 */
public class CurrentTimeMillisClock implements Clock
{
    private long baseTick = 0;

    @Override
    public long getTick()
    {
        return System.currentTimeMillis() + baseTick;
    }

    @Override
    public long setTick(long value)
    {
        long oldValue = baseTick;
        baseTick = value;
        return oldValue;

    }
}
