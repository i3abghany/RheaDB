package RheaDB;

import BPlusTree.BPlusTree;
import Predicate.Predicate;
import QueryProcessor.DDLStatement;
import QueryProcessor.DDLStatement.*;
import QueryProcessor.DMLStatement;
import QueryProcessor.Parser;
import QueryProcessor.SQLStatement;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RheaDB {
    static final int maxTuplesPerPage = 32;

    private final String rootDirectory = "." + File.separator + "data";
    private final HashMap<String, Table> createdTables;

    public void run() {
        String statementStr;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            statementStr = scanner.nextLine();

            if (statementStr.equals("@exit"))
                return;

            SQLStatement sqlStatement = new Parser(statementStr).parse();
            if (sqlStatement == null)
                System.out.println("Error parsing the statement.");

            executeStatement(sqlStatement);
            if (sqlStatement.getKind() == SQLStatement.SQLStatementKind.DDL)
                saveMetadata();
        }
    }

    private QueryResult executeStatement(SQLStatement sqlStatement) {
        if (sqlStatement.getKind() == SQLStatement.SQLStatementKind.DDL) {
            if (((DDLStatement) sqlStatement).getDDLKind() ==
                    DDLStatement.DDLKind.CreateTable) {
                CreateTableStatement statement =
                        (CreateTableStatement) sqlStatement;
                boolean wasCreated = createTable(statement.getTableName(),
                        statement.getAttributeVector());
            } else {
                CreateIndexStatement statement =
                        (CreateIndexStatement) sqlStatement;
                boolean indexCreated = createIndex(statement.getTableName(),
                        statement.getIndexAttribute());
            }
        } else {
            DMLStatement dmlStatement = (DMLStatement) sqlStatement;
            if (dmlStatement.getDMLKind() == DMLStatement.DMLStatementKind.SELECT) {
                return selectFrom((DMLStatement.SelectStatement) dmlStatement);
            }
        }

        return null;
    }

    public RheaDB() throws IOException {
        File file = new File(rootDirectory + File.separator +
                "metadata.db");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            boolean fileCreated = file.createNewFile();
            if (!fileCreated) {
                System.out.println("Could not instantiate a metadata file... " +
                        "Exiting.");
                System.exit(1);
            }
            createdTables = new HashMap<>();
        } else
            createdTables = DiskManager.readMetadata();
    }

    public boolean createTable(String tableName,
                               List<Attribute> attributeList) {
        if (createdTables.containsKey(tableName))
            return false;

        String pageDirectory = this.rootDirectory + File.separator + tableName;
        Table newTable = new Table(tableName, attributeList, pageDirectory,
                maxTuplesPerPage);
        createdTables.put(tableName, newTable);

        return true;
    }

    public QueryResult selectFrom(DMLStatement.SelectStatement selectStatement) {
        Table table = createdTables.get(selectStatement.getTableName());
        Vector<RowRecord> result = new Vector<>();

        for (int i = 1; i <= table.getNumPages(); i++) {
            Page page = DiskManager.getPage(table, i);
            page.getRecords().forEach(
                    (r) -> {
                        boolean ret = true;
                        for (Predicate p : selectStatement.getPredicates()) {
                            Attribute attribute = table
                                    .getAttributeWithName(p.getAttributeName());
                            p.setAttribute(attribute);
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

        if (!lastPage.hasFreeSpace())
            lastPage = table.getNewPage();

        record.setPageId(lastPage.getPageIdx());
        record.setRowId(lastPage.getLastRowIndex());
        lastPage.addRecord(record);
        DiskManager.savePage(table, lastPage);

        for (Attribute attribute : table.getAttributeList()) {
            if (attribute.getIsIndexed())
                updateIndex(tableName, attribute.getName());
        }
    }

    public boolean createIndex(String tableName, String attributeName) {
        Table table = getTable(tableName);
        if (table == null)
            return false;

        Attribute attribute = table.getAttributeWithName(attributeName);
        if (attribute == null)
            return false;

        BPlusTree bPlusTree;
        AttributeType type = attribute.getType();

        switch (type) {
            case INT -> bPlusTree = new BPlusTree<Integer, RowRecord>();
            case STRING -> bPlusTree = new BPlusTree<String, RowRecord>();
            case FLOAT -> bPlusTree = new BPlusTree<Float, RowRecord>();
            default -> bPlusTree = null;
        }

        if (bPlusTree == null)
            return false;

        for (int i = 1; i <= table.getNumPages(); i++) {
            Page page = DiskManager.getPage(table, i);
            page.getRecords().forEach(
                r -> bPlusTree.insert((Comparable) r.getValueOf(attribute), r)
            );
        }

        DiskManager.saveIndex(table.getPageDirectory() + File.separator +
                "index" + File.separator + attributeName + ".idx", bPlusTree);

        attribute.setIsIndexed(true);
        return true;
    }

    public void updateIndex(String tableName, String attributeName) {
        Table table = getTable(tableName);
        DiskManager.deleteIndex(table.getPageDirectory() + File.separator +
                "index" + File.separator + attributeName + ".idx");
        createIndex(tableName, attributeName);
    }

    private Table getTable(String name) {
        return createdTables.get(name);
    }

    private void saveMetadata() {
        DiskManager.saveMetadata(createdTables);
    }

    public static void main(String[] args) throws IOException {
        RheaDB rheaDB = new RheaDB();
        rheaDB.run();
    }
}
