package com.cpesu.cpecraft.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.BatchRepository;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class ListBatchesCommand {
	private ListBatchesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("listbatches")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.LIST_BATCHES))
				.executes(ListBatchesCommand::execute));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) {
		List<BatchRepository.Batch> batches = Cpecraft.batchRepository().findAll();
		if (batches.isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("No batches created yet."), false);
			return 0;
		}

		ctx.getSource().sendSuccess(() -> Component.literal("Batches (" + batches.size() + "):"), false);
		for (BatchRepository.Batch batch : batches) {
			ctx.getSource().sendSuccess(() -> Component.literal(
					"  " + batch.batchNumber() + ": " + batch.displayName() + " -> " + batch.luckpermsGroup()), false);
		}
		return batches.size();
	}
}
