package Predicate;

import QueryParser.PredicateParser.IdentifierExpression;
import QueryParser.PredicateParser.LiteralExpression;
import QueryParser.Token;
import QueryParser.TokenKind;

public class PredicateFactory {
    public static Predicate of(IdentifierExpression identifierExpression, Token operatorToken, LiteralExpression literalExpression) {
        var identifierText = identifierExpression.getIdentifierToken().getTokenText();
        var literalValue = literalExpression.getValueToken().getValue();
        return of(identifierText, operatorToken.getKind(), literalValue);
    }

    public static Predicate of(String identifierName, TokenKind operatorKind, Object literalValue) {
        return switch (operatorKind) {
            case EqualsToken -> new EqualsPredicate(identifierName, literalValue);
            case NotEqualsToken -> new NotEqualsPredicate(identifierName, literalValue);
            case GreaterEqualsToken -> new GreaterThanEqualPredicate(identifierName, literalValue);
            case GreaterToken -> new GreaterThanPredicate(identifierName, literalValue);
            case LessEqualsToken -> new LessThanEqualPredicate(identifierName, literalValue);
            case LessToken -> new LessThanPredicate(identifierName, literalValue);
            default -> null;
        };
    }
}
