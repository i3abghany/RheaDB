package QueryParser.InternalStatements;

public class CompactStatement extends InternalStatement {
    private final String tableName;

    public CompactStatement(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public InternalStatementKind getInternalStatementKind() {
        return InternalStatementKind.COMPACT;
    }

    public String getTableName() {
        return tableName;
    }
}
