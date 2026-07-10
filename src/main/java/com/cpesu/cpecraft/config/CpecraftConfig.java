package com.cpesu.cpecraft.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.cpesu.cpecraft.Cpecraft;

/**
 * Loaded once at startup from {@code config/cpecraft/config.json}. Holds
 * the YB student-verification API's base URL and key - real secrets, so
 * unlike the MOTD there's no in-game command to view or change these.
 */
public record CpecraftConfig(String ybApiBaseUrl, String ybApiKey) {
	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
	private static final CpecraftConfig DEFAULT = new CpecraftConfig("https://example.com", "changeme");

	public static CpecraftConfig load(Path configDir) {
		Path dir = configDir.resolve(Cpecraft.MOD_ID);
		Path file = dir.resolve("config.json");
		try {
			Files.createDirectories(dir);
			if (Files.exists(file)) {
				CpecraftConfig config = MAPPER.readValue(file.toFile(), CpecraftConfig.class);
				if (config.equals(DEFAULT)) {
					Cpecraft.LOGGER.warn("{} still has placeholder values - set ybApiBaseUrl/ybApiKey", file);
				}
				return config;
			}
			MAPPER.writeValue(file.toFile(), DEFAULT);
			Cpecraft.LOGGER.warn("No config found; wrote a default to {} - set ybApiBaseUrl/ybApiKey and restart", file);
			return DEFAULT;
		} catch (IOException e) {
			Cpecraft.LOGGER.warn("Failed to load/create {}, using placeholder values", file, e);
			return DEFAULT;
		}
	}
}
