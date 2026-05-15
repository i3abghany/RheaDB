package BPlusTree;

import Predicate.Predicate;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Vector;

public class BPlusTree<K extends Comparable<K>, V> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private InnerNode<K> root;
    private LeafNode<K, V> firstLeaf;
    private final int order;

    private static final int DEFAULT_ORDER = 16;

    private BPlusTree(int order) {
        if (order < 3) {
            throw new IllegalArgumentException("B+ tree order must be at least 3.");
        }

        this.order = order;
        this.root = null;
    }

    public BPlusTree() {
        this(DEFAULT_ORDER);
    }

    public boolean isEmpty() {
        return this.firstLeaf == null;
    }

    public Vector<ValueList<K, V>> getAllValueLists() {
        Vector<ValueList<K, V>> valueLists = new Vector<>();
        LeafNode<K, V> leaf = this.firstLeaf;

        while (leaf != null) {
            for (int i = 0; i < leaf.getNumberOfLists(); i++) {
                valueLists.add(leaf.getLists()[i]);
            }
            leaf = (LeafNode<K, V>) leaf.getRightSibling();
        }

        return valueLists;
    }

    @SuppressWarnings("unchecked")
    public void insert(Object obj, V val) {
        K key = (K) obj;

        if (isEmpty()) {
            this.firstLeaf = new LeafNode<>(this.order);
            this.firstLeaf.insert(key, val);
            return;
        }

        LeafNode<K, V> leaf = findLeafForKey(key);
        if (!leaf.isFull() || leaf.exists(key) != null) {
            leaf.insert(key, val);
            refreshAncestorSeparators(leaf);
            return;
        }

        leaf.insert(key, val);
        splitLeaf(leaf);
    }

    private void splitLeaf(LeafNode<K, V> leaf) {
        int splitIndex = leaf.getNumberOfLists() / 2;
        int rightListCount = leaf.getNumberOfLists() - splitIndex;
        ValueList<K, V>[] rightLists = leaf.splitLists(splitIndex);

        LeafNode<K, V> rightLeaf = new LeafNode<>(this.order, leaf.getParent());
        rightLeaf.setLists(rightLists, rightListCount);
        linkSiblings(leaf, rightLeaf);

        insertIntoParent(leaf, firstKey(rightLeaf), rightLeaf);
    }

    private void splitInternalNode(InnerNode<K> node) {
        int promotedKeyIndex = node.getNumberOfKeys() / 2;
        K promotedKey = node.getKeys()[promotedKeyIndex];

        K[] rightKeys = node.splitKeys(promotedKeyIndex);
        Node<K>[] rightPointers = node.splitPointers(promotedKeyIndex);
        InnerNode<K> rightNode = new InnerNode<>(this.order, rightKeys, rightPointers);

        for (int i = 0; i < rightNode.getDegree(); i++) {
            rightNode.getChildren()[i].setParent(rightNode);
        }

        linkSiblings(node, rightNode);
        insertIntoParent(node, promotedKey, rightNode);
    }

    @SuppressWarnings("unchecked")
    private void insertIntoParent(Node<K> leftChild, K separatorKey, Node<K> rightChild) {
        InnerNode<K> parent = leftChild.getParent();
        if (parent != null) {
            parent.insertChildAfter(leftChild, separatorKey, rightChild);
            if (parent.isOverFull()) {
                splitInternalNode(parent);
            }
            return;
        }

        K[] rootKeys = (K[]) new Comparable[this.order];
        InnerNode<K> newRoot = new InnerNode<>(this.order, rootKeys);
        newRoot.insertKeyAt(0, separatorKey);
        newRoot.appendPointer(leftChild);
        newRoot.appendPointer(rightChild);
        this.root = newRoot;
    }

    private void linkSiblings(Node<K> leftNode, Node<K> rightNode) {
        rightNode.setRightSibling(leftNode.getRightSibling());
        if (leftNode.getRightSibling() != null) {
            leftNode.getRightSibling().setLeftSibling(rightNode);
        }

        leftNode.setRightSibling(rightNode);
        rightNode.setLeftSibling(leftNode);
    }

    private void refreshAncestorSeparators(Node<K> node) {
        Node<K> current = node;
        while (current != null && current.getParent() != null) {
            InnerNode<K> parent = current.getParent();
            int childIdx = parent.indexOfPointer(current);
            if (childIdx < 0) {
                return;
            }
            if (childIdx > 0) {
                K firstKey = firstKey(current);
                if (firstKey != null) {
                    parent.setKey(childIdx - 1, firstKey);
                }
                return;
            }
            current = parent;
        }
    }

    @SuppressWarnings("unchecked")
    private K firstKey(Node<K> node) {
        Node<K> current = node;
        while (current instanceof InnerNode) {
            current = ((InnerNode<K>) current).getChildren()[0];
        }

        LeafNode<K, V> leaf = (LeafNode<K, V>) current;
        return leaf.getNumberOfLists() == 0 ? null : leaf.getLists()[0].getKey();
    }

    @SuppressWarnings("unchecked")
    private LeafNode<K, V> findLeafNode(Node<K> node, K key) {
        if (node == null) {
            return null;
        }

        Node<K> current = node;
        while (current instanceof InnerNode) {
            InnerNode<K> innerNode = (InnerNode<K>) current;
            current = innerNode.getChildren()[childIndex(innerNode, key)];
        }

        return (LeafNode<K, V>) current;
    }

    private int childIndex(InnerNode<K> node, K key) {
        int low = 0;
        int high = node.getNumberOfKeys();

        while (low < high) {
            int mid = (low + high) >>> 1;
            if (node.getKeys()[mid].compareTo(key) > 0) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }

        return low;
    }

    private LeafNode<K, V> findLeafForKey(K key) {
        if (this.firstLeaf == null) {
            return null;
        }
        if (this.root == null) {
            return this.firstLeaf;
        }

        return findLeafNode(this.root, key);
    }

    public ValueList<K, V> find(K key) {
        LeafNode<K, V> leaf = findLeafForKey(key);
        return leaf == null ? null : leaf.exists(key);
    }

    @SuppressWarnings("unchecked")
    public Vector<V> findWithPredicate(Predicate predicate) {
        K key = (K) predicate.getValue();
        return switch (predicate.getOperation()) {
            case EQUALS -> findEquals(key);
            case LESS_THAN -> findLessThan(key);
            case LESS_THAN_EQUAL -> findLessEquals(key);
            case GREATER_THAN -> findGreaterThan(key);
            case GREATER_THAN_EQUAL -> findGreaterEquals(key);
            case NOT_EQUALS -> findNotEquals(key);
        };
    }

    private Vector<V> findEquals(K key) {
        Vector<V> result = new Vector<>();
        ValueList<K, V> valueList = find(key);
        if (valueList != null) {
            result.addAll(valueList);
        }
        return result;
    }

    private Vector<V> findNotEquals(K key) {
        LeafNode<K, V> lf = this.firstLeaf;
        Vector<V> result = new Vector<>();

        while (lf != null) {
            for (int i = 0; i < lf.getNumberOfLists(); i++) {
                ValueList<K, V> valueList = lf.getLists()[i];
                if (valueList.getKey().compareTo(key) == 0)
                    continue;
                result.addAll(valueList);
            }
            lf = (LeafNode<K, V>) lf.getRightSibling();
        }

        return result;
    }

    public Vector<V> findLessThan(K key) {
        return collectLessThan(key, false);
    }

    public Vector<V> findGreaterThan(K key) {
        return collectGreaterThan(key, false);
    }

    public Vector<V> findGreaterEquals(K key) {
        return collectGreaterThan(key, true);
    }

    public Vector<V> findLessEquals(K key) {
        return collectLessThan(key, true);
    }

    private Vector<V> collectLessThan(K key, boolean includeEquals) {
        LeafNode<K, V> lf = firstLeaf;
        Vector<V> result = new Vector<>();

        while (lf != null) {
            for (int i = 0; i < lf.getNumberOfLists(); i++) {
                ValueList<K, V> valueList = lf.getLists()[i];
                int comparison = valueList.getKey().compareTo(key);
                if (comparison > 0 || (!includeEquals && comparison == 0)) {
                    return result;
                }
                result.addAll(valueList);
            }
            lf = (LeafNode<K, V>) lf.getRightSibling();
        }

        return result;
    }

    private Vector<V> collectGreaterThan(K key, boolean includeEquals) {
        Vector<V> result = new Vector<>();
        LeafNode<K, V> lf = findLeafForKey(key);
        if (lf == null) {
            return result;
        }

        int startIdx = includeEquals ? lf.firstGreaterOrEqualIndex(key) : lf.firstGreaterThanIndex(key);
        while (lf != null) {
            for (int i = startIdx; i < lf.getNumberOfLists(); i++) {
                result.addAll(lf.getLists()[i]);
            }
            lf = (LeafNode<K, V>) lf.getRightSibling();
            startIdx = 0;
        }
        return result;
    }

    @SuppressWarnings("unused")
    public void printDataInOrder() {
        printLeafInOrder(this.firstLeaf);
    }

    @SuppressWarnings("unused")
    private void printBFS() {
        Queue<Node<K>> q = new ArrayDeque<>();
        q.add(this.root);

        while (!q.isEmpty()) {
            var curr = q.peek();
            if (curr instanceof InnerNode) {
                System.out.print("[");
                for (int i = 0; i < curr.getNumberOfKeys(); i++)
                    System.out.print(curr.getKeys()[i] + ", ");
                System.out.print("]\n");
                for (int i = 0; i < ((InnerNode<K>) curr).getDegree(); i++) {
                    q.add(curr.getChildren()[i]);
                }
            } else {
                System.out.print("{");
                for (int i = 0; i < ((LeafNode<K, V>) curr).getNumberOfLists(); i++)
                    System.out.print(((LeafNode<K, V>) curr).getLists()[i] + ", ");
                System.out.print("}\n");
            }

            q.remove();
        }
    }

    @SuppressWarnings("unused")
    public void printTreeInOrder() {
        if (this.root == null) {
            printDataInOrder();
            return;
        }
        var curr = (Node<K>) this.root;
        while (true) {
            if (curr instanceof InnerNode) {
                var nxt = curr;
                while (nxt != null) {
                    System.out.print("[");
                    for (int i = 0; i < nxt.getNumberOfKeys(); i++)
                        System.out.print(nxt.getKeys()[i] + ", ");
                    System.out.print("]");
                    nxt = nxt.getRightSibling();
                }
                System.out.println();
                curr = curr.getChildren()[0];
            } else if (curr instanceof LeafNode) {
                printDataInOrder();
                break;
            }
        }
        System.out.println();
    }

    private void printLeafInOrder(LeafNode<K, V> leaf) {
        LeafNode<K, V> lf = leaf;
        while (lf != null) {
            System.out.print("{");
            ValueList<K, V>[] lists = lf.getLists();
            for (int i = 0; i < lf.getNumberOfLists(); i++) {
                ValueList<K, V> el = lists[i];
                System.out.print(el);
                if (i != lf.getNumberOfLists() - 1)
                    System.out.print(", ");
            }
            System.out.print("} ");
            lf = (LeafNode<K, V>) lf.getRightSibling();
        }
    }

    public boolean delete(K key) {
        if (this.isEmpty()) {
            return false;
        }

        LeafNode<K, V> leaf = findLeafForKey(key);
        if (leaf == null || !leaf.deleteByKey(key)) {
            return false;
        }

        if (this.root == null) {
            if (leaf.getNumberOfLists() == 0) {
                this.firstLeaf = null;
            }
            return true;
        }

        if (!leaf.isUnderFull()) {
            refreshAncestorSeparators(leaf);
            return true;
        }

        rebalanceLeaf(leaf);
        return true;
    }

    private void rebalanceLeaf(LeafNode<K, V> leaf) {
        InnerNode<K> parent = leaf.getParent();
        int leafIdx = parent.indexOfPointer(leaf);
        LeafNode<K, V> leftSibling = leafIdx > 0 ? leftLeafOf(parent.getChildren()[leafIdx - 1]) : null;
        LeafNode<K, V> rightSibling = leafIdx < parent.getDegree() - 1 ? leftLeafOf(parent.getChildren()[leafIdx + 1]) : null;

        if (leftSibling != null && leftSibling.canGiveToSibling()) {
            ValueList<K, V> borrowedList = leftSibling.getLists()[leftSibling.getNumberOfLists() - 1];
            leftSibling.deleteByIndex(leftSibling.getNumberOfLists() - 1);
            leaf.insert(borrowedList);
            parent.setKey(leafIdx - 1, firstKey(leaf));
            refreshAncestorSeparators(leaf);
            return;
        }

        if (rightSibling != null && rightSibling.canGiveToSibling()) {
            ValueList<K, V> borrowedList = rightSibling.getLists()[0];
            rightSibling.deleteByIndex(0);
            leaf.insert(borrowedList);
            parent.setKey(leafIdx, firstKey(rightSibling));
            refreshAncestorSeparators(leaf);
            return;
        }

        if (leftSibling != null) {
            leftSibling.merge(leaf);
            unlinkNode(leaf);
            parent.removeKey(leafIdx - 1);
            parent.removePointer(leafIdx);
            refreshAncestorSeparators(leftSibling);
            rebalanceInner(parent);
            return;
        }

        if (rightSibling != null) {
            leaf.merge(rightSibling);
            unlinkNode(rightSibling);
            parent.removeKey(leafIdx);
            parent.removePointer(leafIdx + 1);
            refreshAncestorSeparators(leaf);
            rebalanceInner(parent);
        }
    }

    private void rebalanceInner(InnerNode<K> node) {
        if (node == null) {
            return;
        }

        if (node == this.root) {
            collapseRootIfNeeded();
            return;
        }

        if (!node.isUnderFull()) {
            refreshAncestorSeparators(node);
            return;
        }

        InnerNode<K> parent = node.getParent();
        int nodeIdx = parent.indexOfPointer(node);
        InnerNode<K> leftSibling = nodeIdx > 0 ? innerNodeOf(parent.getChildren()[nodeIdx - 1]) : null;
        InnerNode<K> rightSibling = nodeIdx < parent.getDegree() - 1 ? innerNodeOf(parent.getChildren()[nodeIdx + 1]) : null;

        if (leftSibling != null && leftSibling.canGiveToSibling()) {
            borrowFromLeftInner(node, leftSibling, parent, nodeIdx);
            return;
        }

        if (rightSibling != null && rightSibling.canGiveToSibling()) {
            borrowFromRightInner(node, rightSibling, parent, nodeIdx);
            return;
        }

        if (leftSibling != null) {
            mergeInnerIntoLeft(node, leftSibling, parent, nodeIdx);
            return;
        }

        if (rightSibling != null) {
            mergeRightInnerIntoNode(node, rightSibling, parent, nodeIdx);
        }
    }

    private void borrowFromLeftInner(
            InnerNode<K> node,
            InnerNode<K> leftSibling,
            InnerNode<K> parent,
            int nodeIdx
    ) {
        Node<K> borrowedPointer = leftSibling.getChildren()[leftSibling.getDegree() - 1];
        K replacementSeparator = leftSibling.getKeys()[leftSibling.getNumberOfKeys() - 1];
        K parentSeparator = parent.getKeys()[nodeIdx - 1];

        leftSibling.removePointer(leftSibling.getDegree() - 1);
        leftSibling.removeKey(leftSibling.getNumberOfKeys() - 1);
        node.insertKeyAt(0, parentSeparator);
        node.insertAt(borrowedPointer, 0);
        parent.setKey(nodeIdx - 1, replacementSeparator);
        refreshAncestorSeparators(node);
    }

    private void borrowFromRightInner(
            InnerNode<K> node,
            InnerNode<K> rightSibling,
            InnerNode<K> parent,
            int nodeIdx
    ) {
        Node<K> borrowedPointer = rightSibling.getChildren()[0];
        K replacementSeparator = rightSibling.getKeys()[0];
        K parentSeparator = parent.getKeys()[nodeIdx];

        rightSibling.removePointer(0);
        rightSibling.removeKey(0);
        node.insertKeyAt(node.getNumberOfKeys(), parentSeparator);
        node.appendPointer(borrowedPointer);
        parent.setKey(nodeIdx, replacementSeparator);
        refreshAncestorSeparators(rightSibling);
    }

    private void mergeInnerIntoLeft(
            InnerNode<K> node,
            InnerNode<K> leftSibling,
            InnerNode<K> parent,
            int nodeIdx
    ) {
        leftSibling.insertKeyAt(leftSibling.getNumberOfKeys(), parent.getKeys()[nodeIdx - 1]);
        leftSibling.merge(node);
        unlinkNode(node);
        parent.removeKey(nodeIdx - 1);
        parent.removePointer(nodeIdx);
        refreshAncestorSeparators(leftSibling);
        rebalanceInner(parent);
    }

    private void mergeRightInnerIntoNode(
            InnerNode<K> node,
            InnerNode<K> rightSibling,
            InnerNode<K> parent,
            int nodeIdx
    ) {
        node.insertKeyAt(node.getNumberOfKeys(), parent.getKeys()[nodeIdx]);
        node.merge(rightSibling);
        unlinkNode(rightSibling);
        parent.removeKey(nodeIdx);
        parent.removePointer(nodeIdx + 1);
        refreshAncestorSeparators(node);
        rebalanceInner(parent);
    }

    private void collapseRootIfNeeded() {
        if (this.root == null || this.root.getNumberOfKeys() > 0) {
            return;
        }

        Node<K> onlyChild = this.root.getDegree() > 0 ? this.root.getChildren()[0] : null;
        if (onlyChild instanceof InnerNode) {
            this.root = innerNodeOf(onlyChild);
            this.root.setParent(null);
            this.root.setLeftSibling(null);
            this.root.setRightSibling(null);
            return;
        }

        if (onlyChild instanceof LeafNode) {
            LeafNode<K, V> leaf = leftLeafOf(onlyChild);
            leaf.setParent(null);
            leaf.setLeftSibling(null);
            leaf.setRightSibling(null);
            this.firstLeaf = leaf;
            this.root = null;
            return;
        }

        this.firstLeaf = null;
        this.root = null;
    }

    private void unlinkNode(Node<K> node) {
        Node<K> leftSibling = node.getLeftSibling();
        Node<K> rightSibling = node.getRightSibling();

        if (leftSibling != null) {
            leftSibling.setRightSibling(rightSibling);
        } else if (node == this.firstLeaf) {
            this.firstLeaf = rightLeafOf(rightSibling);
        }

        if (rightSibling != null) {
            rightSibling.setLeftSibling(leftSibling);
        }

        node.setLeftSibling(null);
        node.setRightSibling(null);
    }

    @SuppressWarnings("unchecked")
    private InnerNode<K> innerNodeOf(Node<K> node) {
        return (InnerNode<K>) node;
    }

    @SuppressWarnings("unchecked")
    private LeafNode<K, V> leftLeafOf(Node<K> node) {
        return (LeafNode<K, V>) node;
    }

    @SuppressWarnings("unchecked")
    private LeafNode<K, V> rightLeafOf(Node<K> node) {
        return (LeafNode<K, V>) node;
    }
}
