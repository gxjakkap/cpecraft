package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import com.cpesu.cpecraft.permission.CpecraftPermissions;


public final class GamemodeCommand {
	private GamemodeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("gm")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.GM))
				.then(Commands.argument("mode", GameModeArgument.gameMode())
						.executes(ctx -> execute(ctx, GameModeArgument.getGameMode(ctx, "mode"), ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(ctx -> execute(ctx, GameModeArgument.getGameMode(ctx, "mode"), EntityArgument.getPlayer(ctx, "player")))))
				.then(Commands.argument("modeId", IntegerArgumentType.integer(0, 3))
						.executes(ctx -> execute(ctx, GameType.byId(IntegerArgumentType.getInteger(ctx, "modeId")), ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(ctx -> execute(ctx, GameType.byId(IntegerArgumentType.getInteger(ctx, "modeId")), EntityArgument.getPlayer(ctx, "player"))))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, GameType mode, ServerPlayer target) throws CommandSyntaxException {
		target.setGameMode(mode);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Set " + target.getGameProfile().name() + "'s gamemode to " + mode.getName()), true);
		return 1;
	}
}
