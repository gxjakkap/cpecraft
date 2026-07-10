package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class DeleteBatchCommand {
	private DeleteBatchCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("deletebatch")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.DELETE_BATCH))
				.then(Commands.argument("batchNumber", IntegerArgumentType.integer())
						.executes(DeleteBatchCommand::execute)));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) {
		int batchNumber = IntegerArgumentType.getInteger(ctx, "batchNumber");

		if (Cpecraft.batchRepository().delete(batchNumber)) {
			ctx.getSource().sendSuccess(() -> Component.literal("Deleted batch " + batchNumber + "."), true);
			return 1;
		}

		ctx.getSource().sendFailure(Component.literal("No batch " + batchNumber + " exists."));
		return 0;
	}
}
