package QueryProcessorTests;

import Predicate.Predicate;
import QueryProcessor.DDLStatement.*;
import QueryProcessor.DMLStatement.*;
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
        Assertions.assertEquals(createTableStatement.getDDLKind(), DDLKind.CreateTable);

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
        } catch (DBError throwables) {
            throwables.printStackTrace();
        }

        Assertions.assertTrue(sqlStatement instanceof CreateIndexStatement);
        CreateIndexStatement createIndexStatement = (CreateIndexStatement) sqlStatement;

        Assertions.assertEquals(createIndexStatement.getTableName(), "FancyTable");
        Assertions.assertEquals(createIndexStatement.getIndexAttribute(), "attributeName");
        Assertions.assertEquals(createIndexStatement.getDDLKind(), DDLKind.CreateIndex);
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

}
