package hashmap;

import net.sf.saxon.expr.Component;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author anhhn
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;
    // You should probably define some more!
    private int initialSize;

    private double maxLoad;

    private int size;

    /** Constructors */
    public MyHashMap() {
        this(16, 0.75);

    }

    public MyHashMap(int initialSize) {
        this(initialSize, 0.75);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.initialSize = initialSize;
        this.maxLoad = maxLoad;
        size = 0;
        buckets = createTable(initialSize);
        for (int i = 0; i < buckets.length; i += 1) {
            buckets[i] = createBucket();
        }
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<Node>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        return new Collection[tableSize];
    }

    // TODO: Implement the methods of the Map61B Interface below
    // Your code won't compile until you do so!
    @Override
    public void clear() {
        size = 0;
        buckets = createTable(buckets.length);
        for (int i = 0; i < buckets.length; i += 1) {
            buckets[i] = createBucket();
        }
    }

    @Override
    public boolean containsKey(K key) {
        Node node = getNode(key);
        return node != null;
    }

    @Override
    public V get(K key) {
        Node node = getNode(key);
        if (node != null) {
            return node.value;
        }
        return null;
    }

    private Node getNode(K key) {
        int hashCode = key.hashCode();
        int bucketIndex = Math.floorMod(hashCode, buckets.length);
        Collection<Node> bucket = buckets[bucketIndex];
        for (Node item: bucket) {
            if (key.equals(item.key)){
                return item;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return size;
    }

    private void resize() {
        Collection<Node>[] newBuckets = createTable(buckets.length * 2);
        for (int i = 0; i < newBuckets.length; i += 1) {
            newBuckets[i] = createBucket();
        }
        // copy
        for (Collection<Node> bucket: buckets) {
            if (!bucket.isEmpty()) {
                for (Node node: bucket) {
                    int index = Math.floorMod(node.key.hashCode(), newBuckets.length);
                    newBuckets[index].add(node);
                }
            }
        }
        // assign to new buckets
        buckets = newBuckets;
    }

//    private int getBucketIndex(K key) {
//        return Math.floorMod(key.hashCode(), buckets.length);
//    }

    @Override
    public void put(K key, V value) {
        Node node = getNode(key);
        if (node != null) {
            node.value = value;
        }else {
            int bucketIndex = Math.floorMod(key.hashCode(), buckets.length);
            Node insertNode = createNode(key, value);
            buckets[bucketIndex].add(insertNode);
            size += 1;
            int currentFactor = size / buckets.length;
            if (currentFactor > maxLoad) {
                resize();
            }
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (Collection<Node> bucket: buckets) {
            if (!bucket.isEmpty()) {
                for (Node node: bucket) {
                   keys.add(node.key);
                }
            }
        }
        return keys;
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
        return keySet().iterator();
    }


}
