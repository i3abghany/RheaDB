package QueryParser.StatementParsers;

import QueryParser.DDLStatements.CreateIndexStatement;
import QueryParser.SQLStatement;
import RheaDB.DBError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateIndexParser extends StatementParser {

    private final int TABLENAME_GROUP = 1;
    private final int ATTRIBUTE_GROUP = 2;

    public CreateIndexParser(String line) {
        super(line);
        this.regex = "create\\s+index\\s+(.*)\\s+(.*)\\s*;";
    }

    @Override
    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            throw new DBError("Error parsing the statement.");
        }

        String tableName = matcher.group(TABLENAME_GROUP);
        String attributeName = matcher.group(ATTRIBUTE_GROUP);

        return new CreateIndexStatement(tableName, attributeName);
    }
}
