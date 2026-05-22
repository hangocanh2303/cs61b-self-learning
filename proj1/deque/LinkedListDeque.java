package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Deque<T>, Iterable<T> {

    private class Node {
        private T item;
        private Node prev;
        private Node next;

        Node(Node p, T i, Node n) {
            item = i;
            next = n;
            prev = p;
        }
    }

    private Node sentinel;
    private int size;

    public LinkedListDeque() {
        sentinel = new Node(null, null, null);
        sentinel.prev = sentinel;
        sentinel.next = sentinel;
        size = 0;
    }

    @Override
    public void addFirst(T item) {
        Node currentFirstNode = sentinel.next;
        Node newFirstNode = new Node(sentinel, item, currentFirstNode);
        currentFirstNode.prev = newFirstNode;
        sentinel.next = newFirstNode;
        size += 1;
    }

    @Override
    public void addLast(T item) {
        Node currentLastNode = sentinel.prev;
        Node newLastNode = new Node(currentLastNode, item, sentinel);
        currentLastNode.next = newLastNode;
        sentinel.prev = newLastNode;
        size += 1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void printDeque() {
        Node p = sentinel.next;
        int count = size;
        while (p != null && count > 0) {
            System.out.print(p.item + " ");
            p = p.next;
            count--;
        }
        System.out.println();
    }

    @Override
    public T removeFirst() {
        if (size > 0) {
            Node currFirstNode = sentinel.next;
            Node newFirstNode = currFirstNode.next;
            sentinel.next = newFirstNode;
            newFirstNode.prev = sentinel;
            size -= 1;
            return currFirstNode.item;
        }
        return null;
    }

    @Override
    public T removeLast() {
        if (size > 0) {
            Node currLastNode = sentinel.prev;
            Node newLastNode = currLastNode.prev;
            sentinel.prev = newLastNode;
            newLastNode.next = sentinel;
            size -= 1;
            return currLastNode.item;
        }
        return null;
    }

    @Override
    public T get(int index) {
        if (index + 1 > size || index < 0) {
            return null;
        }
        int count = index;
        Node p = sentinel.next;
        while (p.next != null && count > 0) {
            p = p.next;
            count -= 1;
        }
        return p.item;
    }

    public T getRecursive(int index) {
        if (index + 1 > size || index < 0) {
            return null;
        }
        return getRecursive(index, sentinel.next);
    }

    private T getRecursive(int index, Node p) {
        if (index == 0) {
            return p.item;
        }
        return getRecursive(index - 1, p.next);
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkedListDequeIterator();
    }

    private class LinkedListDequeIterator implements Iterator<T> {
        private int currPos;

        LinkedListDequeIterator() {
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
                if (!this.get(i).equals(otherDeque.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
