# RheaDB

A simple disk-based DBMS that supports simple SQL-like querying statements.

## What's currently implemented?
* Simple SQL-like queries
* Disk-based storage
* In-memory buffer pool caching
* JDBC Driver

### JDBC driver loading and usage
The connection url passed to `DriverManager.getConnection()` must be in the
format `jdbc:rhea:DIR_PATH`, where `DIR_PATH` is the storage directory for the
database. It must be an existing directory.

`Class.forName("RheaDB.JDBCDriver.JCDriver")` must be used so as to invoke the
`static` portion of the driver, and (hopefully) successfully connect to a 
database insance.


```java
import java.sql.*;

public class ExampleProgram {
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        Class.forName("RheaDB.JDBCDriver.JCDriver");
        try (
                Connection conn = DriverManager.getConnection("jdbc:rhea:/home/USER_NAME/dbdata");
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id name salary FROM FancyTable")
        ) {
            while (rs.next())
                System.out.println(rs.getInt(0) + " - " + rs.getString(1) + " - " +
                        rs.getFloat(2));
        }
    }
}
```
