package com.cpesu.cpecraft.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.types.InheritanceNode;

import com.cpesu.cpecraft.Cpecraft;

/**
 * Thin wrapper around the LuckPerms API for adding/removing a player from
 * their batch's group. The LuckPerms Fabric plugin is a separate,
 * admin-installed mod (only {@code compileOnly} at build time) - if it
 * isn't installed, LuckPerms types (Node, User, ...) aren't even on the
 * runtime classpath. {@link LuckPermsProvider#get()} throws
 * IllegalStateException in that case, but merely *constructing* a lambda
 * that references those types - even just as a method argument, before
 * the callee's try/catch runs - can itself throw NoClassDefFoundError.
 * So the lambda literals below must stay inside their own try block
 * (not built by the caller and passed in), and the catch must cover
 * LinkageError too, not just IllegalStateException.
 */
public final class LuckPermsService {
	private LuckPermsService() {
	}

	public static CompletableFuture<Void> addToGroup(UUID uuid, String groupName) {
		try {
			LuckPerms luckPerms = LuckPermsProvider.get();
			return luckPerms.getUserManager().modifyUser(uuid,
					user -> user.data().add(InheritanceNode.builder(groupName).build()));
		} catch (IllegalStateException | LinkageError e) {
			Cpecraft.LOGGER.warn("LuckPerms is not installed; skipping group change for {} -> {}", uuid, groupName);
			return CompletableFuture.completedFuture(null);
		}
	}

	public static CompletableFuture<Void> removeFromGroup(UUID uuid, String groupName) {
		try {
			LuckPerms luckPerms = LuckPermsProvider.get();
			return luckPerms.getUserManager().modifyUser(uuid,
					user -> user.data().remove(InheritanceNode.builder(groupName).build()));
		} catch (IllegalStateException | LinkageError e) {
			Cpecraft.LOGGER.warn("LuckPerms is not installed; skipping group change for {} -> {}", uuid, groupName);
			return CompletableFuture.completedFuture(null);
		}
	}
}
