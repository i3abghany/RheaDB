package QueryParser.InternalStatements;

public class DescribeStatement extends InternalStatement {
    private final String tableName;

    public DescribeStatement(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public InternalStatementKind getInternalStatementKind() {
        return InternalStatementKind.DESCRIBE;
    }

    public String getTableName() {
        return tableName;
    }
}
