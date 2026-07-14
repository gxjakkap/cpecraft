package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.cpesu.cpecraft.Cpecraft;

public final class DelHomeCommand {
	private DelHomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("delhome")
				.then(Commands.argument("home", StringArgumentType.word())
						.executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "home")))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, String homeName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = homeName.toLowerCase();

		boolean deleted = Cpecraft.homeRepository().deleteByUuidAndName(player.getUUID(), name);
		if (!deleted) {
			ctx.getSource().sendFailure(Component.literal("You don't have a home named '" + homeName + "'."));
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.literal("Deleted home '" + name + "'."), true);
		return 1;
	}
}
