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

/**
 * Always registered with no permission/freeze gate — this is the one
 * command a frozen, unverified player must always be able to run.
 */
public final class VerifyCommand {
	private VerifyCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("verify")
				.then(Commands.argument("studentId", StringArgumentType.word())
						.executes(VerifyCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String studentId = StringArgumentType.getString(ctx, "studentId");

		Cpecraft.LOGGER.info("/verify run by {} ({}), input studentId='{}'",
				player.getGameProfile().name(), player.getUUID(), studentId);

		ctx.getSource().sendSuccess(() -> Component.literal("Checking student ID..."), false);
		Cpecraft.verificationService().verify(player, studentId);
		return 1;
	}
}
