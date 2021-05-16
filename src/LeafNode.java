import java.util.Arrays;

public class LeafNode<K extends Comparable<K>, V> extends Node<K> {
    private ValueList<K, V>[] valueLists;
    private int numberOfLists;
    private final int maxLists;
    private final int minLists;

    public LeafNode(int order) {
        super(order, null);
        this.order = order;
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

    public void insert(Pair<K, V> kvPair) {
        ValueList<K, V> existentList = exists(kvPair.getKey());
        if (existentList != null)
            insertDuplicate(existentList, kvPair.getVal());
        else if (this.numberOfLists < this.order) {
            this.valueLists[this.numberOfLists] = new ValueList<>(kvPair);
            this.numberOfLists++;
            sortLists();
        }
    }

    public void insert(ValueList<K, V> list) {
        if (this.numberOfLists < this.order) {
            this.valueLists[this.numberOfLists] = list;
            this.numberOfLists++;
            sortLists();
        }
    }

    private void insertDuplicate(ValueList<K, V> valueList, V val) {
        valueList.add(val);
    }

    public ValueList<K, V> exists(K key) {
        for (int i = 0; i < numberOfLists; i++) {
            if (this.valueLists[i].getKey().equals(key))
                return this.valueLists[i];
        }
        return null;
    }

    // DELETES A WHOLE LIST...
    public boolean deleteByIndex(int idx) {
        if (idx < 0 || idx > this.numberOfLists - 1)
            return false;
        if (this.numberOfLists - (idx + 1) >= 0)
            System.arraycopy(this.valueLists, idx + 1, this.valueLists, idx, this.numberOfLists - (idx + 1));
        this.valueLists[this.numberOfLists - 1] = null;
        this.numberOfLists--;
        Arrays.sort(valueLists, 0, numberOfLists);

        return true;
    }

    public boolean deleteByKey(K key) {
        int idx = Arrays.binarySearch(
                this.valueLists,
                0,
                this.numberOfLists,
                new ValueList<K, V>(key, null)
        );

        return deleteByIndex(idx);
    }

    public ValueList<K, V>[] getLists() {
        return valueLists;
    }

    public void setLists(ValueList<K, V>[] lists, int n) {
        this.valueLists = lists;
        this.numberOfLists = n;
    }

    public ValueList<K, V>[] splitLists(int mid) {
        ValueList<K, V>[] rightList = new ValueList[this.order];

        for (int i = mid; i < this.order; i++) {
            rightList[i - mid] = this.valueLists[i];
            this.valueLists[i] = null;
            this.numberOfLists--;
        }

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
    public void merge(Node<K> lf) {
        LeafNode<K, V> leaf = (LeafNode<K, V>) lf;
        for (int i = 0; i < leaf.getNumberOfLists(); i++) {
            this.valueLists[this.numberOfLists] = leaf.getLists()[i];
            this.numberOfLists++;
        }
        this.sortLists();
    }

    private void sortLists() {
        Arrays.sort(this.valueLists, (o1, o2) -> {
            if (o1 == null && o2 == null) { return 0; }
            if (o1 == null) { return 1; }
            if (o2 == null) { return -1; }
            return o1.compareTo(o2);
        });
    }
}

