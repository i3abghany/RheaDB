package QueryParser.DDLStatements;

import QueryParser.SQLStatement;

public abstract class DDLStatement extends SQLStatement {

    public enum DDLKind {
        CREATE_INDEX,
        CREATE_TABLE,
    }

    @Override
    public SQLStatementKind getKind() {
        return SQLStatementKind.DDL;
    }

    public abstract DDLKind getDDLKind();
}