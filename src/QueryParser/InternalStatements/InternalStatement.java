package QueryParser.InternalStatements;

import QueryParser.SQLStatement;

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
}


