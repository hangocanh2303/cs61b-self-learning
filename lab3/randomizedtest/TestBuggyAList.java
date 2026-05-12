package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    // YOUR TESTS HERE
    @Test
    public void testThreeAddThreeRemove() {
         AListNoResizing<Integer>  aListNoResizing = new AListNoResizing<>();
         BuggyAList<Integer> buggyAList = new BuggyAList<>();

         for (int i = 4;  i < 7; i++) {
             aListNoResizing.addLast(i);
             buggyAList.addLast(i);
         }
         assertEquals(aListNoResizing.getLast(), buggyAList.getLast());
         assertEquals(aListNoResizing.getLast(), buggyAList.getLast());
         assertEquals(aListNoResizing.getLast(), buggyAList.getLast());
    }

    @Test
    public void randomizedTest() {
        AListNoResizing<Integer> L = new AListNoResizing<>();

        int N = 500;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 2);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                System.out.println("addLast(" + randVal + ")");
            } else if (operationNumber == 1) {
                // size
                int size = L.size();
                System.out.println("size: " + size);
            }
        }
    }
}
