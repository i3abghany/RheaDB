import java.security.InvalidKeyException;
import java.util.Arrays;

public class BPlusTree<K extends Comparable<K>, V extends Comparable<V>> {
    private InnerNode<K> root;
    private LeafNode<K, V> firstLeaf;
    private final int order;

    private static final int DEFAULT_ORDER = 16;

    public BPlusTree() {
        this(DEFAULT_ORDER);
    }

    private BPlusTree(int order) {
        this.order = order;
        this.root = null;
    }

    public boolean isEmpty() {
        return this.firstLeaf == null;
    }

    @SuppressWarnings("unchecked")
    public void insert(K key, V val) {
        if (isEmpty()) {
            this.firstLeaf = new LeafNode<>(this.order);
            this.firstLeaf.insert(new Pair<>(key, val));
        } else {
            LeafNode<K, V> lf = this.root == null ? this.firstLeaf : findLeafNode(key);
            if (lf.isFull()) {
                Pair<K, V>[] lfPairs = lf.getPairs();
                lfPairs[lf.getNumberOfPairs()] = new Pair<>(key, val);
                lf.setNumberOfPairs(lf.getNumberOfPairs() + 1);
                sortPairs(lfPairs);

                int mid = getMidPoint();
                Pair<K, V>[] rightHalf = splitPairs(lf, mid);

                if (lf.getParent() == null) {
                    K[] parentKeys = (K[]) new Comparable[this.order];
                    parentKeys[0] = rightHalf[0].getKey();
                    InnerNode<K> parentNode = new InnerNode<>(this.order, parentKeys);

                    lf.setParent(parentNode);
                    parentNode.addPointer(lf);
                } else {
                    K parentNewKey = rightHalf[0].getKey();
                    lf.getParent().addKey(parentNewKey);
                }

                LeafNode<K, V> newNode = new LeafNode<>(this.order, lf.getParent());
                newNode.setPairs(rightHalf, this.order - mid);

                int childIdx = lf.getParent().indexOf(lf);
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
                lf.insert(new Pair<>(key, val));
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

        K parentNewKey = node.keys[midPoint];

        K[] halfKeys = node.splitKeys(midPoint);
        Node<K>[] halfPointers = node.splitPointers(midPoint);

        InnerNode<K> sib = new InnerNode<>(this.order, halfKeys, halfPointers);

        for (var ch : halfPointers) {
            if (ch != null) ch.setParent(sib);
        }

        correctSiblings(node, sib);

        if (parent != null) {
            parent.addKey(parentNewKey);
            int childIdx = parent.indexOf(node);
            parent.insertAt(sib, childIdx + 1);
            sib.setParent(parent);
        } else {
            K[] keys = (K[])new Comparable[this.order];
            keys[0] = parentNewKey;
            InnerNode<K> newParent = new InnerNode<>(this.order, keys);

            newParent.addPointer(node);
            newParent.addPointer(sib);

            node.setParent(newParent);
            sib.setParent(newParent);

            this.root = newParent;
        }
    }

    private Pair<K, V>[] splitPairs(LeafNode<K, V> lf, int mid) {
        return lf.splitPairs(mid);
    }

    private void sortPairs(Pair<K, V>[] pairsArr) {
        Arrays.sort(pairsArr, (o1, o2) -> {
            if (o1 == null && o2 == null) { return 0; }
            if (o1 == null) { return 1; }
            if (o2 == null) { return -1; }
            return o1.compareTo(o2);
        });
    }

    private int getMidPoint() {
        return (int) Math.ceil((this.order + 1) / 2.0) - 1;
    }

    private LeafNode<K, V> findLeafNode(Node<K> nod, K key) {
        if (nod == null)
            return null;

        K[] ks = nod.getKeys();

        int idx = 0;
        for (; idx < ((InnerNode<K>)nod).getDegree() - 1; idx++) {
            if (key.compareTo(ks[idx]) < 0)
                break;
        }

        Node<K> child = nod.getChildren()[idx];
        if (child instanceof LeafNode) {
            return (LeafNode<K, V>) child;
        } else {
            return findLeafNode(child, key);
        }
    }

    public V find(K key) throws InvalidKeyException {
        LeafNode<K, V> lf = findLeafNode(key);
        if (lf == null) {
            throw new InvalidKeyException("Key: " + key + " does not exist in the index.");
        }

        Pair<K, V>[] pairs = lf.getPairs();
        for (int i = 0; i < lf.getNumberOfPairs(); i++) {
            K pKey = pairs[i].getKey();
            if (pKey.equals(key))
                return pairs[i].getVal();
        }
        throw new InvalidKeyException("Key: " + key + " does not exist in the index.");
    }

    public void printDataInOrder() {
        if (this.root == null) {
            printLeafInOrder(this.firstLeaf);
        } else {
            printDataInOrder(this.root);
        }
    }

    private void printLeafInOrder(LeafNode<K, V> leaf) {
        LeafNode<K, V> lf = leaf;
        while (lf != null) {
            for (Pair<K, V> el : lf.getPairs()) {
                if (el != null) System.out.println(el.getKey() + " ");
            }
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
}

