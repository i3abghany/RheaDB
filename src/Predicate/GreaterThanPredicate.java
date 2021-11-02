package Predicate;

import RheaDB.AttributeType;
import RheaDB.PredicateEvaluator;

public class GreaterThanPredicate extends Predicate {

    public GreaterThanPredicate(String attributeName, Object value) {
        super(attributeName, value);
    }

    @Override
    public boolean doesSatisfy(Object comp) {
        return doesSatisfy(this.attribute.getType(), comp);
    }

    private boolean doesSatisfy(AttributeType type, Object comp) {
        return switch (type) {
            case INT -> ((Integer) value).compareTo((Integer) comp) < 0;
            case STRING -> ((String) value).compareTo((String) comp) < 0;
            case FLOAT -> ((Float) value).compareTo((Float) comp) < 0;
        };
    }

    @Override
    public boolean doesSatisfy(PredicateEvaluator.IdentifierValue identifierValue) {
        return doesSatisfy(identifierValue.type, identifierValue.value);
    }

    @Override
    public Operation getOperation() {
        return Operation.GREATER_THAN;
    }
}
