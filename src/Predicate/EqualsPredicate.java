package Predicate;

import RheaDB.Attribute;

public class EqualsPredicate extends Predicate {

    public EqualsPredicate(Attribute attribute, Object value) {
        super(attribute, value);
    }

    @Override
    public boolean doesSatisfy(Object comp) {
        return this.value.equals(comp);
    }

    @Override
    public Operation getOperation() {
        return Operation.EQUALS;
    }
}
