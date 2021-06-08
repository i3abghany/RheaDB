package QueryProcessor;

import Predicate.*;
import Predicate.Predicate;
import RheaDB.Attribute;
import RheaDB.AttributeType;
import RheaDB.SQLException;

import java.util.Vector;
import java.util.stream.Collectors;

public class Parser {
    private final Vector<Token> tokenVector;

    public Parser(String line) {
        this.tokenVector = new Lexer(line)
                .lex()
                .stream()
                .filter(tok -> tok.getKind() != TokenKind.WhiteSpaceToken)
                .collect(Collectors.toCollection(Vector::new));
    }

    public SQLStatement parse() throws SQLException {
        if (tokenVector.isEmpty())
            return null;

        Token badToken = tokenVector
                .stream()
                .filter(tok -> tok.getKind() == TokenKind.BadToken)
                .findAny()
                .orElse(null);

        if (badToken != null) {
            throw new SQLException("Bad token: \"" + badToken.getTokenText() +
                    "\" at position: " + badToken.getPosition());
        }

        if (SQLStatement.isDDLKeyword(tokenVector.get(0)))
            return parseDDL();
        else if (SQLStatement.isDMLKeyword(tokenVector.get(0)))
            return parseDML();
        else
            throw new SQLException("Could not identify the statement.");
    }

    private SQLStatement parseDDL() throws SQLException {
        if (tokenVector.size() == 1) {
            throw new SQLException("Error parsing statement.");
        }

        Token typeToken = tokenVector.elementAt(1);

        if (matchToken(1, TokenKind.KeywordToken, "table"))
            return parseCreateTable();
        else if (matchToken(1, TokenKind.KeywordToken, "index"))
            return parseCreateIndex();
        else
            throw new SQLException("Unexpected token: \"" + typeToken.getTokenText()
                + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseCreateTable() throws SQLException {
        if (tokenVector.size() < 3) {
            throw new SQLException("Error parsing Create Table statement.");
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

        return new DDLStatement.CreateTableStatement(tableName, attributeVector);
    }

    private SQLStatement parseCreateIndex() throws SQLException {
        if (tokenVector.size() < 4) {
            throw new SQLException("Error parsing Create Index statement.");
        }

        Token tableNameToken = tokenVector.get(2);
        Token attributeNameToken = tokenVector.get(3);

        if (!matchToken(2, TokenKind.IdentifierToken)) {
            throw new SQLException("Unexpected token: \"" + tableNameToken.getTokenText() +
                    "\" at position " + tableNameToken.getPosition());
        }

        if (!matchToken(3, TokenKind.IdentifierToken)) {
            throw new SQLException("Unexpected token: \"" + attributeNameToken.getTokenText() +
                    "\" at position " + attributeNameToken.getPosition());
        }

        String tableName = tableNameToken.getTokenText();
        String attributeName = attributeNameToken.getTokenText();

        return new DDLStatement.CreateIndexStatement(tableName, attributeName);
    }

    private SQLStatement parseDML() throws SQLException {
        if (tokenVector.size() == 1)
            return null;

        Token typeToken = tokenVector.elementAt(0);

        if (matchToken(0, TokenKind.KeywordToken, "select"))
            return parseSelect();
        else if (matchToken(0, TokenKind.KeywordToken, "insert"))
            return parseInsert();
        else
            throw new SQLException("Unexpected token: \"" + typeToken.getTokenText()
                    + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseInsert() throws SQLException {
        assert tokenVector.size() > 3;

        assert matchToken(0, TokenKind.KeywordToken, "insert");
        assert matchToken(1, TokenKind.KeywordToken, "into");
        assert matchToken(2, TokenKind.IdentifierToken);

        String tableName = tokenVector.elementAt(2).getTokenText();
        Vector<Object> objectList = new Vector<>();

        for (int i = 3; i < tokenVector.size(); i++) {
            Token t = tokenVector.elementAt(i);
            if (!t.isLiteral()) {
                throw new SQLException("Invalid token: " + t.getTokenText() + ". Expected a literal.");
            }
            objectList.add(tokenVector.elementAt(i).getValue());
        }

        return new DMLStatement.InsertStatement(tableName, objectList);
    }

    private SQLStatement parseSelect() throws SQLException {
        Vector<String> attributeNames = new Vector<>();
        Vector<Predicate> predicates = new Vector<>();
        Token token;
        int i = 1;
        for (; i < tokenVector.size(); i++) {
            token = tokenVector.elementAt(i);
            if (token.getTokenText().equals("from")) {
                break;
            } else if (!matchToken(i, TokenKind.IdentifierToken)) {
                throw new SQLException("Unexpected token: \"" + token.getTokenText()
                    + "\" at position: " + token.getPosition());
            }
            attributeNames.add(token.getTokenText());
        }

        if (i > tokenVector.size() - 2) {
            throw new SQLException("Error parsing the statement.");
        }

        if (!matchToken(i, TokenKind.KeywordToken, "from")) {
            token = tokenVector.elementAt(i);
            throw new SQLException("Unexpected token: \"" + token.getTokenText() +
                "\" at position: " + token.getPosition());
        }
        i++;

        String tableName = tokenVector.elementAt(i).getTokenText();
        if (i == tokenVector.size() - 1) {
            return new DMLStatement.SelectStatement(tableName, attributeNames,
                    new Vector<>());
        }
        i++;
        token = tokenVector.elementAt(i);
        if (!matchToken(i, TokenKind.KeywordToken, "where")) {
            throw new SQLException("Unexpected token: \"" + token.getTokenText() +
                    "\" at position: " + token.getPosition());
        }

        i++;
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
                throw new SQLException("Error parsing SELECT statement.");
            }
        }

        return new DMLStatement.SelectStatement(tableName,
                 attributeNames, predicates);
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
}
