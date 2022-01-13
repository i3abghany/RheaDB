# RheaDB

A simple disk-based DBMS that supports simple SQL-like querying statements.

## What's currently implemented?
* Simple SQL-like queries
    * Creating & dropping tables
    * Deleting from tables
    * Updating tables
    * Simple selection queries
    * Creating & deleting indices
* Disk-based storage
* In-memory buffer pool caching
* JDBC driver
* B+Tree indexing

### JDBC driver loading and usage
The connection url passed to `DriverManager.getConnection()` must be in the
format `jdbc:rhea:DIR_PATH`, where `DIR_PATH` is the storage directory for the
database. It must be an existing directory.

`Class.forName("RheaDB.JDBCDriver.JCDriver")` must be used so as to invoke the
`static` portion of the driver, and (hopefully) successfully connect to a 
database instance.


```java
import java.sql.*;

public class ExampleProgram {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        Class.forName("RheaDB.JDBCDriver.JCDriver");
        try (
                Connection conn = DriverManager.getConnection("jdbc:rhea:/home/USER_NAME/dbdata");
                Statement stmt = conn.createStatement();
                stmt.executeQuery("CREATE TABLE FancyTable (id INT, name STRING, mass FLOAT);");
                
                stmt.executeQuery("INSERT INTO FancyTable VALUES (1, \"Random Name\", 42.69);");
                stmt.executeQuery("INSERT INTO FancyTable VALUES (2, \"Not Random Name\", 96.24);");
                stmt.executeQuery("INSERT INTO FancyTable VALUES (3, \"Completely Random Name\", 3.1415);");
                
                ResultSet rs = stmt.executeQuery("SELECT * FROM FancyTable;")
        ) {
            while (rs.next())
                System.out.println(rs.getInt(0) + " - " + rs.getString(1) + " - " +
                        rs.getFloat(2));
        }
    }
}
```
