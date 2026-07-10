package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
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

/** Permission-node-gated shortcut for changing a player's gamemode. */
public final class GamemodeCommand {
	private GamemodeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("gm")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.GM))
				.then(Commands.argument("mode", GameModeArgument.gameMode())
						.executes(ctx -> execute(ctx, ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("player", EntityArgument.player())
								.executes(ctx -> execute(ctx, EntityArgument.getPlayer(ctx, "player"))))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
		GameType mode = GameModeArgument.getGameMode(ctx, "mode");
		target.setGameMode(mode);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Set " + target.getGameProfile().name() + "'s gamemode to " + mode.getName()), true);
		return 1;
	}
}
