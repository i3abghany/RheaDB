package Predicate;

public class EqualsPredicate extends Predicate {

    public EqualsPredicate(String attributeName, Object value) {
        super(attributeName, value);
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
