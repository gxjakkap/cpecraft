package com.cpesu.cpecraft.command;

import java.util.Collection;

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

/**
 * Manually marks a player verified without calling the YB API - for
 * students who aren't in the YB database yet (new admissions, data entry
 * lag, etc.) but still need to get in. Uses GameProfileArgument like
 * /unlink so it works on offline players too. Batch is optional; if
 * omitted no LuckPerms group is assigned.
 */
public final class OverrideVerificationCommand {
	private OverrideVerificationCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("overrideverification")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.OVERRIDE_VERIFICATION))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.then(Commands.argument("studentId", StringArgumentType.word())
								.executes(ctx -> execute(ctx, null))
								.then(Commands.argument("batch", StringArgumentType.word())
										.executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "batch")))))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, String batch) throws CommandSyntaxException {
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(ctx, "player");
		String studentId = StringArgumentType.getString(ctx, "studentId");

		int count = 0;
		for (NameAndId target : targets) {
			Cpecraft.verificationService().overrideVerify(
					ctx.getSource().getServer(), target.id(), target.name(), studentId, batch);
			ctx.getSource().sendSuccess(() -> Component.literal(
					target.name() + " has been manually verified"
							+ (batch != null ? " (batch " + batch + ")" : "") + "."), true);
			count++;
		}
		return count;
	}
}
