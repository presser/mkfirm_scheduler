/**
 * Something went wrong with the scheduler
 */
public class DBPSchedulerException extends RuntimeException
{
    public DBPSchedulerException(String message)
    {
        super(message);
    }

    public DBPSchedulerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public DBPSchedulerException(Throwable cause)
    {
        super(cause);
    }
}
