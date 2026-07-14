package com.cpesu.cpecraft.command;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import com.mojang.brigadier.tree.LiteralCommandNode;
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
import com.cpesu.cpecraft.db.LogoutLocationRecord;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class TpOffCommand {
	private TpOffCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> tpOff = dispatcher.register(Commands.literal("tpoff")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.TPOFF))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(TpOffCommand::execute)));
        dispatcher.register(Commands.literal("tpo")
                .requires(CpecraftPermissions.requires(CpecraftPermissions.TPOFF))
                .redirect(tpOff));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		NameAndId target = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();
		Optional<LogoutLocationRecord> location = Cpecraft.logoutLocationRepository().findByUuid(target.id());
		if (location.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal(target.name() + " has no recorded logout location."));
			return 0;
		}

		LogoutLocationRecord loc = location.get();
		ServerLevel level = ctx.getSource().getServer().getLevel(loc.dimension());
		if (level == null) {
			ctx.getSource().sendFailure(Component.literal(
					target.name() + "'s logout location is in a dimension that no longer exists."));
			return 0;
		}

		ServerPlayer source = ctx.getSource().getPlayerOrException();
		TeleportTransition t = new TeleportTransition(
				level, new Vec3(loc.x(), loc.y(), loc.z()), Vec3.ZERO,
				(float) loc.yRot(), (float) loc.xRot(), TeleportTransition.DO_NOTHING);

		source.stopRiding();
		source.teleport(t);

		ctx.getSource().sendSuccess(() -> Component.literal(
				"Teleported to " + target.name() + "'s logout location."), true);
		return 1;
	}
}
