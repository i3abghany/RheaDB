package QueryParser.StatementParsers;

import QueryParser.DMLStatements.InsertStatement;
import QueryParser.Lexer;
import QueryParser.SQLStatement;
import QueryParser.Token;
import RheaDB.DBError;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InsertParser extends StatementParser {

    private final int TABLENAME_GROUP = 1;
    private final int VALUES_GROUP = 2;

    public InsertParser(String line) {
        super(line);
        this.regex = "insert\\s+into\\s+(.*?)\\s*values\\s*\\((.*)\\s*\\);";
    }

    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            diagnostics.add("Error parsing insert statement.");
            return null;
        }

        String tableName = matcher.group(TABLENAME_GROUP);
        String valuesStrings = matcher.group(VALUES_GROUP);

        Vector<Token> tokens = new Lexer(valuesStrings)
                .lex()
                .stream()
                .filter(Token::isLiteral)
                .collect(Collectors.toCollection(Vector::new));

        Vector<Object> valueObjects =
                tokens.stream()
                        .map(Token::getValue)
                        .collect(Collectors.toCollection(Vector::new));

        return new InsertStatement(tableName, valueObjects);
    }
}
