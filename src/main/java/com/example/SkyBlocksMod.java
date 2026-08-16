package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

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
 * Survival is the game. Creative is shut out, because a game about having
 * nothing is pointless when you are handed everything -- unless the Blueprint
 * mod is installed, which is taken as saying you are here to build rather than
 * play. See allowed().
 */
public class SkyBlocksMod implements ModInitializer {
	public static final String MOD_ID = "skyblocks";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Where the first island is built, and where you spawn. */
	private static final BlockPos HOME = new BlockPos(0, 64, 0);

	@Override
	public void onInitialize() {
		Minions.register();
		// Everything SkyBlock has that Minecraft does not: five hundred items,
		// the blocks, and a token for every person in the world. Registered
		// before anything else touches a registry.
		SkyItems.register();
		NpcTokens.register();
		MobTokens.register();
		XpBoosts.register();
		SkyTabs.register();
		Skills.registerHooks();
		Skills.registerCrafting();
		Skills.registerBreeding();
		Skills.registerMoreWays();
		Npcs.registerInteraction();
		Menu.registerKeepInSlot();
		// Right-clicking the star in slot nine opens the SkyBlock Menu.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player instanceof ServerPlayer serverPlayer
					&& world instanceof ServerLevel serverWorld
					&& allowed(serverPlayer, serverWorld)
					&& Menu.isStar(player.getItemInHand(hand))) {
				Menu.open(serverPlayer);
				return net.minecraft.world.InteractionResult.SUCCESS;
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			onJoin(handler.getPlayer());
			// Skill rewards are transient attribute modifiers, so they have to be
			// put back every time somebody logs in.
			Skills.applyRewards(handler.getPlayer());
		});
		// No commands. See the note on this class.
		// Portals aren't real blocks, so somebody has to watch for a player
		// standing in one. See Portals.tick -- it is deliberately cheap.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel world : server.getAllLevels()) {
				if (world.dimension() == Level.OVERWORLD) {
					Portals.tick(world);
					Pages.tickPets(world);
					SafeZone.tick(world);
					Areas.tick(world);
					Warps.tick(world);
					Quests.tick(world);
					HubEdit.tick(world);
				}
			}
		});
		// Anything changed in the last few seconds is written out here rather
		// than lost, since a quit is the most likely end to a building session.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING
				.register(server -> HubEdit.save());
		LOGGER.info("Sky Blocks loaded.");
	}

	/**
	 * Give a new arrival somewhere to stand.
	 *
	 * Only fires when the world really is empty -- joining a normal world does
	 * nothing at all, so installing this mod can't wreck a world you already
	 * have. Once an island exists the check below stops matching, so it never
	 * builds twice.
	 */
	private static void onJoin(ServerPlayer player) {
		ServerLevel level = player.level() instanceof ServerLevel s ? s : null;
		if (level == null || level.dimension() != Level.OVERWORLD) {
			return;
		}
		if (!allowed(player, level)) {
			return;
		}
		if (!level.getBlockState(HOME).isAir() || !isVoid(level)) {
			return;
		}

		keepInventory(level);
		BlockPos stand = Island.build(level, HOME);
		// Dying into the void shouldn't cost you everything -- keep your items.
		// Saved with the world, so setting it once here is enough.
		level.getGameRules().set(GameRules.KEEP_INVENTORY, true, level.getServer());
		player.teleportTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
		player.sendSystemMessage(Component.literal(
				"Welcome to Sky Blocks. Bridge across to the minion and the portal."));
		LOGGER.info("Built the starting island at {}", HOME);
	}

	/**
	 * Switch on keepInventory for a Sky Blocks world.
	 *
	 * Dying here usually means falling into the void, where your things are
	 * gone for good rather than waiting in a pile -- so losing them as well
	 * would be brutal. Coins are the thing death costs you: see
	 * {@link Economy#onDeath}.
	 *
	 * Only ever touched for a world this mod built, so a normal world keeps
	 * whatever rules you set for it.
	 */
	private static void keepInventory(ServerLevel level) {
		// Done through the ordinary command rather than the game-rule field,
		// which Mojang renames between versions.
		level.getServer().getCommands().performPrefixedCommand(
				level.getServer().createCommandSourceStack().withSuppressedOutput(),
				"gamerule keepInventory true");
	}

	/**
	 * Sky Blocks runs in Survival and in Creative. Not in Hardcore.
	 *
	 * Creative used to be shut out, on the grounds that a game about having
	 * nothing is pointless when you are handed everything. That was right about
	 * playing and wrong about building: the Hub this mod invents is not the Hub
	 * you remember, and the only way to fix that is to go and fix it, which
	 * takes Creative. So Creative is now the editor -- everything you change in
	 * the Hub is kept and comes back in every world you make. See
	 * {@link HubEdit}.
	 *
	 * Hardcore stays out. One life and a void world is not a game, it is a
	 * countdown.
	 */
	public static boolean allowed(ServerPlayer player, ServerLevel level) {
		if (level.getLevelData().isHardcore()) {
			return false;
		}
		return player.gameMode() == GameType.SURVIVAL || player.gameMode() == GameType.CREATIVE;
	}

	/** Is Blueprint installed? If so, Creative counts as build mode. */
	public static boolean buildMode() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("blueprint");
	}

	/**
	 * Is this world actually empty?
	 *
	 * Checked by looking straight down the column at spawn. A void world has
	 * nothing in it anywhere; any normal world has ground somewhere below.
	 */
	private static boolean isVoid(ServerLevel level) {
		BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
		for (int y = level.getMinY(); y < level.getMaxY(); y++) {
			probe.set(HOME.getX(), y, HOME.getZ());
			if (!level.getBlockState(probe).isAir()) {
				return false;
			}
		}
		return true;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
