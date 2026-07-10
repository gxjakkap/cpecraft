package com.cpesu.cpecraft.freeze;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Tracks which online players are frozen (unverified), and the position
 * they were frozen at so {@link FreezeEventListeners} can snap them back
 * if they drift.
 */
public final class FreezeManager {
	private static final Map<UUID, Vec3> frozenAnchors = new ConcurrentHashMap<>();

	private FreezeManager() {
	}

	public static void freeze(ServerPlayer player) {
		frozenAnchors.put(player.getUUID(), player.position());
	}

	public static void unfreeze(UUID uuid) {
		frozenAnchors.remove(uuid);
	}

	public static boolean isFrozen(UUID uuid) {
		return frozenAnchors.containsKey(uuid);
	}

	public static Vec3 anchor(UUID uuid) {
		return frozenAnchors.get(uuid);
	}
}
