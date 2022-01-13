package QueryParser.StatementParsers;

import Predicate.Predicate;
import Predicate.PredicateFactory;
import QueryParser.Lexer;
import QueryParser.SQLStatement;
import QueryParser.Token;
import QueryParser.TokenKind;
import RheaDB.DBError;

import java.util.Vector;
import java.util.stream.Collectors;

public abstract class StatementParser {
    protected String line;
    protected String regex;

    public StatementParser(String line) {
        this.line = line;
    }

    public abstract SQLStatement parse() throws DBError;

    protected Predicate getPredicate(String attributeName, TokenKind operatorKind, Object value) {
        return PredicateFactory.of(attributeName, operatorKind, value);
    }

    protected Vector<Predicate> getPredicates(String[] predicateStrings) {
        Vector<Predicate> predicates = new Vector<>();

        for (String predicate : predicateStrings) {
            Vector<Token> tokens = new Lexer(predicate).lex()
                    .stream()
                    .filter(t -> t.getKind() != TokenKind.WhiteSpaceToken)
                    .collect(Collectors.toCollection(Vector::new));

            predicates.add(getPredicate(tokens.elementAt(0).getTokenText(),
                    tokens.elementAt(1).getKind(),
                    tokens.elementAt(2).getValue()));
        }
        return predicates;
    }
}
