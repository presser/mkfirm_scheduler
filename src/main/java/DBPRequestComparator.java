import java.util.Comparator;

/**
 * A comparator for DBP Requests, used to order requests according to
 * a (m,k)-firm stream of requests model. First orders by distance to
 * dynamic fault, then by deadline
 */
public class DBPRequestComparator implements Comparator<Request>
{
    @Override
    public int compare(Request a, Request b)
    {
        if (a.getStream().getDistance() < b.getStream().getDistance())
            return -1;

        if (a.getStream().getDistance() > b.getStream().getDistance())
            return 1;

        if (a.getDeadline() < b.getDeadline())
            return -1;
        else
            return 1;
    }
}
