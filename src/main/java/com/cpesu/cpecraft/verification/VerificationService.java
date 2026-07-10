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

public final class VerificationService {
	private final StudentApiClient apiClient;

	public VerificationService(StudentApiClient apiClient) {
		this.apiClient = apiClient;
	}

	public void verify(ServerPlayer player, String studentId) {
		UUID uuid = player.getUUID();
		String username = player.getGameProfile().name();
		MinecraftServer server = player.level().getServer();

		Cpecraft.LOGGER.info("Verification started for {} ({}), studentId='{}'", username, uuid, studentId);

		apiClient.verify(studentId)
				.thenCompose(result -> {
					if (result.isEmpty()) {
						return CompletableFuture.completedFuture(result);
					}
					StudentInfo info = result.get();
					Cpecraft.studentRepository().save(new StudentRecord(
							uuid, username, studentId, info.name(), info.batch(), System.currentTimeMillis()));
					Cpecraft.LOGGER.info("Saved student record for {} ({}): studentId='{}', name='{}', batch='{}'",
							username, uuid, studentId, info.name(), info.batch());
					return assignBatchGroup(uuid, info.batch()).thenApply(v -> result);
				})
				.whenComplete((result, throwable) ->
						server.execute(() -> onComplete(server, uuid, username, studentId, result, throwable)));
	}

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

		String group = batchRecord.get().luckpermsGroup();
		Cpecraft.LOGGER.info("Assigning LuckPerms group '{}' to {} (batch {})", group, uuid, batchNumber);
		return LuckPermsService.addToGroup(uuid, group)
				.whenComplete((v, e) -> {
					if (e != null) {
						Cpecraft.LOGGER.warn("Failed to assign LuckPerms group '{}' to {}", group, uuid, e);
					} else {
						Cpecraft.LOGGER.info("Assigned LuckPerms group '{}' to {}", group, uuid);
					}
				});
	}

	private void onComplete(MinecraftServer server, UUID uuid, String username, String studentId,
			Optional<StudentInfo> result, Throwable throwable) {
		ServerPlayer player = server.getPlayerList().getPlayer(uuid);
		if (player == null) {
			Cpecraft.LOGGER.info("{} ({}) disconnected before verification of studentId='{}' completed",
					username, uuid, studentId);
		}

		if (throwable != null) {
			Cpecraft.LOGGER.warn("Verification errored for {} ({}), studentId='{}'", username, uuid, studentId, throwable);
			if (player != null) {
				player.sendSystemMessage(Component.literal(
						"Verification failed: could not reach the verification service. Try again."));
			}
			return;
		}

		if (result.isEmpty()) {
			Cpecraft.LOGGER.info("Verification rejected for {} ({}): studentId='{}' not recognized",
					username, uuid, studentId);
			if (player != null) {
				player.sendSystemMessage(Component.literal(
						"Verification failed: student ID not recognized. Try again with /verify <studentId>."));
			}
			return;
		}

		FreezeManager.unfreeze(uuid);
		Cpecraft.LOGGER.info("Verification succeeded for {} ({}): studentId='{}', name='{}' - unfrozen",
				username, uuid, studentId, result.get().name());
		if (player != null) {
			player.sendSystemMessage(Component.literal("Verified! Welcome, " + result.get().name() + "."));
		}
	}
}
