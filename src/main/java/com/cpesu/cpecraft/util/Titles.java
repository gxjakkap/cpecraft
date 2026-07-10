package com.cpesu.cpecraft.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;

/** Sends the on-screen title/subtitle banner to a player. */
public final class Titles {
	private Titles() {
	}

	public static void send(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitleTextPacket(title));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
	}
}
