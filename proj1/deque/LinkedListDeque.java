package deque;

public class LinkedListDeque<T> implements Deque<T> {

    private class Node {
        public T item;
        public Node prev;
        public Node next;

        public Node(Node p, T i, Node n) {
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
    public boolean isEmpty() {
        return size == 0;
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
        return null;
    }

    @Override
    public T removeLast() {
        return null;
    }

    @Override
    public T get(int index) {
        return null;
    }
}
