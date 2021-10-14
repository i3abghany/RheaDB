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

    /**
     * Checks whether a tree contains at least one predicate or a binary expression.
     *
     * @param node The node to recurse from.
     * @return Whether the tree has at least one binary expression.
     */
    private boolean isPredicate(ASTNode node) {
        if (node instanceof ParenthesizedExpression parenthesizedExpression) {
            return isPredicate(parenthesizedExpression);
        } else {
            return node instanceof BinaryLogicalExpression;
        }
    }

    public PredicateAST parse() throws Exception {
        ASTNode root = parseExpression();
        return isPredicate(root) ? new PredicateAST(root) : null;
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
        } else if (token.isLiteral()) {
            return parseLiteralExpression();
        } else {
            throw new Exception("Unexpected token: \"" + token.getTokenText() +
                    "\", Expected a primary expression token.");
        }
    }

    private ASTNode parseLiteralExpression() throws Exception {
        Token token = nextToken();
        if (!token.isLiteral()) {
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

            return new ParenthesizedExpression(openParen, expression, closeParen);
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

    public static void main(String[] args) throws Exception {
        var c = new PredicateParser("a").parse();
    }
}
