package QueryProcessorTests;

import Predicate.*;
import QueryParser.*;
import QueryParser.DDLStatements.CreateIndexStatement;
import QueryParser.DDLStatements.CreateTableStatement;
import QueryParser.DDLStatements.DDLStatement.*;
import QueryParser.DMLStatements.*;
import QueryParser.DMLStatements.DMLStatement.*;
import QueryParser.InternalStatements.CompactStatement;
import QueryParser.InternalStatements.DescribeStatement;
import QueryParser.InternalStatements.InternalStatement.*;
import RheaDB.Attribute;
import RheaDB.AttributeType;
import RheaDB.DBError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static RheaDB.AttributeType.*;

public class ParserTests {
    @Test
    public void parseEmptyStatement() {
        Assertions.assertDoesNotThrow(() -> new Parser("").parse());
    }

    @Test
    public void parseIncompleteStatement() {

        String[] incompleteStatements = {
                "SELECT",
                "SELECT FROM",
                "SELECT *",
                "SELECT * FROM",
                "SELECT * FROM TABLENAME",
                "CREATE",
                "CREATE TABLE",
                "CREATE TABLE TABLENAME (I INT, J INT)"
        };

        for (var statement : incompleteStatements) {
            Assertions.assertThrows(DBError.class, () ->
                    new Parser(statement).parse());
        }
    }

    @Test
    public void parseCreateTableStatement() throws DBError {
        String sqlString = "CREATE TABLE FancyTable (id INT, name STRING, mass FLOAT);";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof CreateTableStatement);
        CreateTableStatement createTableStatement = (CreateTableStatement) sqlStatement;
        Assertions.assertEquals(createTableStatement.getTableName(), "FancyTable");
        Assertions.assertEquals(createTableStatement.getDDLKind(), DDLKind.CREATE_TABLE);

        Vector<Attribute> attributeVector = createTableStatement.getAttributeVector();

        Assertions.assertEquals(attributeVector.size(), 3);

        String[] attributeNames = {"id", "name", "mass"};
        AttributeType[] attributeTypes = {INT, STRING, FLOAT};

        for (int i = 0; i < attributeTypes.length; i++) {
            Assertions.assertEquals(attributeVector.get(i).getName(), attributeNames[i]);
            Assertions.assertEquals(attributeVector.get(i).getType(), attributeTypes[i]);
        }
    }

    @Test
    public void parseCreateIndexStatement() throws DBError {
        String sqlString = "CREATE INDEX FancyTable attributeName;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof CreateIndexStatement);
        CreateIndexStatement createIndexStatement = (CreateIndexStatement) sqlStatement;

        Assertions.assertEquals(createIndexStatement.getTableName(), "FancyTable");
        Assertions.assertEquals(createIndexStatement.getIndexAttribute(), "attributeName");
        Assertions.assertEquals(createIndexStatement.getDDLKind(), DDLKind.CREATE_INDEX);
    }

    @Test
    public void parseSelectStatementWithoutPredicates() throws DBError {
        String sqlString = "SELECT attrA, attrB, attrC FROM tableName;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof SelectStatement);
        SelectStatement selectStatement = (SelectStatement) sqlStatement;

        Assertions.assertEquals(selectStatement.getPredicates().size(), 0);
        Assertions.assertEquals(selectStatement.getTableName(), "tableName");
        String[] attributeNames = {"attrA", "attrB", "attrC"};

        for (int i = 0; i < attributeNames.length; i++) {
            Assertions.assertEquals(attributeNames[i], selectStatement.getSelectedAttributes().get(i));
        }

    }

    @Test
    public void parseSelectStatementWithAttributes() throws DBError {
        String sqlString = "SELECT attrA, attrB, attrC FROM tableName where attrA = 1, attrB = 2, attrC = \"Hello World\";";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof SelectStatement);
        SelectStatement selectStatement = (SelectStatement) sqlStatement;

        Assertions.assertEquals(selectStatement.getTableName(), "tableName");

        Assertions.assertEquals(selectStatement.getPredicates().size(), 3);

        Vector<Predicate> predicates = selectStatement.getPredicates();

        Assertions.assertEquals(predicates.get(0).getAttributeName(), "attrA");
        Assertions.assertEquals((Integer) predicates.get(0).getValue(), 1);

        Assertions.assertEquals(predicates.get(1).getAttributeName(), "attrB");
        Assertions.assertEquals((Integer) predicates.get(1).getValue(), 2);

        Assertions.assertEquals(predicates.get(2).getAttributeName(), "attrC");
        Assertions.assertEquals(predicates.get(2).getValue(), "Hello World");
    }

    @Test
    public void parseSelectWithStarAttribute() throws DBError {
        String sqlString = "SELECT * FROM tableName;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof SelectStatement);
        SelectStatement selectStatement = (SelectStatement) sqlStatement;
        Assertions.assertEquals(selectStatement.getSelectedAttributes().get(0), "*");
    }

    @Test
    public void parseDeleteWithoutPredicates() throws DBError {
        String sqlString = "DELETE FROM tableName;";
        var sqlStatement = new Parser(sqlString).parse();


        Assertions.assertTrue(sqlStatement instanceof DeleteStatement);
        DeleteStatement deleteStatement = (DeleteStatement) sqlStatement;

        Assertions.assertEquals(deleteStatement.getPredicateVector().size(), 0);
        Assertions.assertEquals(deleteStatement.getTableName(), "tableName");
    }

    @Test
    public void parseDeleteWithPredicates() throws DBError {
        String sqlString = "DELETE FROM tableName WHERE attrA = 1, attrB = 2;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof DeleteStatement);
        DeleteStatement deleteStatement = (DeleteStatement) sqlStatement;

        Assertions.assertEquals(deleteStatement.getPredicateVector().size(), 2);
        Assertions.assertEquals(deleteStatement.getTableName(), "tableName");

        String[] attrNames = {"attrA", "attrB"};
        Integer[] values = {1, 2};

        for (int i = 0; i < attrNames.length; i++) {
            Assertions.assertEquals(deleteStatement.getPredicateVector().get(i).getAttributeName(), attrNames[i]);
            Assertions.assertEquals((Integer) deleteStatement.getPredicateVector().get(i).getValue(), values[i]);
        }
    }

    @Test
    public void parseDropTable() throws DBError {
        String sqlString = "DROP TABLE TableName;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof DropTableStatement);
        DropTableStatement dropTableStatement = (DropTableStatement) sqlStatement;
        Assertions.assertEquals(dropTableStatement.getTableName(), "TableName");
    }

    @Test
    public void parseDescribe() throws DBError {
        String sqlString = "DESCRIBE TableName;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof DescribeStatement);
        DescribeStatement describeStatement = (DescribeStatement) sqlStatement;

        Assertions.assertEquals(describeStatement.getTableName(), "TableName");
        Assertions.assertEquals(describeStatement.getInternalStatementKind(), InternalStatementKind.DESCRIBE);
    }

    @Test
    public void parseCompact() throws DBError {
        String sqlString = "COMPACT TableName;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof CompactStatement);
        CompactStatement compactStatement = (CompactStatement) sqlStatement;

        Assertions.assertEquals(compactStatement.getTableName(), "TableName");
        Assertions.assertEquals(compactStatement.getInternalStatementKind(), InternalStatementKind.COMPACT);
    }

    @Test
    void parseDropIndex() throws DBError {
        String sqlString = "DROP INDEX FancyTable attributeName;";
        var sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof DropIndexStatement);
        DropIndexStatement dropIndexStatement = (DropIndexStatement) sqlStatement;

        Assertions.assertEquals(dropIndexStatement.getTableName(), "FancyTable");
        Assertions.assertEquals(dropIndexStatement.getAttributeName(), "attributeName");
        Assertions.assertEquals(dropIndexStatement.getDMLKind(), DMLStatementKind.DROP_INDEX);
    }

    private static UpdateStatement testInitialPartOfUpdate(String sqlString) throws DBError {
        SQLStatement sqlStatement = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof UpdateStatement);
        UpdateStatement updateStatement = (UpdateStatement) sqlStatement;

        Assertions.assertEquals(updateStatement.getTableName(), "FancyTable");
        Assertions.assertEquals(updateStatement.getSetPredicates().size(), 2);

        Predicate p1 = updateStatement.getSetPredicates().get(0);
        Assertions.assertEquals(p1.getAttributeName(), "intAttribute");
        Assertions.assertEquals(p1.getOperation(), Predicate.Operation.EQUALS);
        Assertions.assertEquals((int) p1.getValue(), 1);

        Predicate p2 = updateStatement.getSetPredicates().get(1);
        Assertions.assertEquals(p2.getAttributeName(), "stringAttribute");
        Assertions.assertEquals(p2.getOperation(), Predicate.Operation.EQUALS);
        Assertions.assertEquals(p2.getValue(), "Random String");

        return (UpdateStatement) sqlStatement;
    }

    @Test
    void parseUpdateWithPredicates() throws DBError {
        String sqlString = "UPDATE FancyTable SET intAttribute = 1, stringAttribute = "
                + "\"Random String\" WHERE x <= 10, y > 2;";

        var updateStatement = testInitialPartOfUpdate(sqlString);

        Assertions.assertEquals(updateStatement.getWherePredicates().size(), 2);

        Predicate p1 = updateStatement.getWherePredicates().get(0);
        Assertions.assertEquals(p1.getAttributeName(), "x");
        Assertions.assertEquals(p1.getOperation(), Predicate.Operation.LESS_THAN_EQUAL);
        Assertions.assertEquals((int) p1.getValue(), 10);

        Predicate p2 = updateStatement.getWherePredicates().get(1);
        Assertions.assertEquals(p2.getAttributeName(), "y");
        Assertions.assertEquals(p2.getOperation(), Predicate.Operation.GREATER_THAN);
        Assertions.assertEquals((int) p2.getValue(), 2);
    }

    @Test
    void parseInvalidUpdateWithoutWherePredicates() {
        String sqlString = "UPDATE FancyTable SET intAttribute = 1, stringAttribute = "
                + "\"Random String\" WHERE";

        Assertions.assertThrows(DBError.class, () -> new Parser(sqlString).parse());
    }

    @Test
    void parseInsert() throws DBError {
        String sqlString = "INSERT INTO TableName VALUES(1, 2, \"Hello\");";
        var sqlStatement  = new Parser(sqlString).parse();

        Assertions.assertTrue(sqlStatement instanceof InsertStatement);

        InsertStatement insertStatement = (InsertStatement) sqlStatement;

        Assertions.assertEquals(insertStatement.getTableName(), "TableName");

        Assertions.assertEquals(insertStatement.getValues().size(), 3);

        Assertions.assertEquals(insertStatement.getValues().get(0), 1);
        Assertions.assertEquals(insertStatement.getValues().get(1), 2);
        Assertions.assertEquals(insertStatement.getValues().get(2), "Hello");
    }

    @Test
    void parseUpdateAllRows() throws DBError {
        String sqlString = "UPDATE FancyTable SET intAttribute = 1, stringAttribute = "
                + "\"Random String\";";

        var updateStatement = testInitialPartOfUpdate(sqlString);
        Assertions.assertTrue(updateStatement.getWherePredicates().isEmpty());
    }
}
