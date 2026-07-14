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
            GamemodeSurvivalCommand.register(dispatcher);
            GamemodeCreativeCommand.register(dispatcher);
            GamemodeAdventureCommand.register(dispatcher);
            GamemodeSpectatorCommand.register(dispatcher);
			InvseeCommand.register(dispatcher);
			UserStatsCommand.register(dispatcher);
			UserInfoCommand.register(dispatcher);
			MotdCommand.register(dispatcher);
			UnlinkCommand.register(dispatcher);
			OverrideVerificationCommand.register(dispatcher);
            WhoisCommand.register(dispatcher);
            SpawnCommand.register(dispatcher);
            SetSpawnCommand.register(dispatcher);
            PlayerListCommand.register(dispatcher);
            SetHomeCommand.register(dispatcher);
            HomeCommand.register(dispatcher);
            ListHomeCommand.register(dispatcher);
            DefaultHomeCommand.register(dispatcher);
            DelHomeCommand.register(dispatcher);
            TpHereCommand.register(dispatcher);
            TpOffCommand.register(dispatcher);
		});
	}
}
