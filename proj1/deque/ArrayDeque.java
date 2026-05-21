package deque;

/**
 * invariants:
 * nextFirst always the position immediately before the first element.
 * nextLast always the position immediately after the last element.
 *
 */
public class ArrayDeque<T> implements Deque<T> {

    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;

    public ArrayDeque() {
        items = (T[]) new Object[8];
        nextFirst = 0;
        nextLast = 1;
        size = 0;
    }

    @Override
    public void addFirst(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[nextFirst] = item;
        nextFirst = (nextFirst - 1 + items.length) % items.length;
        size += 1;
    }

    @Override
    public void addLast(T item) {
        if (size == items.length) {
            resize(size * 2);
        }
        items[nextLast] = item;
        nextLast = (nextLast + 1) % items.length;
        size += 1;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        for (int i = 0; i < size; i += 1) {
            System.out.print(get(i) + " ");
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if (size > 0) {
            if ((size <= items.length / 4) && (items.length >= 16)) {
                resize(items.length / 2);
            }
            T firstItem = get(0);
            items[(nextFirst + 1) % items.length] = null;
            nextFirst = (nextFirst + 1) % items.length;
            size -= 1;
            return firstItem;
        }
        return null;
    }

    @Override
    public T removeLast() {
        if (size > 0) {
            if ((size <= items.length / 4) && (items.length >= 16)) {
                resize(items.length / 2);
            }
            T lastItem = get(size - 1);
            items[(nextLast - 1 + items.length) % items.length] = null;
            nextLast = (nextLast - 1 + items.length) % items.length;
            size -= 1;
            return lastItem;
        }
        return null;
    }

    @Override
    public T get(int index) {
        if (index + 1 > size || index < 0) {
            return null;
        }
        int resultIndex = (nextFirst + 1 + index) % items.length;
        return items[resultIndex];
    }

    @Override
    public T getRecursive(int index) {
        return null;
    }

    private void resize(int capacity) {
        T[] a = (T[]) new Object[capacity];
        for (int i = 0; i < size; i++) {
            a[i] = get(i);
        }
        items = a;
        nextFirst = capacity - 1;
        nextLast = size;
    }

//    private void resizeUpWithArrayCopy(int capacity) {
//        T[] a = (T[]) new Object[capacity];
//
//        System.arraycopy(items, (nextFirst + 1) % items.length,
//                a, capacity - (size - nextFirst),
//                size - nextFirst);
//        System.arraycopy(items, 0,
//                a, 0,
//                (nextLast + items.length) % items.length);
//        items = a;
//        nextFirst = (capacity - (size - nextFirst) - 1) % items.length;
//    }
//
//    private void resizeUp(int capacity) {
//        // step 1: create new array with items size is capacity
//        T[] a = (T[]) new Object[capacity];
//        // Step 2: getNewArrayNextFirst
//        int newNextFirst = getNewArrayNextFirst(capacity);
//        // Step 3: getNewArrayNextLast
//        int newNextLast = getNewArrayNextLast();
//        // Step 4: Copy data from old array to new array
//        moveOldDataToNewArray(a, newNextFirst, newNextLast);
//        // Step 5: assign old items to new array
//        items = a;
//        // Step 6: set new value for nextFirst and nextLast
//        nextFirst = newNextFirst;
//        nextLast = newNextLast;
//    }
//
//    /* Return new nextFirst value if resize success */
//    private int getNewArrayNextFirst(int capacity) {
//        return nextFirst + (capacity - size);
//    }
//
//    private int getNewArrayNextLast() {
//        return nextLast;
//    }
//
//    private void moveOldDataToNewArray(T[] newItems, int newNextFirst, int newNextLast) {
//        int index = 0;
//        for (int i = newNextFirst + 1; i < newItems.length; i += 1) {
//            newItems[i] = get(index);
//            index += 1;
//        }
//
//        for (int j = 0; j <= newNextLast; j += 1) {
//            newItems[j] = get(index);
//            index += 1;
//        }
//    }
}
