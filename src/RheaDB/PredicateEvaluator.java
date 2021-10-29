package RheaDB;

import Predicate.PredicateFactory;
import QueryParser.Parser;
import QueryParser.PredicateParser.*;
import QueryParser.Token;
import QueryParser.TokenKind;

import java.util.HashMap;
import java.util.Map;

public class PredicateEvaluator {
    private final PredicateAST predicateAST;
    private final Map<String, IdentifierValue> identifierValues;

    public static class IdentifierValue {
        public Object value;
        public AttributeType type;

        public IdentifierValue(Object value, AttributeType type) {
            this.value = value;
            this.type = type;
        }
    }

    public PredicateEvaluator(PredicateAST predicateAST, Map<String, IdentifierValue> identifierValues) {
        this.predicateAST = predicateAST;
        this.identifierValues = identifierValues;
    }

    public boolean Evaluate() throws Exception {
        if (predicateAST == null || predicateAST.root() == null) {
            throw new DBError("Null predicateAST.");
        }

        return Evaluate(predicateAST.root());
    }

    private boolean castTokenToBool(Token token) {
        return ((token.getKind() == TokenKind.IntegralToken) || (token.getValue() == TokenKind.FloatingPointToken)) &&
                castValueToBool(token.getValue());
    }

    private boolean castValueToBool(Object value) {
        return (float) value != 0.0;
    }

    private boolean Evaluate(ASTNode node) throws Exception {
        if (node instanceof LiteralExpression literalExpression) {
            var valueToken = literalExpression.getValueToken();
            return castTokenToBool(valueToken);
        } else if (node instanceof IdentifierExpression identifierExpression) {
            var variableName = identifierExpression.getIdentifierToken().getTokenText();
            var value = identifierValues.get(variableName);
            if (value == null) {
                throw new DBError("Unexpected identifier: " + variableName);
            } else {
                return castValueToBool(identifierValues.get(variableName));
            }
        } else if (node instanceof ParenthesizedExpression parenthesizedExpression) {
            return Evaluate(parenthesizedExpression.getExpression());
        } else if (node instanceof BinaryLogicalExpression binaryLogicalExpression) {
            return evaluateBinaryLogicalExpression(binaryLogicalExpression);
        } else {
            throw new Exception("Unexpected node type: " + node.getClass().getName());
        }
    }

    private boolean evaluateBinaryLogicalExpression(BinaryLogicalExpression node) throws Exception {
        boolean lhs, rhs;

        if (node.getLhs() instanceof BinaryLogicalExpression lhsPredicate) {
            lhs = evaluateBinaryLogicalExpression(lhsPredicate);
        } else if (node.getRhs() instanceof LiteralExpression || node.getLhs() instanceof LiteralExpression) {
            var literalExpression = node.getRhs() instanceof LiteralExpression ? (LiteralExpression) node.getRhs() : (LiteralExpression) node.getLhs();
            var identifierExpression = node.getRhs() instanceof IdentifierExpression ? (IdentifierExpression) node.getRhs() : (IdentifierExpression) node.getLhs();

            var predicate = PredicateFactory.of(identifierExpression, node.getOperatorToken(), literalExpression);
            return predicate.doesSatisfy(identifierValues.get(identifierExpression.getIdentifierToken().getTokenText()));
        } else {
            return Evaluate(node.getLhs());
        }

        if (node.getRhs() instanceof BinaryLogicalExpression rhsPredicate) {
            rhs = evaluateBinaryLogicalExpression(rhsPredicate);
        } else if (node.getRhs() instanceof LiteralExpression || node.getLhs() instanceof LiteralExpression) {
            var literalExpression = node.getRhs() instanceof LiteralExpression ? (LiteralExpression) node.getRhs() : (LiteralExpression) node.getLhs();
            var identifierExpression = node.getRhs() instanceof IdentifierExpression ? (IdentifierExpression) node.getRhs() : (IdentifierExpression) node.getLhs();

            var predicate = PredicateFactory.of(identifierExpression, node.getOperatorToken(), literalExpression);
            rhs = predicate.doesSatisfy(identifierValues.get(identifierExpression.getIdentifierToken().getTokenText()));
        } else {
            return Evaluate(node.getRhs());
        }

        return switch (node.getOperatorToken().getKind()) {
            case BarBarToken -> lhs || rhs;
            case AmpersandAmpersandToken -> lhs && rhs;
            default -> throw new DBError("Incorrect operator: " + node.getOperatorToken().getTokenText());
        };
    }

    public PredicateAST getPredicateAST() {
        return predicateAST;
    }

    public Map<String, IdentifierValue> getIdentifierValues() {
        return identifierValues;
    }
}
