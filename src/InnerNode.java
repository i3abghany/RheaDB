public class InnerNode<K extends Comparable<K>> extends Node<K> {
    private int minKeys;
    private int degree;

    private Node<K>[] children;

    @SuppressWarnings("unchecked")
    public InnerNode(int order, K[] keys) {
        super(order, keys);
        minKeys = (int) Math.ceil(this.order / 2.0);
        this.children = new Node[this.order + 1];
        this.degree = 0;
        this.validateNumOfKeys();
    }

    @Override
    public K[] getKeys() {
        return this.keys;
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

    public int getNumberOfKeys() {
        return numberOfKeys;
    }

    @Override
    public Node<K>[] getChildren() {
        return children;
    }

    public void addPointer(Node<K> lf) {
        children[this.degree] = lf;
        this.degree++;
    }

    public void addKey(K newKey) {
        this.keys[this.degree - 1] = newKey;
        this.sortKeys();
        this.validateNumOfKeys();
    }

    public int getDegree() {
        return degree;
    }

    public int indexOf(Node<K> nd) {
        for (int i = 0; i < children.length; i++) {
            if (nd == children[i])
                return i;
        }

        return -1;
    }

    public void insertAt(Node<K> newNode, int idx) {
        assert this.degree - idx >= 0;
        if (this.degree - idx >= 0)
            System.arraycopy(children, idx, children, idx + 1, this.degree - idx);

        this.children[idx] = newNode;
        this.degree++;
    }

    public boolean isOverFull() {
        return this.degree == this.order + 1;
    }

    public Node<K>[] splitPointers(int midPoint) {
        Node<K>[] retArr = new Node[this.order + 1];

        for (int i = midPoint + 1; i < this.children.length; i++) {
            retArr[i - midPoint - 1] = this.children[i];
            this.children[i] = null;
        }
        this.validateDegree();
        return retArr;
    }

    public void validateDegree() {
        this.degree = 0;
        for (int i = 0; i < this.children.length; i++) {
            if (this.children[i] == null) {
                this.degree = i;
                return;
            }
        }

        assert false;
    }

    @Override
    public Node<K> getRightSibling() {
        return rightSibling;
    }

    @Override
    public void setRightSibling(Node<K> rightSibling) {
        this.rightSibling = (InnerNode<K>) rightSibling;
    }

    @Override
    public Node<K> getLeftSibling() {
        return leftSibling;
    }

    @Override
    public void setLeftSibling(Node<K> leftSibling) {
        assert leftSibling instanceof InnerNode;
        this.leftSibling = (InnerNode<K>) leftSibling;
    }
}
