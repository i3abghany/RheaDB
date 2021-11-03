package QueryParser.StatementParsers;

import Predicate.Predicate;
import QueryParser.DMLStatements.UpdateStatement;
import QueryParser.SQLStatement;
import RheaDB.DBError;

import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateParser extends StatementParser {

    private final int TABLENAME_GROUP = 1;
    private final int SET_ATTRIBUTES_GROUP = 2;
    private final int PREDICATES_GROUP = 3;

    public UpdateParser(String line) {
        super(line);
        this.regex = "update\\s+(.*?)\\s*set\\s+(.*?)(\\s+where\\s+(.*)\\s*)?;";
    }

    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            throw new DBError("Error parsing the statement.");
        }

        String tableName = matcher.group(TABLENAME_GROUP);
        boolean usePredicates = matcher.group(PREDICATES_GROUP) != null;

        String setPredicatesString = matcher.group(SET_ATTRIBUTES_GROUP);
        Vector<Predicate> setPredicates =
                getPredicates(setPredicatesString.split(","));

        if (!usePredicates) {
            return new UpdateStatement(tableName, setPredicates,
                    new Vector<>());
        }

        String[] predicateStrings =
                Arrays.stream(matcher.group(PREDICATES_GROUP).split(","))
                        .map(String::trim)
                        .toArray(String[]::new);
        predicateStrings[0] = predicateStrings[0].split(" ", 2)[1];

        Vector<Predicate> predicates = getPredicates(predicateStrings);

        return new UpdateStatement(tableName, setPredicates,
                predicates);
    }
}
