/**
 * Can't add request as the stream is blacklisted to avoid starvation
 */
public class BlacklistedStreamExcception extends DBPSchedulerException
{
    public BlacklistedStreamExcception() {
        super("Can't add request as the stream is blacklisted to avoid starvation");
    }
}
