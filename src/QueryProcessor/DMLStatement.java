package QueryProcessor;

public class DMLStatement extends SQLStatement {

    @Override
    public SQLStatementKind getKind() {
        return SQLStatementKind.DML;
    }

}
