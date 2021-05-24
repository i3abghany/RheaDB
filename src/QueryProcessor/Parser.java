package QueryProcessor;

import RheaDB.Attribute;
import RheaDB.AttributeType;

import java.util.Vector;
import java.util.stream.Collectors;

/*
* CREATE TABLE tableName (col1 DTYPE, col2 DTYPE, ...)
* SELECT col1, col2, col3, col4 FROM tableName WHERE colN = xxx && colK = yyy || colM = zzz
* INSERT INTO tableName (VAL1 VAL2 VAL3 VAL4 VAL5)
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

    public SQLStatement parse() {
        if (tokenVector.isEmpty())
            return null;

        if (SQLStatement.isDDLKeyword(tokenVector.get(0)))
            return parseDDL();
        else if (SQLStatement.isDMLKeyword(tokenVector.get(0)))
            return parseDML();
        else
            return null;
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
        return null;
    }
}
