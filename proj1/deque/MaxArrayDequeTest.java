package deque;

import net.sf.saxon.expr.Component;
import org.checkerframework.checker.units.qual.A;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class MaxArrayDequeTest {

    @Test
    public void testIntegerComparator() {
        ArrayDequeIntComparator arrayDequeIntComparator = new ArrayDequeIntComparator();
        MaxArrayDeque<Integer> maxArrayDeque = new MaxArrayDeque<>(arrayDequeIntComparator);
        maxArrayDeque.addFirst(1);
        maxArrayDeque.addFirst(2);
        maxArrayDeque.addLast(3);
        assertEquals(3, (int) maxArrayDeque.max());

        maxArrayDeque.removeFirst();
        maxArrayDeque.removeFirst();
        maxArrayDeque.removeFirst();

        assertNull(maxArrayDeque.max());
    }

    @Test
    public void testStringComparator() {
        ArrayDequeStringComparator arrayDequeStringComparator = new ArrayDequeStringComparator();
        MaxArrayDeque<String> maxArrayDeque = new MaxArrayDeque<>(arrayDequeStringComparator);
        maxArrayDeque.addFirst("i");
        maxArrayDeque.addFirst("can't");
        maxArrayDeque.addLast("fly");
        assertEquals("i",  maxArrayDeque.max());

        maxArrayDeque.removeFirst();
        maxArrayDeque.removeFirst();
        maxArrayDeque.removeFirst();

        assertNull(maxArrayDeque.max());
    }
}
