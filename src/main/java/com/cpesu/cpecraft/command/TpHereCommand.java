package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class TpHereCommand {
	private TpHereCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralCommandNode<CommandSourceStack> tphere = dispatcher.register(Commands.literal("tphere")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.TPHERE))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(TpHereCommand::execute)));
		dispatcher.register(Commands.literal("s")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.TPHERE))
				.redirect(tphere));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer source = ctx.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		TeleportTransition t = new TeleportTransition(
				source.level(), source.position(), Vec3.ZERO,
				source.getYRot(), source.getXRot(), TeleportTransition.DO_NOTHING);

		target.stopRiding();
		target.teleport(t);

		ctx.getSource().sendSuccess(() -> Component.literal(
				"Teleported " + target.getGameProfile().name() + " to you."), true);
		return 1;
	}
}
