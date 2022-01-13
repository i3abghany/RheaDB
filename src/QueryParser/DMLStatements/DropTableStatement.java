package QueryParser.DMLStatements;

public class DropTableStatement extends DMLStatement {
    private final String tableName;

    public DropTableStatement(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public DMLStatementKind getDMLKind() {
        return DMLStatementKind.DROP_TABLE;
    }

    public String getTableName() {
        return tableName;
    }
}
