import java.util.Arrays;

public class LeafNode<K extends Comparable<K>, V> extends Node<K> {
    private Pair<K, V>[] pairs;
    private int numberOfPairs;
    private final int maxPairs;
    private final int minPairs;

    public LeafNode(int order) {
        super(order, null);
        this.order = order;
        this.maxPairs = order - 1;
        this.minPairs = (int) Math.ceil(this.order / 2.0) - 1;
        this.pairs = new Pair[this.order];
    }

    public LeafNode(int order, InnerNode<K> parent) {
        this(order);
        this.parent = parent;
    }

    public boolean isFull() {
        return this.numberOfPairs == maxPairs;
    }

    public void insert(Pair<K, V> kvPair) {
        if (!isFull()) {
            this.pairs[this.numberOfPairs] = kvPair;
            this.numberOfPairs++;
            Arrays.sort(pairs, 0, numberOfPairs);
        }
    }

    public boolean deleteByIndex(int idx) {
        if (idx < 0 || idx > this.getNumberOfPairs() - 1)
            return false;

        if (this.numberOfPairs - (idx + 1) >= 0)
            System.arraycopy(this.pairs, idx + 1, this.pairs, idx, this.numberOfPairs - (idx + 1));
        this.pairs[this.numberOfPairs - 1] = null;
        this.numberOfPairs--;
        Arrays.sort(pairs, 0, numberOfPairs);

        return true;
    }

    public boolean deleteByKey(K key) {
        int idx = Arrays.binarySearch(
                this.pairs,
                0,
                this.getNumberOfPairs(),
                new Pair<K, V>(key, null)
        );

        return deleteByIndex(idx);
    }

    public Pair<K, V>[] getPairs() {
        return pairs;
    }

    public void setPairs(Pair<K, V>[] pairs, int n) {
        this.pairs = pairs;
        this.numberOfPairs = n;
    }

    public Pair<K, V>[] splitPairs(int mid) {
        Pair<K, V>[] rightPairs = new Pair[this.order];

        for (int i = mid; i < this.order; i++) {
            rightPairs[i - mid] = this.pairs[i];
            this.pairs[i] = null;
            this.numberOfPairs--;
        }

        return rightPairs;
    }

    public int getNumberOfPairs() {
        return numberOfPairs;
    }

    @Override
    public Node<K> getLeftSibling() {
        return leftSibling;
    }

    @Override
    public void setLeftSibling(Node<K> leftSibling) {
        this.leftSibling = leftSibling;
    }

    @Override
    public Node<K> getRightSibling() {
        return rightSibling;
    }

    @Override
    public void setRightSibling(Node<K> rightSibling) {
        this.rightSibling = rightSibling;
    }

    @Override
    public boolean isUnderFull() {
        return this.numberOfPairs < this.minPairs;
    }

    @Override
    public boolean canGiveToSibling() {
        return this.numberOfPairs > this.minPairs;
    }

    public void setNumberOfPairs(int i) {
        this.numberOfPairs = i;
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
        return this.numberOfPairs == this.minPairs;
    }

    @Override
    public void merge(Node<K> lf) {
        for (int i = 0; i < ((LeafNode<K, V>) lf).getNumberOfPairs(); i++) {
            this.pairs[this.numberOfPairs] = ((LeafNode<K, V>) lf).getPairs()[i];
            this.numberOfPairs++;
        }
    }
}

