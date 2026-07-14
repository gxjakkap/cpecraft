package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.HomeRecord;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class AHomeCommand {
	private AHomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("ahome")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.ADMIN_HOME))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(ctx -> execute(ctx, null))
						.then(Commands.argument("home", StringArgumentType.word())
								.executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "home"))))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, String homeName) throws CommandSyntaxException {
		NameAndId target = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();
		HomeRecord home = HomeCommand.resolveHome(target.id(), homeName);
		if (home == null) {
			ctx.getSource().sendFailure(Component.literal(homeName == null
					? target.name() + " doesn't have any homes set."
					: target.name() + " doesn't have a home named '" + homeName + "'."));
			return 0;
		}

		ServerLevel level = ctx.getSource().getServer().getLevel(home.dimension());
		if (level == null) {
			ctx.getSource().sendFailure(Component.literal("That home is in a dimension that no longer exists."));
			return 0;
		}

		ServerPlayer admin = ctx.getSource().getPlayerOrException();
		TeleportTransition t = new TeleportTransition(
				level, new Vec3(home.x(), home.y(), home.z()), Vec3.ZERO,
				home.yRot().floatValue(), home.xRot().floatValue(), TeleportTransition.DO_NOTHING);
		admin.stopRiding();
		admin.teleport(t);

		ctx.getSource().sendSuccess(() -> Component.literal(
				"Teleported to " + target.name() + "'s home '" + home.name() + "'."), true);
		return 1;
	}
}
