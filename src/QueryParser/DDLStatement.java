package QueryParser;

import RheaDB.Attribute;
import java.util.Vector;

public abstract class DDLStatement extends SQLStatement {

    public enum DDLKind {
        CREATE_INDEX,
        CREATE_TABLE,
    }

    @Override
    public SQLStatementKind getKind() {
        return SQLStatementKind.DDL;
    }

    public abstract DDLKind getDDLKind();

    public static class CreateTableStatement extends DDLStatement {
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

    public static class CreateIndexStatement extends DDLStatement {
        private final String tableName;
        private final String attributeName;

        public CreateIndexStatement(String tableName, String attributeName) {
            this.tableName = tableName;
            this.attributeName = attributeName;
        }

        @Override
        public DDLKind getDDLKind() {
            return DDLKind.CREATE_INDEX;
        }

        public String getTableName() {
            return tableName;
        }

        public String getIndexAttribute() {
            return attributeName;
        }
    }
}
