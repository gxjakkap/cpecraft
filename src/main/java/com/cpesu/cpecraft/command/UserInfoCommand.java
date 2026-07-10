package com.cpesu.cpecraft.command;

import java.time.Instant;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.StudentRecord;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

/** Shows combined online/UUID info and this mod's verification record for a player. */
public final class UserInfoCommand {
	private UserInfoCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("userinfo")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.USERINFO))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(UserInfoCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		String name = target.getGameProfile().name();

		ctx.getSource().sendSuccess(() -> Component.literal("Info for " + name + ":"), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  UUID: " + target.getUUID()), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  Online: yes"), false);

		Optional<StudentRecord> record = Cpecraft.studentRepository().findByUuid(target.getUUID());
		if (record.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("  Verification: not verified"), false);
			return 1;
		}

		StudentRecord student = record.get();
		ctx.getSource().sendSuccess(() -> Component.literal("  Student ID: " + student.studentId()), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  Name: " + student.name()), false);
		ctx.getSource().sendSuccess(() -> Component.literal("  Batch: " + student.batch()), false);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"  Verified at: " + Instant.ofEpochMilli(student.verifiedAt())), false);
		return 1;
	}
}
