package Predicate;

import RheaDB.PredicateEvaluator;

public class NotEqualsPredicate extends Predicate {

    public NotEqualsPredicate(String attributeName, Object value) {
        super(attributeName, value);
    }

    @Override
    public boolean doesSatisfy(Object comp) {
        return !this.value.equals(comp);
    }

    @Override
    public boolean doesSatisfy(PredicateEvaluator.IdentifierValue identifierValue) {
        return !this.value.equals(identifierValue.value);
    }

    @Override
    public Operation getOperation() {
        return Operation.NOT_EQUALS;
    }
}
