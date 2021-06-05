package QueryProcessor;

import Predicate.Predicate;

public abstract class DMLStatement extends SQLStatement {
    public enum DMLStatementKind {
        SELECT,
        INSERT
    }
    @Override
    public SQLStatementKind getKind() {
        return SQLStatementKind.DML;
    }

    public abstract DMLStatementKind getDMLKind();

    public static class SelectStatement extends DMLStatement {
        private final String tableName;
        private final String[] selectedAttributes;
        private final Predicate[] predicates;

        public SelectStatement(String tableName, String[] attributes, Predicate[] predicates) {
            this.tableName = tableName;
            this.selectedAttributes = attributes;
            this.predicates = predicates;
        }

        public String getTableName() {
            return tableName;
        }

        public String[] getSelectedAttributes() {
            return selectedAttributes;
        }

        public Predicate[] getPredicates() {
            return predicates;
        }

        @Override
        public DMLStatementKind getDMLKind() {
            return DMLStatementKind.SELECT;
        }
    }

    public static class InsertStatement extends DMLStatement {
        private final String tableName;
        private final Object[] values;

        public InsertStatement(String tableName, Object[] values) {
            this.tableName = tableName;
            this.values = values;
        }

        public String getTableName() {
            return tableName;
        }

        public Object[] getValues() {
            return values;
        }

        @Override
        public DMLStatementKind getDMLKind() {
            return DMLStatementKind.INSERT;
        }
    }
}
