package RheaDB;

import org.w3c.dom.Attr;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RowRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<Object> attributeValues;
    private final List<Attribute> attributeList;

    private int pageId, rowId;

    RowRecord(List<Attribute> attributeList, List<Object> attributeValues) {
        this.attributeValues = attributeValues;
        this.attributeList = attributeList;
    }

    public List<Object> getAttributeValues() {
        return attributeValues;
    }

    public List<Attribute> getAttributeTypes() {
        return attributeList;
    }

    public void setAttributeValue(int idx, Object val) {
        this.attributeValues.set(idx, val);
    }

    public void setAttributeValues(Object[] values) {
        this.attributeValues = Arrays.stream(values).collect(Collectors.toList());
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
}
