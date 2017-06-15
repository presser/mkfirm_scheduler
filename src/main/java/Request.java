/**
 * A request from a (m,k)-firm Stream
 */
public interface Request
{
    long getDeadline();
    Stream getStream();
}
