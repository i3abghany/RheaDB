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
        if (tokenVector.size() < 3) {
            throw new DBError("Error parsing Create Table statement.");
        }

        String tableName = tokenVector.get(2).getTokenText();
        Vector<Attribute> attributeVector = new Vector<>();
        for (int i = 3; i < tokenVector.size() - 1; i += 2) {
            String attributeName = tokenVector.get(i).getTokenText();
            AttributeType attributeType = Attribute.getAttributeTypeFromString(
                    tokenVector.get(i + 1).getTokenText()
            );
            attributeVector.add(new Attribute(attributeType, attributeName, false));
        }
        if (attributeVector.isEmpty()) {
            throw new DBError("Error parsing Create Table statement. " +
                    "Expected a set of attributes.");
        } else {
            return new DDLStatement.CreateTableStatement(tableName, attributeVector);
        }
    }

    private SQLStatement parseCreateIndex() throws DBError {
        if (tokenVector.size() < 4) {
            throw new DBError("Error parsing Create Index statement.");
        }

        Token tableNameToken = tokenVector.get(2);
        Token attributeNameToken = tokenVector.get(3);

        if (!matchToken(2, TokenKind.IdentifierToken)) {
            throw new DBError("Unexpected token: \"" + tableNameToken.getTokenText() +
                    "\" at position " + tableNameToken.getPosition());
        }

        if (!matchToken(3, TokenKind.IdentifierToken)) {
            throw new DBError("Unexpected token: \"" + attributeNameToken.getTokenText() +
                    "\" at position " + attributeNameToken.getPosition());
        }

        String tableName = tableNameToken.getTokenText();
        String attributeName = attributeNameToken.getTokenText();

        return new DDLStatement.CreateIndexStatement(tableName, attributeName);
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
        else
            throw new DBError("Unexpected token: \"" + typeToken.getTokenText()
                    + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseDelete() throws DBError {
        if (tokenVector.size() < 3) {
            throw new DBError("Error parsing statement.");
        }

        assert matchToken(0, TokenKind.KeywordToken, "delete");

        if (!matchToken(1, TokenKind.KeywordToken, "from")) {
            throw new DBError("Unexpected token \"" + tokenVector.get(1).getTokenText() +
                    "\" at position: " + tokenVector.get(1).getPosition());
        }

        if (!matchToken(2, TokenKind.IdentifierToken)) {
            throw new DBError("Unexpected token \"" + tokenVector.get(2).getTokenText() +
                    "\" at position: " + tokenVector.get(2).getPosition());
        }

        String tableName = tokenVector.get(2).getTokenText();

        if (tokenVector.size() == 3) {
            return new DMLStatement.DeleteStatement(tableName, new Vector<>());
        }

        if (!matchToken(3, TokenKind.KeywordToken, "where")) {
            throw new DBError("Unexpected token \"" + tokenVector.get(3).getTokenText() +
                    "\" at position: " + tokenVector.get(3).getPosition());
        }
        Vector<Predicate> predicates = parsePredicates(4);

        return new DMLStatement.DeleteStatement(tableName, predicates);
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
        Vector<String> attributeNames = new Vector<>();
        Token token;
        int i = 1;
        for (; i < tokenVector.size(); i += 2) {
            token = tokenVector.elementAt(i);
            if (matchToken(i, TokenKind.IdentifierToken) &&
                matchToken(i + 1, TokenKind.CommaToken)) {
                attributeNames.add(token.getTokenText());
            } else if (matchToken(i, TokenKind.IdentifierToken) &&
                       matchToken(i + 1, TokenKind.KeywordToken, "from")) {
                attributeNames.add(token.getTokenText());
                break;
            } else {
                if (!matchToken(i + 1, TokenKind.KeywordToken, "from") &&
                    i + 1 < tokenVector.size()) {
                    token = tokenVector.get(i + 1);
                    throw new DBError("Unexpected token: \"" + token.getTokenText()
                            + "\" at position: " + token.getPosition());
                } else {
                    throw new DBError("Error parsing SELECT statement. Expected a FROM keyword");
                }
            }
        }

        i++;
        if (!matchToken(i, TokenKind.KeywordToken, "from")) {
            throw new DBError("Error parsing SELECT statement. Expected FROM keyword.");
        }

        i++;
        if (!matchToken(i, TokenKind.IdentifierToken)) {
            throw new DBError("Error parsing SELECT statement. Expected a table name.");
        }

        String tableName = tokenVector.elementAt(i).getTokenText();
        if (i == tokenVector.size() - 1) {
            return new DMLStatement.SelectStatement(tableName, attributeNames,
                    new Vector<>());
        }
        i++;
        token = tokenVector.elementAt(i);
        if (!matchToken(i, TokenKind.KeywordToken, "where")) {
            throw new DBError("Unexpected token: \"" + token.getTokenText() +
                    "\" at position: " + token.getPosition());
        }

        i++;
        Vector<Predicate> predicates = parsePredicates(i);

        if (predicates.isEmpty()) {
            throw new DBError("Error parsing the statement. Expected a list " +
                    "of predicates.");
        }

        return new DMLStatement.SelectStatement(tableName,
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

    private Vector<Predicate> parsePredicates(int i) throws DBError {
        Vector<Predicate> predicates = new Vector<>();
        for (; i < tokenVector.size(); i += 4) {
            try {
                Token attributeNameToken = tokenVector.elementAt(i);
                Token operatorToken = tokenVector.elementAt(i + 1);
                Token valueToken = tokenVector.elementAt(i + 2);
                if (i + 3 < tokenVector.size() - 1 && !matchToken(i + 3, TokenKind.CommaToken)) {
                    Token badToken = tokenVector.elementAt(i + 3);
                    throw new Exception("Unexpected token: \"" + badToken.getTokenText()
                        + "\" at position " + badToken.getPosition());
                }
                predicates.add(parsePredicate(attributeNameToken, operatorToken, valueToken));
            } catch (Exception e) {
                throw new DBError("Error parsing SELECT statement.");
            }
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
