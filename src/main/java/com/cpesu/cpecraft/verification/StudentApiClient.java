package com.cpesu.cpecraft.verification;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Looks up a claimed student ID against the (external, not-yet-built)
 * student-verification API. Runs off the server thread; callers must hop
 * back via {@code server.execute(...)} before touching player/world state.
 */
public interface StudentApiClient {
	CompletableFuture<Optional<StudentInfo>> verify(String studentId);
}
