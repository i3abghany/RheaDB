import RheaDB.JDBCDriver.JCResultSet;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Comparator;

public class JDBCTest {
    private Connection conn;
    private static final String userName = System.getProperty("user.name");
    private static final String dataDirPath = "/home/" + userName + "/dbdata";

    static Connection connect(String url) throws ClassNotFoundException, SQLException {
        Class.forName("RheaDB.JDBCDriver.JCDriver");
        return DriverManager.getConnection(url);
    }

    @BeforeAll
    static void createDataDir() {
        File dir = new File(dataDirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (!created) {
                System.out.println("Could not create data dir.");
                Assertions.fail();
            }
        }
    }

    @AfterAll
    static void deleteDataDir() {
        try {
            Files.walk(Paths.get(dataDirPath))
                    .map(Path::toFile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(File::delete);
        } catch (IOException ioException) {
            System.out.println("Could not delete data dir.");
            Assertions.fail();
        }
    }

    void createTestingTable(String tableName) {
        try {
            String userName = System.getProperty("user.name");
            conn = connect("jdbc:rhea:/home/" + userName + "/dbdata");
            Statement statement = conn.createStatement();
            statement.executeQuery("CREATE TABLE " + tableName +
                    " (id INT, name STRING, mass FLOAT)");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    void dropTestTable(String tableName) {
        try {
            Statement statement = conn.createStatement();
            statement.executeQuery("DROP TABLE " + tableName);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void connectivityTest() {
        try {
            connect("jdbc:rhea:/home/pwng/dbdata");
        } catch (Exception exception) {
            Assertions.fail();
        }
    }

    @Test
    public void getColumnsByName() {
        try {
            createTestingTable("TestTableGetColumnsByName");
            Statement statement = conn.createStatement();
            statement.executeQuery("INSERT INTO TestTableGetColumnsByName VALUES (1, \"Random Name\", 69.42)");
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("SELECT * from TestTableGetColumnsByName");
            Assertions.assertTrue(resultSet.next());
            Assertions.assertEquals(resultSet.getInt("id"), 1);
            Assertions.assertEquals(resultSet.getString("name"), "Random Name");
            Assertions.assertEquals(resultSet.getFloat("mass"), 69.42, 0.0001);
            dropTestTable("TestTableGetColumnsByName");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void getColumnsByIndex() {
        try {
            createTestingTable("TestTableGetColumnsByIndex");
            Statement statement = conn.createStatement();
            statement.executeQuery("INSERT INTO TestTableGetColumnsByIndex VALUES (1, \"Random Name\", 69.42)");
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("SELECT * from TestTableGetColumnsByIndex");
            Assertions.assertTrue(resultSet.next());
            Assertions.assertEquals(resultSet.getInt(0), 1);
            Assertions.assertEquals(resultSet.getString(1), "Random Name");
            Assertions.assertEquals(resultSet.getFloat(2), 69.42, 0.0001);
            dropTestTable("TestTableGetColumnsByIndex");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void insertThousandRows() {
        try {
            createTestingTable("TestTableInsertThousandRows");
            Statement statement = conn.createStatement();
            for (int i = 0; i < 1000; i++) {
                JCResultSet resultSet = (JCResultSet) statement.executeQuery(
                        "INSERT INTO TestTableInsertThousandRows VALUES (" +
                        (i + 2) + ", \"Random Name\", 69.42)");
                Assertions.assertNull(resultSet.getIterator());
            }

            ResultSet rs = statement.executeQuery("SELECT * FROM TestTableInsertThousandRows");
            int i = 0;
            while (rs.next()) i++;

            Assertions.assertEquals(i, 1000);

            dropTestTable("TestTableInsertThousandRows");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void selectWithoutPredicates() {
        try {
            createTestingTable("TestTableSelectWithoutPredicates");
            Statement statement = conn.createStatement();
            statement.executeQuery("INSERT INTO TestTableSelectWithoutPredicates VALUES (1, \"Random String\", 42.69)");
            statement.executeQuery("INSERT INTO TestTableSelectWithoutPredicates VALUES (2, \"Not a random String\", 69.42)");
            statement.executeQuery("INSERT INTO TestTableSelectWithoutPredicates VALUES (3, \"Second random String\", 3.141)");
            ResultSet resultSet = statement.executeQuery("SELECT * from TestTableSelectWithoutPredicates");
            int count = 0;
            while (resultSet.next()) count++;
            Assertions.assertEquals(count, 3);
            dropTestTable("TestTableSelectWithoutPredicates");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void selectWithPredicate() {
        try {
            createTestingTable("TestTableSelectWithPredicates");
            Statement statement = conn.createStatement();
            statement.executeQuery("INSERT INTO TestTableSelectWithPredicates VALUES (1, \"Random String\", 42.69)");
            statement.executeQuery("INSERT INTO TestTableSelectWithPredicates VALUES (2, \"Not a random String\", 69.42)");

            JCResultSet resultSet = (JCResultSet) statement.executeQuery("SELECT * FROM TestTableSelectWithPredicates WHERE id = 2");
            Assertions.assertNotNull(resultSet.getIterator());

            Assertions.assertTrue(resultSet.next());
            Assertions.assertFalse(resultSet.next());

            Assertions.assertEquals(resultSet.getInt(0), 2);
            Assertions.assertEquals(resultSet.getString(1), "Not a random String");
            dropTestTable("TestTableSelectWithPredicates");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    void createIndexTest() {
        try {
            createTestingTable("TestTableWithIndex");
            Statement statement = conn.createStatement();
            statement.executeQuery("INSERT INTO TestTableWithIndex VALUES (1, \"Random String\", 42.69)");
            statement.executeQuery("INSERT INTO TestTableWithIndex VALUES (2, \"Not a random String\", 69.42)");

            statement.executeQuery("CREATE INDEX TestTableWithIndex id");
            String userName = System.getProperty("user.name");
            File idx_file = new File("/home/" + userName + "/dbdata/TestTableWithIndex/index/id.idx");
            Assertions.assertTrue(idx_file.exists());
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("SELECT * FROM TestTableWithIndex WHERE id = 2");
            Assertions.assertNotNull(resultSet.getIterator());

            Assertions.assertTrue(resultSet.next());
            Assertions.assertFalse(resultSet.next());

            Assertions.assertEquals(resultSet.getInt(0), 2);
            Assertions.assertEquals(resultSet.getString(1), "Not a random String");
            dropTestTable("TestTableWithIndex");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    void dropIndexTest() {
        try {
            createTestingTable("TestTableDeleteIndex");
            Statement statement = conn.createStatement();
            statement.executeQuery("INSERT INTO TestTableDeleteIndex VALUES (1, \"Random String\", 42.69)");
            statement.executeQuery("INSERT INTO TestTableDeleteIndex VALUES (2, \"Not a random String\", 69.42)");

            statement.executeQuery("CREATE INDEX TestTableDeleteIndex id");
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("SELECT * FROM TestTableDeleteIndex WHERE id = 2");
            Assertions.assertNotNull(resultSet.getIterator());

            Assertions.assertTrue(resultSet.next());
            Assertions.assertFalse(resultSet.next());

            Assertions.assertEquals(resultSet.getInt(0), 2);
            Assertions.assertEquals(resultSet.getString(1), "Not a random String");
            statement.executeQuery("DROP INDEX TestTableDeleteIndex id");
            String userName = System.getProperty("user.name");
            File idx_file = new File("/home/" + userName + "/dbdata/TestTableDeleteIndex/index/id.idx");
            Assertions.assertFalse(idx_file.exists());
            dropTestTable("TestTableDeleteIndex");
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }
}
