package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class ADelHomeCommand {
	private ADelHomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("adelhome")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.ADMIN_HOME))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.then(Commands.argument("home", StringArgumentType.word())
								.executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "home"))))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, String homeName) throws CommandSyntaxException {
		NameAndId target = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();
		String name = homeName.toLowerCase();

		boolean deleted = Cpecraft.homeRepository().deleteByUuidAndName(target.id(), name);
		if (!deleted) {
			ctx.getSource().sendFailure(Component.literal(target.name() + " doesn't have a home named '" + homeName + "'."));
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.literal(
				"Deleted " + target.name() + "'s home '" + name + "'."), true);
		return 1;
	}
}
