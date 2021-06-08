package BPlusTree;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.lang.*;
import Predicate.*;
import RheaDB.SQLException;

public class BPlusTree<K extends Comparable<K>, V> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private InnerNode<K> root;
    private LeafNode<K, V> firstLeaf;
    private final int order;

    private static final int DEFAULT_ORDER = 16;

    private BPlusTree(int order) {
        this.order = order;
        this.root = null;
    }

    public BPlusTree() {
        this(DEFAULT_ORDER);
    }

    public boolean isEmpty() {
        return this.firstLeaf == null;
    }

    @SuppressWarnings("unchecked")
    public void insert(K key, V val) {
        if (isEmpty()) {
            this.firstLeaf = new LeafNode<K, V>(this.order);
            this.firstLeaf.insert(key, val);
        } else {
            LeafNode<K, V> lf = this.root == null ? this.firstLeaf : findLeafNode(key);

            if (lf.exists(key) != null) {
                lf.insert(key, val);
                return;
            }

            if (lf.isFull()) {
                lf.insert(key, val);
                int mid = getMidPoint();
                ValueList<K, V>[] rightHalf = splitLists(lf, mid);

                if (lf.getParent() == null) {
                    K[] parentKeys = (K[]) new Comparable[this.order];
                    parentKeys[0] = rightHalf[0].getKey();
                    InnerNode<K> parentNode = new InnerNode<>(this.order, parentKeys);

                    lf.setParent(parentNode);
                    parentNode.appendPointer(lf);
                } else {
                    K parentNewKey = rightHalf[0].getKey();
                    lf.getParent().addKey(parentNewKey);
                }

                LeafNode<K, V> newNode = new LeafNode<K, V>(this.order, lf.getParent());
                newNode.setLists(rightHalf, this.order - mid);

                int childIdx = lf.getParent().indexOfPointer(lf);
                lf.getParent().insertAt(newNode, childIdx + 1);

                correctSiblings(lf, newNode);

                if (this.root == null) this.root = lf.getParent();
                else {
                    InnerNode<K> par = lf.getParent();
                    while (par != null) {
                        if (par.isOverFull())
                            splitInternalNode(par);
                        else break;
                        par = par.getParent();
                    }
                }
            } else {
                lf.insert(key, val);
            }
        }
    }

    private void correctSiblings(Node<K> lf, Node<K> newNode) {
        newNode.setRightSibling(lf.getRightSibling());
        if (lf.getRightSibling() != null)
            lf.getRightSibling().setLeftSibling(newNode);

        lf.setRightSibling(newNode);
        newNode.setLeftSibling(lf);
    }

    @SuppressWarnings("unchecked")
    private void splitInternalNode(InnerNode<K> node) {
        InnerNode<K> parent = node.getParent();
        int midPoint = getMidPoint();

        K parentNewKey = node.getKeys()[midPoint];

        K[] halfKeys = node.splitKeys(midPoint);
        Node<K>[] halfPointers = node.splitPointers(midPoint);

        InnerNode<K> sib = new InnerNode<K>(this.order, halfKeys, halfPointers);

        for (var ch : halfPointers) {
            if (ch != null) ch.setParent(sib);
        }

        correctSiblings(node, sib);

        if (parent != null) {
            parent.addKey(parentNewKey);
            int childIdx = parent.indexOfPointer(node);
            parent.insertAt(sib, childIdx + 1);
            sib.setParent(parent);
        } else {
            K[] keys = (K[])new Comparable[this.order];
            keys[0] = parentNewKey;
            InnerNode<K> newParent = new InnerNode<>(this.order, keys);

            newParent.appendPointer(node);
            newParent.appendPointer(sib);

            node.setParent(newParent);
            sib.setParent(newParent);

            this.root = newParent;
        }
    }

    private ValueList<K, V>[] splitLists(LeafNode<K, V> lf, int mid) {
        return lf.splitLists(mid);
    }

    private int getMidPoint() {
        return (int) Math.ceil((this.order + 1) / 2.0) - 1;
    }

    private LeafNode<K, V> findLeafNode(Node<K> node, K key) {
        if (node == null)
            return null;

        K[] ks = node.getKeys();

        int idx = 0;
        for (; idx < node.getNumberOfKeys(); idx++) {
            if (ks[idx].compareTo(key) > 0)
                break;
        }

        Node<K> child = node.getChildren()[idx];
        if (child instanceof LeafNode) {
            return (LeafNode<K, V>) child;
        } else {
            return findLeafNode(child, key);
        }
    }

    public ValueList<K, V> find(K key) {
        LeafNode<K, V> lf = this.root == null ? this.firstLeaf : findLeafNode(key);
        if (lf == null) {
            return null;
        }

        ValueList<K, V>[] lists = lf.getLists();

        int idx = Arrays.binarySearch(
                lists,
                0,
                lf.getNumberOfLists(),
                new ValueList<K, V>(key, null)
        );

        if (idx >= 0) return lists[idx];
        else return null;
    }

    @SuppressWarnings("unchecked")
    public Vector<V> findWithPredicate(Predicate predicate) {
        return switch (predicate.getOperation()) {
            case EQUALS -> find((K) predicate.getValue());
            case LESS_THAN -> findLessThan((K) predicate.getValue());
            case LESS_THAN_EQUAL -> findLessEquals((K) predicate.getValue());
            case GREATER_THAN -> findGreaterThan((K) predicate.getValue());
            case GREATER_THAN_EQUAL -> findGreaterEquals((K) predicate.getValue());
            case NOT_EQUALS -> findNotEquals((K) predicate.getValue());
        };
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
        LeafNode<K, V> lf = firstLeaf;
        Vector<V> allRecords = new Vector<>();

        loop: while (lf != null) {
            for (int i = 0; i < lf.getNumberOfLists(); i++) {
                ValueList<K, V> valueList = lf.getLists()[i];
                if (valueList.getKey().compareTo(key) >= 0)
                    break loop;
                allRecords.addAll(valueList);
            }
            lf = (LeafNode<K, V>)lf.getRightSibling();
        }
        return allRecords;
    }

    public Vector<V> findGreaterThan(K key) {
        Vector<V> result = new Vector<>();
        LeafNode<K, V> lf = this.root == null ? this.firstLeaf : findLeafNode(key);

        while (lf != null) {
            for (int i = 0; i < lf.getNumberOfLists(); i++) {
                ValueList<K, V> valueList = lf.getLists()[i];
                if (valueList.getKey().compareTo(key) > 0)
                    result.addAll(valueList);
            }
            lf = (LeafNode<K, V>) lf.getRightSibling();
        }

        return result;
    }

    public Vector<V> findGreaterEquals(K key) {
        Vector<V> result = findGreaterThan(key);
        ValueList<K, V> equalsResult = find(key);

        if (equalsResult != null)
            result.addAll(equalsResult);

        return result;
    }

    public Vector<V> findLessEquals(K key) {
        Vector<V> result = findLessThan(key);
        ValueList<K, V> equalsResult = find(key);

        if (equalsResult != null)
            result.addAll(equalsResult);
        return result;
    }

    @SuppressWarnings("unused")
    public void printDataInOrder() {
        if (this.root == null) {
            printLeafInOrder(this.firstLeaf);
        } else {
            printDataInOrder(this.root);
        }
    }

    @SuppressWarnings("unused")
    private void printBFS() {
        Queue<Node<K>> q = new LinkedList<>();
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
        var curr = (Node<K>)this.root;
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

    private void printDataInOrder(Node<K> node) {
        if (node == null)
            return;

        while (!(node instanceof LeafNode) && node != null) {
            node = node.getChildren()[0];
        }
        printLeafInOrder((LeafNode<K, V>) node);
    }

    private LeafNode<K, V> findLeafNode(K key) {
        return findLeafNode(this.root, key);
    }

    public boolean delete(K key) {
        if (this.isEmpty())
            return false;

        LeafNode<K, V> lf = this.root == null ? this.firstLeaf : findLeafNode(key);

        if (this.root == null) {
            boolean ret = lf.deleteByKey(key);
            if (lf.getNumberOfLists() == 0)
                this.firstLeaf = null;
            return ret;
        }
        InnerNode<K> indexNode = findInnerNode(key);

        if (!lf.deleteByKey(key))
            return false;

        if (!lf.isUnderFull()) {
            replaceWithSuccessor(indexNode, key);
            return true;
        }

        LeafNode<K, V> rightSib = (LeafNode<K, V>) lf.getRightSibling();
        LeafNode<K, V> leftSib = (LeafNode<K, V>) lf.getLeftSibling();
        InnerNode<K> par = lf.getParent();
        int lfIdx = par.indexOfPointer(lf);

        if (leftSib != null &&
            leftSib.canGiveToSibling() &&
            leftSib.getParent() == lf.getParent()) {
            ValueList<K, V> borrowedPair = leftSib.getLists()[leftSib.getNumberOfLists() - 1];
            lf.insert(borrowedPair);
            leftSib.deleteByIndex(leftSib.getNumberOfLists() - 1);
            if (par.getKeys()[lfIdx - 1].compareTo(borrowedPair.getKey()) > 0) {
                par.setKey(lfIdx - 1, borrowedPair.getKey());
            }
            replaceWithSuccessor(indexNode, key);
        } else if (rightSib != null &&
                   rightSib.canGiveToSibling() &&
                   rightSib.getParent() == lf.getParent()) {
            ValueList<K, V> borrowedPair = rightSib.getLists()[0];
            lf.insert(borrowedPair);
            rightSib.deleteByIndex(0);
            if (par.getKeys()[lfIdx].compareTo(borrowedPair.getKey()) <= 0) {
                par.setKey(lfIdx, rightSib.getLists()[0].getKey());
            }
            replaceWithSuccessor(indexNode, key);
        } else if (leftSib != null &&
                   leftSib.canBeMerged() &&
                   leftSib.getParent() == par) {
            leftSib.merge(lf);
            par.removePointer(lfIdx);
            par.removeKey(lfIdx - 1);

            leftSib.setRightSibling(lf.getRightSibling());
            if (leftSib.getRightSibling() != null)
                leftSib.getRightSibling().setLeftSibling(leftSib);

            replaceWithSuccessor(indexNode, key);

            if (par.isUnderFull())
                afterDeleteFix(par);

        } else if (rightSib != null &&
                   rightSib.canBeMerged() &&
                   rightSib.getParent() == par) {
            rightSib.merge(lf);

            par.removePointer(lfIdx);
            par.removeKey(lfIdx);

            rightSib.setLeftSibling(lf.getLeftSibling());

            if (rightSib.getLeftSibling() == null)
                this.firstLeaf = rightSib;
            else
                rightSib.getLeftSibling().setRightSibling(rightSib);

            replaceWithSuccessor(indexNode, key);


            if (par.isUnderFull())
                afterDeleteFix(par);
        }

        return true;
    }

    private void replaceWithSuccessor(InnerNode<K> indexNode, K key) {
        if (indexNode == null)
            return;

        int idx = Arrays.binarySearch(
                indexNode.getKeys(),
                0,
                indexNode.getNumberOfKeys(),
                key
        );

        if (idx < 0) return;

        K successorKey = getSuccessorKey(indexNode, idx);
        indexNode.setKey(idx, successorKey);
    }

    private K getSuccessorKey(Node<K> node, int keyIdx) {
        if (node == null) {
            return null;
        }

        if (node instanceof LeafNode) {
            return node.getKeys()[0];
        }

        Node<K> nextRightNode = node.getChildren()[keyIdx + 1];
        return getLeftMost(nextRightNode);
    }

    private K getLeftMost(Node<K> node) {
        Node<K> tmp = node;
        while (tmp instanceof InnerNode) {
            tmp = tmp.getChildren()[0];
        }
        return ((LeafNode<K, V>) tmp).getLists()[0].getKey();
    }

    private void afterDeleteFix(InnerNode<K> node) {
        InnerNode<K> leftSib = (InnerNode<K>) node.getLeftSibling();
        InnerNode<K> rightSib = (InnerNode<K>) node.getRightSibling();
        InnerNode<K> par = node.getParent();

        if (node == this.root) {
            if (node.getNumberOfKeys() > 0)
                return;
            if (node.getChildren()[0] instanceof InnerNode) {
                this.root = (InnerNode<K>) node.getChildren()[0];
                this.root.setParent(null);
                this.root.setLeftSibling(null);
                this.root.setRightSibling(null);
            } else if (node.getChildren()[0] instanceof LeafNode)
                this.root = null;
        } else if (leftSib != null && leftSib.canGiveToSibling() && leftSib.getParent() == par) {
            K borrowedKey = leftSib.getKeys()[leftSib.getNumberOfKeys() - 1];
            Node<K> borrowedPtr = leftSib.getChildren()[leftSib.getDegree() - 1];

            K parentKey = par.getKeys()[par.getNumberOfKeys() - 1];
            par.removeKey(par.getNumberOfKeys() - 1);
            node.addKey(parentKey);
            node.insertAt(borrowedPtr, 0);
            borrowedPtr.setParent(node);

            par.addKey(borrowedKey);
            leftSib.removeKey(leftSib.getNumberOfKeys() - 1);
            leftSib.removePointer(leftSib.getDegree() - 1);

        } else if (rightSib != null && rightSib.canGiveToSibling() && rightSib.getParent() == par) {
            K borrowedKey = rightSib.getKeys()[0];
            Node<K> borrowedPtr = rightSib.getChildren()[0];

            K parentKey = par.getKeys()[0];
            par.removeKey(0);
            node.addKey(parentKey);
            node.appendPointer(borrowedPtr);
            borrowedPtr.setParent(node);

            par.addKey(borrowedKey);
            rightSib.removeKey(0);
            rightSib.removePointer(0);

        } else if (leftSib != null && leftSib.canBeMerged() && leftSib.getParent() == par) {
            leftSib.addKey(par.getKeys()[par.getNumberOfKeys() - 1]);
            par.removeKey(par.getNumberOfKeys() - 1);
            par.removePointer(par.indexOfPointer(node));
            leftSib.merge(node);
            leftSib.setRightSibling(node.getRightSibling());
            if (leftSib.getRightSibling() != null)
                leftSib.getRightSibling().setLeftSibling(leftSib);
        } else if (rightSib != null && rightSib.canBeMerged() && rightSib.getParent() == par) {
            node.addKey(par.getKeys()[0]);
            par.removeKey(0);
            par.removePointer(par.indexOfPointer(rightSib));
            node.merge(rightSib);
            node.setRightSibling(rightSib.getRightSibling());
            if (node.getRightSibling() != null)
                node.getRightSibling().setLeftSibling(node);
        }

        if (par != null && par.isUnderFull())
            afterDeleteFix(par);
    }

    private InnerNode<K> findInnerNode(K key) {
        return findInnerNode(this.root, key);
    }

    private InnerNode<K> findInnerNode(InnerNode<K> node, K key) {
        if (node == null)
            return null;

        K[] keys = node.getKeys();

        for (int i = 0; i < node.getNumberOfKeys(); i++) {
            if (keys[i].equals(key)) {
                return node;
            }
        }

        if (node.getChildren()[0] instanceof LeafNode)
            return null;

        int idx = 0;
        for (; idx < node.getNumberOfKeys(); idx++) {
            if (keys[idx].compareTo(key) > 0)
                break;
        }

        return findInnerNode((InnerNode<K>) node.getChildren()[idx], key);
    }
}
