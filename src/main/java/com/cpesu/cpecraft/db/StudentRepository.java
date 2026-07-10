package com.cpesu.cpecraft.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Blocking JDBC access to the {@code students} table. Callers must invoke
 * these methods off the server thread.
 */
public final class StudentRepository {
	private final Connection connection;

	public StudentRepository(Database database) {
		this.connection = database.connection();
	}

	public Optional<StudentRecord> findByUuid(UUID uuid) {
		String sql = "SELECT uuid, username, student_id, name, batch, verified_at FROM students WHERE uuid = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid.toString());
			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return Optional.empty();
				}
				return Optional.of(new StudentRecord(
						UUID.fromString(rs.getString("uuid")),
						rs.getString("username"),
						rs.getString("student_id"),
						rs.getString("name"),
						rs.getString("batch"),
						rs.getLong("verified_at")));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to look up student " + uuid, e);
		}
	}

	/** Returns true if a record was deleted. */
	public boolean deleteByUuid(UUID uuid) {
		String sql = "DELETE FROM students WHERE uuid = ?";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid.toString());
			return statement.executeUpdate() > 0;
		} catch (SQLException e) {
			throw new RuntimeException("Failed to delete student " + uuid, e);
		}
	}

	public void save(StudentRecord record) {
		String sql = """
				INSERT INTO students (uuid, username, student_id, name, batch, verified_at)
				VALUES (?, ?, ?, ?, ?, ?)
				ON CONFLICT(uuid) DO UPDATE SET
					username = excluded.username,
					student_id = excluded.student_id,
					name = excluded.name,
					batch = excluded.batch,
					verified_at = excluded.verified_at
				""";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, record.uuid().toString());
			statement.setString(2, record.username());
			statement.setString(3, record.studentId());
			statement.setString(4, record.name());
			statement.setString(5, record.batch());
			statement.setLong(6, record.verifiedAt());
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to save student " + record.uuid(), e);
		}
	}
}
