import java.util.Vector;

public class ValueList<K extends Comparable<K>, V> extends Vector<V> implements Comparable<ValueList<K, V>> {
    private final K key;
    public ValueList(K key, V initialValue) {
        this.key = key;
        this.add(initialValue);
    }

    public V getOneValue() {
        return this.elementAt(0);
    }

    public K getKey() {
        return key;
    }

    @Override
    public int compareTo(ValueList<K, V> o2) {
        return this.key.compareTo(o2.getKey());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.key);
        builder.append(": ");
        builder.append("(");
        for (int i = 0; i < this.size(); i++) {
            V v = this.get(i);
            builder.append(v);

            if (i != this.size() - 1)
                builder.append(", ");
        }
        return builder.append(")").toString();
    }
}
