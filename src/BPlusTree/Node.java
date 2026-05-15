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
        validateNumOfKeys();
    }

    public abstract K[] getKeys();

    public int getNumberOfKeys() {
        return this.numberOfKeys;
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

    protected int findKeyIndex(K key) {
        int low = 0;
        int high = numberOfKeys - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int comparison = keys[mid].compareTo(key);

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

    protected int insertionPoint(K key) {
        int idx = findKeyIndex(key);
        return idx >= 0 ? idx : -(idx + 1);
    }

    protected void insertKeyAt(int idx, K key) {
        if (keys == null || numberOfKeys >= keys.length) {
            throw new IllegalStateException("Cannot insert key into a full node.");
        }
        if (idx < 0 || idx > numberOfKeys) {
            throw new IndexOutOfBoundsException("Invalid key index: " + idx);
        }

        int movedKeys = numberOfKeys - idx;
        if (movedKeys > 0) {
            System.arraycopy(keys, idx, keys, idx + 1, movedKeys);
        }

        keys[idx] = key;
        numberOfKeys++;
    }

    @SuppressWarnings("unchecked")
    public K[] splitKeys(int midPoint) {
        K[] retArray = (K[]) new Comparable[this.order];

        int rightKeyCount = numberOfKeys - midPoint - 1;
        if (rightKeyCount > 0) {
            System.arraycopy(this.keys, midPoint + 1, retArray, 0, rightKeyCount);
        }

        Arrays.fill(this.keys, midPoint, numberOfKeys, null);
        this.numberOfKeys = midPoint;
        return retArray;
    }

    protected void validateNumOfKeys() {
        this.numberOfKeys = 0;
        if (this.keys == null) {
            return;
        }

        while (this.numberOfKeys < this.keys.length && this.keys[this.numberOfKeys] != null) {
            this.numberOfKeys++;
        }
    }

    public void setKey(int idx, K key) {
        if (idx < 0 || idx >= numberOfKeys) {
            throw new IndexOutOfBoundsException("Invalid key index: " + idx);
        }

        this.keys[idx] = key;
    }

    public K removeKey(int idx) {
        if (idx < 0 || idx >= numberOfKeys) {
            throw new IndexOutOfBoundsException("Invalid key index: " + idx);
        }

        K removedKey = this.keys[idx];
        int movedKeys = numberOfKeys - idx - 1;
        if (movedKeys > 0) {
            System.arraycopy(this.keys, idx + 1, this.keys, idx, movedKeys);
        }

        this.keys[numberOfKeys - 1] = null;
        numberOfKeys--;
        return removedKey;
    }

}

