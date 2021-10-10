package QueryParser.PredicateParser;

import QueryParser.Lexer;
import QueryParser.Token;
import QueryParser.TokenKind;

public class PredicateParser {
    private final Token[] tokenList;
    private int position = 0;

    public PredicateParser(String predicateString) {
        this.tokenList = new Lexer(predicateString)
                .lex()
                .stream().filter(t -> t.getKind() != TokenKind.WhiteSpaceToken)
                .toArray(Token[]::new);
    }

    public PredicateAST parse() throws Exception {
        return new PredicateAST(parseExpression());
    }

    private ASTNode parseExpression() throws Exception {
        return parseExpression(0);
    }

    private ASTNode parseExpression(int parentPrecedence) throws Exception {
        ASTNode left = parsePrimaryExpression();
        while (true) {
            Token operatorToken = getCurrent();
            int precedence = SyntaxFacts.binaryOperatorPrecedence(operatorToken.getKind());
            if (precedence == 0 || precedence <= parentPrecedence) {
                break;
            }
            nextToken();
            ASTNode right = parseExpression(precedence);
            left = new BinaryLogicalExpression(left, operatorToken, right);
        }

        return left;
    }

    private ASTNode parsePrimaryExpression() throws Exception {
        Token token = getCurrent();
        if (token.getKind() == TokenKind.OpenParenToken) {
            return parseParenthesizedExpression();
        } else if (token.getKind() == TokenKind.IdentifierToken) {
            return parseIdentifierExpression();
        } else if (token.getKind() == TokenKind.StringLiteralToken ||
                   token.getKind() == TokenKind.IntegralToken ||
                   token.getKind() == TokenKind.FloatingPointToken) {
            return parseLiteralExpression();
        } else {
            throw new Exception("Unexpected token: \"" + token.getTokenText() +
                    "\", Expected a primary expression token.");
        }
    }

    private ASTNode parseLiteralExpression() throws Exception {
        Token token = nextToken();
        if (token.getKind() != TokenKind.IntegralToken &&
            token.getKind() != TokenKind.FloatingPointToken &&
            token.getKind() != TokenKind.StringLiteralToken) {
            throw new Exception("Unexpected token: \"" + token.getTokenText() + "\", Expected a literal token.");
        } else {
            return new LiteralExpression(token);
        }
    }

    private ASTNode parseIdentifierExpression() throws Exception {
        Token token = nextToken();
        if (token.getKind() != TokenKind.IdentifierToken) {
            throw new Exception("Expected an IdentifierToken.");
        } else {
            return new IdentifierExpression(token);
        }
    }

    private ASTNode parseParenthesizedExpression() throws Exception {
        Token openParen = nextToken();
        if (openParen.getKind() != TokenKind.OpenParenToken) {
            throw new Exception("Expected an OpenParenToken.");
        } else {
            ASTNode expression = parseExpression();
            Token closeParen = nextToken();
            if (closeParen.getKind() != TokenKind.ClosedParenToken) {
                throw new Exception("Expected a ClosedParenToken.");
            }

            return expression;
        }
    }

    private Token nextToken() {
        if (position >= tokenList.length)
            return tokenList[tokenList.length - 1];

        Token result = tokenList[position];
        position++;
        return result;
    }

    private Token getCurrent() {
        if (position >= tokenList.length)
            return tokenList[tokenList.length - 1];

        return tokenList[position];
    }
}
