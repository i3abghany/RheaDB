package RheaDB;

import BPlusTree.BPlusTree;
import Predicate.Predicate;
import QueryProcessor.DDLStatement;
import QueryProcessor.DMLStatement;
import QueryProcessor.DDLStatement.*;
import QueryProcessor.DMLStatement.*;
import QueryProcessor.Parser;
import QueryProcessor.SQLStatement;
import RheaDB.StorageManagement.BufferPool;
import RheaDB.StorageManagement.DiskManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RheaDB {
    static final int maxTuplesPerPage = 32;

    private final String rootDirectory = "." + File.separator + "data";
    private final HashMap<String, Table> createdTables;

    private final BufferPool bufferPool;

    public void run() {
        String statementStr;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            statementStr = scanner.nextLine();

            if (statementStr.equals("@exit"))
                return;

            if (statementStr.isEmpty())
                continue;

            SQLStatement sqlStatement;
            QueryResult queryResult;

            try {
                sqlStatement = new Parser(statementStr).parse();
                queryResult = executeStatement(sqlStatement);
            } catch (SQLException sqlException) {
                System.out.println(sqlException.getMessage());
                continue;
            }

            if (queryResult != null) {
                queryResult.print();
            }

            saveMetadata();
        }
    }

    private QueryResult executeStatement(SQLStatement sqlStatement) throws SQLException {
        if (sqlStatement.getKind() == SQLStatement.SQLStatementKind.DDL) {
            if (((DDLStatement) sqlStatement).getDDLKind() ==
                    DDLKind.CreateTable) {
                CreateTableStatement statement =
                        (CreateTableStatement) sqlStatement;
                boolean wasCreated = createTable(statement.getTableName(),
                        statement.getAttributeVector());
                if (!wasCreated) {
                    throw new SQLException("Could not create the table. Table already exists.");
                }
            } else {
                CreateIndexStatement statement =
                        (CreateIndexStatement) sqlStatement;
                Table table = getTable(statement.getTableName());
                String indexAttributeName = statement.getIndexAttribute();
                Attribute indexAttribute =
                        table.getAttributeWithName(indexAttributeName);
                if (table.getName() == null) {
                    throw new SQLException("Name " + statement.getTableName() +
                            " Does not resolve to a table.");
                }

                if (indexAttribute.getIsIndexed()) {
                    throw new SQLException("There already exists an index for the" +
                            "provided attribute.");
                }

                boolean indexCreated = createIndex(table, indexAttribute);
                if (!indexCreated) {
                    throw new SQLException("Could not create the index.");
                }
            }
        } else if (sqlStatement.getKind() == SQLStatement.SQLStatementKind.DML) {
            DMLStatement dmlStatement = (DMLStatement) sqlStatement;
            if (dmlStatement.getDMLKind() == DMLStatementKind.SELECT) {
                return selectFrom((SelectStatement) dmlStatement);
            } else if (dmlStatement.getDMLKind() == DMLStatementKind.INSERT) {
                insertInto((InsertStatement) dmlStatement);
            } else {
                throw new SQLException("Could not identify and parse the statement");
            }
        } else {
            throw new SQLException("Could not identify and parse the statement");
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
        bufferPool = new BufferPool();
    }

    public boolean createTable(String tableName, Vector<Attribute> attributeList) {
        if (createdTables.containsKey(tableName))
            return false;

        String pageDirectory = this.rootDirectory + File.separator + tableName;
        Table newTable = new Table(tableName, attributeList, pageDirectory,
                maxTuplesPerPage);
        createdTables.put(tableName, newTable);

        return true;
    }
    @SuppressWarnings("unchecked")
    public QueryResult selectFrom(SelectStatement selectStatement) throws SQLException {
        Table table = getTable(selectStatement.getTableName());
        Vector<String> selectedAttributes = selectStatement.getSelectedAttributes();
        if (table == null) {
            throw new SQLException("The name " + selectStatement.getTableName()
                    + " does not resolve to a table in the database.");
        }
        HashSet<RowRecord> result = new HashSet<>();
        Vector<Predicate> predicates = selectStatement.getPredicates();

        verifySelectedAttributesExist(table, selectedAttributes);

        if (predicates.isEmpty()) {
            return getAllRows(table, selectedAttributes);
        }
        for (Predicate predicate : predicates) {
            Attribute attribute = table.getAttributeWithName(predicate.getAttributeName());
            if (attribute == null) {
                System.out.println("Invalid attribute: " + predicate.getAttributeName());
                System.exit(1);
            }
            predicate.setAttribute(attribute);
            if (attribute.getIsIndexed()) {
                BPlusTree indexTree = bufferPool.getIndex(table, attribute);
                if (indexTree == null) {
                    System.out.println("Could not read the index file for attribute: " + attribute.getName());
                    System.exit(1);
                }
                result.addAll(indexTree.findWithPredicate(predicate));
            }
        }

        predicates = predicates.stream().filter(
            p -> !table.getAttributeWithName(p.getAttributeName()).getIsIndexed()
        ).collect(Collectors.toCollection(Vector::new));
        final Vector<Predicate> finalPredicates = predicates;

        for (int i = 1; i <= table.getNumPages(); i++) {
            Page page = bufferPool.getPage(table, i);
            page.getRecords().forEach(
                    (r) -> {
                        boolean ret = false;
                        for (Predicate p : finalPredicates) {
                            Attribute attribute = table
                                    .getAttributeWithName(p.getAttributeName());
                            ret |= p.doesSatisfy(r.getValueOf(attribute));
                        }
                        if (ret) result.add(r);
                    }
            );
        }
        return result.size() == 0 ? null :
            new QueryResult(result, table.getAttributeList(),
                selectedAttributes);
    }

    private void verifySelectedAttributesExist(Table table, Vector<String> selectedAttributes) throws SQLException {
        for (String attributeName : selectedAttributes) {
            if (table.getAttributeWithName(attributeName) == null) {
                throw new SQLException("Invalid attribute name " + attributeName);
            }
        }
    }

    private QueryResult getAllRows(Table table, Vector<String> selectedAttributes) {
        HashSet<RowRecord> result = new HashSet<>();
        for (int i = 1; i <= table.getNumPages(); i++) {
            Page page = bufferPool.getPage(table, i);
            result.addAll(page.getRecords());
        }

        return result.size() == 0 ? null :
            new QueryResult(result, table.getAttributeList(), selectedAttributes);
    }

    public void insertInto(InsertStatement insertStatement) throws SQLException {
        String tableName = insertStatement.getTableName();
        Table table = getTable(tableName);

        if (table == null) {
            throw new SQLException("The name\"" + tableName + "\" does not resolve" +
                    "to a valid table.");
        }

        Vector<Attribute> attributes = table.getAttributeList();
        Vector<Object> values = insertStatement.getValues();

        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.elementAt(i);
            Object value = values.elementAt(i);
            Object testObject;
            try {
                switch (attribute.getType()) {
                    case INT: testObject = (int) value; break;
                    case FLOAT: testObject = (float) value; break;
                    case STRING: testObject = (String) value; break;
                    default: assert false;
                }
            } catch (Exception e) {
                throw new SQLException("Invalid value(" + value + ") provided " +
                        "for attribute: " + attribute.getName());
            }
        }
        RowRecord rowRecord = new RowRecord(attributes, values);
        insertInto(table, rowRecord);
    }

    private void insertInto(Table table, RowRecord record) {
        Page lastPage = bufferPool.getPage(table, table.getNumPages());
        if (lastPage == null)
            lastPage = table.getNewPage();

        if (!lastPage.hasFreeSpace())
            lastPage = table.getNewPage();

        record.setPageId(lastPage.getPageIdx());
        record.setRowId(lastPage.getLastRowIndex());
        lastPage.addRecord(record);
        bufferPool.savePage(table, lastPage);

        for (Attribute attribute : table.getAttributeList()) {
            if (attribute.getIsIndexed())
                updateIndex(table, attribute);
        }
    }

    @SuppressWarnings({"unchecked", "SwitchLabeledRuleCanBeCodeBlock"})
    public boolean createIndex(Table table, Attribute attribute) {
        if (table == null)
            return false;

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
            Page page = bufferPool.getPage(table, i);
            page.getRecords().forEach(
                r -> bPlusTree.insert((Comparable) r.getValueOf(attribute), r)
            );
        }

        bufferPool.saveIndex(table, attribute, bPlusTree);

        attribute.setIsIndexed(true);
        return true;
    }

    public void updateIndex(Table table, Attribute attribute) {
        bufferPool.deleteIndex(table, attribute);
        createIndex(table, attribute);
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
