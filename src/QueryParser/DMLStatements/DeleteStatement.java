package QueryParser.DMLStatements;

import Predicate.Predicate;

import java.util.Vector;

public class DeleteStatement extends DMLStatement {
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
