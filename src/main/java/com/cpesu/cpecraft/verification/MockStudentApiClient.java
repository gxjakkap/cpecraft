package com.cpesu.cpecraft.verification;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Stand-in for the YB student-verification API, useful for testing the
 * freeze/verify/unfreeze flow without hitting the real endpoint or needing
 * real credentials in config.json. Not wired up by default anymore -
 * {@link HttpStudentApiClient} is - but swap it back in
 * {@code Cpecraft#onInitialize} for local dev if needed.
 */
public final class MockStudentApiClient implements StudentApiClient {
	private static final Map<String, StudentInfo> KNOWN_STUDENTS = Map.of(
			"6501234", new StudentInfo("6501234", "Test Student One", "One", "29"),
			"6501235", new StudentInfo("6501235", "Test Student Two", "Two", "30"));

	@Override
	public CompletableFuture<Optional<StudentInfo>> verify(String studentId) {
		return CompletableFuture.completedFuture(Optional.ofNullable(KNOWN_STUDENTS.get(studentId)));
	}
}
