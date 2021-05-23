package QueryProcessor;

import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

public class Lexer {
    private final String text;
    private int position;

    private final HashSet<String> keywordSet = new HashSet<>();

    public Lexer(String input) {
        this.text = input;
        this.position = 0;
        populateKeywordSet();
    }

    private void populateKeywordSet() {
        keywordSet.add("create");
        keywordSet.add("from");
        keywordSet.add("insert");
        keywordSet.add("into");
        keywordSet.add("select");
        keywordSet.add("table");
        keywordSet.add("where");
    }

    public Vector<Token> lex() {
        Vector<Token> tokens = new Vector<>();
        while (position < text.length()) {
            int tokenPosition = position;
            String tokenText;

            if (Character.isWhitespace(getCurr())) {
                while (inBounds() && Character.isWhitespace(getCurr()))
                    advance();
                tokenText = text.substring(tokenPosition, position);
                tokens.add(new Token(tokenPosition, tokenText, null, TokenKind.WhiteSpaceToken));
            } else if (Character.isDigit(getCurr())) {
                while (inBounds() && (Character.isDigit(getCurr()) || getCurr() == '.'))
                    advance();

                tokenText = text.substring(tokenPosition, position);
                if (tokenText.contains(".")) {
                    tokens.add(new Token(tokenPosition, tokenText, Float.parseFloat(tokenText), TokenKind.FloatingPointToken));
                } else {
                    tokens.add(new Token(tokenPosition, tokenText, Integer.parseInt(tokenText), TokenKind.IntegralToken));
                }
            } else if (Character.isAlphabetic(getCurr()) || getCurr() == '_') {
                while (inBounds() && (Character.isAlphabetic(getCurr()) || getCurr() == '_'))
                    advance();
                tokenText = text.substring(tokenPosition, position);
                if (isKeyword(tokenText))
                    tokens.add(new Token(tokenPosition, tokenText, tokenText, TokenKind.KeywordToken));
                else
                    tokens.add(new Token(tokenPosition, tokenText, tokenText, TokenKind.IdentifierToken));
            } else if (getCurr() == '=') {
                advance();
                tokens.add(new Token(tokenPosition, "=", "=", TokenKind.EqualsToken));
            } else if (getCurr() == '"') {
                advance();
                while (inBounds() && getCurr() != '"')
                    advance();
                advance();
                if (!inBounds()) {
                    tokenText = text.substring(tokenPosition);
                    tokens.add(new Token(tokenPosition, tokenText, tokenText, TokenKind.BadToken));
                } else {
                    tokenText = text.substring(tokenPosition, position);
                    tokens.add(new Token(tokenPosition, tokenText, tokenText, TokenKind.StringLiteralToken));
                }
            } else if (inBounds() && getCurr() == '&') {
                advance();
                if (!inBounds() || getCurr() != '&') {
                    tokenText = Character.toString('&');
                    tokens.add(new Token(tokenPosition, tokenText, tokenText, TokenKind.BadToken));
                } else {
                    advance();
                    tokens.add(new Token(tokenPosition, "&&", "&&", TokenKind.ANDToken));
                }
            } else if (inBounds() && getCurr() == '|') {
                advance();
                if (!inBounds() || getCurr() != '|') {
                    tokenText = Character.toString('|');
                    tokens.add(new Token(tokenPosition, tokenText, tokenText, TokenKind.BadToken));
                } else {
                    advance();
                    tokens.add(new Token(tokenPosition, "&&", "&&", TokenKind.ORToken));
                }
            } else {
                tokens.add(new Token(tokenPosition, Character.toString(getCurr()), Character.toString(getCurr()), TokenKind.BadToken));
                advance();
            }
        }
        tokens.add(new Token(this.position, null, "\0", TokenKind.EOFToken));
        return tokens;
    }

    private boolean isKeyword(String tokenText) {
        return this.keywordSet.contains(tokenText.toLowerCase(Locale.ROOT));
    }

    private boolean inBounds() {
        return this.position < this.text.length();
    }

    private char getCurr() {
        if (this.position == this.text.length())
            return '\0';
        return this.text.charAt(position);
    }

    private void advance() {
        if (this.position != this.text.length())
            this.position++;
    }

    public static void main(String[] args) {
        for (var t : new Lexer("SELECT xx from TableName WHERE a = 1 && b = 2").lex())
            System.out.println(t.getKind() + " " + t.getTokenText());
    }
}

