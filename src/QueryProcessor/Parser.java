package QueryProcessor;

import Predicate.*;
import Predicate.Predicate;
import QueryProcessor.StatementParsers.*;
import RheaDB.DBError;

import java.util.Vector;
import java.util.stream.Collectors;

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
        return new CreateTableParser(line).parse();
    }

    private SQLStatement parseCreateIndex() throws DBError {
        return new CreateIndexParser(line).parse();
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
        return new UpdateParser(line).parse();
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

}
