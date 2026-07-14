package com.cpesu.cpecraft.command;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

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
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(UserInfoCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(ctx, "player");
		for (NameAndId target : targets) {
			String name = target.name();
			boolean online = ctx.getSource().getServer().getPlayerList().getPlayer(target.id()) != null;

			ctx.getSource().sendSuccess(() -> Component.literal("Info for " + name + ":"), false);
			ctx.getSource().sendSuccess(() -> Component.literal("  UUID: " + target.id()), false);
			ctx.getSource().sendSuccess(() -> Component.literal("  Online: " + (online ? "yes" : "no")), false);

			Optional<StudentRecord> record = Cpecraft.studentRepository().findByUuid(target.id());
			if (record.isEmpty()) {
				ctx.getSource().sendSuccess(() -> Component.literal("  Verification: not verified"), false);
				continue;
			}

			StudentRecord student = record.get();
			ctx.getSource().sendSuccess(() -> Component.literal("  Student ID: " + student.studentId()), false);
			ctx.getSource().sendSuccess(() -> Component.literal("  Name: " + student.name()), false);
			ctx.getSource().sendSuccess(() -> Component.literal("  Batch: " + student.batch()), false);
			ctx.getSource().sendSuccess(() -> Component.literal(
					"  Verified at: " + Instant.ofEpochMilli(student.verifiedAt())), false);
		}
		return targets.size();
	}
}
