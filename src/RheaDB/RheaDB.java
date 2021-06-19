package RheaDB;

import BPlusTree.BPlusTree;
import Predicate.Predicate;
import QueryProcessor.*;
import QueryProcessor.DDLStatement.*;
import QueryProcessor.DMLStatement.*;
import QueryProcessor.InternalStatement.*;
import RheaDB.StorageManagement.BufferPool;
import RheaDB.StorageManagement.DiskManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RheaDB {
    static final int maxTuplesPerPage = 32;

    private boolean lazyCommit;
    private final String rootDirectory;
    private final HashMap<String, Table> createdTables;

    private final BufferPool bufferPool;

    public void commitOnExit() {
        bufferPool.commitAllPages();
    }

    public void run() {
        String statementStr;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            statementStr = scanner.nextLine();

            if (statementStr.equals("@exit")) {
                if (lazyCommit) {
                    commitOnExit();
                }
                return;
            }

            if (statementStr.isEmpty())
                continue;

            QueryResult queryResult = executeStatement(statementStr);

            if (queryResult != null) {
                System.out.println(queryResult);
            }

            saveMetadata();
        }
    }

    public QueryResult executeStatement(String sql) {
        SQLStatement sqlStatement;
        QueryResult queryResult = null;

        try {
            sqlStatement = new Parser(sql).parse();
            if (sqlStatement != null)
                queryResult = executeStatement(sqlStatement);
        } catch (DBError DBError) {
            System.out.println(DBError.getMessage());
        }

        return queryResult;
    }

    public QueryResult executeDML(DMLStatement dmlStatement) throws DBError {
        switch (dmlStatement.getDMLKind()) {
            case SELECT: return selectFrom((SelectStatement) dmlStatement);
            case INSERT: insertInto((InsertStatement) dmlStatement); return null;
            case DELETE: deleteFrom((DeleteStatement) dmlStatement); return null;
            default: throw new DBError("Could not identify and parse the statement");
        }
    }

    public void executeCreateTable(CreateTableStatement statement) throws DBError {
        boolean wasCreated = createTable(statement.getTableName(),
                statement.getAttributeVector());
        if (!wasCreated) {
            throw new DBError("Could not create the table. Table already exists.");
        }
    }

    public void executeCreateIndex(CreateIndexStatement statement) throws DBError {
        Table table = getTable(statement.getTableName());
        String indexAttributeName = statement.getIndexAttribute();
        Attribute indexAttribute = table.getAttributeWithName(indexAttributeName);

        if (table.getName() == null) {
            throw new DBError("Name " + statement.getTableName() +
                    " Does not resolve to a table.");
        }

        if (indexAttribute == null) {
            throw new DBError("Invalid attribute: \"" + statement.getIndexAttribute() + "\"");
        }

        if (indexAttribute.getIsIndexed()) {
            return;
        }

        boolean indexCreated = createIndex(table, indexAttribute);
        if (!indexCreated) {
            throw new DBError("Could not create the index.");
        }
    }

    public QueryResult executeDDL(DDLStatement ddlStatement) throws DBError {
        switch (ddlStatement.getDDLKind()) {
            case CreateTable -> executeCreateTable((CreateTableStatement) ddlStatement);
            case CreateIndex -> executeCreateIndex((CreateIndexStatement) ddlStatement);
        }
        return null;
    }

    public QueryResult executeStatement(SQLStatement sqlStatement) throws DBError {
        return switch (sqlStatement.getKind()) {
            case DDL -> executeDDL((DDLStatement) sqlStatement);
            case DML -> executeDML((DMLStatement) sqlStatement);
            case INTERNAL -> executeInternal((InternalStatement) sqlStatement);
        };
    }

    private QueryResult executeInternal(InternalStatement sqlStatement) throws DBError {
        if (sqlStatement.getInternalStatementKind() == InternalStatementKind.DESCRIBE) {
            return describeTable((DescribeStatement) sqlStatement);
        }
        return null;
    }

    private QueryResult describeTable(DescribeStatement sqlStatement) throws DBError {
        String tableName = sqlStatement.getTableName();
        Table table = getTable(tableName);
        if (table == null) {
            throw new DBError("The name " + tableName
                    + " does not resolve to a table in the database.");
        }

        Vector<String> attributeNames = table.getAttributeList()
                .stream()
                .map(attr -> attr.getName())
                .collect(Collectors.toCollection(Vector::new));

        Vector<AttributeType> attributeTypes = table.getAttributeList()
                .stream()
                .map(attr -> attr.getType())
                .collect(Collectors.toCollection(Vector::new));

        return new QueryResult(attributeNames, attributeTypes);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public RheaDB(String rootDirectory) throws IOException {
        this.rootDirectory = rootDirectory;
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
        lazyCommit = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::commitOnExit));
    }

    public RheaDB() throws IOException {
        this("." + File.separator + "data");
    }

    public boolean createTable(String tableName, Vector<Attribute> attributeList) {
        if (createdTables.containsKey(tableName))
            return false;

        String pageDirectory = rootDirectory + File.separator + tableName;
        Table newTable = new Table(tableName, attributeList, pageDirectory,
                maxTuplesPerPage);
        createdTables.put(tableName, newTable);

        return true;
    }

    public QueryResult selectFrom(SelectStatement selectStatement) throws DBError {
        Table table = getTable(selectStatement.getTableName());
        Vector<String> selectedAttributes = selectStatement.getSelectedAttributes();
        if (table == null) {
            throw new DBError("The name " + selectStatement.getTableName()
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
                throw new DBError("Invalid attribute: \"" + predicate.getAttributeName()
                    + "\"");
            }
            predicate.setAttribute(attribute);
            if (attribute.getIsIndexed()) {
                BPlusTree<?, RowRecord> indexTree = bufferPool.getIndex(table, attribute);
                if (indexTree == null) {
                    System.out.println("Could not read the index file for attribute: " + attribute.getName());
                    System.exit(1);
                }
                result.addAll(indexTree.findWithPredicate(predicate));
            }
        }

        predicates = predicates
                .stream()
                .filter(p -> !table.getAttributeWithName(p.getAttributeName()).getIsIndexed())
                .collect(Collectors.toCollection(Vector::new));
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

    private void verifySelectedAttributesExist(Table table, Vector<String> selectedAttributes) throws DBError {
        for (String attributeName : selectedAttributes) {
            if (!attributeName.equals("*") &&
                table.getAttributeWithName(attributeName) == null) {
                throw new DBError("Invalid attribute name " + attributeName);
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

    public void deleteFrom(DeleteStatement deleteStatement) throws DBError {
        String tableName = deleteStatement.getTableName();
        Table table = getTable(tableName);
        if (table == null) {
            throw new DBError("The name \"" + tableName + "\" does not resolve " +
                    "to a valid table.");
        }

        Vector<Predicate> predicates = deleteStatement.getPredicateVector();
        if (predicates.isEmpty())
            deleteAllRows(table);

        for (Predicate predicate : predicates) {
            Attribute attribute = table.getAttributeWithName(predicate.getAttributeName());
            if (attribute == null) {
                throw new DBError("Invalid attribute: \"" + predicate.getAttributeName()
                        + "\"");
            }
            predicate.setAttribute(attribute);
        }

        final Vector<Predicate> finalPredicates = predicates;

        for (int i = 1; i <= table.getNumPages(); i++) {
            Page page = bufferPool.getPage(table, i);
            int rowsBeforeDelete = page.getNumberOfRows();
            page.getRecords()
                    .removeIf((r) -> {
                        boolean ret = false;
                        for (Predicate p : finalPredicates) {
                            Attribute attribute = table.getAttributeWithName(p.getAttributeName());
                            ret |= p.doesSatisfy(r.getValueOf(attribute));
                        }
                        return ret;
                    });
            if (!lazyCommit && page.getNumberOfRows() != rowsBeforeDelete) {
                bufferPool.updatePage(table, page);
            }
        }
    }

    private void deleteAllRows(Table table) {
        for (int i = table.getNumPages(); i > 0; i--) {
            bufferPool.deletePage(table, i);
            table.popPage();
        }
    }

    public void insertInto(InsertStatement insertStatement) throws DBError {
        String tableName = insertStatement.getTableName();
        Table table = getTable(tableName);

        if (table == null) {
            throw new DBError("The name \"" + tableName + "\" does not resolve " +
                    "to a valid table.");
        }

        Vector<Attribute> attributes = table.getAttributeList();
        Vector<Object> values = insertStatement.getValues();

        if (values.size() != table.getAttributeList().size()) {
            throw new DBError("Invalid number of attribute values.");
        }

        for (int i = 0; i < attributes.size(); i++) {
            Attribute attribute = attributes.elementAt(i);
            Object value = values.elementAt(i);
            try {
                switch (attribute.getType()) {
                    case INT: {
                        int testObject = (int) value;
                        break;
                    }
                    case FLOAT:{
                        float testObject = (float) value;
                        break;
                    }
                    case STRING: {
                        String testObject = (String) value;
                        break;
                    }
                    default: assert false;
                }
            } catch (Exception e) {
                throw new DBError("Invalid value(" + value + ") provided " +
                        "for attribute: " + attribute.getName());
            }
        }
        RowRecord rowRecord = new RowRecord(attributes, values);
        insertInto(table, rowRecord);
    }

    private void insertInto(Table table, RowRecord record) {
        Page lastPage = bufferPool.getPage(table, table.getNumPages());
        if (lastPage == null || lastPage.isFull())
            lastPage = bufferPool.insertPage(table, table.getNewPage());

        record.setPageId(lastPage.getPageIdx());
        record.setRowId(lastPage.getLastRowIndex());
        lastPage.addRecord(record);

        if (!lazyCommit) {
            bufferPool.updatePage(table, lastPage);
        }

        table.getAttributeList()
                .stream()
                .filter(attr -> attr.getIsIndexed())
                .forEach(attr -> updateIndex(table, attr));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean createIndex(Table table, Attribute attribute) {
        if (table == null || attribute == null)
            return false;

        BPlusTree<?, RowRecord> bPlusTree = new BPlusTree();

        for (int i = 1; i <= table.getNumPages(); i++) {
            Page page = bufferPool.getPage(table, i);
            page.getRecords().forEach(
                r -> bPlusTree.insert(r.getValueOf(attribute), r)
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

    public void saveMetadata() {
        DiskManager.saveMetadata(createdTables);
    }

    public void setLazyCommit(boolean b) {
        lazyCommit = b;
    }

    public boolean getLazyCommit() {
        return lazyCommit;
    }

    public static void main(String[] args) throws IOException {
        RheaDB rheaDB = new RheaDB();
        rheaDB.run();
    }
}
