/**
 * A Dummy request used in unit tests
 */
public class DummyRequest implements Request
{
    private Stream stream;
    private long deadline;
    private String name;

    public DummyRequest(String name, Stream stream, long deadline)
    {
        this.name = name;
        this.stream = stream;
        this.deadline = deadline;
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
    public String toString()
    {
        return name + "(" + stream.toString() + ")";
    }
}
