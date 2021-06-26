package QueryProcessorTests;

import QueryProcessor.Lexer;
import QueryProcessor.Token;
import QueryProcessor.TokenKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Vector;
import java.util.stream.Collectors;

public class LexerTests {
    @Test
    public void lexEmptyStatementTest() {
        String statement = "";
        Vector<Token> tokenVector = new Lexer(statement).lex();
        Assertions.assertEquals(tokenVector.size(), 0);
    }

    public static boolean matchToken(Token t, TokenKind kind, String text) {
        return t.getKind() == kind && t.getTokenText().equals(text);
    }

    public static boolean matchToken(Token t, TokenKind kind) {
        return t.getKind() == kind;
    }

    public static boolean whiteSpaceSeparation(Vector<Token> tokens) {
        boolean ret = true;
        for (int i = 1; i < tokens.size(); i += 2) {
            ret &= matchToken(tokens.get(i), TokenKind.WhiteSpaceToken);
        }
        return ret;
    }

    public static Vector<Token> removeWhiteSpaces(Vector<Token> tokens) {
        return tokens.stream()
                .filter(t -> t.getKind() != TokenKind.WhiteSpaceToken)
                .collect(Collectors.toCollection(Vector::new));
    }

    @Test
    public void lexKeywords() {
        String statement = "SELECT TABLE VALUES DELETE INDEX INTO WHERE DROP";
        Vector<Token> tokenVector = new Lexer(statement).lex();

        Assertions.assertTrue(whiteSpaceSeparation(tokenVector));
        tokenVector = removeWhiteSpaces(tokenVector);

        String[] tokenTexts = Arrays.stream(statement.split(" "))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toArray(String[]::new);

        for (int i = 0; i < tokenTexts.length; i++) {
            Assertions.assertTrue(matchToken(tokenVector.get(i), TokenKind.KeywordToken, tokenTexts[i]));
        }
    }

    @Test
    public void lexDataTypes() {
        String statement = "INT STRING FLOAT";
        Vector<Token> tokenVector = new Lexer(statement).lex();

        Assertions.assertTrue(whiteSpaceSeparation(tokenVector));
        tokenVector = removeWhiteSpaces(tokenVector);

        String[] tokenTexts = Arrays.stream(statement.split(" "))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toArray(String[]::new);

        for (int i = 0; i < tokenTexts.length; i++) {
            Assertions.assertTrue(matchToken(tokenVector.get(i), TokenKind.DataTypeToken, tokenTexts[i]));
        }
    }
}
