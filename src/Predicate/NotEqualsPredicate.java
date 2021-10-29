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
        return switch (identifierValue.type) {
            case INT -> ((Integer) value).compareTo((Integer) identifierValue.value) < 0;
            case STRING -> ((String) value).compareTo((String) identifierValue.value) < 0;
            case FLOAT -> ((Float) value).compareTo((Float) identifierValue.value) < 0;
        };
    }

    @Override
    public Operation getOperation() {
        return Operation.NOT_EQUALS;
    }
}
