package RheaDB;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

public class RowRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Vector<Object> attributeValues;
    private final Vector<Attribute> attributeList;

    private int pageId, rowId;

    RowRecord(Vector<Attribute> attributeList, Vector<Object> attributeValues) {
        this.attributeValues = attributeValues;
        this.attributeList = attributeList;
    }

    public Vector<Object> getAttributeValues() {
        return attributeValues;
    }

    public Vector<Attribute> getAttributes() {
        return attributeList;
    }

    public void setAttributeValue(int idx, Object val) {
        this.attributeValues.set(idx, val);
    }

    public void setAttributeValue(Attribute attribute, Object val) {
        for (int i = 0; i < attributeList.size(); i++) {
            if (attributeList.get(i).equals(attribute)) {
                attributeValues.set(i, val);
                break;
            }
        }
    }

    public void setAttributeValues(Object[] values) {
        this.attributeValues = Arrays.stream(values)
                .collect(Collectors.toCollection(Vector::new));
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public Object getValueOf(Attribute attribute) {
        for (int i = 0; i < attributeList.size(); i++) {
            if (attribute.equals(attributeList.get(i)))
                return attributeValues.get(i);
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Object o : this.attributeValues)
            builder.append(o).append(", ");
        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj.getClass() != this.getClass())
            return false;

        final RowRecord other = (RowRecord) obj;

        if (this.pageId != other.pageId || this.rowId != other.rowId)
            return false;

        if (this.attributeValues.size() != other.attributeValues.size() ||
                this.attributeList.size() != other.attributeList.size())
            return false;

        for (int i = 0; i < attributeList.size(); i++) {
            if (!this.attributeList.get(i).equals(other.attributeList.get(i)))
                return false;
        }

        for (int i = 0; i < attributeValues.size(); i++) {
            if (!this.attributeValues.get(i).equals(other.attributeValues.get(i)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId, rowId);
    }
}
