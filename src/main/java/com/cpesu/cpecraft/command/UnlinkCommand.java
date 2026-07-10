package com.cpesu.cpecraft.command;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.permission.CpecraftPermissions;
import com.cpesu.cpecraft.verification.VerificationService;

/**
 * Unverifies a player and frees their claimed student ID for reuse. Uses
 * GameProfileArgument (not EntityArgument.player()) so it works on offline
 * players too - unlinking is exactly the kind of thing an admin needs to do
 * after someone's already left.
 */
public final class UnlinkCommand {
	private UnlinkCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("unlink")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.UNLINK))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(UnlinkCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(ctx, "player");
		int unlinked = 0;
		for (NameAndId target : targets) {
			VerificationService.UnlinkResult result = Cpecraft.verificationService()
					.unlink(ctx.getSource().getServer(), target.id(), target.name());
			if (result == VerificationService.UnlinkResult.UNLINKED) {
				unlinked++;
				ctx.getSource().sendSuccess(() -> Component.literal(
						target.name() + " has been unlinked; their student ID is free to be claimed again."), true);
			} else {
				ctx.getSource().sendFailure(Component.literal(target.name() + " is not verified."));
			}
		}
		return unlinked;
	}
}
