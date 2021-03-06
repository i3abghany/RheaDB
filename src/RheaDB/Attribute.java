package RheaDB;

import java.io.Serial;
import java.io.Serializable;

public class Attribute implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final AttributeType type;
    private final String name;
    private boolean isPrimaryKey;
    private boolean isIndexed;

    public Attribute(AttributeType type, String name) {
        this(type, name, false);
    }

    public Attribute(AttributeType type, String name, boolean isPrimaryKey) {
        this.type = type;
        this.name = name;
        this.isPrimaryKey = isPrimaryKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Attribute other))
            return false;

        return this.type == other.type &&
                this.name.equals(other.name) &&
                this.isPrimaryKey == other.isPrimaryKey;
    }

    public String getName() {
        return name;
    }

    public AttributeType getType() {
        return type;
    }

    public void setIsPrimaryKey(boolean val) {
        this.isPrimaryKey = val;
    }

    public boolean getIsPrimaryKey() {
        return this.isPrimaryKey;
    }

    public void setIsIndexed(boolean val) {
        this.isIndexed = val;
    }

    public boolean getIsIndexed() {
        return this.isIndexed;
    }

    public static AttributeType getAttributeTypeFromString(String attributeName) throws DBError {
        return switch (attributeName) {
            case "string" -> AttributeType.STRING;
            case "int" -> AttributeType.INT;
            case "float" -> AttributeType.FLOAT;
            default -> throw new DBError("Invalid data type <" + attributeName + ">.");
        };
    }
}

