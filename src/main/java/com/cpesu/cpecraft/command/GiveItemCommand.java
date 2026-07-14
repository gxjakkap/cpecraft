package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class GiveItemCommand {
	private GiveItemCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
		dispatcher.register(Commands.literal("gi")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.GIVE_ITEM))
				.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.argument("item", ItemArgument.item(registryAccess))
								.executes(ctx -> execute(ctx, 1))
								.then(Commands.argument("count", IntegerArgumentType.integer(1, 6400))
										.executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "count")))))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		ItemInput input = ItemArgument.getItem(ctx, "item");
		ItemStack stack = input.createItemStack(count);

		if (!target.getInventory().add(stack)) {
			target.drop(stack, false);
		}

		ctx.getSource().sendSuccess(() -> Component.literal(
				"Gave " + count + " x " + stack.getHoverName().getString() + " to " + target.getGameProfile().name()), true);
		return 1;
	}
}
