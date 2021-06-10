package RheaDB;

import java.sql.SQLException;

public class DBError extends SQLException {

    public DBError() {
        super();
    }

    public DBError(String message) {
        super(message);
    }
}
