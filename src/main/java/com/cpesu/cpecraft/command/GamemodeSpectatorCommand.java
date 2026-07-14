package com.cpesu.cpecraft.command;

import com.cpesu.cpecraft.permission.CpecraftPermissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public final class GamemodeSpectatorCommand {
    private GamemodeSpectatorCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gmsp")
                .requires(CpecraftPermissions.requires(CpecraftPermissions.GM))
                .executes(ctx -> execute(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> execute(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        target.setGameMode(GameType.SPECTATOR);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Set " + target.getGameProfile().name() + "'s gamemode to Spectator"), true);
        return 1;
    }
}
