import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by danie on 13/06/2017.
 */
public class StreamTest
{
    @Test
    public void shouldCalculateDistance23() {
        Stream stream = new Stream(2, 3);

        //[0,0,0]
        assertEquals(-2, stream.getDistance());

        //[1,0,0]
        stream.addMetRequest();
        assertEquals(-1, stream.getDistance());

        //[1,1,0]
        stream.addMetRequest();
        assertEquals(1, stream.getDistance());

        //[1,1,1]
        stream.addMetRequest();
        assertEquals(1, stream.getDistance());

        //[0,1,1]
        stream.addMissedRequest();
        assertEquals(0, stream.getDistance());

        //[1,0,1]
        stream.addMetRequest();
        assertEquals(0, stream.getDistance());

        //[0,1,0]
        stream.addMissedRequest();
        assertEquals(-1, stream.getDistance());

        //[1,0,1]
        stream.addMetRequest();
        assertEquals(0, stream.getDistance());

        //[1,1,1]
        stream.addMetRequest();
        assertEquals(1, stream.getDistance());
    }

    @Test
    public void shouldCalculateDistance36() {
        Stream stream = new Stream(3, 6);

        //[0,0,0,0,0,0]
        assertEquals(-3, stream.getDistance());

        //[1,0,0,0,0,0]
        stream.addMetRequest();
        assertEquals(-2, stream.getDistance());

        //[1,1,0,0,0,0]
        stream.addMetRequest();
        assertEquals(-1, stream.getDistance());


        //[1,1,1,0,0,0]
        stream.addMetRequest();
        assertEquals(3, stream.getDistance());

        //[0,1,1,1,0,0]
        stream.addMissedRequest();
        assertEquals(2, stream.getDistance());

        //[1,0,1,1,1,0]
        stream.addMetRequest();
        assertEquals(2, stream.getDistance());

        //[1,1,0,1,1,1]
        stream.addMetRequest();
        assertEquals(2, stream.getDistance());

        //[1,1,1,0,1,1]
        stream.addMetRequest();
        assertEquals(3, stream.getDistance());

        //[1,1,1,1,0,1]
        stream.addMetRequest();
        assertEquals(3, stream.getDistance());

        //[0,1,1,1,1,0]
        stream.addMissedRequest();
        assertEquals(2, stream.getDistance());

        //[0,0,1,1,1,1]
        stream.addMissedRequest();
        assertEquals(1, stream.getDistance());

        //[0,0,0,1,1,1]
        stream.addMissedRequest();
        assertEquals(0, stream.getDistance());

        //[0,0,0,0,1,1]
        stream.addMissedRequest();
        assertEquals(-1, stream.getDistance());

        //TODO: We should probably use something smarter here, since we'll need at least 2 met requests before
        //it becomes non faulty again (i.e. maybe somehow return -2 here?)
        //[1,0,0,0,0,1]
        stream.addMetRequest();
        assertEquals(-1, stream.getDistance());

        //[1,1,0,0,0,0]
        stream.addMetRequest();
        assertEquals(-1, stream.getDistance());

        //[1,1,1,0,0,0]
        stream.addMetRequest();
        assertEquals(3, stream.getDistance());
    }

}
