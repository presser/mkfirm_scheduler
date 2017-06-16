import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.InterruptedNamingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Distance Based priority scheduler for (m,k)-firm streams of requests
 */
public class DBPScheduler
{
    private static Logger logger = LogManager.getLogger(DBPScheduler.class);
    private boolean needsReorder;
    private List<Request> requests;
    private Clock clock;

    public DBPScheduler(Clock clock)
    {
        requests = new ArrayList<>(100);
        needsReorder = false;
        this.clock = clock;
    }

    public Clock getClock()
    {
        return clock;
    }

    /**
     * Adds a request to be scheduled
     * @param request
     */
    public synchronized void addRequest(Request request)
    {
        if (request.getStream().isBlacklisted())
            throw new BlacklistedStreamExcception();
        requests.add(request);
        needsReorder = true;
        notifyAll();
    }

    private void reorderRequests()
    {
        requests.sort(new DBPRequestComparator());
    }

    /**
     * Gets the next request to be processed according to the order
     * defined by the scheduler
     * @return
     */
    public synchronized Request getNextRequestToExecute() throws InterruptedException
    {
        do
        {
            if (requests.size() == 0)
                wait();

            needsReorder |= removeMissedRequests();

            if (needsReorder)
            {
                reorderRequests();
                needsReorder = false;
            }

        } while (requests.size() == 0);

        Request result = requests.get(0);
        requests.remove(0);
        return result;
    }

    public synchronized boolean isEmpty()
    {
        return requests.isEmpty();
    }

    private boolean removeMissedRequests()
    {
        boolean change = requests.removeIf(request ->
        {
            boolean isMissed = request.getDeadline() < clock.getTick();
            if (isMissed)
                request.getStream().addMissedRequest();
            return isMissed;
        });
        change |= requests.removeIf(request -> {
            Stream stream = request.getStream();
            return request.getStream().isBlacklisted();
        });
        return change;
    }

    /**
     * Notifies the scheduler that a request has finished so that the
     * request's stream history can be updated
     * @param request
     */
    public synchronized void requestFinished(Request request)
    {
        needsReorder = true;
        if (request.getDeadline() < clock.getTick())
            request.getStream().addMissedRequest();
        else
            request.getStream().addMetRequest();
    }
}
