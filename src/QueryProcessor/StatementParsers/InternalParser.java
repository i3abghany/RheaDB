package QueryProcessor.StatementParsers;

import QueryProcessor.DMLStatement;
import QueryProcessor.InternalStatement;
import QueryProcessor.SQLStatement;
import RheaDB.DBError;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternalParser extends StatementParser {

    public int STATEMENT_GROUP = 1;
    public int TABLENAME_GROUP = 2;

    public InternalParser(String line) {
        super(line);
        this.regex = "^(describe|compact)\\s+(.*?)\\s*;";
    }

    @Override
    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            throw new DBError("Error parsing the statement.");
        }

        String statement = matcher.group(STATEMENT_GROUP);
        String tableName = matcher.group(TABLENAME_GROUP);

        if (statement.toLowerCase(Locale.ROOT).equals("describe")) {
            return new InternalStatement.DescribeStatement(tableName);
        } else {
            return new InternalStatement.CompactStatement(tableName);
        }
    }
}
