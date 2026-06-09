package bstmap;

import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V>{

    private int size;

    private BSTNode first;

    private class BSTNode {
        private K key;
        private V value;
        private BSTNode leftNode;
        private BSTNode rightNode;

        public BSTNode(K key, V value, BSTNode leftNode, BSTNode rightNode) {
            this.key = key;
            this.value = value;
            this.leftNode = leftNode;
            this.rightNode = rightNode;
        }
    }

    public BSTMap() {
        size = 0;
    }

    @Override
    public void clear() {
        size = 0;
        first = null;
    }

    @Override
    public boolean containsKey(K key) {
        BSTNode node = get(first, key);
        return node != null;
    }

    @Override
    public V get(K key) {
        BSTNode node = get(first, key);
        if (node != null) {
            return node.value;
        }
        return null;
    }

    private BSTNode get(BSTNode node, K key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            return node;
        } else if (cmp < 0) {
            return get(node.leftNode, key);
        }else {
            return get(node.rightNode, key);
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        first = put(first, key, value);
    }

    private BSTNode put(BSTNode node, K key, V value) {
        if (node == null) {
            size += 1;
            return new BSTNode(key, value, null, null);
        }
        if (key.compareTo(node.key) < 0) {
            node.leftNode = put(node.leftNode, key, value);
        } else if (key.compareTo(node.key) > 0){
            node.rightNode = put(node.rightNode, key, value);
        } else {
            node.value = value;
        }
        return node;
    }

    private void printInOrder() {

    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(K key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<K> iterator() {
        throw new UnsupportedOperationException();
    }
}
