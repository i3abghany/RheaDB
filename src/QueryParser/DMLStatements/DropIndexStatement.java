package QueryParser.DMLStatements;

public class DropIndexStatement extends DMLStatement {
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
