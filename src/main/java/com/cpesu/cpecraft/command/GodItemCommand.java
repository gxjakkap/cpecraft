package com.cpesu.cpecraft.command;

import java.util.Map;
import java.util.function.Function;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import com.cpesu.cpecraft.permission.CpecraftPermissions;
import org.jetbrains.annotations.NotNull;

public final class GodItemCommand {
	private GodItemCommand() {
	}

    // item map
	private static final Map<String, Function<HolderGetter<@NotNull Enchantment>, ItemStack>> KIT = Map.of(
			"sharpstick", enchantments -> {
				ItemStack stick = new ItemStack(Items.STICK);
				stick.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 255);
				return stick;
			},
			"knockbat", enchantments -> {
				ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
				sword.enchant(enchantments.getOrThrow(Enchantments.KNOCKBACK), 10);
				return sword;
			});

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
		dispatcher.register(Commands.literal("goditem")
				.requires(CpecraftPermissions.requires(CpecraftPermissions.GOD_ITEM))
				.then(Commands.argument("item", StringArgumentType.word())
						.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(KIT.keySet(), builder))
						.executes(ctx -> execute(ctx, registryAccess, null))
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(ctx -> execute(ctx, registryAccess, StringArgumentType.getString(ctx, "name"))))));
	}

	private static int execute(CommandContext<CommandSourceStack> ctx, CommandBuildContext registryAccess, String customName) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = StringArgumentType.getString(ctx, "item");

		Function<HolderGetter<@NotNull Enchantment>, ItemStack> factory = KIT.get(name);
		if (factory == null) {
			ctx.getSource().sendFailure(Component.literal(
					"Unknown item '" + name + "'. Options: " + String.join(", ", KIT.keySet())));
			return 0;
		}

		HolderGetter<@NotNull Enchantment> enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
		ItemStack item = factory.apply(enchantments);
		if (customName != null) {
			item.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
		}
		if (!player.getInventory().add(item)) {
			player.drop(item, false);
		}

		ctx.getSource().sendSuccess(() -> Component.literal("Gave yourself " + name + "."), true);
		return 1;
	}
}
