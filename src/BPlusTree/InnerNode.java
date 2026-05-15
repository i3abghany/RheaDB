package BPlusTree;

import java.util.Arrays;

public class InnerNode<K extends Comparable<K>> extends Node<K> {
    private final int minDegree;
    private int degree;

    private Node<K>[] children;

    @SuppressWarnings("unchecked")
    public InnerNode(int order, K[] keys) {
        super(order, keys != null ? keys : (K[]) new Comparable[order]);
        this.minDegree = (int) Math.ceil(this.order / 2.0);
        this.children = new Node[this.order + 1];
        this.degree = 0;
        this.validateNumOfKeys();
    }

    public InnerNode(int order, K[] keys, Node<K>[] ptrs) {
        this(order, keys);
        this.children = ptrs;
        this.validateDegree();
        this.validateNumOfKeys();
    }

    public InnerNode(int order) {
        this(order, null);
    }

    @Override
    public K[] getKeys() {
        return this.keys;
    }

    @Override
    public Node<K>[] getChildren() {
        return children;
    }

    public void appendPointer(Node<K> node) {
        insertAt(node, this.degree);
    }

    public void insertChildAfter(Node<K> leftChild, K separatorKey, Node<K> rightChild) {
        int childIdx = indexOfPointer(leftChild);
        if (childIdx < 0) {
            throw new IllegalArgumentException("Left child is not attached to this parent.");
        }

        insertKeyAt(childIdx, separatorKey);
        insertAt(rightChild, childIdx + 1);
    }

    public void removePointer(int idx) {
        if (idx < 0 || idx >= this.degree) {
            throw new IndexOutOfBoundsException("Invalid pointer index: " + idx);
        }

        int movedPointers = this.degree - idx - 1;
        if (movedPointers > 0) {
            System.arraycopy(this.children, idx + 1, this.children, idx, movedPointers);
        }

        this.children[this.degree - 1] = null;
        this.degree--;
    }

    public void addKey(K newKey) {
        insertKeyAt(insertionPoint(newKey), newKey);
    }

    public int getDegree() {
        return degree;
    }

    public int indexOfPointer(Node<K> nd) {
        for (int i = 0; i < degree; i++) {
            if (nd == children[i]) {
                return i;
            }
        }

        return -1;
    }

    public void insertAt(Node<K> newNode, int idx) {
        if (idx < 0 || idx > this.degree) {
            throw new IndexOutOfBoundsException("Invalid pointer index: " + idx);
        }
        if (this.degree >= this.children.length) {
            throw new IllegalStateException("Cannot insert pointer into a full inner node.");
        }

        int movedPointers = this.degree - idx;
        if (movedPointers > 0) {
            System.arraycopy(children, idx, children, idx + 1, movedPointers);
        }

        this.children[idx] = newNode;
        this.degree++;
        if (newNode != null) {
            newNode.setParent(this);
        }
    }

    public boolean isOverFull() {
        return this.degree > this.order;
    }

    @SuppressWarnings("unchecked")
    public Node<K>[] splitPointers(int midPoint) {
        Node<K>[] retArr = new Node[this.order + 1];
        int rightPointerStart = midPoint + 1;
        int rightPointerCount = this.degree - rightPointerStart;

        if (rightPointerCount > 0) {
            System.arraycopy(this.children, rightPointerStart, retArr, 0, rightPointerCount);
            Arrays.fill(this.children, rightPointerStart, this.degree, null);
        }

        this.degree = rightPointerStart;
        return retArr;
    }

    public void validateDegree() {
        this.degree = 0;
        while (this.degree < this.children.length && this.children[this.degree] != null) {
            this.degree++;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void merge(Node<K> node) {
        InnerNode<K> innerNode = (InnerNode<K>) node;

        for (int i = 0; i < innerNode.getNumberOfKeys(); i++) {
            insertKeyAt(this.numberOfKeys, innerNode.getKeys()[i]);
        }
        for (int i = 0; i < innerNode.getDegree(); i++) {
            appendPointer(innerNode.getChildren()[i]);
        }
    }

    @Override
    public boolean canGiveToSibling() {
        return this.degree > this.minDegree;
    }

    @Override
    public boolean canBeMerged() {
        return this.degree == this.minDegree;
    }

    @Override
    public boolean isUnderFull() {
        return this.degree < this.minDegree;
    }
}
