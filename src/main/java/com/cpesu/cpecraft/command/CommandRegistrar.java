package com.cpesu.cpecraft.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class CommandRegistrar {
	private CommandRegistrar() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CreateBatchCommand.register(dispatcher);
			ListBatchesCommand.register(dispatcher);
			DeleteBatchCommand.register(dispatcher);
			VerifyCommand.register(dispatcher);
			GamemodeCommand.register(dispatcher);
			InvseeCommand.register(dispatcher);
			UserStatsCommand.register(dispatcher);
			UserInfoCommand.register(dispatcher);
			MotdCommand.register(dispatcher);
			UnlinkCommand.register(dispatcher);
			OverrideVerificationCommand.register(dispatcher);
		});
	}
}
