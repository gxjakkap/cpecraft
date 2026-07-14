package com.cpesu.cpecraft.command;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.HomeRecord;
import com.cpesu.cpecraft.freeze.FreezeManager;
import com.cpesu.cpecraft.home.HomeTeleportManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HomeCommand {
    private HomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> home = dispatcher.register(Commands.literal("home")
                .executes(ctx -> execute(ctx, null, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("home", StringArgumentType.word())
                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "home"), ctx.getSource().getPlayerOrException()))));
        dispatcher.register(Commands.literal("h").redirect(home));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String homeName, ServerPlayer player) {
        UUID uuid = player.getUUID();

        if (FreezeManager.isFrozen(uuid)){
            ctx.getSource().sendSuccess(() -> Component.literal("You need to verify yourself first before using this command!"), false);
            return 0;
        }

        if (HomeTeleportManager.isPending(uuid)) {
            ctx.getSource().sendFailure(Component.literal("You're already teleporting."));
            return 0;
        }

        HomeRecord target = resolveHome(uuid, homeName);
        if (target == null) {
            ctx.getSource().sendFailure(Component.literal(homeName == null
                    ? "You don't have any homes set. Use /sethome first."
                    : "You don't have a home named '" + homeName + "'."));
            return 0;
        }

        HomeTeleportManager.start(uuid, target, player.position(), player.level().dimension(),
                ctx.getSource().getServer().getTickCount());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Teleporting to '" + target.name() + "' in 3 seconds. Don't move!"), false);
        return 1;
    }

    static HomeRecord resolveHome(UUID uuid, String homeName) {
        if (homeName != null) {
            return Cpecraft.homeRepository().findByPlayerUuidAndHomeName(uuid, homeName.toLowerCase()).orElse(null);
        }

        Optional<HomeRecord> defaultHome = Cpecraft.homeRepository().findDefaultByUuid(uuid);
        if (defaultHome.isPresent()) {
            return defaultHome.get();
        }

        List<HomeRecord> homes = Cpecraft.homeRepository().findByUuid(uuid);
        return homes.isEmpty() ? null : homes.getFirst();
    }
}
