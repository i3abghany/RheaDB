package RheaDB;

import java.util.HashSet;
import java.util.Vector;
import java.util.stream.Collectors;

public class QueryResult {
    private HashSet<RowRecord> rows = new HashSet<>();
    private Vector<Attribute> allAttributes = new Vector<>();
    private Vector<String> selectedAttributes = new Vector<>();

    QueryResult(Vector<String> attributeNames, Vector<AttributeType> types) {
        Vector<Attribute> artificialAttributes = new Vector<>();

        artificialAttributes.add(new Attribute(null, "Column", false));
        artificialAttributes.add(new Attribute(null, "Type", false));

        allAttributes = artificialAttributes;
        selectedAttributes.add("Column");
        selectedAttributes.add("Type");

        for (int i = 0; i < attributeNames.size(); i++) {
            Vector<Object> vals = new Vector<>();
            vals.add(attributeNames.get(i));
            vals.add(types.get(i));
            RowRecord r = new RowRecord(artificialAttributes, vals);
            rows.add(r);
        }
    }

    QueryResult(HashSet<RowRecord> rows, Vector<Attribute> attributes,
                Vector<String> selectedAttributes) {
        this.rows = rows;
        this.allAttributes = attributes;
        boolean starAttribute = selectedAttributes.contains("*");
        if (starAttribute) {
            this.selectedAttributes = attributes.stream()
                    .map(attr -> attr.getName())
                    .collect(Collectors.toCollection(Vector::new));
        } else {
            this.selectedAttributes = selectedAttributes;
        }
    }

    public HashSet<RowRecord> getRows() {
        return rows;
    }

    private Vector<Integer> getLengths() {
        Vector<Integer> lengths = new Vector<>();
        for (int i = 0; i < allAttributes.size(); i++) {
            lengths.add(-1);
            for (RowRecord rowRecord : rows) {
                String strVal = rowRecord.getValueOf(allAttributes.get(i)).toString();
                lengths.set(i, Math.max(lengths.get(i), strVal.length()));
            }
        }

        for (int i = 0; i < allAttributes.size(); i++) {
            Attribute attribute = allAttributes.get(i);
            String attributeName = attribute.getName();
            if (selectedAttributes.stream().anyMatch(s -> s.equals(attributeName))) {
                lengths.set(i, Math.max(lengths.get(i), attributeName.length()));
            }
        }

        return lengths;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Vector<Integer> lengths = getLengths();

        for (int i = 0; i < allAttributes.size(); i++) {
            Attribute attribute = allAttributes.get(i);
            String attributeName = attribute.getName();
            if (selectedAttributes.stream().anyMatch(s -> s.equals(attributeName))) {
                builder.append(String.format("%-" + lengths.get(i) + "s ", allAttributes.get(i).getName()));
            }
        }
        builder.append("\n");
        for (RowRecord rowRecord : rows) {
            Vector<Object> attributeValues = rowRecord.getAttributeValues();
            for (int i = 0; i < attributeValues.size(); i++) {
                int finalIndex = i;
                if (selectedAttributes.stream()
                        .anyMatch(s -> s.equals(allAttributes.get(finalIndex).getName()))) {
                    Object attributeVal = attributeValues.get(i);
                    builder.append(String.format("%-" + lengths.get(i) + "s ", attributeVal));
                }
            }
            builder.append("\n");
        }
        return builder.toString();
    }
}
