/**
 * A clock that is manually ticked
 */
public class ManualTickClock implements Clock
{
    private long tick;

    public ManualTickClock(long current)
    {
        this.tick = current;
    }

    @Override
    public long getTick()
    {
        return tick;
    }

    @Override
    public long setTick(long value)
    {
        long oldTick = tick;
        tick = value;
        return oldTick;
    }
}
