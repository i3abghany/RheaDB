package RheaDB;

import Predicate.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RheaDB {
    static final int maxTuplesPerPage = 32;

    private final String rootDirectory = "." + File.separator + "data";
    private final HashMap<String, Table> createdTables;

    public RheaDB() throws IOException {
        File file = new File(rootDirectory + File.separator + "metadata.db");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            boolean fileCreated = file.createNewFile();
            if (!fileCreated) {
                System.out.println("Could not instantiate a metadata file... Exiting.");
                System.exit(1);
            }
            createdTables = new HashMap<>();
        }
        else
            createdTables = DiskManager.readMetadata();
    }

    public boolean createTable(String tableName, List<Attribute> attributeList) {
        if (createdTables.containsKey(tableName))
            return false;

        String pageDirectory = this.rootDirectory + File.separator + tableName;
        Table newTable = new Table(tableName, attributeList, pageDirectory, maxTuplesPerPage);
        createdTables.put(tableName, newTable);

        return true;
    }

    public QueryResult selectFrom(String tableName, List<Predicate> predicateList) {
        Table table = createdTables.get(tableName);
        Vector<RowRecord> result = new Vector<>();
        for (int i = 1; i <= table.getNumPages(); i++) {
            Page page = DiskManager.getPage(table, i);
            page.getRecords().forEach(
                (r) -> {
                    boolean ret = true;
                    for (Predicate p : predicateList) {
                        Attribute attribute = p.getAttribute();
                        ret &= p.doesSatisfy(r.getValueOf(attribute));
                    }
                    if (ret) result.add(r);
                }
            );
        }
        return new QueryResult(result, table.getAttributeList());
    }

    public void insertInto(String tableName, RowRecord record) {
        Table table = createdTables.get(tableName);
        Page lastPage = DiskManager.getPage(table, table.getNumPages());
        if (lastPage == null)
            lastPage = table.getNewPage();

        if (!lastPage.hasFreeSpace()) {
            lastPage = table.getNewPage();
        }
        record.setPageId(lastPage.getPageIdx());
        record.setRowId(lastPage.getLastRowIndex());
        lastPage.addRecord(record);
        DiskManager.savePage(table, lastPage);
    }

    private Table getTable(String name) {
        return createdTables.get(name);
    }

    private void saveMetadata() {
        DiskManager.saveMetadata(createdTables);
    }

    public static void main(String[] args) throws IOException {
        RheaDB rheaDB = new RheaDB();

        List<Attribute> attributeList = new ArrayList<>();
        attributeList.add(new Attribute(AttributeType.INT, "ID", true, -1));
        attributeList.add(new Attribute(AttributeType.STRING, "name", false, 80));
        attributeList.add(new Attribute(AttributeType.INT, "age", false, -1));
        if (!rheaDB.createTable("MyTable", attributeList)) {
            System.out.println("RheaDB.Table already Exists... Exiting");
            System.exit(1);
        }
        for (int i = 0; i < 33; i++) {
            Object[] objects = {i, "A", 42};
            rheaDB.insertInto("MyTable", new RowRecord(attributeList, Arrays.asList(objects)));
        }

        List<Predicate> predicateList = new ArrayList<>();
        predicateList.add(new EqualsPredicate(new Attribute(AttributeType.INT, "ID", true, -1), 10));
        var res = rheaDB.selectFrom("MyTable", predicateList);
        res.print();

        rheaDB.saveMetadata();
    }
}
