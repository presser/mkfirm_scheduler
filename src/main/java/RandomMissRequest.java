/**
 * A request that misses the deadline randomly
 */
public class RandomMissRequest implements Request
{
    private Stream stream;
    private long deadline;

    public RandomMissRequest(Stream stream, long deadline)
    {
        this.stream = stream;
        this.deadline = deadline;
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

    }
}
