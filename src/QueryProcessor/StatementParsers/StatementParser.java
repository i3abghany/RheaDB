package QueryProcessor.StatementParsers;

import Predicate.*;
import Predicate.Predicate;
import QueryProcessor.*;
import RheaDB.DBError;

import java.util.Vector;
import java.util.stream.Collectors;

public abstract class StatementParser {
    protected String line;
    protected String regex;

    public StatementParser(String line) {
        this.line = line;
    }

    public abstract SQLStatement parse() throws DBError;

    protected OperatorKind getOperatorKind(String op) {
        return switch (op) {
            case "=" -> OperatorKind.EqualsOperator;
            case "!=" -> OperatorKind.NotEqualsOperator;
            case ">" -> OperatorKind.GreaterOperator;
            case ">=" -> OperatorKind.GreaterEqualsOperator;
            case "<" -> OperatorKind.LessOperator;
            case "<=" -> OperatorKind.LessEqualsOperator;
            default -> OperatorKind.UnsupportedOperator;
        };
    }

    public enum OperatorKind {
        EqualsOperator,
        NotEqualsOperator,
        GreaterOperator,
        GreaterEqualsOperator,
        LessOperator,
        LessEqualsOperator,
        UnsupportedOperator,
    }

    protected Predicate parsePredicate(String attributeName, String operator, Object value) {
        return switch (getOperatorKind(operator)) {
            case EqualsOperator -> new EqualsPredicate(attributeName, value);
            case NotEqualsOperator -> new NotEqualsPredicate(attributeName, value);
            case GreaterOperator -> new GreaterThanPredicate(attributeName, value);
            case GreaterEqualsOperator -> new GreaterThanEqualPredicate(attributeName, value);
            case LessOperator -> new LessThanPredicate(attributeName, value);
            case LessEqualsOperator -> new LessThanEqualPredicate(attributeName, value);
            default -> null;
        };
    }

    protected Vector<Predicate> getPredicates(String[] predicateStrings) {
        Vector<Predicate> predicates = new Vector<>();

        for (String predicate : predicateStrings) {
            Vector<Token> tokens = new Lexer(predicate).lex()
                    .stream()
                    .filter(t -> t.getKind() != TokenKind.WhiteSpaceToken)
                    .collect(Collectors.toCollection(Vector::new));

            predicates.add(parsePredicate(tokens.elementAt(0).getTokenText(),
                    tokens.elementAt(1).getTokenText(),
                    tokens.elementAt(2).getValue()));
        }
        return predicates;
    }
}
