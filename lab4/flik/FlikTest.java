package flik;

import org.junit.Test;
import static org.junit.Assert.*;


public class FlikTest {
    @Test
    public void testIsSameNumber() {
        int a = 128;
        int b = 128;
//        boolean isSameNumber = Flik.isSameNumber(a, b);
        assertTrue(Flik.isSameNumber(a, b));
    }
}
