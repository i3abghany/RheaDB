package QueryProcessor;

public enum TokenKind {
    EOFToken,
    BadToken,
    WhiteSpaceToken,
    KeywordToken,
    IdentifierToken,
    IntegralToken,
    FloatingPointToken,
    StringLiteralToken,
    DataTypeToken,
    EqualsToken,
    NotEqualsToken,
    GreaterEqualsToken,
    LessEqualsToken,
    GreaterToken,
    LessToken,
    CommaToken
}
