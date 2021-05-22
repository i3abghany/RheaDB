package Predicate;

import RheaDB.Attribute;

public abstract class Predicate {
    protected final Attribute attribute;
    protected final Object value;

    public enum Operation {
        EQUALS,
        NOT_EQUALS,
        LESS_THAN,
        GREATER_THAN,
        GREATER_THAN_EQUAL,
        LESS_THAN_EQUAL,
    }

    public Predicate(Attribute attribute, Object value) {
        this.attribute = attribute;
        this.value = value;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public abstract boolean doesSatisfy(Object comp);
    public abstract Operation getOperation();
}

