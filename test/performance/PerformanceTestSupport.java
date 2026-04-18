package performance;

import RheaDB.QueryResult;
import RheaDB.RheaDB;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class PerformanceTestSupport {
    static final int WARMUP_ITERATIONS = 1;
    static final int MEASUREMENT_ITERATIONS = 5;

    private PerformanceTestSupport() {
    }

    static BenchmarkResult runBenchmark(String name,
                                        BenchmarkScenario scenario,
                                        int warmupIterations,
                                        int measurementIterations) throws Exception {
        for (int i = 0; i < warmupIterations; i++) {
            runSingleIteration(scenario);
        }

        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < measurementIterations; i++) {
            durations.add(runSingleIteration(scenario));
        }

        BenchmarkResult result = new BenchmarkResult(name, durations);
        System.out.println(result.format());
        return result;
    }

    private static long runSingleIteration(BenchmarkScenario scenario) throws Exception {
        try (BenchmarkContext context = BenchmarkContext.create()) {
            try {
                scenario.setup(context);
                long start = System.nanoTime();
                scenario.run(context);
                long elapsed = System.nanoTime() - start;
                scenario.verify(context);
                return elapsed;
            } finally {
                scenario.cleanup();
            }
        }
    }

    static String insertStatement(String tableName, int id) {
        double mass = 100.0 + (id % 1000) / 10.0;
        return String.format(Locale.US,
                "INSERT INTO %s VALUES (%d, \"name_%d\", %.1f);",
                tableName,
                id,
                id,
                mass);
    }

    static void createBenchmarkTable(RheaDB db, String tableName) {
        db.executeStatement("CREATE TABLE " + tableName + " (id INT, name STRING, mass FLOAT);");
    }

    static void populateTable(RheaDB db, String tableName, int rowCount) {
        for (int i = 0; i < rowCount; i++) {
            db.executeStatement(insertStatement(tableName, i));
        }
    }

    static int countRows(QueryResult queryResult) {
        return queryResult == null ? 0 : queryResult.getRows().size();
    }

    static int[] pickUniqueIds(int size, int upperBound, long seed) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative.");
        }
        if (upperBound < 0) {
            throw new IllegalArgumentException("upperBound must be non-negative.");
        }
        if (size > upperBound) {
            throw new IllegalArgumentException(
                    "Cannot pick " + size + " unique ids from range size " + upperBound + "."
            );
        }

        Random random = new Random(seed);
        int[] ids = new int[size];
        boolean[] used = new boolean[upperBound];
        int index = 0;
        while (index < size) {
            int candidate = random.nextInt(upperBound);
            if (used[candidate]) {
                continue;
            }
            used[candidate] = true;
            ids[index++] = candidate;
        }
        return ids;
    }

    interface BenchmarkScenario {
        void setup(BenchmarkContext context) throws Exception;

        void run(BenchmarkContext context) throws Exception;

        void verify(BenchmarkContext context) throws Exception;

        default void cleanup() throws Exception {
        }
    }

    static final class BenchmarkContext implements AutoCloseable {
        private final Path dataDirectory;
        private final RheaDB db;

        private BenchmarkContext(Path dataDirectory, RheaDB db) {
            this.dataDirectory = dataDirectory;
            this.db = db;
        }

        static BenchmarkContext create() throws IOException {
            Path directory = Files.createTempDirectory("rheadb-perf-");
            RheaDB db = new RheaDB(directory.toString());
            db.setLazyCommit(false);
            return new BenchmarkContext(directory, db);
        }

        RheaDB db() {
            return db;
        }

        Path dataDirectory() {
            return dataDirectory;
        }

        @Override
        public void close() throws Exception {
            try {
                db.close();
            } finally {
                deleteDirectory(dataDirectory);
            }
        }

        private static void deleteDirectory(Path directory) throws IOException {
            if (directory == null || !Files.exists(directory)) {
                return;
            }

            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    static final class BenchmarkResult {
        private final String name;
        private final List<Long> samplesNanos;

        BenchmarkResult(String name, List<Long> samplesNanos) {
            this.name = name;
            this.samplesNanos = new ArrayList<>(samplesNanos);
            this.samplesNanos.sort(Long::compareTo);
        }

        double minMillis() {
            return nanosToMillis(samplesNanos.get(0));
        }

        double avgMillis() {
            long total = 0L;
            for (Long sample : samplesNanos) {
                total += sample;
            }
            return nanosToMillis(total / (double) samplesNanos.size());
        }

        double p50Millis() {
            return percentileMillis(0.50);
        }

        double p95Millis() {
            return percentileMillis(0.95);
        }

        String format() {
            return String.format(Locale.US,
                    "[PERF] %s -> min=%.2f ms avg=%.2f ms p50=%.2f ms p95=%.2f ms samples=%s",
                    name,
                    minMillis(),
                    avgMillis(),
                    p50Millis(),
                    p95Millis(),
                    samplesAsMillis());
        }

        private double percentileMillis(double percentile) {
            int index = (int) Math.ceil(percentile * samplesNanos.size()) - 1;
            index = Math.max(0, Math.min(index, samplesNanos.size() - 1));
            return nanosToMillis(samplesNanos.get(index));
        }

        private String samplesAsMillis() {
            List<String> formatted = new ArrayList<>();
            for (Long sample : samplesNanos) {
                formatted.add(String.format(Locale.US, "%.2f", nanosToMillis(sample)));
            }
            return formatted.toString();
        }

        private static double nanosToMillis(double nanos) {
            return nanos / 1_000_000.0;
        }
    }
}
