package QueryParser.DMLStatements;

import Predicate.Predicate;

import java.util.Vector;

public class UpdateStatement extends DMLStatement {
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
