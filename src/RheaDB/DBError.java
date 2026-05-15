package RheaDB;

import java.sql.SQLException;

public class DBError extends SQLException {
    public DBError(String message) {
        super(message);
    }
}
