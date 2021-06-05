package RheaDB;

import java.io.*;
import java.util.Vector;

public class Page implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int maxTuples;
    private final int pageIdx;
    private final Vector<RowRecord> records;

    public Page(int maxTuples, int pageIdx) {
        this.maxTuples = maxTuples;
        this.pageIdx = pageIdx;
        this.records = new Vector<>();
    }

    public boolean addRecord(RowRecord record) {
        if (this.records.size() == maxTuples)
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

    public boolean hasFreeSpace() {
        return this.records.size() < this.maxTuples;
    }

    public int getLastRowIndex() {
        return this.records.size();
    }
}
