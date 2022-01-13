package RheaDB.JDBCDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JCDriver implements Driver {
    private static final JCDriver INSTANCE = new JCDriver();
    private static boolean registered = false;

    static {
        registerDriver();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static synchronized Driver registerDriver() {
        if (!registered) {
            registered = true;
            try {
                DriverManager.registerDriver(INSTANCE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return INSTANCE;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        String[] tokens = url.split(":");
        if (tokens.length < 2 ||
                !tokens[0].toLowerCase(Locale.ROOT).equals("jdbc") ||
                !tokens[1].toLowerCase(Locale.ROOT).equals("rhea")) {
            return null;
        }


        String dataDir = Arrays.stream(tokens).skip(2).collect(Collectors.joining(":"));
        Path path = Paths.get(dataDir).toAbsolutePath();
        if (!Files.isDirectory(path)) {
            throw new SQLException("\"" + path + "\"" + " is not a valid directory.");
        }

        try {
            return new JCConnection(path);
        } catch (IOException e) {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public boolean acceptsURL(String url) {
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
