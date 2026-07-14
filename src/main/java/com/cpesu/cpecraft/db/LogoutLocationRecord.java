package com.cpesu.cpecraft.db;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LogoutLocationRecord(UUID uuid, double x, double y, double z, double xRot, double yRot,
                                    ResourceKey<@NotNull Level> dimension, long loggedOutAt) {
}
