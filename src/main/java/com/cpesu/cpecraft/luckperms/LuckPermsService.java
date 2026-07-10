package com.cpesu.cpecraft.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.types.InheritanceNode;

import com.cpesu.cpecraft.Cpecraft;

/**
 * Thin wrapper around the LuckPerms API for adding a verified player to
 * their batch's group. The LuckPerms Fabric plugin is a separate,
 * admin-installed mod (only {@code compileOnly} at build time) - if it
 * isn't installed, {@link LuckPermsProvider#get()} throws and this no-ops
 * with a logged warning rather than crashing.
 */
public final class LuckPermsService {
	private LuckPermsService() {
	}

	public static CompletableFuture<Void> addToGroup(UUID uuid, String groupName) {
		LuckPerms luckPerms;
		try {
			luckPerms = LuckPermsProvider.get();
		} catch (IllegalStateException e) {
			Cpecraft.LOGGER.warn("LuckPerms is not installed; skipping group assignment for {} -> {}", uuid, groupName);
			return CompletableFuture.completedFuture(null);
		}

		return luckPerms.getUserManager().modifyUser(uuid, user -> user.data().add(InheritanceNode.builder(groupName).build()));
	}
}
