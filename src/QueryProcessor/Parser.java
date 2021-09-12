package QueryProcessor;

import Predicate.*;
import Predicate.Predicate;
import RheaDB.Attribute;
import RheaDB.AttributeType;
import RheaDB.DBError;

import java.util.Vector;
import java.util.stream.Collectors;

import QueryProcessor.DMLStatement.*;
import QueryProcessor.DDLStatement.*;
import QueryProcessor.InternalStatement.*;

public class Parser {
    private final Vector<Token> tokenVector;
    private int position;
    private final String line;

    public Parser(String line) {
        this.line = line;
        tokenVector = new Lexer(line)
                .lex()
                .stream()
                .filter(tok -> tok.getKind() != TokenKind.WhiteSpaceToken)
                .collect(Collectors.toCollection(Vector::new));
        position = 0;
    }

    public SQLStatement parse() throws DBError {
        if (tokenVector.size() < 2)
            throw new DBError("Error parsing the statement.");

        Token badToken = tokenVector
                .stream()
                .filter(tok -> tok.getKind() == TokenKind.BadToken)
                .findAny()
                .orElse(null);

        if (badToken != null) {
            throw new DBError("Bad token: \"" + badToken.getTokenText() +
                    "\" at position: " + badToken.getPosition());
        }

        if (SQLStatement.isDDLKeyword(tokenVector.get(0)))
            return parseDDL();
        else if (SQLStatement.isDMLKeyword(tokenVector.get(0)))
            return parseDML();
        else if (SQLStatement.isInternalKeyword(tokenVector.get(0)))
            return parseInternalStatement();
        else
            throw new DBError("Unexpected token: \"" +
                    tokenVector.get(0).getTokenText() + "\".");
    }

    private SQLStatement parseInternalStatement() throws DBError {
        if (matchToken(0, TokenKind.KeywordToken, "describe"))
            return parseDescribe();
        else if (matchToken(0, TokenKind.KeywordToken, "compact"))
            return parseCompact();
        else
            throw new DBError("Error parsing statement.");
    }

    private SQLStatement parseDescribe() throws DBError {
        Token tableNameToken = tokenVector.get(1);
        matchToken(tableNameToken, TokenKind.IdentifierToken);
        return new DescribeStatement(tableNameToken.getTokenText());
    }

    private SQLStatement parseCompact() throws DBError {
        Token tableNameToken = tokenVector.get(1);
        matchToken(tableNameToken, TokenKind.IdentifierToken);
        return new CompactStatement(tableNameToken.getTokenText());
    }

    private SQLStatement parseDDL() throws DBError {
        if (tokenVector.size() == 1) {
            throw new DBError("Error parsing statement.");
        }

        Token typeToken = tokenVector.elementAt(1);

        if (matchToken(1, TokenKind.KeywordToken, "table"))
            return parseCreateTable();
        else if (matchToken(1, TokenKind.KeywordToken, "index"))
            return parseCreateIndex();
        else
            throw new DBError("Unexpected token: \"" + typeToken.getTokenText()
                + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseCreateTable() throws DBError {
        Token curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "create");

        curr = nextToken();
        matchToken(curr, TokenKind.KeywordToken, "table");

        Token tableNameToken = nextToken();
        matchToken(tableNameToken, TokenKind.IdentifierToken);

        Token openParenToken = nextToken();
        matchToken(openParenToken, TokenKind.OpenParenToken);

        Vector<Attribute> attributes = new Vector<>();
        while (true) {
            Token attributeName = nextToken();
            matchToken(attributeName, TokenKind.IdentifierToken);

            Token dataTypeToken = nextToken();
            if (dataTypeToken == null || dataTypeToken.getKind() != TokenKind.DataTypeToken) {
                throw new DBError("Error parsing the statement. Expected a " +
                        "datatype.");
            }

            AttributeType type = switch (dataTypeToken.getTokenText()) {
                case "int" -> AttributeType.INT;
                case "string" -> AttributeType.STRING;
                case "float" -> AttributeType.FLOAT;
                default -> null;
            };
            attributes.add(new Attribute(type, attributeName.getTokenText()));

            Token commaOrClosedParenToken = nextToken();
            if (commaOrClosedParenToken == null ||
                    (commaOrClosedParenToken.getKind() != TokenKind.CommaToken &&
                     commaOrClosedParenToken.getKind() != TokenKind.ClosedParenToken)) {
                throw new DBError("Error parsing the statement. Expected a " +
                        "continuation of attribute definitions or a closed paren.");
            }

            if (commaOrClosedParenToken.getKind() == TokenKind.ClosedParenToken)
                break;
        }
        return new CreateTableStatement(tableNameToken.getTokenText(),
               attributes);
    }

    private SQLStatement parseCreateIndex() throws DBError {
        return new CreateIndexParser(line).parse();
    }

    private boolean done() {
        return this.position >= this.tokenVector.size() - 1;
    }

    private SQLStatement parseDML() throws DBError {
        if (tokenVector.size() <= 1) {
            throw new DBError("Error parsing statement.");
        }

        Token typeToken = tokenVector.elementAt(0);

        if (matchToken(0, TokenKind.KeywordToken, "select"))
            return parseSelect();
        else if (matchToken(0, TokenKind.KeywordToken, "insert"))
            return parseInsert();
        else if (matchToken(0, TokenKind.KeywordToken, "delete"))
            return parseDelete();
        else if (matchToken(0, TokenKind.KeywordToken, "drop"))
            return parseDrop();
        else if (matchToken(0, TokenKind.KeywordToken, "update"))
            return parseUpdate();
        else
            throw new DBError("Unexpected token: \"" + typeToken.getTokenText()
                    + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseUpdate() throws DBError {
        Token curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "update");

        Token tableName = nextToken();
        matchToken(tableName, TokenKind.IdentifierToken);

        curr = nextToken();
        matchToken(curr, TokenKind.KeywordToken, "set");

        Vector<Predicate> setPredicates = parsePredicates();

        if (this.position == this.tokenVector.size()) {
            return new UpdateStatement(tableName.getTokenText(), setPredicates,
                    new Vector<>());
        }

        curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "where");

        Vector<Predicate> wherePredicates = parsePredicates();

        if (wherePredicates.isEmpty()) {
            throw new DBError("Unexpected token: \"" + curr.getTokenText() + "\", expected a trail of predicates.");
        }

        return new UpdateStatement(tableName.getTokenText(), setPredicates,
                wherePredicates);
    }

    private SQLStatement parseDrop() throws DBError {
        if (matchToken(1, TokenKind.KeywordToken, "table"))
            return parseDropTable();
        else if (matchToken(1, TokenKind.KeywordToken, "index"))
            return parseDropIndex();
        else
            throw new DBError("Error parsing statement.");
    }

    private SQLStatement parseDropIndex() throws DBError {
        Token curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "drop");

        curr = nextToken();
        matchToken(curr, TokenKind.KeywordToken, "index");

        Token tableNameToken = nextToken();
        matchToken(tableNameToken, TokenKind.IdentifierToken);

        Token attributeNameToken = nextToken();
        matchToken(attributeNameToken, TokenKind.IdentifierToken);

        if (!done()) {
            Token badToken = nextToken();
            assert badToken != null;
            throw new DBError("Error parsing the statement. " +
                    "Unexpected token: \"" + badToken.getTokenText() + "\"" +
                    " at position: " + badToken.getPosition());
        }

        return new DropIndexStatement(tableNameToken.getTokenText(),
                attributeNameToken.getTokenText());
    }

    private SQLStatement parseDropTable() throws DBError {
        Token curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "drop");

        curr = nextToken();
        matchToken(curr, TokenKind.KeywordToken, "table");

        Token tableNameToken = nextToken();
        matchToken(tableNameToken, TokenKind.IdentifierToken);

        if (!done()) {
            Token badToken = nextToken();
            assert badToken != null;
            throw new DBError("Error parsing the statement. " +
                    "Unexpected token: \"" + badToken.getTokenText() + "\"" +
                    " at position: " + badToken.getPosition());
        }

        return new DropTableStatement(tableNameToken.getTokenText());
    }

    private SQLStatement parseDelete() throws DBError {
        Token curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "delete");

        curr = nextToken();
        matchToken(curr, TokenKind.KeywordToken, "from");

        Token tableNameToken = nextToken();
        matchToken(tableNameToken, TokenKind.IdentifierToken);

        Token optionalWhereToken = nextToken();

        if (optionalWhereToken == null) {
            return new DeleteStatement(tableNameToken.getTokenText(),
                    new Vector<>());
        }

        Vector<Predicate> predicates = parsePredicates();

        if (matchToken(this.position, TokenKind.KeywordToken, "where")) {
            throw new DBError("Unexpected token \"where\" at position: " + currentToken().getPosition());
        }

        if (predicates.isEmpty()) {
            throw new DBError("Error parsing the statement. " +
                    "Expected a list of predicates.");
        }

        return new DeleteStatement(tableNameToken.getTokenText(),
                predicates);
    }

    private SQLStatement parseInsert() throws DBError {
        Token insertKeywordToken = currentToken();
        matchToken(insertKeywordToken, TokenKind.KeywordToken, "insert");

        Token intoToken = nextToken();
        matchToken(intoToken, TokenKind.KeywordToken, "into");

        Token tableNameToken = nextToken();
        matchToken(tableNameToken, TokenKind.IdentifierToken);

        Token valuesKeywordToken = nextToken();
        matchToken(valuesKeywordToken, TokenKind.KeywordToken, "values");

        Token openParenToken = nextToken();
        matchToken(openParenToken, TokenKind.OpenParenToken);

        Vector<Object> valueVector = new Vector<>();

        while (true) {
            Token valueToken = nextToken();
            if (valueToken == null) {
                throw new DBError("Error while parsing. Expected value tokens.");
            }

            if (!valueToken.isLiteral()) {
                throw new DBError("Unexpected token \"" + valueToken.getTokenText()
                    + "\" at position " + valueToken.getPosition() +
                        ". Expected a literal value.");
            }

            valueVector.add(valueToken.getValue());

            Token commaOrClosedParenToken = nextToken();
            if (commaOrClosedParenToken == null) {
                throw new DBError("Error parsing the statement. Expected a" +
                        "continuation of values or closed parenthesis.");
            }

            if (commaOrClosedParenToken.getKind() == TokenKind.ClosedParenToken)
                break;

            if (commaOrClosedParenToken.getKind() != TokenKind.CommaToken) {
                throw new DBError("Unexpected token \"" + valueToken.getTokenText()
                        + "\" at position " + valueToken.getPosition() +
                        ". Expected a continuation of values or a closed parenthesis.");
            }
        }
        return new InsertStatement(tableNameToken.getTokenText(),
                                                valueVector);
    }

    private SQLStatement parseSelect() throws DBError {
        return new SelectParser(line).parse();
    }

    private boolean matchToken(Token token, TokenKind tokenKind, String text) throws DBError {
        matchToken(token, tokenKind);
        if (!token.getTokenText().equals(text)) {
            throw new DBError("Unexpected token \"" + token.getTokenText() +
                    "\" at position " + token.getPosition() + ". Expected a " +
                    tokenKind + " with value \"" + text + "\".");
        }
        return true;
    }

    private boolean matchToken(Token token, TokenKind tokenKind) throws DBError {
        if (token == null) {
            throw new DBError("Error parsing the statement. Expected a " +
                    tokenKind + ".");
        }

        if (token.getKind() != tokenKind) {
            throw new DBError("Unexpected token \"" + token.getTokenText() + "\"" +
                    " at position " + token.getPosition() + ". Expected a " +
                    tokenKind);
        }

        return true;
    }

    private Vector<Predicate> parsePredicates() throws DBError {
        Vector<Predicate> predicates = new Vector<>();

        Token firstToken = nextToken();
        if (firstToken == null) {
            return predicates;
        }

        while (true) {
            Token attributeNameToken = currentToken();
            matchToken(attributeNameToken, TokenKind.IdentifierToken);

            Token operatorToken = nextToken();
            if (operatorToken == null || !operatorToken.isOperator()) {
                throw new DBError("Error parsing the statement. Expected operator" +
                        " token.");
            }

            Token literalToken = nextToken();
            if (literalToken == null || !literalToken.isLiteral()) {
                throw new DBError("Error parsing the statement. Expected a literal" +
                        " token.");
            }
            predicates.add(parsePredicate(attributeNameToken.getTokenText(), operatorToken.getTokenText(), literalToken));

            Token optionalComma = nextToken();
            if (optionalComma == null || matchToken(this.position, TokenKind.KeywordToken, "where"))
                break;
            else matchToken(optionalComma, TokenKind.CommaToken);
            nextToken();
        }

        return predicates;
    }

    private boolean matchToken(int i, TokenKind tokenKind) {
        if (i >= tokenVector.size())
            return false;

        return tokenVector.elementAt(i).getKind() == tokenKind;
    }

    private boolean matchToken(int i, TokenKind tokenKind, String tokenText) {
        if (!matchToken(i, tokenKind))
            return false;
        return tokenVector.elementAt(i).getTokenText().equals(tokenText);
    }

    enum OperatorKind {
        EqualsOperator,
        NotEqualsOperator,
        GreaterOperator,
        GreaterEqualsOperator,
        LessOperator,
        LessEqualsOperator,
        UnsupportedOperator,
    }

    private OperatorKind getOperatorKind(String op) {
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

    private Token currentToken() {
        if (position >= tokenVector.size())
            return null;

        return tokenVector.get(position);
    }

    private Token nextToken() {
        position++;

        if (position >= tokenVector.size())
            return null;

        return tokenVector.get(position);
    }
}
