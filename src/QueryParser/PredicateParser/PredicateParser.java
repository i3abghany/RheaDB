package QueryParser.PredicateParser;

import QueryParser.Lexer;
import QueryParser.Token;
import QueryParser.TokenKind;
import RheaDB.DBError;

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

    public PredicateAST parse() throws DBError {
        ASTNode root = parseExpression();
        return isPredicate(root) ? new PredicateAST(root) : null;
    }

    private ASTNode parseExpression() throws DBError {
        return parseExpression(0);
    }

    private ASTNode parseExpression(int parentPrecedence) throws DBError {
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

    private ASTNode parsePrimaryExpression() throws DBError {
        Token token = getCurrent();
        if (token.getKind() == TokenKind.OpenParenToken) {
            return parseParenthesizedExpression();
        } else if (token.getKind() == TokenKind.IdentifierToken) {
            return parseIdentifierExpression();
        } else if (token.isLiteral()) {
            return parseLiteralExpression();
        } else {
            throw new DBError("Unexpected token: \"" + token.getTokenText() +
                    "\", Expected a primary expression token.");
        }
    }

    private ASTNode parseLiteralExpression() throws DBError {
        Token token = nextToken();
        if (!token.isLiteral()) {
            throw new DBError("Unexpected token: \"" + token.getTokenText() + "\", Expected a literal token.");
        } else {
            return new LiteralExpression(token);
        }
    }

    private ASTNode parseIdentifierExpression() throws DBError {
        Token token = nextToken();
        if (token.getKind() != TokenKind.IdentifierToken) {
            throw new DBError("Expected an IdentifierToken.");
        } else {
            return new IdentifierExpression(token);
        }
    }

    private ASTNode parseParenthesizedExpression() throws DBError {
        Token openParen = nextToken();
        if (openParen.getKind() != TokenKind.OpenParenToken) {
            throw new DBError("Expected an OpenParenToken.");
        } else {
            ASTNode expression = parseExpression();
            Token closeParen = nextToken();
            if (closeParen.getKind() != TokenKind.ClosedParenToken) {
                throw new DBError("Expected a ClosedParenToken.");
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
