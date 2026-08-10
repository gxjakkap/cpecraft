package com.cpesu.cpecraft.command;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.ConfigRecord;
import com.cpesu.cpecraft.freeze.FreezeManager;
import com.cpesu.cpecraft.teleport.TeleportManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class SpawnCommand {
    private SpawnCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn")
                .executes(ctx -> execute(ctx, ctx.getSource().getPlayerOrException())));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        UUID uuid = player.getUUID();
        if (FreezeManager.isFrozen(uuid)){
            ctx.getSource().sendSuccess(() -> Component.literal("You need to verify yourself first before using this command!"), false);
            return 0;
        }

        if (TeleportManager.isPending(uuid)) {
            ctx.getSource().sendFailure(Component.literal("You're already teleporting."));
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

        TeleportManager.Destination destination = new TeleportManager.Destination(
                Level.OVERWORLD, new Vec3(spawnX, spawnY, spawnZ), 0.0f, 0.0f, "Teleported you to spawn!");
        TeleportManager.start(uuid, destination, player.position(), player.level().dimension(),
                ctx.getSource().getServer().getTickCount());

        ctx.getSource().sendSuccess(() -> Component.literal(
                "Teleporting to spawn in 3 seconds. Don't move!"), false);
        return 1;
    }
}
