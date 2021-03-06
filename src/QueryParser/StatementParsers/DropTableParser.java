package QueryParser.StatementParsers;

import QueryParser.DMLStatements.DropTableStatement;
import QueryParser.SQLStatement;
import RheaDB.DBError;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DropTableParser extends StatementParser {

    private final int TABLENAME_GROUP = 1;

    public DropTableParser(String line) {
        super(line);
        this.regex = "drop\\s+table\\s+(.*);";
    }

    @Override
    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            diagnostics.add("Error parsing drop table statement.");
            return null;
        }

        String tableName = matcher.group(TABLENAME_GROUP);

        return new DropTableStatement(tableName);
    }
}
