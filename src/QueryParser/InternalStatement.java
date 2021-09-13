package QueryParser;

public abstract class InternalStatement extends SQLStatement {
    public enum InternalStatementKind {
        DESCRIBE,
        COMPACT,
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

    public static class CompactStatement extends InternalStatement {
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
}
