package QueryProcessor;

import Predicate.*;
import Predicate.Predicate;
import RheaDB.DBError;

import java.util.Arrays;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SelectParser {
    private final String regex;
    private final String line;
    public SelectParser(String line) {
        this.line  = line;
        this.regex = "select\\s+(.*?)\\s*from\\s+(.*?)\\s*(where\\s(.*?)\\s*)?;";
    }

    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            throw new DBError("Error parsing the statement.");
        }

        Vector<String> attributeNames = Arrays.stream(matcher.group(1).split(
                ","))
                .map(String::trim)
                .collect(Collectors.toCollection(Vector::new));
        String tableName = matcher.group(2);
        boolean usePredicates = matcher.group(3) != null;

        if (!usePredicates) {
            return new DMLStatement.SelectStatement(tableName, attributeNames, new Vector<>());
        }

        String[] predicateStrings = matcher.group(3).split(",");
        predicateStrings[0] = predicateStrings[0].split(" ", 2)[1];

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

        return new DMLStatement.SelectStatement(tableName, attributeNames, predicates);
    }

    // TODO: This may be removed once we can inherit from Parser.
    private Parser.OperatorKind getOperatorKind(String op) {
        return switch (op) {
            case "=" -> Parser.OperatorKind.EqualsOperator;
            case "!=" -> Parser.OperatorKind.NotEqualsOperator;
            case ">" -> Parser.OperatorKind.GreaterOperator;
            case ">=" -> Parser.OperatorKind.GreaterEqualsOperator;
            case "<" -> Parser.OperatorKind.LessOperator;
            case "<=" -> Parser.OperatorKind.LessEqualsOperator;
            default -> Parser.OperatorKind.UnsupportedOperator;
        };
    }

    // TODO: This may be removed once we can inherit from Parser.
    private Predicate parsePredicate(String attributeName, String operator, Object value) {
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
}
