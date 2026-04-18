package QueryParser.StatementParsers;

import Predicate.Predicate;
import QueryParser.DMLStatements.UpdateStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;

import java.util.Vector;

public class UpdateParser extends StatementParser {

    public UpdateParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    public SQLStatement parse() {
        if (consumeToken(TokenKind.UpdateToken, "Expected UPDATE.") == null) {
            return null;
        }

        Token tableNameToken = consumeIdentifier("Expected table name after UPDATE.");
        if (tableNameToken == null) {
            return null;
        }

        if (consumeToken(TokenKind.SetTotken, "Expected SET after table name.") == null) {
            return null;
        }

        Vector<Predicate> setPredicates = parsePredicateList();
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

        return new UpdateStatement(tableNameToken.getTokenText(), setPredicates, predicates);
    }
}
