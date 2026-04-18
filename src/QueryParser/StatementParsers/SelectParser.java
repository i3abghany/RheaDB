package QueryParser.StatementParsers;

import Predicate.Predicate;
import QueryParser.DMLStatements.SelectStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;

import java.util.Vector;

public class SelectParser extends StatementParser {

    public SelectParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    public SQLStatement parse() {
        if (consumeToken(TokenKind.SelectToken, "Expected SELECT.") == null) {
            return null;
        }

        Vector<String> attributeNames = new Vector<>();
        Token attributeToken = consumeIdentifier("Expected selected attribute.");
        if (attributeToken == null) {
            return null;
        }
        attributeNames.add(attributeToken.getTokenText());

        while (matchToken(TokenKind.CommaToken)) {
            advanceToken();
            attributeToken = consumeIdentifier("Expected selected attribute after ','.");
            if (attributeToken == null) {
                return null;
            }
            attributeNames.add(attributeToken.getTokenText());
        }

        if (consumeToken(TokenKind.FromToken, "Expected FROM after selected attributes.") == null) {
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

        return new SelectStatement(tableNameToken.getTokenText(), attributeNames, predicates);
    }
}
