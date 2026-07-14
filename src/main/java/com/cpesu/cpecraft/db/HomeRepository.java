package com.cpesu.cpecraft.db;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HomeRepository {
    private final Connection connection;

    public HomeRepository(Database database) {
        this.connection = database.connection();
    }

    private static final String SELECT_COLUMNS = "uuid, name, x, y, z, x_rot, y_rot, dimension, created_at, is_default";

    public Optional<HomeRecord> findByPlayerUuidAndHomeName(UUID uuid, String name) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM home WHERE uuid = ? AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(toHomeRecord(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up home " + name + " for player " + uuid, e);
        }
    }

    public Optional<HomeRecord> findDefaultByUuid(UUID uuid) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM home WHERE uuid = ? AND is_default = 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(toHomeRecord(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up default home for player " + uuid, e);
        }
    }

    public List<HomeRecord> findByUuid(UUID uuid) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM home WHERE uuid = ? ORDER BY created_at ASC";
        List<HomeRecord> homes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    homes.add(toHomeRecord(rs));
                }
                return homes;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up homes for player " + uuid, e);
        }
    }

    private static HomeRecord toHomeRecord(ResultSet rs) throws SQLException {
        return new HomeRecord(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getDouble("x_rot"),
                rs.getDouble("y_rot"),
                ResourceKey.create(Registries.DIMENSION, Identifier.parse(rs.getString("dimension"))),
                rs.getLong("created_at"),
                rs.getBoolean("is_default"));
    }

    public void save(HomeRecord record) {
        String sql = """
                INSERT INTO home (uuid, name, x, y, z, x_rot, y_rot, dimension, created_at, is_default)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid, name) DO UPDATE SET
                	x = excluded.x,
                	y = excluded.y,
                	z = excluded.z,
                	x_rot = excluded.x_rot,
                	y_rot = excluded.y_rot,
                	dimension = excluded.dimension,
                	created_at = excluded.created_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, record.uuid().toString());
            statement.setString(2, record.name());
            statement.setDouble(3, record.x());
            statement.setDouble(4, record.y());
            statement.setDouble(5, record.z());
            statement.setDouble(6, record.xRot());
            statement.setDouble(7, record.yRot());
            statement.setString(8, record.dimension().identifier().toString());
            statement.setLong(9, record.createdAt());
            statement.setBoolean(10, record.isDefault());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save home " + record.name() + " for player " + record.uuid(), e);
        }
    }

    public void setDefault(UUID uuid, String name) {
        try (PreparedStatement clear = connection.prepareStatement("UPDATE home SET is_default = 0 WHERE uuid = ?")) {
            clear.setString(1, uuid.toString());
            clear.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear default home for player " + uuid, e);
        }
        try (PreparedStatement set = connection.prepareStatement("UPDATE home SET is_default = 1 WHERE uuid = ? AND name = ?")) {
            set.setString(1, uuid.toString());
            set.setString(2, name);
            set.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set default home '" + name + "' for player " + uuid, e);
        }
    }

    public boolean deleteByUuidAndName(UUID uuid, String name) {
        String sql = "DELETE FROM home WHERE uuid = ? AND name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete home " + name + " for player " + uuid, e);
        }
    }
}
