package QueryParser;

import QueryParser.StatementParsers.*;
import RheaDB.DBError;

import java.util.Vector;
import java.util.stream.Collectors;

public class Parser {
    private final Vector<Token> tokenVector;
    private final String line;
    private int position = 0;

    public Parser(String line) {
        this.line = line;
        tokenVector = new Lexer(line)
                .lex()
                .stream()
                .filter(tok -> tok.getKind() != TokenKind.WhiteSpaceToken)
                .collect(Collectors.toCollection(Vector::new));
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

        Token token = getCurrent();

        if (SQLStatement.isDDLKeyword(token))
            return parseDDL();
        else if (SQLStatement.isDMLKeyword(token))
            return parseDML();
        else if (SQLStatement.isInternalKeyword(token))
            return parseInternalStatement();
        else
            throw new DBError("Unexpected token: \"" +
                    tokenVector.get(0).getTokenText() + "\".");
    }

    private SQLStatement parseInternalStatement() throws DBError {
        return new InternalParser(line).parse();
    }

    private SQLStatement parseDDL() throws DBError {

        if (matchToken(TokenKind.CreateToken)) {
            return parseCreate();
        } else {
            throw new DBError("Unexpected token: \"" + getCurrent().getTokenText()
                    + "\" at position " + getCurrent().getPosition());
        }
    }

    private SQLStatement parseCreate() throws DBError {
        Token typeToken = nextToken();

        if (matchToken(TokenKind.TableToken))
            return parseCreateTable();
        else if (matchToken(TokenKind.IndexToken))
            return parseCreateIndex();
        else
            throw new DBError("Unexpected token: \"" + typeToken.getTokenText()
                    + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseCreateTable() throws DBError {
        return new CreateTableParser(line).parse();
    }

    private SQLStatement parseCreateIndex() throws DBError {
        return new CreateIndexParser(line).parse();
    }

    private SQLStatement parseDML() throws DBError {
        Token typeToken = getCurrent();

        if (matchToken(TokenKind.SelectToken))
            return parseSelect();
        else if (matchToken(TokenKind.InsertToken))
            return parseInsert();
        else if (matchToken(TokenKind.DeleteToken))
            return parseDelete();
        else if (matchToken(TokenKind.DropToken))
            return parseDrop();
        else if (matchToken(TokenKind.UpdateToken))
            return parseUpdate();
        else
            throw new DBError("Unexpected token: \"" + typeToken.getTokenText()
                    + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseUpdate() throws DBError {
        return new UpdateParser(line).parse();
    }

    private SQLStatement parseDrop() throws DBError {
        Token typeToken = nextToken();
        if (matchToken(TokenKind.TableToken))
            return parseDropTable();
        else if (matchToken(TokenKind.IndexToken))
            return parseDropIndex();
        else
            throw new DBError("Unexpected token: \"" + typeToken.getTokenText()
                    + "\" at position " + typeToken.getPosition());
    }

    private SQLStatement parseDropIndex() throws DBError {
        return new DropIndexParser(line).parse();
    }

    private SQLStatement parseDropTable() throws DBError {
        return new DropTableParser(line).parse();
    }

    private SQLStatement parseDelete() throws DBError {
        return new DeleteParser(line).parse();
    }

    private SQLStatement parseInsert() throws DBError {
        return new InsertParser(line).parse();
    }

    private SQLStatement parseSelect() throws DBError {
        return new SelectParser(line).parse();
    }

    private boolean matchToken(TokenKind tokenKind) {
        Token currentToken = getCurrent();

        if (currentToken == null) {
            return false;
        }

        return currentToken.getKind() == tokenKind;
    }

    private Token getCurrent() {
        return peekToken(0);
    }

    private Token nextToken() {
        Token token = getCurrent();
        position++;
        return token;
    }

    private Token peekToken(int offset) {
        int index = position + offset;

        if (index >= tokenVector.size())
            return null;

        return tokenVector.get(index);
    }
}
