package Predicate;

public class GreaterThanEqualPredicate extends Predicate {

    public GreaterThanEqualPredicate(String attributeName, Object value) {
        super(attributeName, value);
    }

    @Override
    public boolean doesSatisfy(Object comp) {
        return switch (this.attribute.getType()) {
            case INT -> ((Integer) value).compareTo((Integer) comp) <= 0;
            case STRING -> ((String) value).compareTo((String) comp) <= 0;
            case FLOAT -> ((Float) value).compareTo((Float) comp) <= 0;
        };
    }

    @Override
    public Operation getOperation() {
        return Operation.GREATER_THAN_EQUAL;
    }
}
