package QueryParser.DDLStatements;

public class CreateIndexStatement extends DDLStatement {
    private final String tableName;
    private final String attributeName;

    public CreateIndexStatement(String tableName, String attributeName) {
        this.tableName = tableName;
        this.attributeName = attributeName;
    }

    @Override
    public DDLKind getDDLKind() {
        return DDLKind.CREATE_INDEX;
    }

    public String getTableName() {
        return tableName;
    }

    public String getIndexAttribute() {
        return attributeName;
    }
}
