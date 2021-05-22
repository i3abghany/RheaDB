package Predicate;

import RheaDB.Attribute;
import RheaDB.AttributeType;

public class LessThanPredicate extends Predicate {

    public LessThanPredicate(Attribute attribute, Object value) {
        super(attribute, value);
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
    public Operation getOperation() {
        return Operation.LESS_THAN;
    }
}
