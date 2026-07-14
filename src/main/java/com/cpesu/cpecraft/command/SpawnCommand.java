package com.cpesu.cpecraft.command;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.ConfigRecord;
import com.cpesu.cpecraft.freeze.FreezeManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class SpawnCommand {
    private SpawnCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
                .executes(ctx -> execute(ctx, ctx.getSource().getPlayerOrException())));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        if (FreezeManager.isFrozen(target.getUUID())){
            ctx.getSource().sendSuccess(() -> Component.literal("You need to verify yourself first before using this command!"), false);
            return 0;
        }

        Optional<ConfigRecord> spawnXRecord = Cpecraft.configRepository().findByKey("spawn_x");
        Optional<ConfigRecord> spawnYRecord = Cpecraft.configRepository().findByKey("spawn_y");
        Optional<ConfigRecord> spawnZRecord = Cpecraft.configRepository().findByKey("spawn_z");

        if (spawnXRecord.isEmpty() || spawnYRecord.isEmpty() || spawnZRecord.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("Spawn coordinates is not configured!"), false);
            return 0;
        }

        double spawnX = Double.parseDouble(spawnXRecord.get().value());
        double spawnY = Double.parseDouble(spawnYRecord.get().value());
        double spawnZ = Double.parseDouble(spawnZRecord.get().value());

        Vec3 pos = new Vec3(spawnX, spawnY, spawnZ);
        ServerLevel overworld = ctx.getSource().getServer().getLevel(Level.OVERWORLD);

        assert overworld != null;
        TeleportTransition t = new TeleportTransition(
                overworld, pos, Vec3.ZERO, 0.0f, 0.0f, TeleportTransition.DO_NOTHING);

        target.stopRiding();
        target.teleport(t);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Teleported you to spawn!"), true);
        return 1;
    }
}
