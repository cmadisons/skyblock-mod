package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hub — laid out from the SkyBlock wiki rather than guessed.
 *
 * Spawn is the middle of the Village, and the portal home lies flat in the
 * floor right there, because that is where the real one is: "the Portal to a
 * player's Private Island is found in the center of the village".
 *
 * Directions, standing at spawn looking forward (north):
 *
 *                       Mining District  ->  Gold Mine
 *                              |
 *      Combat Settlement       |
 *        -> Graveyard          |
 *              \               |
 *   Forest ---------------  THE VILLAGE  --------------- Fishing Outpost
 *   -> The Park                |
 *                       Community Center
 *
 * The Bank sits left-and-forward of spawn at roughly (-20, -65), which is
 * where the wiki puts it. The Village around spawn holds the Community Center,
 * the Auction House, Bazaar Alley and the Library.
 *
 * What this is not: a block-for-block copy. Hypixel's Hub is hand-built and
 * they do not publish the world file, so the districts, their directions and
 * the Bank's position are real, while the buildings themselves are mine.
 */
public final class Hub {
	private Hub() {
	}

	/** Spawn: the middle of the Village. */
	public static final BlockPos CENTRE = new BlockPos(0, 64, 1000);

	/** Half-width of the Village plaza itself. */
	private static final int VILLAGE = 26;

	/**
	 * Where you land: a few blocks south of the portal.
	 *
	 * Off the pad on purpose -- landing on it would send you straight home
	 * again. From here the Community Center is behind you and the Mining
	 * District is straight ahead.
	 */
	public static BlockPos arrival() {
		return CENTRE.offset(0, 1, 6);
	}

	public static boolean exists(ServerLevel level) {
		return !level.getBlockState(CENTRE.below()).isAir();
	}

	/** Build the whole Hub. Returns where to stand an arriving player. */
	public static BlockPos build(ServerLevel level) {
		ground(level);
		buildings(level);
		people(level);
		roads(level);

		// The districts sit beyond the buildings, in the directions the wiki
		// gives, clear of everything the coordinate guide covers.
		mining(level, CENTRE.offset(0, 0, HubMap.northEdge() - 40));
		combat(level, CENTRE.offset(-110, 0, -150));
		forest(level, CENTRE.offset(HubMap.westEdge() - 40, 0, -40));
		fishing(level, CENTRE.offset(HubMap.eastEdge() + 40, 0, -70));

		Portals.pad(level, CENTRE);                          // the way home
		return arrival();
	}

	/**
	 * The floor the whole Village stands on.
	 *
	 * Sized from the buildings themselves rather than a fixed number, so it
	 * always reaches far enough -- the real Hub runs 177 blocks north of spawn
	 * and 84 east, which is a great deal bigger than it looks on a map.
	 */
	private static void ground(ServerLevel level) {
		BlockState floor = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
		BlockState dirt = Blocks.DIRT.defaultBlockState();

		int west = HubMap.westEdge();
		int east = HubMap.eastEdge();
		int north = HubMap.northEdge();
		int south = HubMap.southEdge();

		for (int x = west; x <= east; x++) {
			for (int z = north; z <= south; z++) {
				// Stone through the middle where the buildings are, grass out
				// towards the edges, so it reads as a town on an island rather
				// than one enormous slab.
				boolean paved = Math.abs(x) < 60 && z > -150;
				level.setBlockAndUpdate(CENTRE.offset(x, -1, z), paved ? floor : grass);
				level.setBlockAndUpdate(CENTRE.offset(x, -2, z), dirt);
			}
		}
	}

	/**
	 * Every building, at its measured position.
	 *
	 * Each checks for one you saved with the Blueprint mod first and only falls
	 * back to the built-in shape if you have not made one, so a Hub you built
	 * yourself gradually replaces this one building at a time.
	 */
	private static void buildings(ServerLevel level) {
		for (HubMap.Place place : HubMap.BUILDINGS) {
			BlockPos at = CENTRE.offset(place.x(), place.y(), place.z());

			if (!place.blueprint().isEmpty() && Custom.place(level, at, place.blueprint())) {
				continue;                        // yours, so leave it alone
			}
			// Centred on the coordinate, since that is what the guide records --
			// the position of the building, not the corner of it.
			hall(level, at.offset(-place.width() / 2, 0, -place.depth() / 2),
					place.width(), place.depth(), place.height(), place.wall(), place.roof());
		}
	}

	/** The residents, standing where the guide puts them. */
	private static void people(ServerLevel level) {
		for (HubMap.Person person : HubMap.PEOPLE) {
			Npcs.spawnVillager(level, CENTRE.offset(person.x(), person.y(), person.z()),
					person.name());
		}
		// The three shopkeepers stand inside their own buildings, so the
		// Auction House and Bazaar Alley are places you go to do something
		// rather than empty shells.
		for (HubMap.Place place : HubMap.BUILDINGS) {
			BlockPos inside = CENTRE.offset(place.x(), place.y() + 1, place.z() + 3);
			switch (place.name()) {
				case "Bank" -> Npcs.spawnBanker(level, inside);
				case "Auction House" -> Npcs.spawnVillager(level, inside, Npcs.AUCTIONEER);
				case "Bazaar Alley" -> Npcs.spawnVillager(level, inside, Npcs.BAZAAR);
				default -> {
				}
			}
		}
	}

	/**
	 * Paths joining the buildings up.
	 *
	 * A road runs to each one from the portal, which is crude next to a
	 * hand-laid street plan but does the job the streets do: it stops the
	 * Village being a field with sheds dotted about in it.
	 */
	private static void roads(ServerLevel level) {
		for (HubMap.Place place : HubMap.BUILDINGS) {
			road(level, 0, 0, place.x(), place.z());
		}
		// Lamps down the two main lines out of the portal.
		for (int d = -10; d >= HubMap.northEdge() + 10; d -= 12) {
			lamp(level, CENTRE.offset(4, 0, d));
			lamp(level, CENTRE.offset(-4, 0, d));
		}
	}

	/**
	 * A rectangular building: walls, a roof, a doorway and lights inside.
	 *
	 * Every building in the Hub is one of these with different materials and
	 * sizes, which keeps them consistent and the code short.
	 */
	private static void hall(ServerLevel level, BlockPos corner, int w, int d, int h,
			net.minecraft.world.level.block.Block wallBlock,
			net.minecraft.world.level.block.Block roofBlock) {
		BlockState wall = wallBlock.defaultBlockState();
		BlockState beam = Blocks.OAK_LOG.defaultBlockState();

		// --- walls, with log beams at the corners -----------------------------
		for (int dx = 0; dx < w; dx++) {
			for (int dz = 0; dz < d; dz++) {
				boolean edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1;
				boolean atCorner = (dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1);
				for (int y = 0; y < h; y++) {
					if (edge) {
						level.setBlockAndUpdate(corner.offset(dx, y, dz), atCorner ? beam : wall);
					} else {
						level.setBlockAndUpdate(corner.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
					}
				}
			}
		}
		// A beam along the top of the walls, which is what gives village
		// buildings their timbered look rather than a plain stone box.
		for (int dx = 0; dx < w; dx++) {
			level.setBlockAndUpdate(corner.offset(dx, h - 1, 0), beam);
			level.setBlockAndUpdate(corner.offset(dx, h - 1, d - 1), beam);
		}
		for (int dz = 0; dz < d; dz++) {
			level.setBlockAndUpdate(corner.offset(0, h - 1, dz), beam);
			level.setBlockAndUpdate(corner.offset(w - 1, h - 1, dz), beam);
		}

		// --- a pitched roof, in stairs ----------------------------------------
		// This is the biggest single difference between something that reads as
		// a building and something that reads as a box. It rises from both long
		// sides to a ridge down the middle.
		net.minecraft.world.level.block.Block stairBlock = stairsFor(roofBlock);
		int peak = (d + 1) / 2;
		for (int step = 0; step < peak; step++) {
			int y = h + step;
			for (int dx = -1; dx <= w; dx++) {
				BlockPos north = corner.offset(dx, y, step - 1);
				BlockPos south = corner.offset(dx, y, d - step);
				level.setBlockAndUpdate(north, stairBlock.defaultBlockState()
						.setValue(net.minecraft.world.level.block.StairBlock.FACING,
								net.minecraft.core.Direction.SOUTH));
				level.setBlockAndUpdate(south, stairBlock.defaultBlockState()
						.setValue(net.minecraft.world.level.block.StairBlock.FACING,
								net.minecraft.core.Direction.NORTH));
				// Fill under the slope so you can't see daylight through it.
				level.setBlockAndUpdate(corner.offset(dx, y, step), roofBlock.defaultBlockState());
				level.setBlockAndUpdate(corner.offset(dx, y, d - 1 - step), roofBlock.defaultBlockState());
			}
		}
		// Cap the ridge.
		for (int dx = -1; dx <= w; dx++) {
			level.setBlockAndUpdate(corner.offset(dx, h + peak - 1, peak - 1),
					roofBlock.defaultBlockState());
		}

		// --- door, windows and light ------------------------------------------
		int door = w / 2;
		for (int dx = door - 1; dx <= door + 1; dx++) {
			for (int y = 0; y < 3; y++) {
				level.setBlockAndUpdate(corner.offset(dx, y, 0), Blocks.AIR.defaultBlockState());
			}
		}
		// Lanterns either side of the doorway, the way a village entrance has.
		level.setBlockAndUpdate(corner.offset(door - 2, 2, 0), Blocks.LANTERN.defaultBlockState());
		level.setBlockAndUpdate(corner.offset(door + 2, 2, 0), Blocks.LANTERN.defaultBlockState());

		for (int dx = 2; dx < w - 2; dx += 3) {
			for (int y = 2; y <= 3; y++) {
				if (dx >= door - 1 && dx <= door + 1) {
					continue;                        // don't glaze over the door
				}
				level.setBlockAndUpdate(corner.offset(dx, y, 0), Blocks.GLASS_PANE.defaultBlockState());
				level.setBlockAndUpdate(corner.offset(dx, y, d - 1), Blocks.GLASS_PANE.defaultBlockState());
			}
		}
		for (int dz = 2; dz < d - 2; dz += 3) {
			for (int y = 2; y <= 3; y++) {
				level.setBlockAndUpdate(corner.offset(0, y, dz), Blocks.GLASS_PANE.defaultBlockState());
				level.setBlockAndUpdate(corner.offset(w - 1, y, dz), Blocks.GLASS_PANE.defaultBlockState());
			}
		}
		// Lit inside so nothing spawns and it reads as occupied.
		for (int dx = 2; dx < w - 1; dx += 3) {
			for (int dz = 2; dz < d - 1; dz += 3) {
				level.setBlockAndUpdate(corner.offset(dx, h - 2, dz), Blocks.SEA_LANTERN.defaultBlockState());
			}
		}
	}

	/**
	 * The stair block that goes with a roof material.
	 *
	 * Vanilla has no way to ask a block for its stair form, so the pairs are
	 * listed. Anything unknown falls back to oak, which never looks wrong.
	 */
	private static net.minecraft.world.level.block.Block stairsFor(
			net.minecraft.world.level.block.Block roof) {
		if (roof == Blocks.DARK_OAK_PLANKS) {
			return Blocks.DARK_OAK_STAIRS;
		}
		if (roof == Blocks.SPRUCE_PLANKS) {
			return Blocks.SPRUCE_STAIRS;
		}
		if (roof == Blocks.OAK_PLANKS) {
			return Blocks.OAK_STAIRS;
		}
		if (roof == Blocks.STONE_BRICKS) {
			return Blocks.STONE_BRICK_STAIRS;
		}
		if (roof == Blocks.BRICKS) {
			return Blocks.BRICK_STAIRS;
		}
		return Blocks.OAK_STAIRS;
	}

	private static void lamp(ServerLevel level, BlockPos foot) {
		level.setBlockAndUpdate(foot, Blocks.POLISHED_ANDESITE.defaultBlockState());
		level.setBlockAndUpdate(foot.above(), Blocks.POLISHED_ANDESITE.defaultBlockState());
		level.setBlockAndUpdate(foot.above(2), Blocks.SEA_LANTERN.defaultBlockState());
	}

	// ------------------------------------------------------------------- roads

	/** A straight five-wide road between two points in the plaza's plane. */
	private static void road(ServerLevel level, int x1, int z1, int x2, int z2) {
		int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
		for (int i = 0; i <= steps; i++) {
			int x = x1 + (x2 - x1) * i / steps;
			int z = z1 + (z2 - z1) * i / steps;
			for (int w = -2; w <= 2; w++) {
				boolean sideways = Math.abs(x2 - x1) > Math.abs(z2 - z1);
				BlockPos at = CENTRE.offset(sideways ? x : x + w, -1, sideways ? z + w : z);
				level.setBlockAndUpdate(at, Blocks.POLISHED_ANDESITE.defaultBlockState());
			}
		}
	}

	/** The same, for the two districts that sit off the compass points. */
	private static void roadDiagonal(ServerLevel level, int x1, int z1, int x2, int z2) {
		int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1));
		for (int i = 0; i <= steps; i++) {
			int x = x1 + (x2 - x1) * i / steps;
			int z = z1 + (z2 - z1) * i / steps;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					level.setBlockAndUpdate(CENTRE.offset(x + dx, -1, z + dz),
							Blocks.POLISHED_ANDESITE.defaultBlockState());
				}
			}
		}
	}

	// --------------------------------------------------------------- districts

	/** Forward: the Mining District, a stone hill you can walk into. */
	private static void mining(ServerLevel level, BlockPos middle) {
		ground(level, middle, 20, Blocks.STONE);

		for (int dx = -14; dx <= 14; dx++) {
			for (int dz = -14; dz <= 14; dz++) {
				int dist = (int) Math.sqrt(dx * dx + dz * dz);
				if (dist > 14) {
					continue;
				}
				for (int y = 0; y < 14 - dist; y++) {
					level.setBlockAndUpdate(middle.offset(dx, y, dz), Blocks.STONE.defaultBlockState());
				}
			}
		}
		// Ore through the hill, placed by a fixed pattern so every world matches.
		BlockState[] ores = {
				Blocks.COAL_ORE.defaultBlockState(), Blocks.IRON_ORE.defaultBlockState(),
				Blocks.COPPER_ORE.defaultBlockState(), Blocks.GOLD_ORE.defaultBlockState(),
				Blocks.REDSTONE_ORE.defaultBlockState(), Blocks.LAPIS_ORE.defaultBlockState(),
				Blocks.DIAMOND_ORE.defaultBlockState(),
		};
		int n = 0;
		for (int dx = -12; dx <= 12; dx += 3) {
			for (int dz = -12; dz <= 12; dz += 3) {
				for (int y = 1; y < 10; y += 3) {
					BlockPos spot = middle.offset(dx, y, dz);
					if (level.getBlockState(spot).is(Blocks.STONE)) {
						level.setBlockAndUpdate(spot, ores[(n % 19 == 0) ? 6 : (n % 5)]);
					}
					n++;
				}
			}
		}
		// The way in, from the village side.
		for (int dz = 0; dz <= 16; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int y = 0; y < 3; y++) {
					level.setBlockAndUpdate(middle.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	/**
	 * Middle-left: the Combat Settlement, with the Graveyard behind it.
	 *
	 * The wiki lists what lives here: Rosetta and the Weaponsmith selling
	 * early weapons and armour, Jax at an Archery Range, and the
	 * Thaumaturgist. So there are three buildings and a row of targets, not
	 * one shed.
	 */
	private static void combat(ServerLevel level, BlockPos middle) {
		ground(level, middle, 24, Blocks.COARSE_DIRT);

		hall(level, middle.offset(-6, 0, -4), 13, 10, 7,
				Blocks.COBBLESTONE, Blocks.DARK_OAK_PLANKS);        // Weaponsmith
		hall(level, middle.offset(9, 0, -4), 9, 8, 6,
				Blocks.DEEPSLATE_BRICKS, Blocks.DARK_OAK_PLANKS);   // Thaumaturgist

		// The Archery Range: a line of targets to shoot at.
		for (int i = 0; i < 4; i++) {
			BlockPos target = middle.offset(-14, 0, 6 + i * 3);
			for (int y = 0; y < 3; y++) {
				level.setBlockAndUpdate(target.above(y), Blocks.HAY_BLOCK.defaultBlockState());
			}
			level.setBlockAndUpdate(target.above(1).offset(1, 0, 0), Blocks.TARGET.defaultBlockState());
		}

		// The Graveyard, further out: rows of headstones, and at the back of it
		// the public portal to the Spider's Den -- which is how the wiki says
		// you get there.
		BlockPos graves = middle.offset(0, 0, -26);
		ground(level, graves, 16, Blocks.COARSE_DIRT);
		for (int dx = -8; dx <= 8; dx += 4) {
			for (int dz = -8; dz <= 8; dz += 4) {
				level.setBlockAndUpdate(graves.offset(dx, 0, dz), Blocks.STONE_BRICKS.defaultBlockState());
				level.setBlockAndUpdate(graves.offset(dx, 1, dz), Blocks.STONE_BRICK_WALL.defaultBlockState());
			}
		}
		Portals.frame(level, graves.offset(0, 1, -12));
		BlockPos den = graves.offset(0, 0, -60);
		spidersDen(level, den);
		// The named spiders the wiki lists, rather than whatever the dark makes.
		Spiders.populate(level, den);
	}

	/**
	 * The Spider's Den, through the portal at the back of the Graveyard.
	 *
	 * A dark mound riddled with cobwebs. Deliberately unlit, because unlike
	 * the Village this is somewhere things are meant to spawn.
	 */
	private static void spidersDen(ServerLevel level, BlockPos middle) {
		ground(level, middle, 26, Blocks.COARSE_DIRT);

		for (int dx = -16; dx <= 16; dx++) {
			for (int dz = -16; dz <= 16; dz++) {
				int dist = (int) Math.sqrt(dx * dx + dz * dz);
				if (dist > 16) {
					continue;
				}
				for (int y = 0; y < (16 - dist) / 2; y++) {
					level.setBlockAndUpdate(middle.offset(dx, y, dz), Blocks.STONE.defaultBlockState());
				}
			}
		}
		// Webs through it, and a hollow to walk into.
		for (int dx = -10; dx <= 10; dx += 3) {
			for (int dz = -10; dz <= 10; dz += 3) {
				BlockPos web = middle.offset(dx, 1, dz);
				if (level.getBlockState(web).isAir()) {
					level.setBlockAndUpdate(web, Blocks.COBWEB.defaultBlockState());
				}
			}
		}
		for (int dz = 0; dz <= 16; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int y = 0; y < 3; y++) {
					level.setBlockAndUpdate(middle.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	/** Left: the Forest, leading on to the Park. Trees to chop. */
	private static void forest(ServerLevel level, BlockPos middle) {
		ground(level, middle, 24, Blocks.GRASS_BLOCK);

		for (int dx = -18; dx <= 18; dx += 6) {
			for (int dz = -18; dz <= 18; dz += 6) {
				if ((dx / 6 + dz / 6) % 2 == 0) {
					tree(level, middle.offset(dx, 1, dz));
				}
			}
		}
		// A couple of huts, as the Park has.
		hall(level, middle.offset(-4, 1, 12), 7, 6, 5, Blocks.OAK_PLANKS, Blocks.OAK_PLANKS);
	}

	/** Right: the Fishing Outpost — a pond and a dock. */
	private static void fishing(ServerLevel level, BlockPos middle) {
		ground(level, middle, 22, Blocks.STONE);

		for (int dx = -14; dx <= 14; dx++) {
			for (int dz = -14; dz <= 14; dz++) {
				if (dx * dx + dz * dz < 169) {
					level.setBlockAndUpdate(middle.offset(dx, -1, dz), Blocks.WATER.defaultBlockState());
				}
			}
		}
		for (int dx = -14; dx <= 0; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				level.setBlockAndUpdate(middle.offset(dx, 0, dz), Blocks.OAK_PLANKS.defaultBlockState());
			}
		}
		level.setBlockAndUpdate(middle.offset(-14, 1, 0), Blocks.SEA_LANTERN.defaultBlockState());
	}

	/**
	 * Left and forward: the Bank.
	 *
	 * Two real coordinates anchor this, both from the wiki: the Bank itself at
	 * (-20, 71, -65) from spawn, and Banker Broadjaw at (-29.5, 72, -38). They
	 * are 27 blocks apart, which says the building is long and that he stands
	 * near the entrance rather than at the middle -- so it is built to span
	 * both, with the door at his end.
	 *
	 * Two things inside are real, from the wiki: Banker Broadjaw stands here,
	 * and the Personal Vault is "to your right" as you walk in, which is why
	 * the iron-walled room sits against the east wall by the door.
	 *
	 * The rest of the architecture is mine. No wiki records what the Bank
	 * actually looks like, so the two floors, the counter and the gold inside
	 * the vault are invention; the positions are what's real.
	 */
	private static void bank(ServerLevel level) {
		// A Bank you built yourself wins over this one. Build it, highlight it
		// with a stick, /blueprint save bank -- and it appears here instead.
		if (Custom.place(level, CENTRE.offset(-27, 0, -47), "bank")) {
			Npcs.spawnBanker(level, CENTRE.offset(-27, 1, -44));
			return;
		}
		BlockState wall = Blocks.SMOOTH_STONE.defaultBlockState();
		BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
		BlockState floorBlock = Blocks.POLISHED_ANDESITE.defaultBlockState();

		// Small enough to feel like a building rather than a warehouse:
		// 13 wide, 19 deep, one storey with a raised roof.
		int west = -33;
		int east = -21;
		int south = -56;
		int north = -38;
		int height = 8;

		ground(level, CENTRE.offset((west + east) / 2, 0, (north + south) / 2), 17, Blocks.STONE_BRICKS);

		for (int x = west; x <= east; x++) {
			for (int z = south; z <= north; z++) {
				boolean edge = x == west || x == east || z == south || z == north;
				level.setBlockAndUpdate(CENTRE.offset(x, -1, z), floorBlock);
				for (int y = 0; y < height; y++) {
					if (edge) {
						boolean corner = (x == west || x == east) && (z == south || z == north);
						level.setBlockAndUpdate(CENTRE.offset(x, y, z), corner ? gold : wall);
					} else {
						level.setBlockAndUpdate(CENTRE.offset(x, y, z), Blocks.AIR.defaultBlockState());
					}
				}
			}
		}

		// A pitched gold roof, so the Bank reads as a building rather than a
		// gold-lidded box -- and stays the grandest thing in the Hub.
		int span = (north - south + 1 + 1) / 2;
		for (int step = 0; step < span; step++) {
			int y = height + step;
			for (int x = west - 1; x <= east + 1; x++) {
				level.setBlockAndUpdate(CENTRE.offset(x, y, south + step - 1),
						Blocks.CUT_COPPER_STAIRS.defaultBlockState()
								.setValue(net.minecraft.world.level.block.StairBlock.FACING,
										net.minecraft.core.Direction.NORTH));
				level.setBlockAndUpdate(CENTRE.offset(x, y, north - step + 1),
						Blocks.CUT_COPPER_STAIRS.defaultBlockState()
								.setValue(net.minecraft.world.level.block.StairBlock.FACING,
										net.minecraft.core.Direction.SOUTH));
				level.setBlockAndUpdate(CENTRE.offset(x, y, south + step), gold);
				level.setBlockAndUpdate(CENTRE.offset(x, y, north - step), gold);
			}
		}

		// Entrance in the south wall, with a gold pillar either side.
		for (int x = -29; x <= -25; x++) {
			for (int y = 0; y < 4; y++) {
				level.setBlockAndUpdate(CENTRE.offset(x, y, north), Blocks.AIR.defaultBlockState());
			}
		}
		for (int y = 0; y < 5; y++) {
			level.setBlockAndUpdate(CENTRE.offset(-30, y, north + 1), gold);
			level.setBlockAndUpdate(CENTRE.offset(-24, y, north + 1), gold);
		}

		// Windows down both long walls.
		for (int z = south + 3; z < north - 1; z += 4) {
			for (int y = 2; y <= 3; y++) {
				level.setBlockAndUpdate(CENTRE.offset(west, y, z), Blocks.GLASS.defaultBlockState());
				level.setBlockAndUpdate(CENTRE.offset(east, y, z), Blocks.GLASS.defaultBlockState());
			}
		}

		// The counter the Banker stands behind.
		for (int x = -31; x <= -26; x++) {
			level.setBlockAndUpdate(CENTRE.offset(x, 0, north - 5), Blocks.SMOOTH_STONE.defaultBlockState());
			level.setBlockAndUpdate(CENTRE.offset(x, 1, north - 5), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
		}

		// --- the vault, on your right as you walk in ---------------------------
		// The wiki is specific: "you can find the Personal Vault by looking to
		// your right". You come in facing north, so right is east.
		int vaultWest = east - 6;
		int vaultSouth = north - 3;
		int vaultNorth = north - 10;

		for (int x = vaultWest; x < east; x++) {
			for (int z = vaultNorth; z <= vaultSouth; z++) {
				if (x != vaultWest && z != vaultNorth && z != vaultSouth) {
					continue;
				}
				for (int y = 0; y < 5; y++) {
					level.setBlockAndUpdate(CENTRE.offset(x, y, z), Blocks.IRON_BLOCK.defaultBlockState());
				}
			}
		}
		for (int y = 0; y < 3; y++) {                                    // vault doorway
			level.setBlockAndUpdate(CENTRE.offset(vaultWest, y, vaultSouth - 3), Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(CENTRE.offset(vaultWest, y, vaultSouth - 4), Blocks.AIR.defaultBlockState());
		}
		for (int x = vaultWest + 2; x < east; x += 2) {                  // gold inside
			for (int z = vaultNorth + 2; z < vaultSouth - 1; z += 2) {
				level.setBlockAndUpdate(CENTRE.offset(x, 0, z), gold);
			}
		}

		// --- lighting -----------------------------------------------------------
		// Deliberately heavy. Monsters spawn in the dark, and nothing should
		// ever be waiting for you inside a bank -- so every part of the floor
		// is lit well past the level anything can spawn at.
		for (int x = west + 1; x < east; x += 3) {
			for (int z = south + 1; z < north; z += 3) {
				level.setBlockAndUpdate(CENTRE.offset(x, height - 2, z), Blocks.SEA_LANTERN.defaultBlockState());
			}
		}
		for (int z = south + 2; z < north; z += 5) {                     // wall lamps too
			level.setBlockAndUpdate(CENTRE.offset(west + 1, 3, z), Blocks.GLOWSTONE.defaultBlockState());
			level.setBlockAndUpdate(CENTRE.offset(east - 1, 3, z), Blocks.GLOWSTONE.defaultBlockState());
		}

		// Banker Broadjaw, behind his counter, facing the door.
		Npcs.spawnBanker(level, CENTRE.offset(-28, 0, north - 6));
	}

	/** A flat circle of ground for a district to sit on. */
	private static void ground(ServerLevel level, BlockPos middle, int radius,
			net.minecraft.world.level.block.Block surface) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					continue;
				}
				level.setBlockAndUpdate(middle.offset(dx, -1, dz), surface.defaultBlockState());
				level.setBlockAndUpdate(middle.offset(dx, -2, dz), Blocks.DIRT.defaultBlockState());
			}
		}
	}

	/** A full-size oak. */
	private static void tree(ServerLevel level, BlockPos foot) {
		for (int y = 0; y < 6; y++) {
			level.setBlockAndUpdate(foot.above(y), Blocks.OAK_LOG.defaultBlockState());
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int y = 4; y <= 6; y++) {
					if ((dx == 0 && dz == 0 && y < 6) || (Math.abs(dx) == 2 && Math.abs(dz) == 2)) {
						continue;
					}
					BlockPos leaf = foot.offset(dx, y, dz);
					if (level.getBlockState(leaf).isAir()) {
						level.setBlockAndUpdate(leaf, Blocks.OAK_LEAVES.defaultBlockState());
					}
				}
			}
		}
	}
}
