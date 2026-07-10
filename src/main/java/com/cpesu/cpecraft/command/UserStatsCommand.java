package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;

import com.cpesu.cpecraft.permission.CpecraftPermissions;

/** Displays a handful of vanilla statistics for an online player. */
public final class UserStatsCommand {
	private UserStatsCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("userstats")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.USERSTATS))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(UserStatsCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		ServerStatsCounter stats = target.getStats();

		Stat<net.minecraft.resources.Identifier> playTime = Stats.CUSTOM.get(Stats.PLAY_TIME);
		Stat<net.minecraft.resources.Identifier> deaths = Stats.CUSTOM.get(Stats.DEATHS);
		Stat<net.minecraft.resources.Identifier> mobKills = Stats.CUSTOM.get(Stats.MOB_KILLS);
		Stat<net.minecraft.resources.Identifier> playerKills = Stats.CUSTOM.get(Stats.PLAYER_KILLS);

		String name = target.getGameProfile().name();
		ctx.getSource().sendSuccess(() -> Component.literal("Stats for " + name + ":"), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  Play time: " + playTime.format(stats.getValue(playTime))), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  Deaths: " + stats.getValue(deaths)), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  Mob kills: " + stats.getValue(mobKills)), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  Player kills: " + stats.getValue(playerKills)), false);
		return 1;
	}
}
