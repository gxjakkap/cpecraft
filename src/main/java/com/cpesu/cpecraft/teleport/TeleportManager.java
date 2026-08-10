package com.cpesu.cpecraft.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

public final class TeleportManager {
	public static final int DELAY_TICKS = 60;

	/** Where a pending teleport lands, and what to tell the player once it fires. */
	public record Destination(ResourceKey<@NotNull Level> dimension, Vec3 pos, float yRot, float xRot, String successMessage) {
	}

	public record PendingTeleport(Destination target, Vec3 anchorPos, ResourceKey<@NotNull Level> anchorDimension, long readyAtTick) {
	}

	private static final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

	private TeleportManager() {
	}

	public static void start(UUID uuid, Destination target, Vec3 anchorPos, ResourceKey<@NotNull Level> anchorDimension, long currentTick) {
		pending.put(uuid, new PendingTeleport(target, anchorPos, anchorDimension, currentTick + DELAY_TICKS));
	}

	public static boolean isPending(UUID uuid) {
		return pending.containsKey(uuid);
	}

	public static PendingTeleport get(UUID uuid) {
		return pending.get(uuid);
	}

	public static void cancel(UUID uuid) {
		pending.remove(uuid);
	}
}
