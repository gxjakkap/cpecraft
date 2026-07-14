package com.cpesu.cpecraft.permission;

import java.util.function.Predicate;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * Permission node constants for every gated command, plus a helper that
 * wraps {@link Permissions#require(String, PermissionLevel)} with this mod's
 * default op-level fallback (GAMEMASTERS, matching vanilla's admin-command
 * threshold) so command builders don't repeat it.
 */
public final class CpecraftPermissions {
	private CpecraftPermissions() {
	}

	public static final String GM = "cpecraft.command.gm";
	public static final String INVSEE = "cpecraft.command.invsee";
	public static final String USERSTATS = "cpecraft.command.userstats";
	public static final String USERINFO = "cpecraft.command.userinfo";
	public static final String CREATE_BATCH = "cpecraft.command.createbatch";
	public static final String LIST_BATCHES = "cpecraft.command.listbatches";
	public static final String DELETE_BATCH = "cpecraft.command.deletebatch";
	public static final String MOTD = "cpecraft.command.motd";
	public static final String UNLINK = "cpecraft.command.unlink";
	public static final String OVERRIDE_VERIFICATION = "cpecraft.command.overrideverification";
	public static final String SET_SPAWN = "cpecraft.command.setspawn";
	public static final String PLAYERLIST = "cpecraft.command.playerlist";
	public static final String TPHERE = "cpecraft.command.tphere";
	public static final String TPOFF = "cpecraft.command.tpoff";
	public static final String ADMIN_HOME = "cpecraft.command.adminhome";
	public static final String GIVE_ITEM = "cpecraft.command.giveitem";
	public static final String GOD_ITEM = "cpecraft.command.goditem";
    public static final String BYPASS_MAX_HOME_QUOTA = "cpecraft.command.home.quotabypass";

	private static final PermissionLevel DEFAULT_OP_LEVEL = PermissionLevel.GAMEMASTERS;

	public static Predicate<CommandSourceStack> requires(String node) {
		return Permissions.require(node, DEFAULT_OP_LEVEL);
	}
}
