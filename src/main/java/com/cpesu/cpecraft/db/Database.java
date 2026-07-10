package com.cpesu.cpecraft.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.cpesu.cpecraft.Cpecraft;

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
			}
		} catch (IOException | SQLException e) {
			throw new RuntimeException("Failed to initialize cpecraft database", e);
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
