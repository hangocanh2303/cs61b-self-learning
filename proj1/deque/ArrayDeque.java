package deque;

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
        items[nextFirst] = item;
        nextFirst = (nextFirst - 1 + items.length) % items.length;
        size += 1;
    }

    @Override
    public void addLast(T item) {
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
        int resultIndex = (nextFirst + 1) % items.length;

        for (int i = index; i > 0; i -= 1) {
            resultIndex = (resultIndex + 1) % items.length;
        }
        return items[resultIndex];
    }

    @Override
    public T getRecursive(int index) {
        return null;
    }
}
