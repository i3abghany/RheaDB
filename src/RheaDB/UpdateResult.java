package RheaDB;

public class UpdateResult extends QueryResult {
    private final int affectedRows;

    public UpdateResult(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    @Override
    public String toString() {
        return "";
    }
}
