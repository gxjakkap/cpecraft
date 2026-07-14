package com.cpesu.cpecraft.command;

import com.cpesu.cpecraft.Cpecraft;
import com.cpesu.cpecraft.db.StudentRecord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.Optional;

public class WhoisCommand {
    private WhoisCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("whois")
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(WhoisCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<NameAndId> targets = GameProfileArgument.getGameProfiles(ctx, "player");
        for (NameAndId t : targets) {
            String name = t.name();

            Optional<StudentRecord> record = Cpecraft.studentRepository().findByUuid(t.id());
            if (record.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("Player " + name + " is not registered."), false);
                continue;
            }

            StudentRecord student = record.get();
            ctx.getSource().sendSuccess(() -> Component.literal(name + " is " + student.nickName() + " CPE " + student.batch() + "."), false);
        }

        return targets.size();
    }
}
