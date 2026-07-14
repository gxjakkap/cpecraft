package com.cpesu.cpecraft.db;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record HomeRecord(UUID uuid, String name, Double x, Double y, Double z, Double xRot, Double yRot, ResourceKey<@NotNull Level> dimension,
                         long createdAt, boolean isDefault) {
}
