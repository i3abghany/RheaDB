package QueryParser.StatementParsers;

import Predicate.Predicate;
import QueryParser.DMLStatements.DeleteStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;

import java.util.Vector;

public class DeleteParser extends StatementParser {

    public DeleteParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    @Override
    public SQLStatement parse() {
        if (consumeToken(TokenKind.DeleteToken, "Expected DELETE.") == null) {
            return null;
        }

        if (consumeToken(TokenKind.FromToken, "Expected FROM after DELETE.") == null) {
            return null;
        }

        Token tableNameToken = consumeIdentifier("Expected table name after FROM.");
        if (tableNameToken == null) {
            return null;
        }

        Vector<Predicate> predicates = new Vector<>();
        if (matchToken(TokenKind.WhereToken)) {
            advanceToken();
            predicates = parsePredicateList();
        }

        consumeSemicolon();
        consumeEndOfInput();

        if (!diagnostics.isEmpty()) {
            return null;
        }

        return new DeleteStatement(tableNameToken.getTokenText(), predicates);
    }
}
