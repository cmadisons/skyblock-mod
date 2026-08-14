package com.example;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Where everything in the Hub actually is.
 *
 * These coordinates are real. They come from a community guide that lists the
 * Hub's buildings and residents by position, so this is not a guess at the
 * layout -- it is the layout, measured from spawn.
 *
 * The real Hub's ground sits at y 70 and ours at the Village floor, so a
 * building's y here is stored as the difference: 71 becomes +1, 68 becomes -2.
 * That keeps the relative heights right -- the Fish Shop really is lower than
 * the Bank -- without pinning us to Mojang's numbers.
 *
 * What is real and what is not
 * ----------------------------
 * Real: every name, and every x, y and z.
 * Not real: how big each building is and what it is made of. Nothing records
 * that, so the sizes and materials below are chosen to suit what the building
 * is for. A blueprint you save replaces them -- see {@link Custom}.
 */
public final class HubMap {
	private HubMap() {
	}

	/** One building: where it goes, how big it is, what it's made of. */
	public record Place(String name, String blueprint, int x, int y, int z,
			int width, int depth, int height, Block wall, Block roof) {
	}

	/** One resident: a name and a spot to stand. */
	public record Person(String name, int x, int y, int z) {
	}

	/**
	 * The buildings, at their measured positions.
	 *
	 * The second field is the blueprint name that replaces it, or an empty
	 * string for buildings you can't yet override.
	 */
	public static final Place[] BUILDINGS = {
			// --- the essentials --------------------------------------------------
			new Place("Bank", "bank", -20, 1, -65, 15, 13, 8,
					Blocks.SMOOTH_STONE, Blocks.GOLD_BLOCK),
			new Place("Auction House", "auction_house", -25, 2, -90, 15, 12, 8,
					Blocks.SMOOTH_STONE, Blocks.DARK_OAK_PLANKS),
			new Place("Community Center", "community_center", 4, 1, -97, 19, 14, 10,
					Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS),
			new Place("Bazaar Alley", "bazaar_alley", -32, 1, -76, 13, 11, 7,
					Blocks.DEEPSLATE_BRICKS, Blocks.SPRUCE_PLANKS),
			new Place("Blacksmith", "", -28, -1, -125, 13, 11, 7,
					Blocks.COBBLESTONE, Blocks.DARK_OAK_PLANKS),
			new Place("Rune-crafting Room", "", -37, -1, -128, 9, 9, 6,
					Blocks.DEEPSLATE_BRICKS, Blocks.DARK_OAK_PLANKS),

			// --- the shops --------------------------------------------------------
			new Place("Builder Shop", "builders_house", -48, 0, -34, 11, 9, 6,
					Blocks.COBBLESTONE, Blocks.OAK_PLANKS),
			new Place("Farm Shop", "", 16, 0, -71, 11, 9, 6,
					Blocks.OAK_PLANKS, Blocks.OAK_PLANKS),
			new Place("Pet Shop", "pet_care", 24, 0, -90, 11, 9, 6,
					Blocks.OAK_PLANKS, Blocks.OAK_PLANKS),
			new Place("Fish Shop", "", 52, -2, -82, 11, 9, 6,
					Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS),
			new Place("Armor & Weapon Shop", "", -15, -2, -133, 13, 11, 7,
					Blocks.STONE_BRICKS, Blocks.DARK_OAK_PLANKS),
			new Place("Adventure & Lumber Shop", "", -41, 0, -70, 11, 9, 6,
					Blocks.OAK_PLANKS, Blocks.OAK_PLANKS),

			// --- everything else --------------------------------------------------
			new Place("Carpenter", "", 15, 2, -21, 11, 9, 6,
					Blocks.OAK_PLANKS, Blocks.OAK_PLANKS),
			new Place("Marco's House", "", -9, 1, -13, 9, 9, 6,
					Blocks.BRICKS, Blocks.OAK_PLANKS),
			new Place("Alchemist", "", 41, 0, -63, 11, 9, 7,
					Blocks.DEEPSLATE_BRICKS, Blocks.SPRUCE_PLANKS),
			new Place("Library", "museum", -35, -1, -122, 15, 12, 8,
					Blocks.BRICKS, Blocks.DARK_OAK_PLANKS),
			new Place("Wool Weaver", "", -76, 6, -76, 11, 9, 6,
					Blocks.OAK_PLANKS, Blocks.OAK_PLANKS),
			new Place("Colosseum", "", 84, 0, -48, 25, 25, 12,
					Blocks.STONE_BRICKS, Blocks.STONE_BRICKS),

			// --- the player houses ------------------------------------------------
			new Place("Minikloon's House", "", 83, 2, -177, 13, 11, 8,
					Blocks.QUARTZ_BLOCK, Blocks.DARK_OAK_PLANKS),
			new Place("Donpireso's House", "", 70, 0, -62, 11, 9, 7,
					Blocks.OAK_PLANKS, Blocks.OAK_PLANKS),
			new Place("Vinny8ball's House", "", 62, 1, -115, 11, 9, 7,
					Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS),
			new Place("Sylent_'s House", "", -78, 0, -69, 11, 9, 7,
					Blocks.BIRCH_PLANKS, Blocks.OAK_PLANKS),
			new Place("Relenter's House", "", -75, 0, -95, 11, 9, 7,
					Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS),
			new Place("Nitroholic_'s House", "", -64, 0, -116, 11, 9, 7,
					Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_PLANKS),
	};

	/** The residents, at their measured positions. */
	public static final Person[] PEOPLE = {
			new Person("Duke", -6, 0, -89),
			new Person("Vex", -16, 0, -81),
			new Person("Jack", 0, 0, -54),
			new Person("Leo", -7, 0, -75),
			new Person("Tom", 28, -1, -57),
			new Person("Andrew", 38, -2, -46),
			new Person("Lynn", -21, -2, -124),
			new Person("Felix", -25, -2, -103),
			new Person("Stella", 17, 0, -100),
			new Person("Ryu", 27, 0, -116),
			new Person("Liam", -75, 0, -107),
			new Person("Jamie", -35, 0, -36),
	};

	/**
	 * How far the ground has to reach to hold all of it.
	 *
	 * Worked out from the positions above rather than written down, so adding a
	 * building to the list extends the ground automatically instead of leaving
	 * it hanging over the void.
	 */
	public static int westEdge() {
		int edge = 0;
		for (Place place : BUILDINGS) {
			edge = Math.min(edge, place.x() - place.width());
		}
		return edge - 12;
	}

	public static int eastEdge() {
		int edge = 0;
		for (Place place : BUILDINGS) {
			edge = Math.max(edge, place.x() + place.width());
		}
		return edge + 12;
	}

	public static int northEdge() {
		int edge = 0;
		for (Place place : BUILDINGS) {
			edge = Math.min(edge, place.z() - place.depth());
		}
		return edge - 12;
	}

	public static int southEdge() {
		return 20;                       // a little apron behind the portal
	}
}
