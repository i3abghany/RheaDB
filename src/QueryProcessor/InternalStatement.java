package QueryProcessor;

public abstract class InternalStatement extends SQLStatement {
    public enum InternalStatementKind {
        DESCRIBE,
    }

    @Override
    public SQLStatementKind getKind() {
        return SQLStatementKind.INTERNAL;
    }

    public abstract InternalStatementKind getInternalStatementKind();

    public static class DescribeStatement extends InternalStatement {
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
}
