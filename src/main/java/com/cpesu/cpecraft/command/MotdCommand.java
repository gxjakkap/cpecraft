package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.motd.Markdown;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

/** Bare /motd (view) is open to everyone; /motd set is permission-gated. */
public final class MotdCommand {
	private MotdCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("motd")
				.executes(MotdCommand::view)
				.then(Commands.literal("set")
						.requires(CpecraftPermissions.requires(CpecraftPermissions.MOTD))
						.then(Commands.argument("message", StringArgumentType.greedyString())
								.executes(MotdCommand::set))));
	}

	private static int view(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendSuccess(() -> Component.literal("MOTD: ")
				.append(Markdown.toComponent(Cpecraft.motdService().get())), false);
		return 1;
	}

	private static int set(CommandContext<CommandSourceStack> ctx) {
		String message = StringArgumentType.getString(ctx, "message");
		Cpecraft.motdService().set(message);
		ctx.getSource().sendSuccess(() -> Component.literal("MOTD updated."), true);
		return 1;
	}
}
