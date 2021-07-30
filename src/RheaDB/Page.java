package RheaDB;

import java.io.*;
import java.util.Objects;
import java.util.Vector;

public class Page implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int maxRows;
    private final int pageIdx;
    private final Vector<RowRecord> records;
    private final String tableName;

    public Page(String tableName, int maxRows, int pageIdx) {
        this.tableName = tableName;
        this.maxRows = maxRows;
        this.pageIdx = pageIdx;
        this.records = new Vector<>();
    }

    public int getNumberOfRows() {
        return records.size();
    }

    public boolean addRecord(RowRecord record) {
        if (isFull())
            return false;

        this.records.add(record);
        return true;
    }

    public Vector<RowRecord> getRecords() {
        return this.records;
    }

    public int getPageIdx() {
        return pageIdx;
    }

    public boolean isFull() {
        return this.records.size() == this.maxRows;
    }

    public boolean isEmpty() {
        return this.records.size() == 0;
    }

    public int getLastRowIndex() {
        return this.records.size();
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return maxRows == page.maxRows &&
               pageIdx == page.pageIdx &&
               tableName.equals(page.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxRows, pageIdx, tableName);
    }

    public int getMaxRows() {
        return maxRows;
    }

    public RowRecord popRow() {
        if (records.isEmpty()) {
            return null;
        }

        return records.remove(records.size() - 1);
    }
}
