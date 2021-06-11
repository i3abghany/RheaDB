import RheaDB.*;

import RheaDB.JDBCDriver.JCResultSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.*;

public class JDBCTest {
    private Connection connect(String url) throws ClassNotFoundException, SQLException {
        Class.forName("RheaDB.JDBCDriver.JCDriver");
        return DriverManager.getConnection(url);
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
    public void createTable() {
        try {
            Connection conn = connect("jdbc:rhea:/home/pwng/dbdata");
            Statement statement = conn.createStatement();
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("CREATE TABLE FancyTable id INT name STRING mass FLOAT");
            Assertions.assertNull(resultSet.getIterator());
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void insertOneRow() {
        try {
            Connection conn = connect("jdbc:rhea:/home/pwng/dbdata");
            Statement statement = conn.createStatement();
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("INSERT INTO FancyTable 1 \"Random Name\" 69.42");
            Assertions.assertNull(resultSet.getIterator());
        } catch (Exception exception) {
            Assertions.fail();
        }
    }

    @Test
    public void getColumnsByName() {
        try {
            Connection conn = connect("jdbc:rhea:/home/pwng/dbdata");
            Statement statement = conn.createStatement();
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("SELECT id, name, mass from FancyTable");
            Assertions.assertTrue(resultSet.next());
            Assertions.assertEquals(resultSet.getInt("id"), 1);
            Assertions.assertEquals(resultSet.getString("name"), "Random Name");
            Assertions.assertEquals(resultSet.getFloat("mass"), 69.42, 0.0001);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void getColumnsByIndex() {
        try {
            Connection conn = connect("jdbc:rhea:/home/pwng/dbdata");
            Statement statement = conn.createStatement();
            JCResultSet resultSet = (JCResultSet) statement.executeQuery("SELECT id, name, mass from FancyTable");
            Assertions.assertTrue(resultSet.next());
            Assertions.assertEquals(resultSet.getInt(0), 1);
            Assertions.assertEquals(resultSet.getString(1), "Random Name");
            Assertions.assertEquals(resultSet.getFloat(2), 69.42, 0.0001);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void insertThousandRows() {
        try {
            Connection conn = connect("jdbc:rhea:/home/pwng/dbdata");
            Statement statement = conn.createStatement();
            for (int i = 0; i < 1000; i++) {
                JCResultSet resultSet = (JCResultSet) statement.executeQuery("INSERT INTO FancyTable " +
                        (i + 2) + " \"Random Name\" 69.42");
                Assertions.assertNull(resultSet.getIterator());
            }
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }

    @Test
    public void selectWithoutPredicates() {
        try {
            Connection conn = connect("jdbc:rhea:/home/pwng/dbdata");
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT id, name, mass from FancyTable");
            int count = 0;
            while (resultSet.next()) {
                System.out.println(resultSet.getInt(0) + "-" +
                                   resultSet.getString(1) + "-" +
                                   resultSet.getFloat(2));
                count++;
            }
            Assertions.assertEquals(count, 1001);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Assertions.fail();
        }
    }
}
