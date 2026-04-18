package QueryParser.StatementParsers;

import QueryParser.DMLStatements.DropIndexStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;

import java.util.Vector;

public class DropIndexParser extends StatementParser {

    public DropIndexParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    @Override
    public SQLStatement parse() {
        if (consumeToken(QueryParser.TokenKind.IndexToken, "Expected INDEX after DROP.") == null) {
            return null;
        }

        Token tableNameToken = consumeIdentifier("Expected table name after INDEX.");
        Token attributeNameToken = consumeIdentifier("Expected attribute name after table name.");

        if (tableNameToken == null || attributeNameToken == null) {
            return null;
        }

        consumeSemicolon();
        consumeEndOfInput();

        if (!diagnostics.isEmpty()) {
            return null;
        }

        return new DropIndexStatement(tableNameToken.getTokenText(), attributeNameToken.getTokenText());
    }
}
