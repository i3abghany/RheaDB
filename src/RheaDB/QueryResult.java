package RheaDB;

import java.util.List;
import java.util.Vector;

public class QueryResult {
    private final Vector<RowRecord> rows;
    private final List<Attribute> attributes;

    QueryResult(Vector<RowRecord> rows, List<Attribute> attributes) {
        this.rows = rows;
        this.attributes = attributes;
    }

    public void print() {
        Vector<Integer> lengths = new Vector<>();

        for (int i = 0; i < attributes.size(); i++) {
            lengths.add(-1);
            for (RowRecord rowRecord : rows) {
                lengths.set(i, Math.max(lengths.get(i), rowRecord.getValueOf(attributes.get(i)).toString().length()));
            }
        }

        for (int i = 0; i < attributes.size(); i++) {
            lengths.set(i, Math.max(lengths.get(i), attributes.get(i).getName().length()));
        }

        for (int i = 0; i < attributes.size(); i++) {
            System.out.printf("%-" + lengths.get(i) + "s ", attributes.get(i).getName());
        }
        System.out.println();
        for (RowRecord rowRecord : rows) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < rowRecord.getAttributeValues().size(); i++)
                builder.append(String.format("%-" + lengths.get(i) +"s ", rowRecord.getAttributeValues().get(i)));
            System.out.println(builder);
        }
    }
}
