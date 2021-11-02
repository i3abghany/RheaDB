package QueryParser.StatementParsers;

import Predicate.Predicate;
import QueryParser.DMLStatements.DMLStatement;
import QueryParser.SQLStatement;
import RheaDB.DBError;

import java.util.Arrays;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SelectParser extends StatementParser {

    private final int ATTRIBUTES_GROUP = 1;
    private final int TABLENAME_GROUP = 2;
    private final int PREDICATES_GROUP = 3;

    public SelectParser(String line) {
        super(line);
        this.regex = "select\\s+(.*?)\\s*from\\s+(.*?)\\s*(where\\s(.*?)\\s*)?;";
    }

    public SQLStatement parse() throws DBError {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) {
            throw new DBError("Error parsing the statement.");
        }

        Vector<String> attributeNames =
                Arrays.stream(matcher.group(ATTRIBUTES_GROUP).split(","))
                        .map(String::trim)
                        .collect(Collectors.toCollection(Vector::new));
        String tableName = matcher.group(TABLENAME_GROUP);
        boolean usePredicates = matcher.group(PREDICATES_GROUP) != null;

        if (!usePredicates) {
            return new DMLStatement.SelectStatement(tableName, attributeNames
                    , new Vector<>());
        }

        String[] predicateStrings = matcher.group(PREDICATES_GROUP).split(",");
        predicateStrings[0] = predicateStrings[0].split(" ", 2)[1];

        Vector<Predicate> predicates = getPredicates(predicateStrings);

        return new DMLStatement.SelectStatement(tableName, attributeNames,
                predicates);
    }
}
