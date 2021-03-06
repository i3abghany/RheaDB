package QueryParser;

public enum TokenKind {
    BadToken,
    WhiteSpaceToken,
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
    CommaToken,
    OpenParenToken,
    ClosedParenToken,
    AmpersandAmpersandToken,
    BarBarToken,
    SemiColonToken,

    // Keywords
    CompactToken,
    CreateToken,
    DeleteToken,
    DescribeToken,
    DropToken,
    FromToken,
    IndexToken,
    InsertToken,
    IntoToken,
    SelectToken,
    SetTotken,
    TableToken,
    UpdateToken,
    ValuesToken,
    WhereToken,
}
