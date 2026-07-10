package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class InvseeCommand {
	private InvseeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("invsee")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.INVSEE))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(InvseeCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer admin = ctx.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

		admin.openMenu(new SimpleMenuProvider(
				(syncId, viewerInventory, viewerPlayer) ->
						new ChestMenu(MenuType.GENERIC_9x4, syncId, viewerInventory, target.getInventory(), 4),
				Component.literal(target.getGameProfile().name() + "'s Inventory")));
		return 1;
	}
}
