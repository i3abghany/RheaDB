package RheaDB;

public class DBError extends Exception {

    public DBError() {
        super();
    }

    public DBError(String message) {
        super(message);
    }
}
