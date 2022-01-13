package BPlusTree;

import java.io.Serializable;
import java.util.Arrays;

public abstract class Node<K extends Comparable<K>> implements Serializable {
    protected InnerNode<K> parent;
    protected int order;
    protected K[] keys;
    protected int numberOfKeys;
    protected Node<K> leftSibling, rightSibling;

    public Node(int order, K[] keys) {
        this.order = order;
        this.keys = keys;
    }

    public abstract K[] getKeys();

    public int getNumberOfKeys() {
        return this.numberOfKeys;
    }

    protected void sortKeys() {
        Arrays.sort(keys, (o1, o2) -> {
            if (o2 == o1 && o1 == null) return 0;
            if (o1 == null) return 1;
            if (o2 == null) return -1;
            return o1.compareTo(o2);
        });
    }

    public abstract Node<K>[] getChildren();

    public Node<K> getLeftSibling() {
        return leftSibling;
    }

    public void setLeftSibling(Node<K> leftSibling) {
        this.leftSibling = leftSibling;
    }

    public Node<K> getRightSibling() {
        return rightSibling;
    }

    public void setRightSibling(Node<K> rightSibling) {
        this.rightSibling = rightSibling;
    }


    public abstract boolean canGiveToSibling();

    public abstract boolean canBeMerged();

    public abstract void merge(Node<K> node);

    public abstract boolean isUnderFull();

    public InnerNode<K> getParent() {
        return parent;
    }

    public void setParent(InnerNode<K> par) {
        this.parent = par;
    }

    public K[] splitKeys(int midPoint) {
        K[] retArray = (K[]) new Comparable[this.order];
        this.keys[midPoint] = null;
        for (int i = midPoint + 1; i < this.keys.length; i++) {
            retArray[i - midPoint - 1] = this.keys[i];
            this.keys[i] = null;
        }
        this.validateNumOfKeys();
        return retArray;
    }

    protected void validateNumOfKeys() {
        if (this.keys == null) this.numberOfKeys = 0;
        else {
            this.numberOfKeys = this.order;
            for (int i = 0; i < this.keys.length; i++) {
                if (this.keys[i] == null) {
                    this.numberOfKeys = i;
                    break;
                }
            }
        }
    }

    public void setKey(int i, K key) {
        this.keys[i] = key;
        this.sortKeys();
        this.validateNumOfKeys();
    }

    public void removeKey(int i) {
        this.keys[i] = null;
        this.sortKeys();
        this.numberOfKeys--;
        this.sortKeys();
        this.validateNumOfKeys();
    }
}

