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
        text = input;
        position = 0;
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
        dataTypeSet.add("int");
        dataTypeSet.add("string");
        dataTypeSet.add("float");
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
        keywordSet.add("values");
    }

    private Token lexWhiteSpaceToken(int tokenPosition) {
        while (Character.isWhitespace(getCurr()))
            advance();
        String tokenText = text.substring(tokenPosition, position);
        return new Token(tokenPosition, tokenText, null,
                TokenKind.WhiteSpaceToken);
    }

    private Token lexNumericLiteral(int tokenPosition) {
        while ((Character.isDigit(getCurr()) || getCurr() == '.'))
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

    private Token lexAlphabeticalToken(int tokenPosition) {
        while (Character.isAlphabetic(getCurr()) || getCurr() == '_')
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
        while (getCurr() != '"')
            advance();
        if (inBounds()) {
            String tokenText = text.substring(tokenPosition + 1, position);
            advance();
            return new Token(tokenPosition, tokenText, tokenText, TokenKind.StringLiteralToken);
        } else {
            String tokenText = text.substring(tokenPosition);
            return new Token(tokenPosition, tokenText, tokenText, TokenKind.BadToken);
        }
    }

    private Token lexBeginningWithEquals(int tokenPosition) {
        advance();
        return new Token(tokenPosition, "=", "=",
                TokenKind.EqualsToken);
    }

    private Token lexBeginningWithBang(int tokenPosition) {
        advance();
        if (getCurr() == '=') {
            advance();
            String tokenText = "!=";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.NotEqualsToken);
        } else {
            String tokenText = "!";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.BadToken);
        }
    }

    private Token lexGreaterThan(int tokenPosition) {
        advance();
        if (getCurr() == '=') {
            advance();
            String tokenText = ">=";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.GreaterEqualsToken);
        } else {
            String tokenText = ">";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.GreaterToken);
        }
    }

    private Token lexLessThan(int tokenPosition) {
        advance();
        if (getCurr() == '=') {
            advance();
            String tokenText = "<=";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.LessEqualsToken);
        } else {
            String tokenText = "<";
            return new Token(tokenPosition, tokenText, tokenText,
                    TokenKind.LessToken);
        }
    }

    private Token lexOperatorToken(int tokenPosition) {
        return switch (getCurr()) {
            case '=' -> lexBeginningWithEquals(tokenPosition);
            case '!' -> lexBeginningWithBang(tokenPosition);
            case '>' -> lexGreaterThan(tokenPosition);
            case '<' -> lexLessThan(tokenPosition);
            default -> null;
        };
    }

    private Token lexOpenParen(int tokenPosition) {
        advance();
        String tokenText = "(";
        return new Token(tokenPosition, tokenText, tokenText,
                TokenKind.OpenParenToken);
    }

    private Token lexClosedParen(int tokenPosition) {
        advance();
        String tokenText = ")";
        return new Token(tokenPosition, tokenText, tokenText,
                TokenKind.ClosedParenToken);
    }

    private Token lexCommaToken(int tokenPosition) {
        advance();
        String tokenText = ",";
        return new Token(tokenPosition, tokenText, tokenText,
                TokenKind.CommaToken);
    }

    private Token lexBadToken(int tokenPosition) {
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
                else if (getCurr() == '(')
                    tokens.add(lexOpenParen(tokenPosition));
                else if (getCurr() == ')')
                    tokens.add(lexClosedParen(tokenPosition));
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
        return dataTypeSet.contains(tokenText.toLowerCase(Locale.ROOT));
    }

    private boolean isKeyword(String tokenText) {
        return keywordSet.contains(tokenText.toLowerCase(Locale.ROOT));
    }

    private boolean inBounds() {
        return position < text.length();
    }

    private char getCurr() {
        if (position >= text.length())
            return '\0';
        return text.charAt(position);
    }

    private void advance() {
        if (position != text.length())
            position++;
    }
}

