package QueryParser.DMLStatements;

import QueryParser.SQLStatement;

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
}