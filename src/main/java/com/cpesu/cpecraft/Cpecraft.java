package com.cpesu.cpecraft;

import java.net.URI;

import com.cpesu.cpecraft.db.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cpesu.cpecraft.command.CommandRegistrar;
import com.cpesu.cpecraft.config.CpecraftConfig;
import com.cpesu.cpecraft.freeze.FreezeEventListeners;
import com.cpesu.cpecraft.home.HomeEventListeners;
import com.cpesu.cpecraft.motd.MotdService;
import com.cpesu.cpecraft.verification.HttpStudentApiClient;
import com.cpesu.cpecraft.verification.VerificationService;
import com.cpesu.cpecraft.welcome.WelcomeEventListeners;

public class Cpecraft implements ModInitializer {
	public static final String MOD_ID = "cpecraft";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static Database database;
	private static StudentRepository studentRepository;
	private static BatchRepository batchRepository;
    private static ConfigRepository configRepository;
    private static HomeRepository homeRepository;
	private static VerificationService verificationService;
	private static MotdService motdService;

	@Override
	public void onInitialize() {
		CpecraftConfig config = CpecraftConfig.load(FabricLoader.getInstance().getConfigDir());

		database = new Database(FabricLoader.getInstance().getConfigDir());
		studentRepository = new StudentRepository(database);
        configRepository = new ConfigRepository(database);
		batchRepository = new BatchRepository(database);
        homeRepository = new HomeRepository(database);
		verificationService = new VerificationService(
				new HttpStudentApiClient(URI.create(config.ybApiBaseUrl()), config.ybApiKey()));
		motdService = new MotdService(FabricLoader.getInstance().getConfigDir());

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> database.close());

		CommandRegistrar.register();
		FreezeEventListeners.register();
		HomeEventListeners.register();
		WelcomeEventListeners.register();

		LOGGER.info("CPECraft loaded");
	}

	public static StudentRepository studentRepository() {
		return studentRepository;
	}

	public static BatchRepository batchRepository() {
		return batchRepository;
	}

    public static ConfigRepository configRepository() { return configRepository; }

	public static VerificationService verificationService() {
		return verificationService;
	}

	public static MotdService motdService() {
		return motdService;
	}

    public static HomeRepository homeRepository() { return homeRepository; }
}
