package QueryParser.StatementParsers;

import QueryParser.DMLStatements.DropTableStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;

import java.util.Vector;

public class DropTableParser extends StatementParser {

    public DropTableParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    @Override
    public SQLStatement parse() {
        if (consumeToken(QueryParser.TokenKind.TableToken, "Expected TABLE after DROP.") == null) {
            return null;
        }

        Token tableNameToken = consumeIdentifier("Expected table name after TABLE.");
        if (tableNameToken == null) {
            return null;
        }

        consumeSemicolon();
        consumeEndOfInput();

        if (!diagnostics.isEmpty()) {
            return null;
        }

        return new DropTableStatement(tableNameToken.getTokenText());
    }
}
