package com.cpesu.cpecraft.command;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.ConfigRecord;
import com.cpesu.cpecraft.db.HomeRecord;
import com.cpesu.cpecraft.permission.CpecraftPermissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SetHomeCommand {
    private static final String HOME_NAME = "^[a-zA-Z0-9_]{1,16}$";

    private SetHomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
                .executes(ctx ->
                        setAsNotDefault(
                                ctx,
                                "home",
                                ctx.getSource().getPlayerOrException()
                        )
                )
                .then(Commands.argument("home", StringArgumentType.word())
                        .executes(ctx ->
                                setAsNotDefault(
                                        ctx,
                                        StringArgumentType.getString(ctx, "home"),
                                        ctx.getSource().getPlayerOrException()
                                )
                        )
                )
                .then(Commands.argument("isDefault", BoolArgumentType.bool())
                        .executes(ctx ->
                                set(
                                        ctx,
                                        StringArgumentType.getString(ctx, "home"),
                                        ctx.getSource().getPlayerOrException(),
                                        BoolArgumentType.getBool(ctx, "isDefault")
                                )
                        )
                )
        );
    }

    private static int setAsNotDefault(CommandContext<CommandSourceStack> ctx, String homeName, ServerPlayer player) throws CommandSyntaxException {
        return set(ctx, homeName, player, false);
    }

    private static int set(CommandContext<CommandSourceStack> ctx, String homeName, ServerPlayer player, boolean isDefault) throws CommandSyntaxException {
        if (!homeName.matches(HOME_NAME)) {
            ctx.getSource().sendFailure(Component.literal(
                    "Home name must be 1-16 characters: letters, numbers, _ only."));
            return 0;
        }

        List<HomeRecord> existing = Cpecraft.homeRepository().findByUuid(player.getUUID());
        HomeRecord alreadyExisted = existing.stream().filter(h -> h.name().equalsIgnoreCase(homeName)).findFirst().orElse(null);

        if (alreadyExisted != null) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                    "Home " + homeName + " is already set to %.1f, %.1f, %.1f.", alreadyExisted.x(), alreadyExisted.y(), alreadyExisted.z())), true);
            return 0;
        }

        Optional<ConfigRecord> maxHome = Cpecraft.configRepository().findByKey("max_home_quota");

        if (maxHome.isEmpty()) {
            Cpecraft.LOGGER.error("max_home_quota is empty!");
            ctx.getSource().sendSuccess(() -> Component.literal("Server isn't set up properly. Contact admin to check logs."), false);
            return 0;
        }

        int maxHomeInt = Integer.parseInt(maxHome.get().value());

        if (existing.size() >= maxHomeInt && maxHomeInt != -1 && !CpecraftPermissions.requires(CpecraftPermissions.BYPASS_MAX_HOME_QUOTA).test(ctx.getSource())) {
            ctx.getSource().sendSuccess(() -> Component.literal(String.format("You already have %d homes which is the maximum amount set by the admins. Remove unused locations with /delhome first.", maxHomeInt)), false);
            return 0;
        }

        double x = Objects.requireNonNull(ctx.getSource().getPlayer()).getX();
        double y = Objects.requireNonNull(ctx.getSource().getPlayer()).getY();
        double z = Objects.requireNonNull(ctx.getSource().getPlayer()).getZ();
        double xRot = ctx.getSource().getPlayer().getXRot();
        double yRot = ctx.getSource().getPlayer().getYRot();

        Cpecraft.homeRepository().save(new HomeRecord(
                ctx.getSource().getPlayer().getUUID(),
                homeName.toLowerCase(),
                x,
                y,
                z,
                xRot,
                yRot,
                ctx.getSource().getPlayer().level().dimension(),
                System.currentTimeMillis(),
                isDefault
        ));

        ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                "Home %s set to %.1f, %.1f, %.1f.", homeName, x, y, z)), false);
        return 1;
    }
}
