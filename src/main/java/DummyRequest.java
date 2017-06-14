/**
 * A Dummy request used in unit tests
 */
public class DummyRequest implements Request
{
    private Stream stream;
    private long deadline;

    public DummyRequest(Stream stream, long remainingDeadline)
    {
        this.stream = stream;
        this.deadline = System.currentTimeMillis() + remainingDeadline;
    }

    @Override
    public long getDeadline()
    {
        return this.deadline;
    }

    @Override
    public Stream getStream()
    {
        return this.stream;
    }

    @Override
    public void run()
    {

    }
}
