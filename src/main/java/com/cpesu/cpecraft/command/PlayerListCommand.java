package com.cpesu.cpecraft.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.StudentRecord;
import com.cpesu.cpecraft.permission.CpecraftPermissions;
import org.jetbrains.annotations.NotNull;

public final class PlayerListCommand {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final DateTimeFormatter JOIN_DATE_FORMAT =
			DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter LAST_JOIN_FORMAT =
			DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault());

	private PlayerListCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("playerlist")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.PLAYERLIST))
				.executes(PlayerListCommand::execute));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		List<StudentRecord> students = Cpecraft.studentRepository().findAll();
		if (students.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No verified players yet."), false);
			return 0;
		}

		Map<String, List<StudentRecord>> byBatch = new TreeMap<>();
		for (StudentRecord student : students) {
			String batch = (student.batch() == null || student.batch().isBlank()) ? "Unassigned" : student.batch();
			byBatch.computeIfAbsent(batch, b -> new ArrayList<>()).add(student);
		}

		Stat<@NotNull Identifier> playTimeStat = Stats.CUSTOM.get(Stats.PLAY_TIME);
		for (Map.Entry<String, List<StudentRecord>> entry : byBatch.entrySet()) {
			ctx.getSource().sendSuccess(() -> Component.literal("CPE " + entry.getKey() + ":"), false);
			for (StudentRecord student : entry.getValue()) {
				ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(student.uuid());
				String joinDate = JOIN_DATE_FORMAT.format(Instant.ofEpochMilli(student.verifiedAt()));
				String lastJoin = student.lastJoinAt() == null ? "never" : formatLastJoin(student.lastJoinAt());
				String playtime = playTimeStat.format(onlinePlayer != null
						? onlinePlayer.getStats().getValue(playTimeStat)
						: readOfflinePlayTime(server, student.uuid()));

				MutableComponent name = Component.literal("  " + student.username());
				if (onlinePlayer != null) {
					name = name.withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
				}
				MutableComponent line = name.append(Component.literal(" (" + student.nickName() + ")"
						+ ":\n    joined " + joinDate + "\n    last seen " + lastJoin + "\n    playtime " + playtime));
				ctx.getSource().sendSuccess(() -> line, false);
			}
		}
		return students.size();
	}

	private static String formatLastJoin(long epochMilli) {
		Instant instant = Instant.ofEpochMilli(epochMilli);
		return LAST_JOIN_FORMAT.format(instant) + " (" + relativeTime(instant) + ")";
	}

	private static String relativeTime(Instant instant) {
		long seconds = Duration.between(instant, Instant.now()).getSeconds();
		if (seconds < 60) {
			return "just now";
		}
		long minutes = seconds / 60;
		if (minutes < 60) {
			return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
		}
		long hours = minutes / 60;
		if (hours < 24) {
			return hours + (hours == 1 ? " hour ago" : " hours ago");
		}
		long days = hours / 24;
		if (days < 30) {
			return days + (days == 1 ? " day ago" : " days ago");
		}
		long months = days / 30;
		if (months < 12) {
			return months + (months == 1 ? " month ago" : " months ago");
		}
		long years = months / 12;
		return years + (years == 1 ? " year ago" : " years ago");
	}

	private static int readOfflinePlayTime(MinecraftServer server, UUID uuid) {
		Path statsFile = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(uuid + ".json");
		if (!Files.exists(statsFile)) {
			return 0;
		}
		try {
			JsonNode root = MAPPER.readTree(statsFile.toFile());
			return root.path("stats").path("minecraft:custom").path("minecraft:play_time").asInt(0);
		} catch (IOException e) {
			Cpecraft.LOGGER.warn("Failed to read stats file for {}", uuid, e);
			return 0;
		}
	}
}
