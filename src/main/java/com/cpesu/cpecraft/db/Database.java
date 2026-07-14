package com.cpesu.cpecraft.db;

import com.cpesu.cpecraft.Cpecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/**
 * Owns the single SQLite connection for the mod. SQLite handles concurrent
 * writers poorly, so all repository calls share this one connection and
 * must run off the server thread (they're blocking JDBC calls).
 */
public final class Database implements AutoCloseable {
    private final Connection connection;

    public Database(Path configDir) {
        try {
            Path dir = configDir.resolve(Cpecraft.MOD_ID);
            Files.createDirectories(dir);
            Path dbFile = dir.resolve("data.db");

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS students (
                        	uuid          TEXT PRIMARY KEY,
                        	username      TEXT NOT NULL,
                        	student_id    TEXT NOT NULL,
                        	name          TEXT,
                        	nickname      TEXT,
                        	batch         TEXT,
                        	verified_at   INTEGER NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS batches (
                        	batch_number    INTEGER PRIMARY KEY,
                        	luckperms_group TEXT NOT NULL,
                        	display_name    TEXT NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS config (
                        	key     TEXT PRIMARY KEY,
                        	value   TEXT
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS logout_location (
                            uuid           TEXT PRIMARY KEY,
                            x              REAL NOT NULL,
                            y              REAL NOT NULL,
                            z              REAL NOT NULL,
                            x_rot          REAL NOT NULL,
                            y_rot          REAL NOT NULL,
                            dimension      TEXT NOT NULL,
                            logged_out_at  INTEGER NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS home (
                            uuid        TEXT NOT NULL,
                            name        TEXT NOT NULL,
                            x           REAL NOT NULL,
                            y           REAL NOT NULL,
                            z           REAL NOT NULL,
                            x_rot       REAL NOT NULL,
                            y_rot       REAL NOT NULL,
                            dimension   TEXT NOT NULL,
                            created_at  INTEGER NOT NULL,
                            is_default  BOOLEAN NOT NULL DEFAULT FALSE,
                            PRIMARY KEY (uuid, name)
                        )
                        """);

                // for migration after table is created
                ensureColumn(statement, "students", "nickname", "TEXT");
                ensureColumn(statement, "home", "x_rot", "REAL NOT NULL DEFAULT 0");
                ensureColumn(statement, "home", "y_rot", "REAL NOT NULL DEFAULT 0");
                ensureColumn(statement, "home", "is_default", "BOOLEAN NOT NULL DEFAULT FALSE");

                ensureConfig(connection, "max_home_quota", "3");
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to initialize cpecraft database", e);
        }
    }

    private static void ensureColumn(Statement statement, String table, String column, String type) throws SQLException {
        try (ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(column)) {
                    return;
                }
            }
        }
        statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }

    private static void ensureConfig(Connection connection, String key, String defaultValue) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement("SELECT 1 FROM config WHERE key = ?")) {
            check.setString(1, key);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO config (key, value) VALUES (?, ?)")) {
            insert.setString(1, key);
            insert.setString(2, defaultValue);
            insert.executeUpdate();
        }
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            Cpecraft.LOGGER.warn("Failed to close database connection", e);
        }
    }
}
