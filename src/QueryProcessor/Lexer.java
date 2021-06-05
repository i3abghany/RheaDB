package QueryProcessor;

import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.stream.Collectors;

public class Lexer {
    private final String text;
    private int position;

    private final HashSet<String> keywordSet = new HashSet<>();
    private final HashSet<String> dataTypeSet = new HashSet<>();

    public Lexer(String input) {
        this.text = input;
        this.position = 0;
        populateKeywordSet();
        populateDataTypeSet();
    }

    private void populateDataTypeSet() {
        keywordSet.add("int");
        keywordSet.add("string");
        keywordSet.add("float");
    }

    private void populateKeywordSet() {
        keywordSet.add("create");
        keywordSet.add("from");
        keywordSet.add("index");
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
                tokens.add(new Token(tokenPosition, tokenText, null,
                        TokenKind.WhiteSpaceToken));
            } else if (Character.isDigit(getCurr())) {
                while (inBounds() && (Character.isDigit(getCurr()) || getCurr() == '.'))
                    advance();

                tokenText = text.substring(tokenPosition, position);
                if (tokenText.contains(".")) {
                    tokens.add(new Token(tokenPosition, tokenText,
                            Float.parseFloat(tokenText),
                            TokenKind.FloatingPointToken));
                } else {
                    tokens.add(new Token(tokenPosition, tokenText,
                            Integer.parseInt(tokenText),
                            TokenKind.IntegralToken));
                }
            } else if (Character.isAlphabetic(getCurr()) || getCurr() == '_') {
                while (inBounds() && (Character.isAlphabetic(getCurr()) || getCurr() == '_'))
                    advance();
                tokenText = text.substring(tokenPosition, position);
                if (isKeyword(tokenText))
                    tokens.add(new Token(tokenPosition,
                            tokenText.toLowerCase(Locale.ROOT),
                            tokenText.toLowerCase(Locale.ROOT),
                            TokenKind.KeywordToken));
                else if (isDataType(tokenText))
                    tokens.add(new Token(tokenPosition,
                            tokenText.toLowerCase(Locale.ROOT),
                            tokenText.toLowerCase(Locale.ROOT),
                            TokenKind.DataTypeToken));
                else
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.IdentifierToken));
            } else if (inBounds() && getCurr() == '=') {
                advance();
                tokens.add(new Token(tokenPosition, "=", "=",
                        TokenKind.EqualsToken));
            } else if (inBounds() && getCurr() == '!') {
                advance();
                if (!inBounds() || getCurr() != '=') {
                    tokenText = "!";
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.BadToken));
                } else {
                    tokenText = "!=";
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.NotEqualsToken));
                }
            } else if (inBounds() && getCurr() == '>') {
                advance();
                if (!inBounds() || getCurr() != '=') {
                    tokenText = "!";
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.GreaterToken));
                } else {
                    tokenText = "!=";
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.GreaterEqualsToken));
                }
            } else if (inBounds() && getCurr() == '<') {
                advance();
                if (!inBounds() || getCurr() != '=') {
                    tokenText = "<";
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.LessToken));
                } else {
                    tokenText = "!=";
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.LessEqualsToken));
                }
            } else if (inBounds() && getCurr() == '"') {
                advance();
                while (inBounds() && getCurr() != '"')
                    advance();
                advance();
                if (!inBounds()) {
                    tokenText = text.substring(tokenPosition);
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.BadToken));
                } else {
                    tokenText = text.substring(tokenPosition + 1, position - 2);
                    tokens.add(new Token(tokenPosition, tokenText, tokenText,
                            TokenKind.StringLiteralToken));
                }
            } else if (getCurr() == ',') {
                advance();
                tokenText = ",";
                tokens.add(new Token(tokenPosition, tokenText, tokenText,
                        TokenKind.CommaToken));
            } else {
                tokens.add(new Token(tokenPosition,
                        Character.toString(getCurr()),
                        Character.toString(getCurr()), TokenKind.BadToken));
                advance();
            }
        }
        tokens.add(new Token(this.position, null, "\0", TokenKind.EOFToken));
        return tokens;
    }

    private boolean isDataType(String tokenText) {
        return this.dataTypeSet.contains(tokenText.toLowerCase(Locale.ROOT));
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
}

