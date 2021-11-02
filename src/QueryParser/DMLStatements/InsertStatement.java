package QueryParser.DMLStatements;

import java.util.Vector;

public class InsertStatement extends DMLStatement {
    private final String tableName;
    private final Vector<Object> values;

    public InsertStatement(String tableName, Vector<Object> values) {
        this.tableName = tableName;
        this.values = values;
    }

    public String getTableName() {
        return tableName;
    }

    public Vector<Object> getValues() {
        return values;
    }

    @Override
    public DMLStatementKind getDMLKind() {
        return DMLStatementKind.INSERT;
    }
}
