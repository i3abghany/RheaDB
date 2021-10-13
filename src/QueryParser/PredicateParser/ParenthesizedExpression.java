package QueryParser.PredicateParser;

import QueryParser.Token;

public class ParenthesizedExpression extends ASTNode {
    private final Token openParenToken;
    private final ASTNode expression;
    private final Token closedParenToken;

    public ParenthesizedExpression(Token openParenToken, ASTNode expression, Token closedParenToken) {
        this.openParenToken = openParenToken;
        this.expression = expression;
        this.closedParenToken = closedParenToken;
    }

    public Token getOpenParenToken() {
        return openParenToken;
    }

    public ASTNode getExpression() {
        return expression;
    }

    public Token getClosedParenToken() {
        return closedParenToken;
    }
}
