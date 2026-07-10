package com.cpesu.cpecraft.verification;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.BatchRepository;
import com.cpesu.cpecraft.db.StudentRecord;
import com.cpesu.cpecraft.freeze.FreezeManager;
import com.cpesu.cpecraft.luckperms.LuckPermsService;

/**
 * Orchestrates the /verify flow: calls the (mocked, for now) student API,
 * persists a successful result, and unfreezes the player. The DB write
 * happens on whatever thread the API call completes on (fine for local
 * SQLite I/O); only the final player/world-touching step is hopped back
 * onto the main thread via {@code server.execute(...)}.
 */
public final class VerificationService {
	private final StudentApiClient apiClient;

	public VerificationService(StudentApiClient apiClient) {
		this.apiClient = apiClient;
	}

	public void verify(ServerPlayer player, String studentId) {
		UUID uuid = player.getUUID();
		String username = player.getGameProfile().name();
		MinecraftServer server = player.level().getServer();

		apiClient.verify(studentId)
				.thenCompose(result -> {
					if (result.isEmpty()) {
						return CompletableFuture.completedFuture(result);
					}
					StudentInfo info = result.get();
					Cpecraft.studentRepository().save(new StudentRecord(
							uuid, username, studentId, info.name(), info.batch(), System.currentTimeMillis()));
					return assignBatchGroup(uuid, info.batch()).thenApply(v -> result);
				})
				.whenComplete((result, throwable) -> server.execute(() -> onComplete(server, uuid, result, throwable)));
	}

	/**
	 * Matches the API-reported batch against a batch created via
	 * /createbatch and adds the player to its LuckPerms group. No FK
	 * constraint between the two - if the batch isn't recognized (not
	 * created yet, or not a valid number), this just logs and skips rather
	 * than failing verification.
	 */
	private CompletableFuture<Void> assignBatchGroup(UUID uuid, String batch) {
		int batchNumber;
		try {
			batchNumber = Integer.parseInt(batch.strip());
		} catch (NumberFormatException e) {
			Cpecraft.LOGGER.warn("Student {} has non-numeric batch '{}'; skipping group assignment", uuid, batch);
			return CompletableFuture.completedFuture(null);
		}

		Optional<BatchRepository.Batch> batchRecord = Cpecraft.batchRepository().findByNumber(batchNumber);
		if (batchRecord.isEmpty()) {
			Cpecraft.LOGGER.warn("No batch {} registered via /createbatch; skipping group assignment for {}", batchNumber, uuid);
			return CompletableFuture.completedFuture(null);
		}

		return LuckPermsService.addToGroup(uuid, batchRecord.get().luckpermsGroup());
	}

	private void onComplete(MinecraftServer server, UUID uuid, Optional<StudentInfo> result, Throwable throwable) {
		ServerPlayer player = server.getPlayerList().getPlayer(uuid);
		if (player == null) {
			return;
		}

		if (throwable != null) {
			Cpecraft.LOGGER.warn("Student verification API call failed for {}", uuid, throwable);
			player.sendSystemMessage(Component.literal(
					"Verification failed: could not reach the verification service. Try again."));
			return;
		}

		if (result.isEmpty()) {
			player.sendSystemMessage(Component.literal(
					"Verification failed: student ID not recognized. Try again with /verify <studentId>."));
			return;
		}

		FreezeManager.unfreeze(uuid);
		player.sendSystemMessage(Component.literal("Verified! Welcome, " + result.get().name() + "."));
	}
}
