package QueryParser.StatementParsers;

import QueryParser.DMLStatements.InsertStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;

import java.util.Vector;

public class InsertParser extends StatementParser {

    public InsertParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    public SQLStatement parse() {
        if (consumeToken(TokenKind.InsertToken, "Expected INSERT.") == null) {
            return null;
        }

        if (consumeToken(TokenKind.IntoToken, "Expected INTO after INSERT.") == null) {
            return null;
        }

        Token tableNameToken = consumeIdentifier("Expected table name after INTO.");
        if (tableNameToken == null) {
            return null;
        }

        if (consumeToken(TokenKind.ValuesToken, "Expected VALUES after table name.") == null) {
            return null;
        }

        if (consumeToken(TokenKind.OpenParenToken, "Expected '(' before values.") == null) {
            return null;
        }

        Vector<Object> valueObjects = new Vector<>();
        Token valueToken = consumeLiteral("Expected literal value.");
        if (valueToken == null) {
            return null;
        }
        valueObjects.add(valueToken.getValue());

        while (matchToken(TokenKind.CommaToken)) {
            advanceToken();
            valueToken = consumeLiteral("Expected literal value after ','.");
            if (valueToken == null) {
                return null;
            }
            valueObjects.add(valueToken.getValue());
        }

        if (consumeToken(TokenKind.ClosedParenToken, "Expected ')' after values.") == null) {
            return null;
        }

        consumeSemicolon();
        consumeEndOfInput();

        if (!diagnostics.isEmpty()) {
            return null;
        }

        return new InsertStatement(tableNameToken.getTokenText(), valueObjects);
    }
}
