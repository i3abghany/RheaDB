package QueryParser.DMLStatements;

import Predicate.Predicate;

import java.util.Vector;

public class SelectStatement extends DMLStatement {
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
