package com.cpesu.cpecraft.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Blocking JDBC access to the {@code batches} table. Callers must invoke
 * these methods off the server thread.
 */
public final class BatchRepository {
	public record Batch(int batchNumber, String luckpermsGroup, String displayName) {
	}

	private final Connection connection;

	public BatchRepository(Database database) {
		this.connection = database.connection();
	}

	public Optional<Batch> findByNumber(int batchNumber) {
		String sql = "SELECT batch_number, luckperms_group, display_name FROM batches WHERE batch_number = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, batchNumber);
			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return Optional.empty();
				}
				return Optional.of(toBatch(rs));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to look up batch " + batchNumber, e);
		}
	}

	public List<Batch> findAll() {
		String sql = "SELECT batch_number, luckperms_group, display_name FROM batches ORDER BY batch_number";
		List<Batch> batches = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				batches.add(toBatch(rs));
			}
			return batches;
		} catch (SQLException e) {
			throw new RuntimeException("Failed to list batches", e);
		}
	}

	/** Returns false without writing if a batch with this number already exists. */
	public boolean insert(Batch batch) {
		if (findByNumber(batch.batchNumber()).isPresent()) {
			return false;
		}
		String sql = "INSERT INTO batches (batch_number, luckperms_group, display_name) VALUES (?, ?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, batch.batchNumber());
			statement.setString(2, batch.luckpermsGroup());
			statement.setString(3, batch.displayName());
			statement.executeUpdate();
			return true;
		} catch (SQLException e) {
			throw new RuntimeException("Failed to insert batch " + batch.batchNumber(), e);
		}
	}

	/** Returns true if a batch was deleted. */
	public boolean delete(int batchNumber) {
		String sql = "DELETE FROM batches WHERE batch_number = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, batchNumber);
			return statement.executeUpdate() > 0;
		} catch (SQLException e) {
			throw new RuntimeException("Failed to delete batch " + batchNumber, e);
		}
	}

	private static Batch toBatch(ResultSet rs) throws SQLException {
		return new Batch(
				rs.getInt("batch_number"),
				rs.getString("luckperms_group"),
				rs.getString("display_name"));
	}
}
