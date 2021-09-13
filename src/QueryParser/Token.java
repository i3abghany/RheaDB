package QueryParser;

public class Token {
    private final int position;
    private final String tokenText;
    private final Object value;
    private final TokenKind kind;

    public Token(int position, String tokenText, Object value, TokenKind kind) {
        this.position = position;
        this.tokenText = tokenText;
        this.value = value;
        this.kind = kind;
    }

    public boolean isLiteral() {
        return kind == TokenKind.StringLiteralToken ||
               kind == TokenKind.FloatingPointToken ||
               kind == TokenKind.IntegralToken;
    }

    public boolean isOperator() {
        return kind == TokenKind.LessToken ||
               kind == TokenKind.LessEqualsToken ||
               kind == TokenKind.EqualsToken ||
               kind == TokenKind.GreaterToken ||
               kind == TokenKind.GreaterEqualsToken ||
               kind == TokenKind.NotEqualsToken;
    }

    public int getPosition() {
        return position;
    }

    public String getTokenText() {
        return tokenText;
    }

    public Object getValue() {
        return value;
    }

    public TokenKind getKind() {
        return kind;
    }
}
