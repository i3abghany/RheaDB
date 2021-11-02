package QueryParser.DDLStatements;

import QueryParser.SQLStatement;
import RheaDB.Attribute;
import java.util.Vector;

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