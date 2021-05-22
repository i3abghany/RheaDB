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
                String strVal = rowRecord.getValueOf(attributes.get(i)).toString();
                lengths.set(i, Math.max(lengths.get(i), strVal.length()));
            }
        }

        for (int i = 0; i < attributes.size(); i++) {
            String attributeName = attributes.get(i).getName();
            lengths.set(i, Math.max(lengths.get(i), attributeName.length()));
        }

        for (int i = 0; i < attributes.size(); i++) {
            System.out.printf("%-" + lengths.get(i) + "s ", attributes.get(i).getName());
        }
        System.out.println();
        for (RowRecord rowRecord : rows) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < rowRecord.getAttributeValues().size(); i++) {
                Object attributeVal = rowRecord.getAttributeValues().get(i);
                builder.append(String.format("%-" + lengths.get(i) + "s ", attributeVal));
            }
            System.out.println(builder);
        }
    }
}
