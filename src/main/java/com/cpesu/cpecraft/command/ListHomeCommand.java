package com.cpesu.cpecraft.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.HomeRecord;

public final class ListHomeCommand {
	private ListHomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralCommandNode<CommandSourceStack> listHome = dispatcher.register(Commands.literal("listhome")
				.executes(ListHomeCommand::execute));
		dispatcher.register(Commands.literal("lh").redirect(listHome));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		List<HomeRecord> homes = Cpecraft.homeRepository().findByUuid(player.getUUID());
		if (homes.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("You don't have any homes set. Use /sethome first."), false);
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.literal("Your homes (" + homes.size() + "):"), false);
		for (HomeRecord home : homes) {
			ctx.getSource().sendSuccess(() -> Component.literal(String.format(
					"  %s%s - %.1f, %.1f, %.1f",
					home.name(), home.isDefault() ? " [default]" : "", home.x(), home.y(), home.z())), false);
		}
		return homes.size();
	}
}
