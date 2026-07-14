package com.cpesu.cpecraft.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class ConfigRepository {
    private final Connection connection;

    public ConfigRepository(Database database) {
        this.connection = database.connection();
    }

    public Optional<ConfigRecord> findByKey(String key) {
        String sql = "SELECT key, value FROM config WHERE key = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ConfigRecord(
                        rs.getString("key"),
                        rs.getString("value")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up config " + key, e);
        }
    }

    public void save(ConfigRecord record) {
        String sql = """
				INSERT INTO config (key, value)
				VALUES (?, ?)
				ON CONFLICT(key) DO UPDATE SET
					value = excluded.value
				""";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.key());
            statement.setString(2, record.value());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save config " + record.key(), e);
        }
    }
}
