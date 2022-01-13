package QueryParser.StatementParsers;

import QueryParser.DDLStatements.CreateTableStatement;
import QueryParser.Lexer;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;
import RheaDB.Attribute;
import RheaDB.AttributeType;
import RheaDB.DBError;

import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CreateTableParser extends StatementParser {

    private final int TABLENAME_GROUP = 1;
    private final int ATTRIBUTES_GROUP = 2;

    public CreateTableParser(String line) {
        super(line);
        this.regex = "create\\s+table\\s+(.*?)\\s*\\((.*)\\s*\\);";
    }

    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            diagnostics.add("Error parsing create table statement.");
            return null;
        }

        String tableName = matcher.group(TABLENAME_GROUP);
        String attributesStrings = matcher.group(ATTRIBUTES_GROUP);

        Vector<Token> tokens = new Lexer(attributesStrings)
                .lex()
                .stream()
                .filter(t -> t.getKind() == TokenKind.IdentifierToken ||
                        t.getKind() == TokenKind.DataTypeToken)
                .collect(Collectors.toCollection(Vector::new));

        Vector<Attribute> attributes = new Vector<>();

        for (int i = 0; i < tokens.size(); i += 2) {
            Token attributeNameToken = tokens.get(i);
            Token attributeTypeToken = tokens.get(i + 1);

            if (attributeTypeToken == null ||
                    attributeNameToken.getKind() != TokenKind.IdentifierToken ||
                    attributeTypeToken.getKind() != TokenKind.DataTypeToken) {
                throw new DBError("Error parsing the statement.");
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
        }

        return new CreateTableStatement(tableName, attributes);
    }
}
