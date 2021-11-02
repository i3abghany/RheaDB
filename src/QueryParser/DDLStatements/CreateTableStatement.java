package QueryParser.DDLStatements;

import RheaDB.Attribute;

import java.util.Vector;

public class CreateTableStatement extends DDLStatement {
    private final String tableName;
    private final Vector<Attribute> attributeVector;

    public CreateTableStatement(String tableName, Vector<Attribute> attributeVector) {
        this.attributeVector = attributeVector;
        this.attributeVector.elementAt(0).setIsPrimaryKey(true);
        this.tableName = tableName;
    }

    @Override
    public DDLKind getDDLKind() {
        return DDLKind.CREATE_TABLE;
    }

    public String getTableName() {
        return tableName;
    }

    public Vector<Attribute> getAttributeVector() {
        return attributeVector;
    }
}
