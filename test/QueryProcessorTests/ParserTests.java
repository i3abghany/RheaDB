package QueryProcessorTests;

import Predicate.Predicate;
import QueryProcessor.DDLStatement.*;
import QueryProcessor.DMLStatement.*;
import QueryProcessor.InternalStatement.*;
import QueryProcessor.Parser;
import QueryProcessor.SQLStatement;
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
        try {
            SQLStatement sqlStatement = new Parser("").parse();
            Assertions.fail();
        } catch (DBError ignored) {

        }
    }

    @Test
    public void parseIncompleteStatement() {
        try {
            SQLStatement sqlStatement = new Parser("SELECT").parse();
            Assertions.fail();
        } catch (DBError ignored) {

        }
    }

    @Test
    public void parseCreateTableStatement() {
        String sqlString = "CREATE TABLE FancyTable (id INT, name STRING, mass FLOAT)";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

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
    public void parseCreateIndexStatement() {
        String sqlString = "CREATE INDEX FancyTable attributeName";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

        Assertions.assertTrue(sqlStatement instanceof CreateIndexStatement);
        CreateIndexStatement createIndexStatement = (CreateIndexStatement) sqlStatement;

        Assertions.assertEquals(createIndexStatement.getTableName(), "FancyTable");
        Assertions.assertEquals(createIndexStatement.getIndexAttribute(), "attributeName");
        Assertions.assertEquals(createIndexStatement.getDDLKind(), DDLKind.CREATE_INDEX);
    }

    @Test
    public void parseSelectStatementWithoutPredicates() {
        String sqlString = "SELECT attrA, attrB, attrC FROM tableName";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

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
    public void parseSelectStatementWithAttributes() {
        String sqlString = "SELECT attrA, attrB, attrC FROM tableName where attrA = 1, attrB = 2";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

        Assertions.assertTrue(sqlStatement instanceof SelectStatement);
        SelectStatement selectStatement = (SelectStatement) sqlStatement;

        Assertions.assertEquals(selectStatement.getTableName(), "tableName");

        Assertions.assertEquals(selectStatement.getPredicates().size(), 2);

        Vector<Predicate> predicates = selectStatement.getPredicates();

        Assertions.assertEquals(predicates.get(0).getAttributeName(), "attrA");
        Assertions.assertEquals((Integer) predicates.get(0).getValue(), 1);

        Assertions.assertEquals(predicates.get(1).getAttributeName(), "attrB");
        Assertions.assertEquals((Integer) predicates.get(1).getValue(), 2);
    }

    @Test
    public void parseSelectWithStarAttribute() {
        String sqlString = "SELECT * FROM tableName";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

        Assertions.assertTrue(sqlStatement instanceof SelectStatement);
        SelectStatement selectStatement = (SelectStatement) sqlStatement;
        Assertions.assertEquals(selectStatement.getSelectedAttributes().get(0), "*");
    }

    @Test
    public void parseDeleteWithoutPredicates() {
        String sqlString = "DELETE FROM tableName";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

        Assertions.assertTrue(sqlStatement instanceof DeleteStatement);
        DeleteStatement deleteStatement = (DeleteStatement) sqlStatement;

        Assertions.assertEquals(deleteStatement.getPredicateVector().size(), 0);
        Assertions.assertEquals(deleteStatement.getTableName(), "tableName");
    }

    @Test
    public void parseDeleteWithPredicates() {
        String sqlString = "DELETE FROM tableName WHERE attrA = 1, attrB = 2";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

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
    public void parseDropTable() {
        String sqlString = "DROP TABLE TableName";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

        Assertions.assertTrue(sqlStatement instanceof DropTableStatement);
        DropTableStatement dropTableStatement = (DropTableStatement) sqlStatement;
        Assertions.assertEquals(dropTableStatement.getTableName(), "TableName");
    }

    @Test
    public void parseDescribe() {
        String sqlString = "DESCRIBE TableName";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

        Assertions.assertTrue(sqlStatement instanceof DescribeStatement);
        DescribeStatement describeStatement = (DescribeStatement) sqlStatement;

        Assertions.assertEquals(describeStatement.getTableName(), "TableName");
        Assertions.assertEquals(describeStatement.getInternalStatementKind(), InternalStatementKind.DESCRIBE);
    }

    @Test
    void parseDropIndex() {
        String sqlString = "DROP INDEX FancyTable attributeName";
        SQLStatement sqlStatement = null;
        try {
            sqlStatement = new Parser(sqlString).parse();
        } catch (DBError ex) {
            Assertions.fail(ex.getMessage());
        }

        Assertions.assertTrue(sqlStatement instanceof DropIndexStatement);
        DropIndexStatement dropIndexStatement = (DropIndexStatement) sqlStatement;

        Assertions.assertEquals(dropIndexStatement.getTableName(), "FancyTable");
        Assertions.assertEquals(dropIndexStatement.getAttributeName(), "attributeName");
        Assertions.assertEquals(dropIndexStatement.getDMLKind(), DMLStatementKind.DROP_INDEX);
    }
}
