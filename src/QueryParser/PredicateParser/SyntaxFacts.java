package QueryParser.PredicateParser;

import QueryParser.TokenKind;

public class SyntaxFacts {
    public static int binaryOperatorPrecedence(TokenKind kind) {
        return switch (kind) {
            case GreaterEqualsToken, GreaterToken, LessEqualsToken, LessToken, NotEqualsToken, EqualsToken -> 2;
            case AmpersandAmpersandToken, BarBarToken -> 1;
            default -> 0;
        };
    }
}
