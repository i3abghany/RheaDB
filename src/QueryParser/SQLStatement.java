package QueryParser;

public abstract class SQLStatement {
    public enum SQLStatementKind {
        DDL,
        DML,
        INTERNAL
    }

    public abstract SQLStatementKind getKind();

    public static boolean isDDLKeyword(Token token) {
        return token.getKind() == TokenKind.CreateToken;
    }

    public static boolean isDMLKeyword(Token token) {
        return token.getKind() == TokenKind.InsertToken   ||
                 token.getKind() == TokenKind.DeleteToken ||
                token.getKind() == TokenKind.DropToken    ||
                token.getKind() == TokenKind.UpdateToken  ||
                token.getKind() == TokenKind.SelectToken;
    }

    public static boolean isInternalKeyword(Token token) {
        return token.getKind() == TokenKind.DescribeToken ||
                token.getKind() == TokenKind.CompactToken;
    }
}
