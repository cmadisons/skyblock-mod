package com.example;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Getting to every place in the game, and building it when you arrive.
 *
 * The locations in {@link Locations} are descriptions, not buildings. This
 * turns one into an actual island the first time somebody goes there: ground of
 * the right sort, the resources it lists scattered through it, its NPCs
 * standing on it, and its enemies waiting.
 *
 * Built on arrival rather than at world creation, which matters: a hundred
 * islands generated up front would take a long time and fill the save file with
 * places nobody visits. Go somewhere and it exists; don't and it costs nothing.
 *
 * Where they are
 * -------------
 * On a grid, four hundred blocks apart, well clear of the island and the Hub.
 * Far enough that nothing overlaps and you never wander from one into another
 * by accident.
 */
public final class Warps {
	private Warps() {
	}

	/** How far apart the islands sit. */
	private static final int SPACING = 400;

	/** How many to a row before starting another. */
	private static final int PER_ROW = 12;

	/** Where the whole grid starts, well away from the island and Hub. */
	private static final BlockPos ORIGIN = new BlockPos(4000, 64, 4000);

	private static final SuggestionProvider<CommandSourceStack> NAMES = (ctx, builder) ->
			SharedSuggestionProvider.suggest(
					java.util.Arrays.stream(Locations.ALL)
							.map(place -> place.name().replace(' ', '_'))
							.sorted()
							.toList(),
					builder);

	/** Where a place sits on the grid. Fixed by its position in the list. */
	public static BlockPos siteOf(Locations.Place place) {
		int index = 0;
		for (int i = 0; i < Locations.ALL.length; i++) {
			if (Locations.ALL[i] == place) {
				index = i;
				break;
			}
		}
		return ORIGIN.offset((index % PER_ROW) * SPACING, 0, (index / PER_ROW) * SPACING);
	}

	/** Has this one been built yet? */
	public static boolean built(ServerLevel level, Locations.Place place) {
		return !level.getBlockState(siteOf(place).below()).isAir();
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("warp")
				// /warp — everything you can reach, and what the rest need.
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					ctx.getSource().sendSuccess(() -> Component.literal(
							"§6Places §7— /warp <name>"), false);
					for (Locations.Place place : Locations.ALL) {
						int has = place.skill().isEmpty() ? 0
								: Skills.level(Skills.xp(player, place.skill()));
						boolean open = place.openTo(has);
						String need = place.skill().isEmpty() ? ""
								: " §8(" + place.skill() + " " + place.level() + ")";
						ctx.getSource().sendSuccess(() -> Component.literal(
								(open ? "  §a✔ " : "  §8✘ ") + place.name() + need), false);
					}
					return 1;
				})
				// /warp buildall — put every place up, one a second.
				.then(Commands.literal("buildall").executes(ctx -> {
					buildAll(ctx.getSource().getLevel(), ctx.getSource().getPlayerOrException());
					return 1;
				}))

				.then(Commands.argument("place", StringArgumentType.greedyString())
						.suggests(NAMES)
						.executes(ctx -> {
							ServerPlayer player = ctx.getSource().getPlayerOrException();
							String asked = StringArgumentType.getString(ctx, "place")
									.replace('_', ' ');
							Locations.Place place = Locations.byName(asked);
							if (place == null) {
								ctx.getSource().sendFailure(Component.literal(
										"No place called " + asked + ". Type /warp to see them."));
								return 0;
							}

							int has = place.skill().isEmpty() ? 0
									: Skills.level(Skills.xp(player, place.skill()));
							if (!place.openTo(has)) {
								ctx.getSource().sendFailure(Component.literal(
										place.name() + " needs " + place.skill() + " level "
												+ place.level() + ". You are level " + has + "."));
								return 0;
							}

							ServerLevel level = ctx.getSource().getLevel();
							if (!built(level, place)) {
								build(level, place);
								ctx.getSource().sendSuccess(() -> Component.literal(
										"§7Built " + place.name() + "."), false);
							}
							BlockPos site = siteOf(place);
							player.teleportTo(site.getX() + 0.5, site.getY() + 1, site.getZ() + 0.5);
							ctx.getSource().sendSuccess(() -> Component.literal(
									"§aWelcome to §f" + place.name() + "§a."), false);
							return 1;
						})));
	}

	/**
	 * Places still waiting to be built, and who asked for them.
	 *
	 * Building a hundred islands in one go would lock the game up for a long
	 * time -- each is thousands of blocks, and it all happens on the one thread
	 * that runs the world. So they queue and go up one a second, which is slow
	 * enough to stay smooth and fast enough to watch.
	 */
	private static final java.util.ArrayDeque<Locations.Place> queue = new java.util.ArrayDeque<>();
	private static java.util.UUID watcher;

	/** Queue every place that isn't built yet. */
	public static void buildAll(ServerLevel level, ServerPlayer asker) {
		queue.clear();
		for (Locations.Place place : Locations.ALL) {
			if (!built(level, place)) {
				queue.add(place);
			}
		}
		watcher = asker.getUUID();
		asker.sendSystemMessage(Component.literal(
				"§aBuilding " + queue.size() + " places, one a second. "
						+ "§7Carry on playing -- it happens in the background."));
	}

	/**
	 * Build the next one in the queue, once a second.
	 *
	 * Called from the mod's tick. Does nothing at all when the queue is empty,
	 * so this costs one comparison when nobody has asked for anything.
	 */
	public static void tick(ServerLevel level) {
		if (queue.isEmpty() || level.getGameTime() % 20 != 0) {
			return;
		}
		Locations.Place place = queue.poll();
		if (place == null) {
			return;
		}
		if (!built(level, place)) {
			build(level, place);
		}

		ServerPlayer asker = watcher == null ? null : level.getServer().getPlayerList()
				.getPlayer(watcher);
		if (asker == null) {
			return;
		}
		int left = queue.size();
		asker.sendSystemMessage(Component.literal(
				"§7Built §f" + place.name() + "§7"
						+ (left > 0 ? " — " + left + " to go" : " — all done.")));
	}

	/**
	 * Build one place.
	 *
	 * A round island of the right ground, its resources dotted through it in a
	 * fixed pattern, its people standing in a line near the middle, and its
	 * enemies out towards the edge where they have room to come at you.
	 */
	public static void build(ServerLevel level, Locations.Place place) {
		BlockPos middle = siteOf(place);
		int radius = place.size();

		BlockState ground = groundBlock(place.ground());
		BlockState under = place.ground() == Locations.Ground.STONE
				|| place.ground() == Locations.Ground.OBSIDIAN
				? Blocks.STONE.defaultBlockState()
				: Blocks.DIRT.defaultBlockState();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				level.setBlockAndUpdate(middle.offset(dx, -1, dz), ground);
				level.setBlockAndUpdate(middle.offset(dx, -2, dz), under);
				level.setBlockAndUpdate(middle.offset(dx, -3, dz), under);
			}
		}

		scatterResources(level, middle, radius, place.gathers());
		standPeople(level, middle, place.people());

		if (place.enemies() != null && place.enemies().length > 0) {
			Mobs.ring(level, middle, place.enemies(), Math.max(6, radius - 6), 1);
		}

		// A lamp in the middle, so you can find where you landed.
		level.setBlockAndUpdate(middle, Blocks.SEA_LANTERN.defaultBlockState());

		SkyBlocksMod.LOGGER.info("Built {} at {}", place.name(), middle);
	}

	/**
	 * Dot the resources through the island.
	 *
	 * Spaced on a fixed grid rather than randomly, so every world's version of a
	 * place is the same and you can learn where things are.
	 */
	private static void scatterResources(ServerLevel level, BlockPos middle, int radius,
			Block[] gathers) {
		if (gathers.length == 0) {
			return;
		}
		int n = 0;
		for (int dx = -radius + 3; dx <= radius - 3; dx += 4) {
			for (int dz = -radius + 3; dz <= radius - 3; dz += 4) {
				if (dx * dx + dz * dz > (radius - 3) * (radius - 3)) {
					continue;
				}
				Block what = gathers[n % gathers.length];
				n++;
				// Logs and cacti stand up; ore and stone sit in the ground.
				boolean standing = what == Blocks.OAK_LOG || what == Blocks.BIRCH_LOG
						|| what == Blocks.SPRUCE_LOG || what == Blocks.DARK_OAK_LOG
						|| what == Blocks.ACACIA_LOG || what == Blocks.JUNGLE_LOG
						|| what == Blocks.MANGROVE_LOG || what == Blocks.CACTUS
						|| what == Blocks.SUGAR_CANE;
				for (int y = 0; y < (standing ? 3 : 1); y++) {
					level.setBlockAndUpdate(middle.offset(dx, standing ? y : -1, dz),
							what.defaultBlockState());
				}
			}
		}
	}

	/** Stand the NPCs in a row a few blocks out from the middle. */
	private static void standPeople(ServerLevel level, BlockPos middle, String[] people) {
		for (int i = 0; i < people.length; i++) {
			double angle = i * (Math.PI * 2 / Math.max(1, people.length));
			int x = (int) Math.round(Math.cos(angle) * 4);
			int z = (int) Math.round(Math.sin(angle) * 4);
			Npcs.spawnVillager(level, middle.offset(x, 0, z), people[i]);
		}
	}

	private static BlockState groundBlock(Locations.Ground ground) {
		return switch (ground) {
			case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
			case STONE -> Blocks.STONE.defaultBlockState();
			case SAND -> Blocks.SAND.defaultBlockState();
			case END_STONE -> Blocks.END_STONE.defaultBlockState();
			case NETHERRACK -> Blocks.NETHERRACK.defaultBlockState();
			case ICE -> Blocks.PACKED_ICE.defaultBlockState();
			case WATER -> Blocks.WATER.defaultBlockState();
			case MYCELIUM -> Blocks.MYCELIUM.defaultBlockState();
			case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
		};
	}

	/** How many places there are, for the menu to show. */
	public static int count() {
		return Locations.ALL.length;
	}

	/** How many of them this player can currently get into. */
	public static int openTo(ServerPlayer player) {
		int open = 0;
		for (Locations.Place place : Locations.ALL) {
			int has = place.skill().isEmpty() ? 0
					: Skills.level(Skills.xp(player, place.skill()));
			if (place.openTo(has)) {
				open++;
			}
		}
		return open;
	}

	/** Colour used by the menu when listing places. */
	public static ChatFormatting colour(boolean open) {
		return open ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
	}
}
