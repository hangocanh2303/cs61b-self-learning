package deque;

import java.util.Comparator;

public class ArrayDequeIntComparator implements Comparator<Integer> {
    @Override
    public int compare(Integer o1, Integer o2) {
        return o1 - o2;
    }
}
