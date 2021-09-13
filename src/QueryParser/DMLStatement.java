package QueryParser;

import Predicate.Predicate;

import java.util.Vector;

public abstract class DMLStatement extends SQLStatement {
    public enum DMLStatementKind {
        SELECT,
        INSERT,
        DELETE,
        DROP_TABLE,
        DROP_INDEX,
        UPDATE,
    }
    @Override
    public SQLStatementKind getKind() {
        return SQLStatementKind.DML;
    }

    public abstract DMLStatementKind getDMLKind();

    public static class SelectStatement extends DMLStatement {
        private final String tableName;
        private final Vector<String> selectedAttributes;
        private final Vector<Predicate> predicates;

        public SelectStatement(String tableName, Vector<String> attributes, Vector<Predicate> predicates) {
            this.tableName = tableName;
            this.selectedAttributes = attributes;
            this.predicates = predicates;
        }

        public String getTableName() {
            return tableName;
        }

        public Vector<String> getSelectedAttributes() {
            return selectedAttributes;
        }

        public Vector<Predicate> getPredicates() {
            return predicates;
        }

        @Override
        public DMLStatementKind getDMLKind() {
            return DMLStatementKind.SELECT;
        }
    }

    public static class InsertStatement extends DMLStatement {
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

    public static class UpdateStatement extends DMLStatement {
        private final String tableName;
        private final Vector<Predicate> setPredicates;
        private final Vector<Predicate> wherePredicates;

        public UpdateStatement(String tableName, Vector<Predicate> setPredicates,
                               Vector<Predicate> wherePredicates) {
            this.tableName = tableName;
            this.setPredicates = setPredicates;
            this.wherePredicates = wherePredicates;
        }

        public String getTableName() {
            return tableName;
        }

        @Override
        public DMLStatementKind getDMLKind() {
            return DMLStatementKind.UPDATE;
        }

        public Vector<Predicate> getSetPredicates() {
            return setPredicates;
        }

        public Vector<Predicate> getWherePredicates() {
            return wherePredicates;
        }
    }

    public static class DeleteStatement extends DMLStatement {
        private final String tableName;
        private final Vector<Predicate> predicateVector;

        public DeleteStatement(String tableName, Vector<Predicate> predicates) {
            this.tableName = tableName;
            this.predicateVector = predicates;
        }

        public String getTableName() {
            return tableName;
        }

        public Vector<Predicate> getPredicateVector() {
            return predicateVector;
        }

        @Override
        public DMLStatementKind getDMLKind() {
            return DMLStatementKind.DELETE;
        }
    }

    public static class DropTableStatement extends DMLStatement {
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

    public static class DropIndexStatement extends DMLStatement {
        private final String tableName;
        private final String attributeName;

        public DropIndexStatement(String tableName, String attributeName) {
            this.tableName = tableName;
            this.attributeName = attributeName;
        }

        public String getTableName() {
            return tableName;
        }

        public String getAttributeName() {
            return attributeName;
        }

        @Override
        public DMLStatementKind getDMLKind() {
            return DMLStatementKind.DROP_INDEX;
        }
    }
}
