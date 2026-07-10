package com.cpesu.cpecraft.freeze;

import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

import com.cpesu.cpecraft.Cpecraft;

/**
 * Freezes unverified players on join and keeps them inert: movement is
 * snapped back to their anchor position every tick, block breaking/placing,
 * attacking, item use, and chat are cancelled. Inventory-slot manipulation
 * is deliberately left unblocked (no Fabric API event exists for it without
 * a mixin, and a frozen player who can't move, interact, or chat has no
 * meaningful way to abuse it) — see the project plan for that decision.
 * The /verify command itself is registered with no permission/freeze gate
 * so it always works regardless of freeze state.
 */
public final class FreezeEventListeners {
	private static final double SNAP_BACK_EPSILON_SQR = 0.01;

	private FreezeEventListeners() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			boolean verified = Cpecraft.studentRepository().findByUuid(player.getUUID()).isPresent();
			if (!verified) {
				FreezeManager.freeze(player);
				player.sendSystemMessage(Component.literal(
						"You must verify with /verify <studentId> before you can play."));
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(FreezeEventListeners::snapBackFrozenPlayers);

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				!FreezeManager.isFrozen(player.getUUID()));

		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				FreezeManager.isFrozen(player.getUUID()) ? InteractionResult.FAIL : InteractionResult.PASS);

		UseBlockCallback.EVENT.register((player, level, hand, hitResult) ->
				FreezeManager.isFrozen(player.getUUID()) ? InteractionResult.FAIL : InteractionResult.PASS);

		UseItemCallback.EVENT.register((player, level, hand) ->
				FreezeManager.isFrozen(player.getUUID()) ? InteractionResult.FAIL : InteractionResult.PASS);

		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
				!FreezeManager.isFrozen(sender.getUUID()));
	}

	private static void snapBackFrozenPlayers(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID uuid = player.getUUID();
			if (!FreezeManager.isFrozen(uuid)) {
				continue;
			}
			Vec3 anchor = FreezeManager.anchor(uuid);
			if (player.position().distanceToSqr(anchor) > SNAP_BACK_EPSILON_SQR) {
				player.teleportTo(anchor.x, anchor.y, anchor.z);
			}
		}
	}
}
