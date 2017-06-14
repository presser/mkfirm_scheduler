import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A Distance Based priority scheduler for (m,k)-firm streams of requests
 */
public class DBPScheduler
{
    private static Logger logger = LogManager.getLogger(DBPScheduler.class);

    private List<Request> requests;

    public DBPScheduler()
    {
        requests = new ArrayList<>(100);
    }

    public synchronized void addRequest(Request request)
    {
        requests.add(request);
        reorderRequests();
        notifyAll();
    }

    private void reorderRequests()
    {
        requests.sort(new DBPRequestComparator());
    }

    public synchronized Request getNextRequestToExecute()
    {
        if (requests.size() == 0)
        {
            try
            {
                wait();
            } catch (InterruptedException e)
            {
                logger.info(e);
            }
        }

        Request result = requests.get(0);
        requests.remove(0);
        return result;
    }

    public synchronized void requestFinished(Request request)
    {
        if (System.currentTimeMillis() > request.getDeadline())
            request.getStream().addMissedRequest();
        else
            request.getStream().addMetRequest();
    }
}
