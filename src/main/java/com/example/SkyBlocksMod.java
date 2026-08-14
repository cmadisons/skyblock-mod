package com.example;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sky Blocks: turns a world into the survival-on-a-tiny-island game.
 *
 * There are two halves to this mod.
 *
 * The world itself is a data file, not code -- worldgen/world_preset/
 * sky_blocks.json describes an overworld with no layers at all, which is to say
 * an empty void. It shows up as a world type called "Sky Blocks" on the Create
 * World screen because a tag adds it to the vanilla list.
 *
 * The island is the code here. An empty void has nothing to stand on, so when
 * you first join a void world this drops in the starting island -- 5x5 grass, a
 * tree, and a walkway out to a portal ten blocks away. Step into the portal and
 * you are taken to the Hub, which is built the first time anyone goes through.
 *
 * All of it is Survival-only. In Creative or Hardcore the mod does nothing.
 */
public class SkyBlocksMod implements ModInitializer {
	public static final String MOD_ID = "skyblocks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Minions.register();
		Skills.registerHooks();
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
			registerCommands(dispatcher);
			Economy.registerCommands(dispatcher);
		});
		// Portals aren't real blocks, so somebody has to watch for a player
		// standing in one. See Portals.tick -- it is deliberately cheap.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel world : server.getAllLevels()) {
				if (world.dimension() == Level.OVERWORLD) {
					Portals.tick(world);
					Mobs.tick(world);
				}
			}
		});
		LOGGER.info("Sky Blocks loaded.");
	}

	/**
	 * Give a new arrival an island of their own.
	 *
	 * Everybody gets their own slot along the row -- see {@link Islands} -- so a
	 * server works the same as single player, just with more islands. A player
	 * who already has one keeps it: the check below finds their island standing
	 * where they left it and does nothing.
	 *
	 * Joining a normal world still does nothing at all. The emptiness check runs
	 * against the column the island would occupy, so any world with ground in it
	 * is left alone exactly as before.
	 */
	private static void onJoin(ServerPlayer player) {
		ServerLevel level = player.level() instanceof ServerLevel s ? s : null;
		if (level == null || level.dimension() != Level.OVERWORLD) {
			return;
		}
		if (!allowed(player, level)) {
			return;
		}

		// Look at where their island would go without claiming a slot yet, so a
		// player who wanders into a normal world doesn't burn one.
		Integer owned = Islands.slot(player);
		int slot = owned != null ? owned : Islands.peekNext(level);
		BlockPos centre = Islands.centreOf(slot);

		// Somewhere to build: their slot is empty void and nothing stands there.
		boolean empty = level.getBlockState(centre).isAir() && isVoid(level, centre);

		if (!empty && owned == null) {
			return;                     // a normal world, and no island here: leave it alone
		}

		// Dying into the void shouldn't cost you everything, so keep inventory is
		// on from the moment a Sky Blocks world starts. Set on every join rather
		// than only when an island is built, so it comes back on if it ever gets
		// turned off -- falling into the void is not a mistake worth a wipe.
		level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());

		if (!empty) {
			return;                     // their island is already standing
		}

		if (owned == null) {
			Islands.claim(level, player, slot);
		}

		BlockPos stand = Island.build(level, centre);
		player.teleportTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
		player.sendSystemMessage(Component.literal(
				"Welcome to Sky Blocks. Bridge across to the minion and the portal."));
		LOGGER.info("Built island {} at {}", slot, centre);
	}

	/**
	 * Sky Blocks runs in Survival only.
	 *
	 * Creative would make the whole thing pointless -- the game is about having
	 * almost nothing, and Creative hands you everything. Hardcore is excluded
	 * too. In either mode this mod does nothing whatsoever: no island, no
	 * commands, exactly as if it weren't installed.
	 */
	public static boolean allowed(ServerPlayer player, ServerLevel level) {
		if (level.getLevelData().isHardcore()) {
			return false;
		}
		return player.gameMode() == GameType.SURVIVAL;
	}

	/**
	 * Is this stretch of world actually empty?
	 *
	 * Checked by looking straight down the column an island would sit in. A void
	 * world has nothing in it anywhere; any normal world has ground somewhere
	 * below, which is what keeps this mod from touching a world you already have.
	 */
	private static boolean isVoid(ServerLevel level, BlockPos column) {
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		for (int y = level.getMinY(); y < level.getMaxY(); y++) {
			probe.set(column.getX(), y, column.getZ());
			if (!level.getBlockState(probe).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** /skyblock island — drops another island where you are standing. */
	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("skyblock")
				.then(Commands.literal("island")
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							ServerLevel level = ctx.getSource().getLevel();
							if (!allowed(player, level)) {
								ctx.getSource().sendFailure(Component.literal(
										"Sky Blocks only works in Survival."));
								return 0;
							}
							// Built at your feet, so it appears where you are looking.
							BlockPos where = player.blockPosition().below();
							BlockPos stand = Island.build(level, where);
							player.teleportTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
							ctx.getSource().sendSuccess(
									() -> Component.literal("New island built."), false);
							return 1;
						})));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
