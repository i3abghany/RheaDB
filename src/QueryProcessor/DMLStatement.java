package QueryProcessor;

import Predicate.Predicate;
import RheaDB.Attribute;

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
}
