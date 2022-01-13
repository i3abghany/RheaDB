package QueryParser;

import QueryParser.StatementParsers.*;
import RheaDB.DBError;

import java.util.Vector;
import java.util.stream.Collectors;

public class Parser {
    private final Vector<Token> tokenVector;
    private final Vector<String> diagnostics = new Vector<>();
    private final Lexer lexer;
    private final String line;
    private int position = 0;

    public Parser(String line) {
        this.line = line;
        this.lexer = new Lexer(line);
        tokenVector = lexer.lex()
                .stream()
                .filter(tok -> tok.getKind() != TokenKind.WhiteSpaceToken)
                .collect(Collectors.toCollection(Vector::new));
        diagnostics.addAll(lexer.getDiagnostics());
    }

    public SQLStatement parse() throws DBError {
        throwIfAny();
        Token token = getCurrent();

        /* Empty statement */
        if (token == null) {
            return null;
        }

        SQLStatement ret = null;
        if (SQLStatement.isDDLKeyword(token))
            ret = parseDDL();
        else if (SQLStatement.isDMLKeyword(token))
            ret = parseDML();
        else if (SQLStatement.isInternalKeyword(token))
            ret = parseInternalStatement();
        else
            diagnostics.add("Unexpected token: \"" + tokenVector.get(0).getTokenText() + "\".");

        throwIfAny();
        return ret;
    }

    private void throwIfAny() throws DBError {
        if (!diagnostics.isEmpty()) {
            throw new DBError(diagnostics.get(0));
        }
    }

    private SQLStatement parseInternalStatement() throws DBError {
        return new InternalParser(line).parse();
    }

    private SQLStatement parseDDL() throws DBError {
        SQLStatement ret = null;
        if (matchToken(TokenKind.CreateToken)) {
            nextToken();
            ret = parseCreate();
        } else {
            diagnostics.add("Unexpected token: \"" + getCurrent().getTokenText() + "\" at position " + getCurrent().getPosition());
        }

        return ret;
    }

    private SQLStatement parseCreate() throws DBError {
        Token typeToken = getCurrent();

        if (typeToken == null) {
            diagnostics.add("Expected TableToken or IndexToken.");
            return null;
        }

        SQLStatement ret = null;
        if (matchToken(TokenKind.TableToken))
            ret = parseCreateTable();
        else if (matchToken(TokenKind.IndexToken))
            ret = parseCreateIndex();
        else
            diagnostics.add("Unexpected token: \"" + typeToken.getTokenText() + "\" at position " + typeToken.getPosition());

        return ret;
    }

    private SQLStatement parseCreateTable() throws DBError {
        var p = new CreateTableParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
    }

    private SQLStatement parseCreateIndex() throws DBError {
        var p = new CreateIndexParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
    }

    private SQLStatement parseDML() throws DBError {
        Token typeToken = getCurrent();
        SQLStatement ret = null;

        if (matchToken(TokenKind.SelectToken))
            ret = parseSelect();
        else if (matchToken(TokenKind.InsertToken))
            ret = parseInsert();
        else if (matchToken(TokenKind.DeleteToken))
            ret = parseDelete();
        else if (matchToken(TokenKind.DropToken))
            ret = parseDrop();
        else if (matchToken(TokenKind.UpdateToken))
            ret = parseUpdate();
        else
            diagnostics.add("Unexpected token: \"" + typeToken.getTokenText() + "\" at position " + typeToken.getPosition());

        return ret;
    }

    private SQLStatement parseUpdate() throws DBError {
        var p = new UpdateParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
    }

    private SQLStatement parseDrop() throws DBError {
        Token typeToken = nextToken();
        SQLStatement ret = null;

        if (matchToken(TokenKind.TableToken))
            ret = parseDropTable();
        else if (matchToken(TokenKind.IndexToken))
            ret = parseDropIndex();
        else
            diagnostics.add("Unexpected token: \"" + typeToken.getTokenText() + "\" at position " + typeToken.getPosition());

        return ret;
    }

    private SQLStatement parseDropIndex() throws DBError {
        var p = new DropIndexParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
    }

    private SQLStatement parseDropTable() throws DBError {
        var p = new DropTableParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
    }

    private SQLStatement parseDelete() throws DBError {
        var p = new DeleteParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
    }

    private SQLStatement parseInsert() throws DBError {
        var p = new InsertParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
    }

    private SQLStatement parseSelect() throws DBError {
        var p = new SelectParser(line);
        var ret = p.parse();
        diagnostics.addAll(p.getDiagnostics());
        return ret;
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

    public Vector<String> getDiagnostics() {
        return diagnostics;
    }
}
