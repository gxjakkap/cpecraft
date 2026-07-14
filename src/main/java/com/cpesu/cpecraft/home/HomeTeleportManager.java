package com.cpesu.cpecraft.home;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.cpesu.cpecraft.db.HomeRecord;
import org.jetbrains.annotations.NotNull;

public final class HomeTeleportManager {
	public static final int HOME_DELAY_TICKS = 60;

	public record PendingTeleport(HomeRecord target, Vec3 anchorPos, ResourceKey<@NotNull Level> anchorDimension, long readyAtTick) {
	}

	private static final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

	private HomeTeleportManager() {
	}

	public static void start(UUID uuid, HomeRecord target, Vec3 anchorPos, ResourceKey<@NotNull Level> anchorDimension, long currentTick) {
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
