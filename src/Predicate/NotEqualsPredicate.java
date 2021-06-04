package Predicate;

import RheaDB.Attribute;

public class NotEqualsPredicate extends Predicate {

    public NotEqualsPredicate(String attributeName, Object value) {
        super(attributeName, value);
    }

    @Override
    public boolean doesSatisfy(Object comp) {
        return !this.value.equals(comp);
    }

    @Override
    public Operation getOperation() {
        return Operation.NOT_EQUALS;
    }
}
