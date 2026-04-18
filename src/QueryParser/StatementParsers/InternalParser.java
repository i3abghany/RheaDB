package QueryParser.StatementParsers;

import QueryParser.InternalStatements.CompactStatement;
import QueryParser.InternalStatements.DescribeStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;

import java.util.Vector;

public class InternalParser extends StatementParser {

    public InternalParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    @Override
    public SQLStatement parse() {
        if (!matchToken(TokenKind.DescribeToken) && !matchToken(TokenKind.CompactToken)) {
            diagnostics.add(getUnexpectedTokenMessage(getCurrent(), "Expected DESCRIBE or COMPACT."));
            return null;
        }

        TokenKind statementKind = advanceToken().getKind();
        var tableNameToken = consumeIdentifier("Expected table name.");
        if (tableNameToken == null) {
            return null;
        }

        consumeSemicolon();
        consumeEndOfInput();

        if (!diagnostics.isEmpty()) {
            return null;
        }

        if (statementKind == TokenKind.DescribeToken) {
            return new DescribeStatement(tableNameToken.getTokenText());
        }

        return new CompactStatement(tableNameToken.getTokenText());
    }
}
