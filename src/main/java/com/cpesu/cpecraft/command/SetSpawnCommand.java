package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.ConfigRecord;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class SetSpawnCommand {
	private SetSpawnCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("setspawn")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.SET_SPAWN))
				.executes(ctx -> execute(ctx, ctx.getSource().getPlayerOrException().position(), ctx.getSource().getPlayerOrException()))
				.then(Commands.argument("position", Vec3Argument.vec3())
						.executes(ctx -> execute(ctx, Vec3Argument.getVec3(ctx, "position"), ctx.getSource().getPlayerOrException()))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, Vec3 pos, ServerPlayer player) throws CommandSyntaxException {
		if (!player.level().dimension().equals(Level.OVERWORLD)) {
			ctx.getSource().sendFailure(Component.literal("World spawn can't be set in other dimension than Overworld!"));
			return 0;
		}

		Cpecraft.configRepository().save(new ConfigRecord("spawn_x", String.valueOf(pos.x)));
		Cpecraft.configRepository().save(new ConfigRecord("spawn_y", String.valueOf(pos.y)));
		Cpecraft.configRepository().save(new ConfigRecord("spawn_z", String.valueOf(pos.z)));

		ctx.getSource().sendSuccess(() -> Component.literal(String.format(
				"World spawn set to %.1f, %.1f, %.1f.", pos.x, pos.y, pos.z)), true);
		return 1;
	}
}
