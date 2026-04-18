package QueryParser.StatementParsers;

import QueryParser.DDLStatements.CreateTableStatement;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;
import RheaDB.Attribute;
import RheaDB.AttributeType;

import java.util.Locale;
import java.util.Vector;

public class CreateTableParser extends StatementParser {

    public CreateTableParser(Vector<Token> tokens, int position) {
        super(tokens);
        this.position = position;
    }

    public SQLStatement parse() {
        if (consumeToken(TokenKind.TableToken, "Expected TABLE after CREATE.") == null) {
            return null;
        }

        Token tableNameToken = consumeIdentifier("Expected table name after TABLE.");
        if (tableNameToken == null) {
            return null;
        }

        if (consumeToken(TokenKind.OpenParenToken, "Expected '(' before attribute definitions.") == null) {
            return null;
        }

        Vector<Attribute> attributes = new Vector<>();
        while (!matchToken(TokenKind.ClosedParenToken)) {
            Token attributeNameToken = consumeIdentifier("Expected attribute name.");
            Token attributeTypeToken = consumeToken(TokenKind.DataTypeToken, "Expected attribute type.");

            if (attributeNameToken == null || attributeTypeToken == null) {
                return null;
            }

            AttributeType type =
                    switch (attributeTypeToken.getTokenText().toLowerCase(Locale.ROOT)) {
                        case "int" -> AttributeType.INT;
                        case "string" -> AttributeType.STRING;
                        case "float" -> AttributeType.FLOAT;
                        default -> null;
                    };

            attributes.add(new Attribute(type,
                    attributeNameToken.getTokenText().toLowerCase(Locale.ROOT)));

            if (!matchToken(TokenKind.CommaToken)) {
                break;
            }

            advanceToken();
        }

        if (consumeToken(TokenKind.ClosedParenToken, "Expected ')' after attribute definitions.") == null) {
            return null;
        }

        consumeSemicolon();
        consumeEndOfInput();

        if (!diagnostics.isEmpty()) {
            return null;
        }

        return new CreateTableStatement(tableNameToken.getTokenText(), attributes);
    }
}
