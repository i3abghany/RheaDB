package BPlusTree;

import java.util.Arrays;

public class LeafNode<K extends Comparable<K>, V> extends Node<K> {
    private ValueList<K, V>[] valueLists;
    private int numberOfLists;
    private final int maxLists;
    private final int minLists;

    @SuppressWarnings("unchecked")
    public LeafNode(int order) {
        super(order, null);
        this.maxLists = order - 1;
        this.minLists = (int) Math.ceil(this.order / 2.0) - 1;
        this.valueLists = new ValueList[this.order];
    }

    public LeafNode(int order, InnerNode<K> parent) {
        this(order);
        this.parent = parent;
    }

    public boolean isFull() {
        return this.numberOfLists == maxLists;
    }

    public void insert(K key, V val) {
        int idx = findListIndex(key);
        if (idx >= 0) {
            this.valueLists[idx].add(val);
            return;
        }

        insertListAt(-(idx + 1), new ValueList<>(key, val));
    }

    public void insert(ValueList<K, V> list) {
        int idx = findListIndex(list.getKey());
        if (idx >= 0) {
            this.valueLists[idx].addAll(list);
            return;
        }

        insertListAt(-(idx + 1), list);
    }

    public ValueList<K, V> exists(K key) {
        int idx = findListIndex(key);
        return idx >= 0 ? this.valueLists[idx] : null;
    }

    public boolean deleteByIndex(int idx) {
        if (idx < 0 || idx >= this.numberOfLists) {
            return false;
        }

        int movedLists = this.numberOfLists - idx - 1;
        if (movedLists > 0) {
            System.arraycopy(this.valueLists, idx + 1, this.valueLists, idx, movedLists);
        }

        this.valueLists[this.numberOfLists - 1] = null;
        this.numberOfLists--;
        return true;
    }

    public boolean deleteByKey(K key) {
        int idx = findListIndex(key);
        return deleteByIndex(idx);
    }

    public ValueList<K, V>[] getLists() {
        return valueLists;
    }

    public void setLists(ValueList<K, V>[] lists, int n) {
        this.valueLists = lists;
        this.numberOfLists = n;
        Arrays.fill(this.valueLists, this.numberOfLists, this.valueLists.length, null);
    }

    @SuppressWarnings("unchecked")
    public ValueList<K, V>[] splitLists(int mid) {
        ValueList<K, V>[] rightList = new ValueList[this.order];
        int rightListCount = this.numberOfLists - mid;

        if (rightListCount > 0) {
            System.arraycopy(this.valueLists, mid, rightList, 0, rightListCount);
            Arrays.fill(this.valueLists, mid, this.numberOfLists, null);
        }

        this.numberOfLists = mid;
        return rightList;
    }

    public int getNumberOfLists() {
        return numberOfLists;
    }

    @Override
    public boolean isUnderFull() {
        return this.numberOfLists < this.minLists;
    }

    @Override
    public boolean canGiveToSibling() {
        return this.numberOfLists > this.minLists;
    }

    @Override
    public K[] getKeys() {
        return null;
    }

    @Override
    public Node<K>[] getChildren() {
        return null;
    }

    @Override
    public boolean canBeMerged() {
        return this.numberOfLists == this.minLists;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void merge(Node<K> lf) {
        LeafNode<K, V> leaf = (LeafNode<K, V>) lf;
        for (int i = 0; i < leaf.getNumberOfLists(); i++) {
            insert(leaf.getLists()[i]);
        }
    }

    public int firstGreaterOrEqualIndex(K key) {
        int idx = findListIndex(key);
        return idx >= 0 ? idx : -(idx + 1);
    }

    public int firstGreaterThanIndex(K key) {
        int idx = firstGreaterOrEqualIndex(key);
        while (idx < numberOfLists && valueLists[idx].getKey().compareTo(key) == 0) {
            idx++;
        }
        return idx;
    }

    private void insertListAt(int idx, ValueList<K, V> list) {
        if (this.numberOfLists >= this.valueLists.length) {
            throw new IllegalStateException("Cannot insert value list into a full leaf.");
        }
        if (idx < 0 || idx > this.numberOfLists) {
            throw new IndexOutOfBoundsException("Invalid list index: " + idx);
        }

        int movedLists = this.numberOfLists - idx;
        if (movedLists > 0) {
            System.arraycopy(this.valueLists, idx, this.valueLists, idx + 1, movedLists);
        }

        this.valueLists[idx] = list;
        this.numberOfLists++;
    }

    private int findListIndex(K key) {
        int low = 0;
        int high = numberOfLists - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int comparison = valueLists[mid].getKey().compareTo(key);

            if (comparison < 0) {
                low = mid + 1;
            } else if (comparison > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        return -(low + 1);
    }
}
