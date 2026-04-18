package Predicate;

import QueryParser.PredicateParser.ASTNode;
import QueryParser.TokenKind;
import RheaDB.Attribute;
import RheaDB.AttributeType;
import RheaDB.PredicateEvaluator;

public class Predicate extends ASTNode {
    protected Attribute attribute;
    protected final String attributeName;
    protected final Object value;
    protected final Operation operation;

    public enum Operation {
        EQUALS {
            @Override
            boolean test(Object value, Object comp) {
                return value.equals(comp);
            }
        },
        NOT_EQUALS {
            @Override
            boolean test(Object value, Object comp) {
                return !value.equals(comp);
            }
        },
        LESS_THAN {
            @Override
            boolean test(AttributeType type, Object value, Object comp) {
                return compare(type, value, comp) > 0;
            }
        },
        GREATER_THAN {
            @Override
            boolean test(AttributeType type, Object value, Object comp) {
                return compare(type, value, comp) < 0;
            }
        },
        GREATER_THAN_EQUAL {
            @Override
            boolean test(AttributeType type, Object value, Object comp) {
                return compare(type, value, comp) <= 0;
            }
        },
        LESS_THAN_EQUAL {
            @Override
            boolean test(AttributeType type, Object value, Object comp) {
                return compare(type, value, comp) >= 0;
            }
        };

        boolean test(Object value, Object comp) {
            return test(null, value, comp);
        }

        boolean test(AttributeType type, Object value, Object comp) {
            return test(value, comp);
        }

        static Operation fromTokenKind(TokenKind operatorKind) {
            return switch (operatorKind) {
                case EqualsToken -> EQUALS;
                case NotEqualsToken -> NOT_EQUALS;
                case GreaterEqualsToken -> GREATER_THAN_EQUAL;
                case GreaterToken -> GREATER_THAN;
                case LessEqualsToken -> LESS_THAN_EQUAL;
                case LessToken -> LESS_THAN;
                default -> null;
            };
        }

        private static int compare(AttributeType type, Object value, Object comp) {
            return switch (type) {
                case INT -> ((Integer) value).compareTo((Integer) comp);
                case STRING -> ((String) value).compareTo((String) comp);
                case FLOAT -> ((Float) value).compareTo((Float) comp);
            };
        }
    }

    public Predicate(String attributeName, Object value, Operation operation) {
        this.attribute = null;
        this.attributeName = attributeName;
        this.value = value;
        this.operation = operation;
    }

    public Attribute getAttribute() {
        return this.attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    public Object getValue() {
        return value;
    }

    public boolean doesSatisfy(PredicateEvaluator.IdentifierValue identifierValue) {
        return operation.test(identifierValue.type, this.value, identifierValue.value);
    }

    public boolean doesSatisfy(Object comp) {
        return operation.test(this.attribute.getType(), this.value, comp);
    }

    public Operation getOperation() {
        return operation;
    }
}

