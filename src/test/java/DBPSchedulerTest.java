import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by danie on 14/06/2017.
 */
public class DBPSchedulerTest
{
    @Test
    public void shouldOrderRequests()
    {
        Stream streamA = new Stream(2, 3);
        Stream streamB = new Stream(2, 5);

        DBPScheduler scheduler = new DBPScheduler();


    }
}