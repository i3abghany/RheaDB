package QueryParser.PredicateParser;

import QueryParser.Token;

public class LiteralExpression extends ASTNode {
    private final Token valueToken;

    LiteralExpression(Token valueToken) {
        this.valueToken = valueToken;
    }

    public Token getValueToken() {
        return valueToken;
    }
}
