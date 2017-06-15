import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by daniel on 14/06/2017.
 */
public class DBPSchedulerTest
{
    @Test
    public void shouldOrderMetRequestsOneAtATime() throws InterruptedException
    {
        Stream streamA = new Stream(2, 3, 2+3);
        Stream streamB = new Stream(1, 3, 3+1);

        ManualTickClock clock = new ManualTickClock(0);

        DBPScheduler scheduler = new DBPScheduler(clock);

        Request reqA1 = new DummyRequest("reqA1", streamA, 100);
        Request reqA2 = new DummyRequest("reqA2", streamA, 150);
        Request reqA3 = new DummyRequest("reqA3", streamA, 200);

        Request reqB1 = new DummyRequest("reqB1", streamB, 110);
        Request reqB2 = new DummyRequest("reqB2", streamB, 160);
        Request reqB3 = new DummyRequest("reqB3", streamB, 180);

        scheduler.addRequest(reqA1);
        scheduler.addRequest(reqA2);
        scheduler.addRequest(reqA3);
        scheduler.addRequest(reqB1);
        scheduler.addRequest(reqB2);
        scheduler.addRequest(reqB3);

        //A=[0,0,0];d=-2
        //B=[0,0,0];d=-1
        //streamA has smaller distance
        assertEquals(reqA1, scheduler.getNextRequestToExecute());

        //A=[1,0,0];d=-1
        //B=[0,0,0];d=-1
        scheduler.requestFinished(reqA1);

        //reqB1 has smaller deadline
        assertEquals(reqB1, scheduler.getNextRequestToExecute());

        //A=[1,0,0];d=-1
        //B=[1,0,0];d=2
        scheduler.requestFinished(reqB1);

        //reqA2 has smaller distance
        assertEquals(reqA2, scheduler.getNextRequestToExecute());

        //A=[1,1,0];d=1
        //B=[1,0,0];d=2
        scheduler.requestFinished(reqA2);

        //reqA3 has smaller distance
        assertEquals(reqA3, scheduler.getNextRequestToExecute());

        //A=[1,1,1];d=1
        //B=[1,0,0];d=2
        scheduler.requestFinished(reqA3);

        //reqB2 has smaller deadline
        assertEquals(reqB2, scheduler.getNextRequestToExecute());

        //A=[1,1,1];d=1
        //B=[1,1,0];d=2
        scheduler.requestFinished(reqB2);

        //reqB3 is the last request
        assertEquals(reqB3, scheduler.getNextRequestToExecute());
    }

    @Test
    public void shouldOrderRequestsByDeadlline() throws InterruptedException
    {
        Stream streamA = new Stream(2, 3, 2+3);
        Stream streamB = new Stream(1, 3, 3+1);

        ManualTickClock clock = new ManualTickClock(0);

        DBPScheduler scheduler = new DBPScheduler(clock);

        Request reqA1 = new DummyRequest("reqA1", streamA, 100);
        Request reqA2 = new DummyRequest("reqA2", streamA, 150);
        Request reqA3 = new DummyRequest("reqA3", streamA, 200);

        Request reqB1 = new DummyRequest("reqB1", streamB, 110);
        Request reqB2 = new DummyRequest("reqB2", streamB, 160);
        Request reqB3 = new DummyRequest("reqB3", streamB, 180);

        scheduler.addRequest(reqA1);
        scheduler.addRequest(reqA2);
        scheduler.addRequest(reqA3);
        scheduler.addRequest(reqB1);
        scheduler.addRequest(reqB2);
        scheduler.addRequest(reqB3);

        //A=[0,0,0];d=-2
        //B=[0,0,0];d=-1
        clock.setTick(10);

        assertEquals(reqA1, scheduler.getNextRequestToExecute());

        //A=[1,0,0];d=-1
        //B=[0,0,0];d=-1
        scheduler.requestFinished(reqA1);

        clock.setTick(100);

        assertEquals(reqB1, scheduler.getNextRequestToExecute());

        //A=[1,0,0];d=-1
        //B=[1,0,0];d=0
        scheduler.requestFinished(reqB1);

        //A=[0,0,0];d=-2
        //B=[1,0,0];d=0
        clock.setTick(151);
        assertEquals(reqA3, scheduler.getNextRequestToExecute());


        //A=[1,0,0];d=-1
        //B=[1,0,0];d=0
        scheduler.requestFinished(reqA3);

        assertEquals(reqB2, scheduler.getNextRequestToExecute());
        scheduler.requestFinished(reqB2);
        assertEquals(reqB3, scheduler.getNextRequestToExecute());
        assertTrue(scheduler.isEmpty());
    }

    @Test
    public void testDynamicFailurePenalty() throws InterruptedException
    {
        Stream streamA = new Stream(2, 3, 2);
        Stream streamB = new Stream(1, 3, 2);

        ManualTickClock clock = new ManualTickClock(0);

        DBPScheduler scheduler = new DBPScheduler(clock);

        Request reqA1 = new DummyRequest("reqA1", streamA, 100);
        Request reqA2 = new DummyRequest("reqA2", streamA, 105);
        Request reqA3 = new DummyRequest("reqA3", streamA, 200);

        Request reqB1 = new DummyRequest("reqB1", streamB, 110);
        Request reqB2 = new DummyRequest("reqB2", streamB, 120);
        Request reqB3 = new DummyRequest("reqB3", streamB, 130);

        scheduler.addRequest(reqA1);
        scheduler.addRequest(reqA2);
        scheduler.addRequest(reqA3);
        scheduler.addRequest(reqB1);
        scheduler.addRequest(reqB2);
        scheduler.addRequest(reqB3);

        //A=[0,0,0];d=-2
        //B=[0,0,0];d=-1
        assertEquals(reqA1, scheduler.getNextRequestToExecute());
        //A=[1,0,0];d=-1
        //B=[0,0,0];d=-1
        scheduler.requestFinished(reqA1);

        //A=[1,0,0];d=-1
        //B=[0,0,0];d=-1
        assertEquals(reqA2, scheduler.getNextRequestToExecute());
        //A=[1,1,0];d=0
        //B=[0,0,0];d=-1
        scheduler.requestFinished(reqA2);
        clock.setTick(100);

        //A=[1,1,0];d=0
        //B=[0,0,0];d=-1
        assertEquals(reqB1, scheduler.getNextRequestToExecute());
        clock.setTick(111);
        //A=[1,1,0];d=0
        //B=[0,0,0];d=-1,c=1
        scheduler.requestFinished(reqB1);

        //A=[1,1,0];d=0
        //B=[0,0,0];d=-1,c=1
        assertEquals(reqB2, scheduler.getNextRequestToExecute());
        clock.setTick(121);
        //A=[1,1,0];d=0
        //B=[0,0,0];d=-1,c=2
        scheduler.requestFinished(reqB2);

        //A=[1,1,0];d=0
        //B=[0,0,0];d=-1,c=2
        assertEquals(reqA3, scheduler.getNextRequestToExecute());
        //A=[1,1,1];d=0
        //B=[0,0,0];d=-1,c=2
        scheduler.requestFinished(reqA3);

        assertTrue(scheduler.isEmpty());


        Request reqB4 = new DummyRequest("reqB3", streamB, 140);
        boolean caught = false;
        try  {
            scheduler.addRequest(reqB4);
        } catch (BlacklistedStreamExcception e) {
            caught = true;
        }
        assertTrue(caught);
    }
}