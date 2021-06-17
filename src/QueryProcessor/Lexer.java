package QueryProcessor;

import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

public class Lexer {
    private final String text;
    private int position;

    private final HashSet<String> keywordSet = new HashSet<>();
    private final HashSet<String> dataTypeSet = new HashSet<>();
    private final HashSet<Character> operatorSet = new HashSet<>();

    public Lexer(String input) {
        this.text = input;
        this.position = 0;
        populateKeywordSet();
        populateDataTypeSet();
        populateOperatorSet();
    }

    private void populateOperatorSet() {
        operatorSet.add('=');
        operatorSet.add('>');
        operatorSet.add('<');
        operatorSet.add('!');
    }

    private void populateDataTypeSet() {
        keywordSet.add("int");
        keywordSet.add("string");
        keywordSet.add("float");
    }

    private void populateKeywordSet() {
        keywordSet.add("create");
        keywordSet.add("delete");
        keywordSet.add("from");
        keywordSet.add("index");
        keywordSet.add("insert");
        keywordSet.add("into");
        keywordSet.add("select");
        keywordSet.add("table");
        keywordSet.add("where");
    }

    public Token lexWhiteSpaceToken(int tokenPosition) {
        while (inBounds() && Character.isWhitespace(getCurr()))
            advance();
        String tokenText = text.substring(tokenPosition, position);
        return new Token(tokenPosition, tokenText, null,
                TokenKind.WhiteSpaceToken);
    }

    public Token lexNumericLiteral(int tokenPosition) {
        while (inBounds() && (Character.isDigit(getCurr()) || getCurr() == '.'))
            advance();

        String tokenText = text.substring(tokenPosition, position);
        if (tokenText.contains(".")) {
            return new Token(tokenPosition, tokenText, Float.parseFloat(tokenText),
                    TokenKind.FloatingPointToken);
        } else {
            return new Token(tokenPosition, tokenText, Integer.parseInt(tokenText),
                    TokenKind.IntegralToken);
        }
    }

    public Token lexAlphabeticalToken(int tokenPosition) {
        while (inBounds() && (Character.isAlphabetic(getCurr()) || getCurr() == '_'))
            advance();
        String tokenText = text.substring(tokenPosition, position);
        if (isKeyword(tokenText))
            return new Token(tokenPosition, tokenText.toLowerCase(Locale.ROOT),
                    tokenText.toLowerCase(Locale.ROOT),  TokenKind.KeywordToken);
        else if (isDataType(tokenText))
            return new Token(tokenPosition, tokenText.toLowerCase(Locale.ROOT),
                    tokenText.toLowerCase(Locale.ROOT), TokenKind.DataTypeToken);
        else
            return new Token(tokenPosition, tokenText, tokenText, TokenKind.IdentifierToken);
    }

    private Token lexStringLiteral(int tokenPosition) {
        advance();
        while (inBounds() && getCurr() != '"')
            advance();
        if (!inBounds()) {
            String tokenText = text.substring(tokenPosition);
            return new Token(tokenPosition, tokenText, tokenText, TokenKind.BadToken);
        } else {
            String tokenText = text.substring(tokenPosition + 1, position);
            advance();
            return new Token(tokenPosition, tokenText, tokenText, TokenKind.StringLiteralToken);
        }
    }

    public Token lexBeginningWithEquals(int tokenPosition) {
        advance();
        return new Token(tokenPosition, "=", "=",
                TokenKind.EqualsToken);
    }

    public Token lexBeginningWithBang(int tokenPosition) {
        advance();
        if (!inBounds() || getCurr() != '=') {
            String tokenText = "!";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.BadToken);
        } else {
            advance();
            String tokenText = "!=";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.NotEqualsToken);
        }
    }

    private Token lexGreaterThan(int tokenPosition) {
        advance();
        if (getCurr() != '=') {
            String tokenText = ">";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.GreaterToken);
        } else {
            advance();
            String tokenText = ">=";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.GreaterEqualsToken);
        }
    }

    private Token lexLessThan(int tokenPosition) {
        advance();
        if (!inBounds() || getCurr() != '=') {
            String tokenText = "<";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.LessToken);
        } else {
            advance();
            String tokenText = "<=";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.LessEqualsToken);
        }
    }

    public Token lexOperatorToken(int tokenPosition) {
        return switch (getCurr()) {
            case '=' -> lexBeginningWithEquals(tokenPosition);
            case '!' -> lexBeginningWithBang(tokenPosition);
            case '>' -> lexGreaterThan(tokenPosition);
            case '<' -> lexLessThan(tokenPosition);
            default -> null;
        };
    }

    public Token lexCommaToken(int tokenPosition) {
        advance();
        String tokenText = ",";
        return new Token(tokenPosition, tokenText, tokenText,
                TokenKind.CommaToken);
    }

    public Token lexBadToken(int tokenPosition) {
        advance();
        return new Token(tokenPosition, Character.toString(getCurr()),
                Character.toString(getCurr()), TokenKind.BadToken);
    }

    public Vector<Token> lex() {
            Vector<Token> tokens = new Vector<>();

            while (position < text.length()) {
                int tokenPosition = position;

                if (Character.isWhitespace(getCurr()))
                    tokens.add(lexWhiteSpaceToken(tokenPosition));
                else if (Character.isDigit(getCurr()))
                    tokens.add(lexNumericLiteral(tokenPosition));
                else if (Character.isAlphabetic(getCurr()) || getCurr() == '_')
                    tokens.add(lexAlphabeticalToken(tokenPosition));
                else if (isOperator(getCurr()))
                    tokens.add(lexOperatorToken(tokenPosition));
                else if (getCurr() == '"')
                    tokens.add(lexStringLiteral(tokenPosition));
                else if (getCurr() == ',')
                    tokens.add(lexCommaToken(tokenPosition));
                else if (!inBounds())
                    break;
                else
                    tokens.add(lexBadToken(tokenPosition));
            }

        return tokens;
    }

    private boolean isOperator(char curr) {
        return operatorSet.contains(curr);
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
        if (this.position >= this.text.length())
            return '\0';
        return this.text.charAt(position);
    }

    private void advance() {
        if (this.position != this.text.length())
            this.position++;
    }
}

