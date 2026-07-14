package com.cpesu.cpecraft.db;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class LogoutLocationRepository {
    private static final String SELECT_COLUMNS = "uuid, x, y, z, x_rot, y_rot, dimension, logged_out_at";

    private final Connection connection;

    public LogoutLocationRepository(Database database) {
        this.connection = database.connection();
    }

    public Optional<LogoutLocationRecord> findByUuid(UUID uuid) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM logout_location WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(toRecord(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up logout location for player " + uuid, e);
        }
    }

    public void save(LogoutLocationRecord record) {
        String sql = """
                INSERT INTO logout_location (uuid, x, y, z, x_rot, y_rot, dimension, logged_out_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                	x = excluded.x,
                	y = excluded.y,
                	z = excluded.z,
                	x_rot = excluded.x_rot,
                	y_rot = excluded.y_rot,
                	dimension = excluded.dimension,
                	logged_out_at = excluded.logged_out_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.uuid().toString());
            statement.setDouble(2, record.x());
            statement.setDouble(3, record.y());
            statement.setDouble(4, record.z());
            statement.setDouble(5, record.xRot());
            statement.setDouble(6, record.yRot());
            statement.setString(7, record.dimension().identifier().toString());
            statement.setLong(8, record.loggedOutAt());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save logout location for player " + record.uuid(), e);
        }
    }

    private static LogoutLocationRecord toRecord(ResultSet rs) throws SQLException {
        return new LogoutLocationRecord(
                UUID.fromString(rs.getString("uuid")),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getDouble("x_rot"),
                rs.getDouble("y_rot"),
                ResourceKey.create(Registries.DIMENSION, Identifier.parse(rs.getString("dimension"))),
                rs.getLong("logged_out_at"));
    }
}
