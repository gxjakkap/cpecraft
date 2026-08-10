package com.cpesu.cpecraft.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.players.NameAndId;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.HomeRecord;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class AListHomeCommand {
	private AListHomeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("alisthome")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.ADMIN_HOME))
				.then(Commands.argument("player", GameProfileArgument.gameProfile())
						.executes(AListHomeCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		NameAndId target = GameProfileArgument.getGameProfiles(ctx, "player").iterator().next();
		List<HomeRecord> homes = Cpecraft.homeRepository().findByUuid(target.id());
		if (homes.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal(target.name() + " doesn't have any homes set."), false);
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.literal(target.name() + "'s homes (" + homes.size() + "):"), false);
		for (HomeRecord home : homes) {
			ctx.getSource().sendSuccess(() -> {
				MutableComponent line = Component.literal(String.format(
						" - %s%s: ", home.name(), home.isDefault() ? " [default]" : ""));
				MutableComponent coords = Component.literal(String.format("[%.1f, %.1f, %.1f]", home.x(), home.y(), home.z()))
						.withStyle(Style.EMPTY.withClickEvent(
								new ClickEvent.RunCommand("/ahome " + target.name() + " " + home.name())));
				return line.append(coords);
			}, false);
		}
		return homes.size();
	}
}
