package Predicate;

import RheaDB.AttributeType;
import RheaDB.PredicateEvaluator;

public class LessThanEqualPredicate extends Predicate {

    public LessThanEqualPredicate(String attributeName, Object value) {
        super(attributeName, value);
    }

    @Override
    public boolean doesSatisfy(Object comp) {
        return doesSatisfy(this.attribute.getType(), comp);
    }

    public boolean doesSatisfy(PredicateEvaluator.IdentifierValue identifierValue) {
        return doesSatisfy(identifierValue.type, identifierValue.value);
    }

    private boolean doesSatisfy(AttributeType type, Object comp) {
        return switch (type) {
            case INT -> ((Integer) value).compareTo((Integer) comp) >= 0;
            case STRING -> ((String) value).compareTo((String) comp) >= 0;
            case FLOAT -> ((Float) value).compareTo((Float) comp) >= 0;
        };
    }

    @Override
    public Operation getOperation() {
        return Operation.LESS_THAN_EQUAL;
    }
}
