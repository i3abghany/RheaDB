import java.util.Arrays;

public class LeafNode<K extends Comparable<K>, V extends Comparable<V>> extends Node<K> {
    private Pair<K, V>[] pairs;
    private int numberOfPairs;
    private int maxPairs;
    private int minPairs;

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

    public void setNumberOfPairs(int i) {
        this.numberOfPairs = i;
    }

    @Override
    public Node<K>[] getChildren() {
        return null;
    }
}

