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

## Performance test suite
The repository includes a dedicated performance suite in
`test/performance/RheaDBPerformanceTest.java`.

The suite is intentionally gated behind the JVM system property
`rheadb.perf=true` so it does not slow down the normal unit-test cycle.

### Running it in IntelliJ
Create or edit a JUnit run configuration for `RheaDBPerformanceTest` and add:

```text
-Drheadb.perf=true
```

### JDBC driver loading and usage
The connection url passed to `DriverManager.getConnection()` must be in the
format `jdbc:rhea:DIR_PATH`, where `DIR_PATH` is the storage directory for the
database. The directory will be created if it does not exist.

`Class.forName("RheaDB.JDBCDriver.JCDriver")` must be used so as to invoke the
`static` portion of the driver, and (hopefully) successfully connect to a 
database instance.

```java
import java.sql.DriverManager;

public class RheaDBJDBCTest {
    public static void main(String[] args) throws Exception {
        Class.forName("RheaDB.JDBCDriver.JCDriver");
        try (var c = DriverManager.getConnection("jdbc:rhea:./dbdata");
             var s = c.createStatement()) {

            s.execute("CREATE TABLE Planets (ord INT, name STRING, mass FLOAT);");
            s.execute("INSERT INTO Planets VALUES (1, \"Mercury\", 0.328);");
            s.execute("INSERT INTO Planets VALUES (2, \"Venus\", 4.867);");
            s.execute("INSERT INTO Planets VALUES (3, \"Earth\", 5.972);");

            try (var rs = s.executeQuery("SELECT * FROM Planets;")) {
                while (rs.next())
                    System.out.println(rs.getInt(0) + ", " + rs.getString(1) + ", " + rs.getFloat(2));
            }
        }
    }
}
```

This will create a database in the `./dbdata` directory, create a table named `Planets`, insert some data into it, and then select and print that data.

```console
1, Mercury, 0.328
2, Venus, 4.867
3, Earth, 5.972
```
