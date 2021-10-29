package Predicate;

import RheaDB.PredicateEvaluator;

public class GreaterThanPredicate extends Predicate {

    public GreaterThanPredicate(String attributeName, Object value) {
        super(attributeName, value);
    }

    @Override
    public boolean doesSatisfy(Object comp) {
        return switch (this.attribute.getType()) {
            case INT -> ((Integer) value).compareTo((Integer) comp) < 0;
            case STRING -> ((String) value).compareTo((String) comp) < 0;
            case FLOAT -> ((Float) value).compareTo((Float) comp) < 0;
        };
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
        return Operation.GREATER_THAN;
    }
}
