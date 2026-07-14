package com.cpesu.cpecraft.home;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import com.cpesu.cpecraft.db.HomeRecord;
import com.cpesu.cpecraft.home.HomeTeleportManager.PendingTeleport;

public final class HomeEventListeners {
	private static final double CANCEL_EPSILON_SQR = 0.01;

	private HomeEventListeners() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HomeEventListeners::tickPendingTeleports);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				HomeTeleportManager.cancel(handler.getPlayer().getUUID()));

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseAmount, amount, blocked) -> {
			if (entity instanceof ServerPlayer player && HomeTeleportManager.isPending(player.getUUID())) {
				cancelPending(player, "you took damage");
			}
		});
	}

	private static void tickPendingTeleports(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PendingTeleport pendingTeleport = HomeTeleportManager.get(player.getUUID());
			if (pendingTeleport == null) {
				continue;
			}

			boolean movedOrChangedDimension = !player.level().dimension().equals(pendingTeleport.anchorDimension())
					|| player.position().distanceToSqr(pendingTeleport.anchorPos()) > CANCEL_EPSILON_SQR;
			if (movedOrChangedDimension) {
				cancelPending(player, "you moved");
				continue;
			}

			if (server.getTickCount() >= pendingTeleport.readyAtTick()) {
				HomeTeleportManager.cancel(player.getUUID());
				teleportHome(server, player, pendingTeleport.target());
			}
		}
	}

	private static void cancelPending(ServerPlayer player, String reason) {
		HomeTeleportManager.cancel(player.getUUID());
		player.sendSystemMessage(Component.literal("Teleport cancelled - " + reason + "."));
	}

	private static void teleportHome(MinecraftServer server, ServerPlayer player, HomeRecord home) {
		ServerLevel level = server.getLevel(home.dimension());
		if (level == null) {
			player.sendSystemMessage(Component.literal(
					"Home '" + home.name() + "' is in a dimension that no longer exists."));
			return;
		}

		TeleportTransition transition = new TeleportTransition(
				level, new Vec3(home.x(), home.y(), home.z()), Vec3.ZERO,
				home.yRot().floatValue(), home.xRot().floatValue(), TeleportTransition.DO_NOTHING);
		player.stopRiding();
		player.teleport(transition);
		player.sendSystemMessage(Component.literal("Teleported to home '" + home.name() + "'."));
	}
}
