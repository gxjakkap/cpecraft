package com.cpesu.cpecraft.verification;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.cpesu.cpecraft.util.Titles;
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
							uuid, username, studentId, info.name(), info.nickName(), info.batch(), System.currentTimeMillis()));
					Cpecraft.LOGGER.info("Saved student record for {} ({}): studentId='{}', name='{}', batch='{}'",
							username, uuid, studentId, info.name(), info.batch());
					return assignBatchGroup(uuid, info.batch()).thenApply(v -> result);
				})
				.whenComplete((result, throwable) ->
						server.execute(() -> onComplete(server, uuid, username, studentId, result, throwable)));
	}

	/**
	 * Unverifies a player (deletes their {@code students} row, freeing their
	 * claimed student ID for reuse), revokes their batch's LuckPerms group
	 * if one was assigned, and re-freezes them if they're currently online.
	 * Works for offline players too - only the re-freeze/notify step needs
	 * them online.
	 */
	public UnlinkResult unlink(MinecraftServer server, UUID uuid, String username) {
		Optional<StudentRecord> record = Cpecraft.studentRepository().findByUuid(uuid);
		if (record.isEmpty()) {
			return UnlinkResult.NOT_VERIFIED;
		}

		String studentId = record.get().studentId();
		Cpecraft.studentRepository().deleteByUuid(uuid);
		Cpecraft.LOGGER.info("Unlinked {} ({}): studentId='{}' freed", username, uuid, studentId);

		revokeBatchGroup(uuid, record.get().batch());

		ServerPlayer online = server.getPlayerList().getPlayer(uuid);
		if (online != null) {
			FreezeManager.freeze(online);
			online.sendSystemMessage(Component.literal(
					"An admin has unlinked your verification. Run /verify <studentId> again to unfreeze."));
			Cpecraft.LOGGER.info("Re-froze {} ({}) after unlink", username, uuid);
		}

		return UnlinkResult.UNLINKED;
	}

	public enum UnlinkResult {
		UNLINKED, NOT_VERIFIED
	}

	/**
	 * Admin escape hatch: marks a player verified without calling the YB
	 * API, for students who aren't in the YB database yet (new admissions,
	 * data entry lag, etc.) but still need to get in. {@code batch} may be
	 * null/blank to skip LuckPerms group assignment entirely. Works on
	 * offline players too - only the unfreeze/notify step needs them online.
	 */
	public void overrideVerify(MinecraftServer server, UUID uuid, String username, String studentId, String batch) {
		Cpecraft.LOGGER.info("Verification override by admin for {} ({}): studentId='{}', batch='{}'",
				username, uuid, studentId, batch);

		Cpecraft.studentRepository().save(new StudentRecord(
				uuid, username, studentId, username, username, batch, System.currentTimeMillis()));
		Cpecraft.LOGGER.info("Saved overridden student record for {} ({}): studentId='{}'", username, uuid, studentId);

		CompletableFuture<Void> groupChange = (batch == null || batch.isBlank())
				? CompletableFuture.completedFuture(null)
				: assignBatchGroup(uuid, batch);

		groupChange.whenComplete((v, e) -> server.execute(() -> {
			FreezeManager.unfreeze(uuid);
			Cpecraft.LOGGER.info("Verification override succeeded for {} ({}): studentId='{}' - unfrozen",
					username, uuid, studentId);

			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			if (player != null) {
				player.sendSystemMessage(Component.literal("An admin has manually verified you. Welcome, " + username + "."));
				Titles.send(player, Component.literal("Verified!"), Component.literal("Hi " + username + "!"));
			}
		}));
	}

	private CompletableFuture<Void> assignBatchGroup(UUID uuid, String batch) {
		Optional<String> group = resolveGroupForBatch(uuid, batch);
		if (group.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		Cpecraft.LOGGER.info("Assigning LuckPerms group '{}' to {}", group.get(), uuid);
		try {
			return LuckPermsService.addToGroup(uuid, group.get())
					.whenComplete((v, e) -> {
						if (e != null) {
							Cpecraft.LOGGER.warn("Failed to assign LuckPerms group '{}' to {}", group.get(), uuid, e);
						} else {
							Cpecraft.LOGGER.info("Assigned LuckPerms group '{}' to {}", group.get(), uuid);
						}
					});
		} catch (LinkageError e) {
			// LuckPerms Fabric isn't installed - its API classes aren't even on the
			// runtime classpath, so just resolving LuckPermsService throws here.
			Cpecraft.LOGGER.warn("LuckPerms is not installed; skipping group assignment for {}", uuid);
			return CompletableFuture.completedFuture(null);
		}
	}

	private CompletableFuture<Void> revokeBatchGroup(UUID uuid, String batch) {
		Optional<String> group = resolveGroupForBatch(uuid, batch);
		if (group.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		Cpecraft.LOGGER.info("Removing LuckPerms group '{}' from {}", group.get(), uuid);
		try {
			return LuckPermsService.removeFromGroup(uuid, group.get())
					.whenComplete((v, e) -> {
						if (e != null) {
							Cpecraft.LOGGER.warn("Failed to remove LuckPerms group '{}' from {}", group.get(), uuid, e);
						} else {
							Cpecraft.LOGGER.info("Removed LuckPerms group '{}' from {}", group.get(), uuid);
						}
					});
		} catch (LinkageError e) {
			Cpecraft.LOGGER.warn("LuckPerms is not installed; skipping group removal for {}", uuid);
			return CompletableFuture.completedFuture(null);
		}
	}

	/** Parses batch as a number and looks it up via /createbatch's records; empty if either step fails. */
	private Optional<String> resolveGroupForBatch(UUID uuid, String batch) {
		int batchNumber;
		try {
			batchNumber = Integer.parseInt(batch.strip());
		} catch (NumberFormatException e) {
			Cpecraft.LOGGER.warn("Student {} has non-numeric batch '{}'; skipping group change", uuid, batch);
			return Optional.empty();
		}

		Optional<BatchRepository.Batch> batchRecord = Cpecraft.batchRepository().findByNumber(batchNumber);
		if (batchRecord.isEmpty()) {
			Cpecraft.LOGGER.warn("No batch {} registered via /createbatch; skipping group change for {}", batchNumber, uuid);
			return Optional.empty();
		}

		return Optional.of(batchRecord.get().luckpermsGroup());
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
                        String.format("Verification failed: student ID %s not recognized. Contact guntxjakka on IG or Discord", studentId)
                ));
			}
			return;
		}

		FreezeManager.unfreeze(uuid);
		Cpecraft.LOGGER.info("Verification succeeded for {} ({}): studentId='{}', name='{}' - unfrozen",
				username, uuid, studentId, result.get().name());
		if (player != null) {
			player.sendSystemMessage(Component.literal("Verified! Welcome, " + result.get().name() + "."));
            Titles.send(
                    player,
                    Component.literal("Verified!"),
                    Component.literal(String.format("Hi %s!",result.get().nickName()))
            );
		}
	}
}
