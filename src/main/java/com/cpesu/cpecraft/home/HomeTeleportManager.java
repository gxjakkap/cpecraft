package com.cpesu.cpecraft.home;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.cpesu.cpecraft.db.HomeRecord;

/**
 * Tracks in-progress /home teleports. A request only fires after the player
 * has stood still for HOME_DELAY_TICKS, so /home can't be used to instantly
 * escape combat or mobs - see {@link HomeEventListeners} for the tick check
 * that enforces this and performs the eventual teleport.
 */
public final class HomeTeleportManager {
	public static final int HOME_DELAY_TICKS = 60;

	public record PendingTeleport(HomeRecord target, Vec3 anchorPos, ResourceKey<Level> anchorDimension, long readyAtTick) {
	}

	private static final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

	private HomeTeleportManager() {
	}

	public static void start(UUID uuid, HomeRecord target, Vec3 anchorPos, ResourceKey<Level> anchorDimension, long currentTick) {
		pending.put(uuid, new PendingTeleport(target, anchorPos, anchorDimension, currentTick + HOME_DELAY_TICKS));
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
