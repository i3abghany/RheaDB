package RheaDB;

import org.w3c.dom.Attr;

import java.io.Serial;
import java.io.Serializable;

public class Attribute implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final AttributeType type;
    private final String name;
    private final boolean isPrimaryKey;
    private final int size; // only applicable for data types with non-fixed-size, e.g., strings.

    public Attribute(AttributeType type, String name, boolean isPrimaryKey, int size) {
        this.type = type;
        this.name = name;
        this.isPrimaryKey = isPrimaryKey;
        this.size = size;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Attribute other))
            return false;

        return this.type.equals(other.type) &&
               this.name.equals(other.name) &&
               this.isPrimaryKey == other.isPrimaryKey &&
               this.size == other.size;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public AttributeType getType() {
        return type;
    }

    public static AttributeType getAttributeFromString(String attributeName) {
        return switch (attributeName) {
            case "STRING" -> AttributeType.STRING;
            case "INT" -> AttributeType.INT;
            case "FLOAT" -> AttributeType.FLOAT;
            default -> throw new IllegalArgumentException("Invalid data type.");
        };
    }
}

