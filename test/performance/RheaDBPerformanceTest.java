package performance;

import RheaDB.QueryResult;
import RheaDB.RheaDB;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@EnabledIfSystemProperty(named = "rheadb.perf", matches = "true")
public class RheaDBPerformanceTest {
    private static final int INSERT_ROW_COUNT = 10_000;
    private static final int SCAN_ROW_COUNT = 20_000;
//    private static final int INDEX_ROW_COUNT = 25_000;
    private static final int INDEX_ROW_COUNT = 2500;
    private static final int UPDATE_ROW_COUNT = 12_000;
    private static final int DELETE_ROW_COUNT = 12_000;
    private static final int JDBC_INSERT_ROW_COUNT = 5_000;

    @Test
    void bulkInsertCoreEngine() throws Exception {
        PerformanceTestSupport.runBenchmark(
                "bulk insert via core engine",
                new PerformanceTestSupport.BenchmarkScenario() {
                    @Override
                    public void setup(PerformanceTestSupport.BenchmarkContext context) {
                        PerformanceTestSupport.createBenchmarkTable(context.db(), "PerfInsert");
                    }

                    @Override
                    public void run(PerformanceTestSupport.BenchmarkContext context) {
                        RheaDB db = context.db();
                        for (int i = 0; i < INSERT_ROW_COUNT; i++) {
                            db.executeStatement(PerformanceTestSupport.insertStatement("PerfInsert", i));
                        }
                    }

                    @Override
                    public void verify(PerformanceTestSupport.BenchmarkContext context) {
                        QueryResult result = context.db().executeStatement("SELECT * FROM PerfInsert;");
                        Assertions.assertEquals(INSERT_ROW_COUNT, PerformanceTestSupport.countRows(result));
                    }
                },
                PerformanceTestSupport.WARMUP_ITERATIONS,
                PerformanceTestSupport.MEASUREMENT_ITERATIONS
        );
    }

    @Test
    void fullScanSelectCoreEngine() throws Exception {
        PerformanceTestSupport.runBenchmark(
                "full scan select via core engine",
                new PerformanceTestSupport.BenchmarkScenario() {
                    @Override
                    public void setup(PerformanceTestSupport.BenchmarkContext context) {
                        PerformanceTestSupport.createBenchmarkTable(context.db(), "PerfScan");
                        PerformanceTestSupport.populateTable(context.db(), "PerfScan", SCAN_ROW_COUNT);
                    }

                    @Override
                    public void run(PerformanceTestSupport.BenchmarkContext context) {
                        QueryResult result = context.db().executeStatement("SELECT * FROM PerfScan;");
                        Assertions.assertEquals(SCAN_ROW_COUNT, PerformanceTestSupport.countRows(result));
                    }

                    @Override
                    public void verify(PerformanceTestSupport.BenchmarkContext context) {
                        QueryResult result = context.db().executeStatement("SELECT * FROM PerfScan;");
                        Assertions.assertEquals(SCAN_ROW_COUNT, PerformanceTestSupport.countRows(result));
                    }
                },
                PerformanceTestSupport.WARMUP_ITERATIONS,
                PerformanceTestSupport.MEASUREMENT_ITERATIONS
        );
    }

    @Test
    void indexedLookupCoreEngine() throws Exception {
        final int[] lookupIds = PerformanceTestSupport.pickUniqueIds(3, INDEX_ROW_COUNT, 1337L);
        PerformanceTestSupport.runBenchmark(
                "indexed lookups via core engine",
                new PerformanceTestSupport.BenchmarkScenario() {
                    @Override
                    public void setup(PerformanceTestSupport.BenchmarkContext context) {
                        PerformanceTestSupport.createBenchmarkTable(context.db(), "PerfIndexTable");
                        PerformanceTestSupport.populateTable(context.db(), "PerfIndexTable", INDEX_ROW_COUNT);
                        context.db().executeStatement("CREATE INDEX PerfIndexTable id;");
                    }

                    @Override
                    public void run(PerformanceTestSupport.BenchmarkContext context) {
                        for (int lookupId : lookupIds) {
                            QueryResult result = context.db()
                                    .executeStatement("SELECT * FROM PerfIndexTable WHERE id = " + lookupId + ";");
                            Assertions.assertEquals(1, PerformanceTestSupport.countRows(result));
                        }
                    }

                    @Override
                    public void verify(PerformanceTestSupport.BenchmarkContext context) {
                        QueryResult result = context.db()
                                .executeStatement("SELECT * FROM PerfIndexTable WHERE id = " + lookupIds[0] + ";");
                        Assertions.assertEquals(1, PerformanceTestSupport.countRows(result));
                    }
                },
                PerformanceTestSupport.WARMUP_ITERATIONS,
                PerformanceTestSupport.MEASUREMENT_ITERATIONS
        );
    }

    @Test
    void predicateUpdateCoreEngine() throws Exception {
        PerformanceTestSupport.runBenchmark(
                "predicate update via core engine",
                new PerformanceTestSupport.BenchmarkScenario() {
                    @Override
                    public void setup(PerformanceTestSupport.BenchmarkContext context) {
                        PerformanceTestSupport.createBenchmarkTable(context.db(), "PerfUpdate");
                        PerformanceTestSupport.populateTable(context.db(), "PerfUpdate", UPDATE_ROW_COUNT);
                    }

                    @Override
                    public void run(PerformanceTestSupport.BenchmarkContext context) {
                        context.db().executeStatement(
                                "UPDATE PerfUpdate SET name = \"updated\" WHERE id >= " + (UPDATE_ROW_COUNT / 2) + ";"
                        );
                    }

                    @Override
                    public void verify(PerformanceTestSupport.BenchmarkContext context) {
                        QueryResult result = context.db()
                                .executeStatement("SELECT * FROM PerfUpdate WHERE name = \"updated\";");
                        Assertions.assertEquals(UPDATE_ROW_COUNT / 2, PerformanceTestSupport.countRows(result));
                    }
                },
                PerformanceTestSupport.WARMUP_ITERATIONS,
                PerformanceTestSupport.MEASUREMENT_ITERATIONS
        );
    }

    @Test
    void predicateDeleteCoreEngine() throws Exception {
        PerformanceTestSupport.runBenchmark(
                "predicate delete via core engine",
                new PerformanceTestSupport.BenchmarkScenario() {
                    @Override
                    public void setup(PerformanceTestSupport.BenchmarkContext context) {
                        PerformanceTestSupport.createBenchmarkTable(context.db(), "PerfDelete");
                        PerformanceTestSupport.populateTable(context.db(), "PerfDelete", DELETE_ROW_COUNT);
                    }

                    @Override
                    public void run(PerformanceTestSupport.BenchmarkContext context) {
                        context.db().executeStatement(
                                "DELETE FROM PerfDelete WHERE id >= " + (DELETE_ROW_COUNT / 2) + ";"
                        );
                    }

                    @Override
                    public void verify(PerformanceTestSupport.BenchmarkContext context) {
                        QueryResult result = context.db().executeStatement("SELECT * FROM PerfDelete;");
                        Assertions.assertEquals(DELETE_ROW_COUNT / 2, PerformanceTestSupport.countRows(result));
                    }
                },
                PerformanceTestSupport.WARMUP_ITERATIONS,
                PerformanceTestSupport.MEASUREMENT_ITERATIONS
        );
    }

    @Test
    void bulkInsertJdbcDriver() throws Exception {
        Class.forName("RheaDB.JDBCDriver.JCDriver");

        PerformanceTestSupport.runBenchmark(
                "bulk insert via JDBC driver",
                new PerformanceTestSupport.BenchmarkScenario() {
                    private Connection connection;
                    private Statement statement;

                    @Override
                    public void setup(PerformanceTestSupport.BenchmarkContext context) throws Exception {
                        Path dataDirectory = context.dataDirectory();
                        connection = DriverManager.getConnection("jdbc:rhea:" + dataDirectory);
                        connection.setAutoCommit(true);
                        statement = connection.createStatement();
                        statement.executeQuery("CREATE TABLE PerfJdbc (id INT, name STRING, mass FLOAT);");
                    }

                    @Override
                    public void run(PerformanceTestSupport.BenchmarkContext context) throws Exception {
                        for (int i = 0; i < JDBC_INSERT_ROW_COUNT; i++) {
                            statement.executeQuery(PerformanceTestSupport.insertStatement("PerfJdbc", i));
                        }
                    }

                    @Override
                    public void verify(PerformanceTestSupport.BenchmarkContext context) throws Exception {
                        ResultSet resultSet = statement.executeQuery("SELECT * FROM PerfJdbc;");
                        int rows = 0;
                        while (resultSet.next()) {
                            rows++;
                        }
                        Assertions.assertEquals(JDBC_INSERT_ROW_COUNT, rows);
                    }

                    @Override
                    public void cleanup() throws Exception {
                        if (statement != null) {
                            statement.close();
                        }
                        if (connection != null) {
                            connection.close();
                        }
                    }
                },
                PerformanceTestSupport.WARMUP_ITERATIONS,
                PerformanceTestSupport.MEASUREMENT_ITERATIONS
        );
    }
}
