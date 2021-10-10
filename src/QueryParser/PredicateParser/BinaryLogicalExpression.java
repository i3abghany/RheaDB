package QueryParser.PredicateParser;

import QueryParser.Token;

public class BinaryLogicalExpression extends ASTNode {
    private final ASTNode lhs;
    private final Token operatorToken;
    private final ASTNode rhs;

    public BinaryLogicalExpression(ASTNode leftNode, Token operatorToken, ASTNode rightNode) {
        this.lhs = leftNode;
        this.operatorToken = operatorToken;
        this.rhs = rightNode;
    }

    public Token getOperatorToken() {
        return operatorToken;
    }

    public ASTNode getLhs() {
        return lhs;
    }

    public ASTNode getRhs() {
        return rhs;
    }
}
