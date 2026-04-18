package QueryParser.StatementParsers;

import Predicate.Predicate;
import Predicate.PredicateFactory;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;

import java.util.Vector;

public abstract class StatementParser {
    protected final Vector<Token> tokens;
    protected final Vector<String> diagnostics = new Vector<>();
    protected int position;

    public StatementParser(Vector<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    public abstract SQLStatement parse();

    protected Predicate getPredicate(String attributeName, TokenKind operatorKind, Object value) {
        return PredicateFactory.of(attributeName, operatorKind, value);
    }

    protected Vector<Predicate> parsePredicateList() {
        Vector<Predicate> predicates = new Vector<>();
        Predicate predicate = parsePredicate();
        if (predicate == null) {
            return predicates;
        }

        predicates.add(predicate);

        while (matchToken(TokenKind.CommaToken)) {
            advanceToken();
            predicate = parsePredicate();
            if (predicate == null) {
                return predicates;
            }

            predicates.add(predicate);
        }

        return predicates;
    }

    protected Predicate parsePredicate() {
        Token attributeToken = consumeToken(TokenKind.IdentifierToken, "Expected attribute name.");
        Token operatorToken = getCurrent();

        if (operatorToken == null || !operatorToken.isOperator()) {
            diagnostics.add(getUnexpectedTokenMessage(operatorToken, "Expected comparison operator."));
            return null;
        }

        advanceToken();
        Token valueToken = getCurrent();

        if (valueToken == null || !valueToken.isLiteral()) {
            diagnostics.add(getUnexpectedTokenMessage(valueToken, "Expected literal value."));
            return null;
        }

        advanceToken();
        return getPredicate(attributeToken.getTokenText(), operatorToken.getKind(), valueToken.getValue());
    }

    protected Token consumeToken(TokenKind expectedKind, String errorMessage) {
        Token current = getCurrent();

        if (current == null) {
            diagnostics.add(errorMessage);
            return null;
        }

        if (current.getKind() != expectedKind) {
            diagnostics.add(getUnexpectedTokenMessage(current, errorMessage));
            return null;
        }

        position++;
        return current;
    }

    protected Token consumeIdentifier(String errorMessage) {
        return consumeToken(TokenKind.IdentifierToken, errorMessage);
    }

    protected Token consumeLiteral(String errorMessage) {
        Token current = getCurrent();

        if (current == null) {
            diagnostics.add(errorMessage);
            return null;
        }

        if (!current.isLiteral()) {
            diagnostics.add(getUnexpectedTokenMessage(current, errorMessage));
            return null;
        }

        position++;
        return current;
    }

    protected boolean matchToken(TokenKind expectedKind) {
        Token current = getCurrent();
        return current != null && current.getKind() == expectedKind;
    }

    protected Token getCurrent() {
        return peekToken(0);
    }

    protected Token peekToken(int offset) {
        int index = position + offset;
        if (index >= tokens.size()) {
            return null;
        }

        return tokens.get(index);
    }

    protected Token advanceToken() {
        Token current = getCurrent();
        if (current != null) {
            position++;
        }

        return current;
    }

    protected void consumeSemicolon() {
        consumeToken(TokenKind.SemiColonToken, "Expected ';' at end of statement.");
    }

    protected void consumeEndOfInput() {
        Token current = getCurrent();
        if (current != null) {
            diagnostics.add("Unexpected token: \"" + current.getTokenText() + "\" at position " + current.getPosition());
        }
    }

    protected String getUnexpectedTokenMessage(Token token, String fallbackMessage) {
        if (token == null) {
            return fallbackMessage;
        }

        return "Unexpected token: \"" + token.getTokenText() + "\" at position " + token.getPosition();
    }

    public Vector<String> getDiagnostics() {
        return diagnostics;
    }

    public int getPosition() {
        return position;
    }
}
