package com.cpesu.cpecraft.motd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cpesu.cpecraft.Cpecraft;

/**
 * Persists the server MOTD as plain markdown-ish text (see {@link Markdown})
 * in the config dir, cached in memory after load.
 */
public final class MotdService {
	private static final String DEFAULT_MOTD = "**Welcome** to the *CPE Craft* server!";

	private final Path file;
	private String current;

	public MotdService(Path configDir) {
		Path dir = configDir.resolve(Cpecraft.MOD_ID);
		this.file = dir.resolve("motd.txt");
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			Cpecraft.LOGGER.warn("Failed to create config directory for MOTD storage", e);
		}
		this.current = load();
	}

	private String load() {
		try {
			if (Files.exists(file)) {
				return Files.readString(file);
			}
		} catch (IOException e) {
			Cpecraft.LOGGER.warn("Failed to read MOTD file, using default", e);
		}
		return DEFAULT_MOTD;
	}

	public String get() {
		return current;
	}

	public void set(String motd) {
		current = motd;
		try {
			Files.writeString(file, motd);
		} catch (IOException e) {
			Cpecraft.LOGGER.warn("Failed to persist MOTD", e);
		}
	}
}
