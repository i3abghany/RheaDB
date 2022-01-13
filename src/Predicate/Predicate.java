package Predicate;

import QueryParser.PredicateParser.ASTNode;
import RheaDB.Attribute;
import RheaDB.PredicateEvaluator;

public abstract class Predicate extends ASTNode {
    protected Attribute attribute;
    protected final String attributeName;
    protected final Object value;

    public enum Operation {
        EQUALS,
        NOT_EQUALS,
        LESS_THAN,
        GREATER_THAN,
        GREATER_THAN_EQUAL,
        LESS_THAN_EQUAL,
    }

    public Predicate(String attributeName, Object value) {
        this.attribute = null;
        this.attributeName = attributeName;
        this.value = value;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    public Object getValue() {
        return value;
    }

    public boolean doesSatisfy(PredicateEvaluator.IdentifierValue identifierValue) {
        return false;
    }

    public abstract boolean doesSatisfy(Object comp);

    public abstract Operation getOperation();
}

