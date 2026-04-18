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
        Predicate.Operation operation = Predicate.Operation.fromTokenKind(operatorKind);
        if (operation == null) {
            return null;
        }

        return new Predicate(identifierName, literalValue, operation);
    }
}
