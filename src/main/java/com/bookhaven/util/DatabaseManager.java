package com.bookhaven.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton managing database connections and schema migrations for SQLite.
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:bookhaven.db";

    private static volatile DatabaseManager instance;
    private String databaseUrl;
    private Connection connection;

    private DatabaseManager() {
        this.databaseUrl = DEFAULT_DB_URL;
        initializeSchema();
    }

    /**
     * Retrieves the Singleton instance of DatabaseManager.
     *
     * @return the singleton instance
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Configures a custom database URL (useful for testing with in-memory SQLite).
     *
     * @param url the SQLite JDBC URL
     */
    public synchronized void setDatabaseUrl(String url) {
        this.databaseUrl = url;
        closeConnection();
        initializeSchema();
    }

    /**
     * Retrieves an active Connection to the SQLite database.
     * If the current connection is closed or null, a new connection is created.
     *
     * @return the active SQL Connection
     * @throws SQLException if a database access error occurs
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(databaseUrl);
            try (Statement stmt = connection.createStatement()) {
                // Enforce foreign key constraints in SQLite
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    /**
     * Initializes the SQLite schema using schema.sql located in classpath.
     */
    public synchronized void initializeSchema() {
        try (Connection conn = DriverManager.getConnection(databaseUrl)) {
            try (Statement pragmaStmt = conn.createStatement()) {
                pragmaStmt.execute("PRAGMA foreign_keys = ON;");
            }

            InputStream inputStream = getClass().getResourceAsStream("/sql/schema.sql");
            if (inputStream == null) {
                LOGGER.warning("Could not find /sql/schema.sql on classpath. Schema initialization skipped.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder sql = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    // Skip comment lines
                    if (line.trim().startsWith("--")) {
                        continue;
                    }
                    sql.append(line).append("\n");
                }

                String[] statements = sql.toString().split(";");
                try (Statement stmt = conn.createStatement()) {
                    for (String statement : statements) {
                        String trimmed = statement.trim();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
                LOGGER.info("Database schema initialized successfully.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    /**
     * Closes the active database connection.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            } finally {
                connection = null;
            }
        }
    }
}
