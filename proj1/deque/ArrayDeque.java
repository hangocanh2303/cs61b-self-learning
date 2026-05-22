package deque;

import java.util.Iterator;

/**
 * invariants:
 * nextFirst always the position immediately before the first element.
 * nextLast always the position immediately after the last element.
 *
 */
public class ArrayDeque<T> implements Deque<T>, Iterable<T> {

    private T[] items;
    private int size;
    private int nextFirst;
    private int nextLast;
    private static int RESIZE = 16;

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
            if ((size <= items.length / 4) && (items.length >= RESIZE)) {
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
            if ((size <= items.length / 4) && (items.length >= RESIZE)) {
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

    @Override
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int currPos;

        public ArrayDequeIterator() {
            currPos = 0;
        }

        @Override
        public boolean hasNext() {
            return currPos < size;
        }

        @Override
        public T next() {
            T item = get(currPos);
            currPos += 1;
            return item;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Deque) {
            Deque<?> otherDeque = (Deque<?>) o;
            if (this.size != otherDeque.size()) {
                return false;
            }
            for (int i = 0; i < size; i += 1) {
                if (this.get(i).equals(otherDeque.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
