package QueryProcessor;

import Predicate.*;
import Predicate.Predicate;
import RheaDB.Attribute;
import RheaDB.AttributeType;
import RheaDB.DBError;

import java.util.Vector;
import java.util.stream.Collectors;

public class Parser {
    private final Vector<Token> tokenVector;
    private int position;

    public Parser(String line) {
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
        Token typeToken = tokenVector.get(0);

        if (matchToken(typeToken, TokenKind.KeywordToken, "describe"))
            return parseDescribe();

        return null;
    }

    private SQLStatement parseDescribe() throws DBError {
        Token tableNameToken = tokenVector.get(1);
        matchToken(tableNameToken, TokenKind.IdentifierToken);
        return new InternalStatement.DescribeStatement(tableNameToken.getTokenText());
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
        return new DDLStatement.CreateTableStatement(tableNameToken.getTokenText(),
               attributes);
    }

    private SQLStatement parseCreateIndex() throws DBError {
        Token curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "create");

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

        return new DDLStatement.CreateIndexStatement(tableNameToken.getTokenText(),
                attributeNameToken.getTokenText());
    }

    private boolean done() {
        return this.position == this.tokenVector.size() - 1;
    }

    private SQLStatement parseDML() throws DBError {
        if (tokenVector.size() == 1) {
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
        else
            throw new DBError("Unexpected token: \"" + typeToken.getTokenText()
                    + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseDrop() throws DBError {
        if (matchToken(1, TokenKind.KeywordToken, "table"))
            return parseDropTable();
        else if (matchToken(1, TokenKind.KeywordToken, "index"))
            return parseDropIndex();
        else
            return null;
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

        return new DMLStatement.DropIndexStatement(tableNameToken.getTokenText(),
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

        return new DMLStatement.DropTableStatement(tableNameToken.getTokenText());
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
            return new DMLStatement.DeleteStatement(tableNameToken.getTokenText(),
                    new Vector<>());
        }

        Vector<Predicate> predicates = parsePredicates();
        if (predicates.isEmpty()) {
            throw new DBError("Error parsing the statement. " +
                    "Expected a list of predicates.");
        }

        return new DMLStatement.DeleteStatement(tableNameToken.getTokenText(),
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
        return new DMLStatement.InsertStatement(tableNameToken.getTokenText(),
                                                valueVector);
    }

    private SQLStatement parseSelect() throws DBError {
        Token curr = currentToken();
        matchToken(curr, TokenKind.KeywordToken, "select");

        Vector<String> attributeNames = new Vector<>();

        while (true) {
            Token attributeNameToken = nextToken();
            matchToken(attributeNameToken, TokenKind.IdentifierToken);

            curr = nextToken();
            if (curr == null) {
                throw new DBError("Error parsing SELECT statement. Expected a " +
                        "continuation of identifier names or a FROM keyword.");
            }

            if (curr.getKind() == TokenKind.KeywordToken &&
                    curr.getTokenText().equals("from")) {
                attributeNames.add(attributeNameToken.getTokenText());
                break;
            } else if (curr.getKind() == TokenKind.CommaToken) {
                attributeNames.add(attributeNameToken.getTokenText());
            } else {
                throw new DBError("Error parsing SELECT statement. Unexpected token:" +
                        " \"" + curr.getTokenText() + "\" at position: "
                        + curr.getPosition());
            }
        }

        Token tableNameToken = nextToken();
        matchToken(tableNameToken, TokenKind.IdentifierToken);

        Token whereKeywordToken = nextToken();
        if (whereKeywordToken == null) {
            return new DMLStatement.SelectStatement(tableNameToken.getTokenText(),
                    attributeNames, new Vector<>());
        }

        matchToken(whereKeywordToken, TokenKind.KeywordToken, "where");

        Vector<Predicate> predicates = parsePredicates();
        if (predicates.isEmpty()) {
            throw new DBError("Error parsing the statement. Expected a list of " +
                    "comma-separated predicates.");
        }

        return new DMLStatement.SelectStatement(tableNameToken.getTokenText(),
                attributeNames, predicates);
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

        while (true) {
            Token attributeNameToken = nextToken();
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
            predicates.add(parsePredicate(attributeNameToken, operatorToken, literalToken));

            Token optionalComma = nextToken();
            if (optionalComma == null)
                break;
            else matchToken(optionalComma, TokenKind.CommaToken);
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

    private Predicate parsePredicate(Token attributeNameToken, Token operatorToken, Token valueToken) {
        String attributeName = attributeNameToken.getTokenText();
        Object value = valueToken.getValue();
        return switch (operatorToken.getKind()) {
            case EqualsToken -> new EqualsPredicate(attributeName, value);
            case NotEqualsToken -> new NotEqualsPredicate(attributeName, value);
            case GreaterToken -> new GreaterThanPredicate(attributeName, value);
            case GreaterEqualsToken -> new GreaterThanEqualPredicate(attributeName, value);
            case LessToken -> new LessThanPredicate(attributeName, value);
            case LessEqualsToken -> new LessThanEqualPredicate(attributeName, value);
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
