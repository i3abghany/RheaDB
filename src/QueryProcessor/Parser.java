package QueryProcessor;

import Predicate.*;
import Predicate.Predicate;
import RheaDB.Attribute;
import RheaDB.AttributeType;
import RheaDB.SQLException;

import java.util.Vector;
import java.util.stream.Collectors;

/*
* CREATE TABLE tableName col1 DTYPE, col2 DTYPE, ...
* SELECT col1, col2, col3, col4 FROM tableName WHERE colN = xxx && colK = yyy || colM = zzz
* INSERT INTO tableName VAL1 VAL2 VAL3 VAL4 VAL5
* */

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

        if (SQLStatement.isDDLKeyword(tokenVector.get(0)))
            return parseDDL();
        else if (SQLStatement.isDMLKeyword(tokenVector.get(0)))
            return parseDML();
        else
            throw new SQLException("Could not identify the statement.");
    }

    private SQLStatement parseDDL() {
        if (tokenVector.size() == 1)
            return null;

        if (tokenVector.get(1).getTokenText().equals("table"))
            return parseCreateTable();
        else if (tokenVector.get(1).getTokenText().equals("index"))
            return parseCreateIndex();
        else
            return null;
    }

    private SQLStatement parseCreateTable() {
        if (tokenVector.size() == 2)
            return null;
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

    private SQLStatement parseCreateIndex() {
        if (tokenVector.size() < 4)
            return null;
        Token tableNameToken = tokenVector.get(2);
        Token attributeNameToken = tokenVector.get(3);
        if (tableNameToken.getKind() != TokenKind.IdentifierToken ||
            attributeNameToken.getKind() != TokenKind.IdentifierToken)
            return null;

        String tableName = tableNameToken.getTokenText();
        String attributeName = attributeNameToken.getTokenText();

        return new DDLStatement.CreateIndexStatement(tableName, attributeName);
    }

    private SQLStatement parseDML() {
        if (tokenVector.size() == 1)
            return null;
        if (tokenVector.get(0).getTokenText().equals("select"))
            return parseSelect();
        else if (tokenVector.get(0).getTokenText().equals("insert"))
            return parseInsert();

        return null;
    }

    // insert into myTableName 10 "John Doe" 3.14
    private SQLStatement parseInsert() {
        assert tokenVector.size() > 3;
        assert tokenVector.elementAt(0).getTokenText().equals("insert");
        assert tokenVector.elementAt(1).getTokenText().equals("into");
        assert tokenVector.elementAt(2).getKind() == TokenKind.IdentifierToken;

        String tableName = tokenVector.elementAt(2).getTokenText();
        Vector<Object> objectList = new Vector<>();

        for (int i = 3; i < tokenVector.size() - 1; i++)
            objectList.add(tokenVector.elementAt(i).getValue());

        return new DMLStatement.InsertStatement(tableName, objectList.toArray());
    }

    private SQLStatement parseSelect() {
        Vector<String> attributeNames = new Vector<>();
        Vector<Predicate> predicates = new Vector<>();
        Token token;
        int i = 1;
        for (; i < tokenVector.size(); i++) {
            token = tokenVector.elementAt(i);
            if (token.getTokenText().equals("from")) {
                break;
            } else if (token.getKind() == TokenKind.KeywordToken) {
                return null;
            }
            attributeNames.add(token.getTokenText());
        }

        if (i == tokenVector.size() - 1)
            return null;

        if (!tokenVector.elementAt(i).getTokenText().equals("from"))
            return null;
        i++;

        if (i == tokenVector.size() - 1)
            return null;

        String tableName = tokenVector.elementAt(i).getTokenText();
        if (i == tokenVector.size() - 1) {
            return new DMLStatement.SelectStatement(tableName,
                    (String[]) attributeNames.toArray(),
                    null);
        }
        i++;
        token = tokenVector.elementAt(i);
        if (!token.getTokenText().equals("where"))
            return null;

        i++;
        for (; i < tokenVector.size(); i += 4) {
            try {
                Token attributeNameToken = tokenVector.elementAt(i);
                Token operatorToken = tokenVector.elementAt(i + 1);
                Token valueToken = tokenVector.elementAt(i + 2);
                if (i + 3 < tokenVector.size() - 1 && !matchToken(i + 3, TokenKind.CommaToken)) {
                    throw new Exception("Love must be forgotten. Life can always start up anew");
                }
                predicates.add(parsePredicate(attributeNameToken, operatorToken, valueToken));
            } catch (Exception e) {
                System.out.println("Error parsing SELECT statement.");
            }
        }

        return new DMLStatement.SelectStatement(tableName,
                 attributeNames.toArray(new String[0]),
                 predicates.toArray(new Predicate[0]));
    }

    private boolean matchToken(int i, TokenKind tokenKind) {
        if (i >= tokenVector.size())
            return false;

        return tokenVector.elementAt(i).getKind() == tokenKind;
    }

    private Predicate parsePredicate(Token attributeName, Token operatorToken, Token valueToken) {
        return switch (operatorToken.getKind()) {
            case EqualsToken -> new EqualsPredicate(attributeName.getTokenText(), valueToken.getValue());
            case NotEqualsToken -> new NotEqualsPredicate(attributeName.getTokenText(), valueToken.getValue());
            case GreaterToken -> new GreaterThanPredicate(attributeName.getTokenText(), valueToken.getValue());
            case GreaterEqualsToken -> new GreaterThanEqualPredicate(attributeName.getTokenText(), valueToken.getValue());
            case LessToken -> new LessThanPredicate(attributeName.getTokenText(), valueToken.getValue());
            case LessEqualsToken -> new LessThanEqualPredicate(attributeName.getTokenText(), valueToken.getValue());
            default -> null;
        };
    }
}
