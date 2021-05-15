public class Pair<K extends Comparable<K>, V extends Comparable<V>> implements Comparable<Pair<K, V>> {
    private K key;
    private V val;

    public Pair(K k, V v) {
        this.key = k;
        this.val = v;
    }

    public K getKey() {
        return this.key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getVal() {
        return val;
    }

    public void setVal(V val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return "{" +
                this.key +
                ", " + this.val +
                '}';
    }

    @Override
    public int compareTo(Pair<K, V> o) {
        return this.key.compareTo(o.key);
    }
}
