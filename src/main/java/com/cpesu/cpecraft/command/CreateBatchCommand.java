package com.cpesu.cpecraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.BatchRepository;
import com.cpesu.cpecraft.permission.CpecraftPermissions;

public final class CreateBatchCommand {
	private CreateBatchCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("createbatch")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.CREATE_BATCH))
				.then(Commands.argument("batchNumber", IntegerArgumentType.integer())
						.then(Commands.argument("luckpermsGroup", StringArgumentType.word())
								.then(Commands.argument("displayName", StringArgumentType.greedyString())
										.executes(CreateBatchCommand::execute)))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) {
		int batchNumber = IntegerArgumentType.getInteger(ctx, "batchNumber");
		String group = StringArgumentType.getString(ctx, "luckpermsGroup");
		String displayName = StringArgumentType.getString(ctx, "displayName");

		boolean inserted = Cpecraft.batchRepository().insert(new BatchRepository.Batch(batchNumber, group, displayName));
		if (inserted) {
			ctx.getSource().sendSuccess(() -> Component.literal(
					"Created batch " + batchNumber + " -> LuckPerms group '" + group + "' (" + displayName + ")"), true);
			return 1;
		}

		ctx.getSource().sendFailure(Component.literal("Batch " + batchNumber + " already exists."));
		return 0;
	}
}
