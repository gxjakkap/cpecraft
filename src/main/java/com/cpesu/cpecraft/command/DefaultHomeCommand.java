package com.cpesu.cpecraft.command;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.HomeRecord;

public final class DefaultHomeCommand {
	private DefaultHomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("defaulthome")
				.then(Commands.argument("home", StringArgumentType.word())
						.executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "home")))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, String homeName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = homeName.toLowerCase();

		Optional<HomeRecord> target = Cpecraft.homeRepository().findByPlayerUuidAndHomeName(player.getUUID(), name);
		if (target.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("You don't have a home named '" + homeName + "'."));
			return 0;
		}
		if (target.get().isDefault()) {
			ctx.getSource().sendSuccess(() -> Component.literal("'" + name + "' is already your default home."), false);
			return 0;
		}

		Cpecraft.homeRepository().setDefault(player.getUUID(), name);
		ctx.getSource().sendSuccess(() -> Component.literal("'" + name + "' is now your default home."), true);
		return 1;
	}
}
