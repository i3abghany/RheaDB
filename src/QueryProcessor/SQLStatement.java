package QueryProcessor;

public abstract class SQLStatement {
    public enum SQLStatementKind {
        DDL,
        DML
    }

    public abstract SQLStatementKind getKind();

    public static boolean isDDLKeyword(Token token) {
        return token.getKind() == TokenKind.KeywordToken &&
                token.getTokenText().equals("create");
    }

    public static boolean isDMLKeyword(Token token) {
        return token.getKind() == TokenKind.KeywordToken &&
                (token.getTokenText().equals("insert") ||
                 token.getTokenText().equals("delete") ||
                 token.getTokenText().equals("select"));
    }
}

