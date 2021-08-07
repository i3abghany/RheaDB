package RheaDB;

public class UpdateResult extends QueryResult {
    private int affectedRows;

    public UpdateResult() {

        affectedRows = 0;
    }

    public UpdateResult(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    @Override
    public String toString() {
        return "";
    }
}
