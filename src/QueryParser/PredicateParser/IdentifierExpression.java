package QueryParser.PredicateParser;

import QueryParser.Token;

public class IdentifierExpression extends ASTNode {
    private final Token identifierToken;

    IdentifierExpression(Token identifierToken) {
        this.identifierToken = identifierToken;
    }

    public Token getIdentifierToken() {
        return identifierToken;
    }
}
