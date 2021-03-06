package QueryParser.StatementParsers;

import Predicate.Predicate;
import QueryParser.DMLStatements.DeleteStatement;
import QueryParser.SQLStatement;
import RheaDB.DBError;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteParser extends StatementParser {

    private final int TABLENAME_GROUP = 1;
    private final int PREDICATES_GROUP = 2;

    public DeleteParser(String line) {
        super(line);
        this.regex = "delete\\s+from\\s+(.*?)\\s*(where\\s(.*?)\\s*)?;";
    }

    @Override
    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            diagnostics.add("Error parsing delete statement.");
            return null;
        }

        String tableName = matcher.group(TABLENAME_GROUP);
        boolean usePredicates = matcher.group(PREDICATES_GROUP) != null;

        if (!usePredicates) {
            return new DeleteStatement(tableName, new Vector<>());
        }

        String[] predicateStrings = matcher.group(PREDICATES_GROUP).split(",");
        predicateStrings[0] = predicateStrings[0].split(" ", 2)[1];

        Vector<Predicate> predicates = getPredicates(predicateStrings);
        return new DeleteStatement(tableName, predicates);
    }
}
