package com.cpesu.cpecraft.welcome;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.motd.Markdown;

/**
 * On join, shows a title: unverified players are told to /verify, verified
 * players get a welcome-back title. Independently of that, the admin-set
 * MOTD is sent to the joining player's chat. Registered separately from
 * {@code FreezeEventListeners} - this is presentation, not freeze logic.
 */
public final class WelcomeEventListeners {
	private WelcomeEventListeners() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			boolean verified = Cpecraft.studentRepository().findByUuid(player.getUUID()).isPresent();

			if (verified) {
				sendTitle(player,
						Component.literal("Connected!"),
						Component.literal(String.format("Welcome back, %s.", player.getGameProfile().name())));
			} else {
				sendTitle(player,
						Component.literal("Verification Required"),
						Component.literal("Run /verify <studentId> to play"));
			}

			player.sendSystemMessage(Markdown.toComponent(Cpecraft.motdService().get()));
		});
	}

	private static void sendTitle(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitleTextPacket(title));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
	}
}
