package com.example;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
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

	/** Where the first island is built, and where you spawn. */
	private static final BlockPos HOME = new BlockPos(0, 64, 0);

	@Override
	public void onInitialize() {
		Minions.register();
		Skills.registerHooks();
		Skills.registerBreeding();
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
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
		CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
			registerCommands(dispatcher);
			registerHubCommand(dispatcher);
			Economy.registerCommands(dispatcher);
			Shops.register(dispatcher);
		});
		// Portals aren't real blocks, so somebody has to watch for a player
		// standing in one. See Portals.tick -- it is deliberately cheap.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel world : server.getAllLevels()) {
				if (world.dimension() == Level.OVERWORLD) {
					Portals.tick(world);
					Pages.tickPets(world);
					SafeZone.tick(world);
				}
			}
		});
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

	/**
	 * /hub — jump straight to the Hub.
	 *
	 * A builder's tool, not part of the game. It needs operator permission, so
	 * in ordinary play it isn't there at all and you still have to bridge to
	 * the portal like everyone else. Handy while testing the Hub, which is
	 * otherwise a long walk away.
	 */
	private static void registerHubCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("hub")
				.requires(source -> source.hasPermission(2))
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					ServerLevel level = ctx.getSource().getLevel();
					if (!Hub.exists(level)) {
						Hub.build(level);
						ctx.getSource().sendSuccess(
								() -> Component.literal("Built the Hub."), false);
					}
					BlockPos to = Hub.arrival();
					player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
					ctx.getSource().sendSuccess(
							() -> Component.literal("§7Warped to the Hub. (builder command)"), false);
					return 1;
				}));
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
