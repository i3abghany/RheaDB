package QueryParser;

public abstract class SQLStatement {
    public enum SQLStatementKind {
        DDL,
        DML,
        INTERNAL
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
                 token.getTokenText().equals("drop")   ||
                 token.getTokenText().equals("update") ||
                 token.getTokenText().equals("select"));
    }

    public static boolean isInternalKeyword(Token token) {
        return token.getKind() == TokenKind.KeywordToken &&
                (token.getTokenText().equals("describe") ||
                 token.getTokenText().equals("compact"));
    }
}
