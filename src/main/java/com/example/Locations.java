package com.example;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Every place in SkyBlock, as data.
 *
 * The game has well over a hundred locations. Hand-building them all is not
 * something anybody is going to finish, so instead each one is described here --
 * its name, what it takes to get in, what you can gather there, who lives there
 * and what wants to kill you -- and {@link Warps} builds it from that
 * description the first time you visit.
 *
 * That is the trade, said plainly: every location is real, reachable, correctly
 * gated, and stocked with the right resources, NPCs and enemies. None of them
 * is hand-crafted architecture. A generated Gold Mine is a stone island with
 * coal, iron and gold in it and Lazy Miner standing on it -- which is what the
 * Gold Mine is for, without being what the Gold Mine looks like.
 *
 * Anything you would rather have properly built, build it yourself and save it
 * with the Blueprint mod. It replaces the generated one.
 *
 * Requirements are the real ones from the game's own location table. Skill
 * names are matched to the closest of our seven, since we do not have every
 * skill Hypixel does -- Heart of the Mountain becomes Mining, for instance.
 */
public final class Locations {
	private Locations() {
	}

	/** What the ground of a place is made of, which sets how it reads. */
	public enum Ground {
		GRASS, STONE, SAND, END_STONE, NETHERRACK, ICE, WATER, MYCELIUM, OBSIDIAN
	}

	/**
	 * One place.
	 *
	 * @param skill    which skill gates it, or empty for none
	 * @param level    the level that skill needs
	 * @param gathers  what you can collect there
	 * @param people   who stands there
	 * @param enemies  what spawns there, or null for somewhere safe
	 */
	public record Place(String name, Ground ground, int size, String skill, int level,
			Block[] gathers, String[] people, Mobs.Kind[] enemies) {

		/** Is this place open to somebody with these levels? */
		public boolean openTo(int has) {
			return skill.isEmpty() || has >= level;
		}
	}

	private static final Block[] NOTHING = {};
	private static final String[] NOBODY = {};

	/**
	 * The places, in roughly the order you would see them.
	 *
	 * Names, requirements, resources and NPCs are all from the game's location
	 * table. Where a place lists no resources or people, it gets none.
	 */
	public static final Place[] ALL = {
			// --- the start ------------------------------------------------------
			new Place("Private Island", Ground.GRASS, 12, "", 0,
					new Block[]{Blocks.OAK_LOG, Blocks.OAK_LEAVES},
					new String[]{"Jerry"}, null),

			// --- the Hub and its rooms -------------------------------------------
			new Place("Village", Ground.STONE, 30, "", 0, NOTHING,
					new String[]{"Alda", "Karis", "Seymour", "Marco"}, null),
			new Place("Auction House", Ground.STONE, 14, "", 0, NOTHING,
					new String[]{"Auction Master", "Auction Agent"}, null),
			new Place("Bank", Ground.STONE, 14, "", 0, NOTHING,
					new String[]{"Banker"}, null),
			new Place("Bazaar Alley", Ground.STONE, 14, "", 0, NOTHING,
					new String[]{"Bazaar", "Bazaar Agent"}, null),
			new Place("Builder's House", Ground.STONE, 12, "", 0, NOTHING,
					new String[]{"Builder", "Wool Weaver", "Amelia", "Christopher"}, null),
			new Place("Community Center", Ground.STONE, 16, "", 0, NOTHING,
					new String[]{"Elizabeth", "Seraphine", "Biblio"}, null),
			new Place("Museum", Ground.STONE, 14, "", 0, NOTHING,
					new String[]{"Curator", "Madame Goldsworth III"}, null),
			new Place("Pet Care", Ground.GRASS, 12, "", 0, NOTHING,
					new String[]{"Bea", "Kat", "Fann", "George", "Zog"}, null),
			new Place("Flower House", Ground.GRASS, 10, "", 0, NOTHING,
					new String[]{"Marco"}, null),
			new Place("Fashion Shop", Ground.STONE, 10, "", 0, NOTHING,
					new String[]{"Seymour"}, null),
			new Place("Rabbit House", Ground.GRASS, 12, "", 0, NOTHING,
					new String[]{"Hoppity", "Coach Jackrabbit", "Finance Rabbit"}, null),
			new Place("Sewer", Ground.STONE, 12, "", 0, NOTHING, NOBODY, null),
			new Place("Colosseum", Ground.STONE, 20, "", 0, NOTHING, NOBODY, null),
			new Place("Election Room", Ground.STONE, 12, "", 0, NOTHING, NOBODY, null),
			new Place("Canvas Room", Ground.STONE, 10, "", 0, NOTHING, NOBODY, null),
			new Place("Carnival", Ground.GRASS, 18, "", 0, NOTHING, NOBODY, null),
			new Place("Tavern", Ground.STONE, 12, "", 0, NOTHING,
					new String[]{"Bartender", "Maddox the Slayer"}, null),
			new Place("Library", Ground.STONE, 12, "", 0, NOTHING,
					new String[]{"Librarian"}, null),
			new Place("Thaumaturgist", Ground.STONE, 12, "", 0, NOTHING,
					new String[]{"Jacobus", "Ozanne", "Maxwell", "Adventurer"}, null),
			new Place("Trade Center", Ground.STONE, 12, "", 0, NOTHING,
					new String[]{"Liz", "Richard", "Zarina"}, null),
			new Place("Wizard Tower", Ground.STONE, 14, "", 0, NOTHING,
					new String[]{"Elise", "Nicole", "Erihann", "Udium", "Grumblefoot"}, null),
			new Place("Wilderness", Ground.GRASS, 20, "", 0, NOTHING,
					new String[]{"Tia the Fairy", "Fisherman", "Lucius", "Shifty"}, null),
			new Place("Dark Auction", Ground.OBSIDIAN, 14, "", 0, NOTHING,
					new String[]{"Sirius", "Bob"}, null),

			// --- combat ----------------------------------------------------------
			new Place("Combat Settlement", Ground.STONE, 20, "", 0, NOTHING,
					new String[]{"Jax", "Rosetta", "Weaponsmith"}, null),
			new Place("Archery Range", Ground.GRASS, 14, "", 0, NOTHING,
					new String[]{"Jax"}, null),
			new Place("Graveyard", Ground.GRASS, 18, "", 0,
					new Block[]{Blocks.COARSE_DIRT},
					new String[]{"Pat", "Romero"}, Mobs.GRAVEYARD),
			new Place("Hub Crypts", Ground.STONE, 16, "", 0, NOTHING, NOBODY, Mobs.GRAVEYARD),
			new Place("Ruins", Ground.GRASS, 18, "", 0,
					new Block[]{Blocks.BONE_BLOCK},
					new String[]{"Lonely Philosopher"}, Mobs.FOREST),

			// --- farming ---------------------------------------------------------
			new Place("Farm", Ground.GRASS, 18, "", 0,
					new Block[]{Blocks.WHEAT, Blocks.HAY_BLOCK},
					new String[]{"Farmer", "Arthur", "Shania", "Alchemist"}, null),
			new Place("Farmhouse", Ground.GRASS, 12, "", 0, NOTHING,
					new String[]{"Anita", "Jacob"}, null),
			new Place("The Barn", Ground.GRASS, 22, Skills.FARMING, 1,
					new Block[]{Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.MELON,
							Blocks.PUMPKIN},
					new String[]{"Farmhand", "Windmill Operator"}, Mobs.BARN),
			new Place("The Garden", Ground.GRASS, 20, Skills.FARMING, 5,
					new Block[]{Blocks.WHEAT, Blocks.CARROTS},
					new String[]{"Anita", "Jacob", "Sam", "Jeff"}, null),

			// --- foraging --------------------------------------------------------
			new Place("Forest", Ground.GRASS, 20, "", 0,
					new Block[]{Blocks.OAK_LOG, Blocks.OAK_LEAVES}, NOBODY, null),
			new Place("Foraging Camp", Ground.GRASS, 16, "", 0,
					new Block[]{Blocks.OAK_LOG, Blocks.OAK_LEAVES},
					new String[]{"Lumber Merchant", "Carpenter", "Lumber Jack"}, null),
			new Place("Birch Park", Ground.GRASS, 20, Skills.FORAGING, 1,
					new Block[]{Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES},
					new String[]{"Charlie", "Vanessa"}, null),
			new Place("Spruce Woods", Ground.GRASS, 20, Skills.FORAGING, 2,
					new Block[]{Blocks.SPRUCE_LOG, Blocks.SPRUCE_LEAVES},
					new String[]{"Kelly", "Gustave", "Melancholic Viking"}, null),
			new Place("Dark Thicket", Ground.GRASS, 20, Skills.FORAGING, 3,
					new Block[]{Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_LEAVES}, NOBODY, null),
			new Place("Howling Cave", Ground.STONE, 20, Skills.FORAGING, 3,
					new Block[]{Blocks.BONE_BLOCK, Blocks.OAK_LOG},
					new String[]{"Old Shaman Nyko"}, Mobs.FOREST),
			new Place("Savanna Woodland", Ground.GRASS, 20, Skills.FORAGING, 4,
					new Block[]{Blocks.ACACIA_LOG, Blocks.ACACIA_LEAVES},
					new String[]{"Worker Xavier", "Master Tactician Funk"}, null),
			new Place("Jungle Island", Ground.GRASS, 20, Skills.FORAGING, 5,
					new Block[]{Blocks.JUNGLE_LOG, Blocks.JUNGLE_LEAVES},
					new String[]{"Molbert", "Juliette"}, null),
			new Place("Soul Cave", Ground.STONE, 18, Skills.FORAGING, 5,
					new Block[]{Blocks.BONE_BLOCK}, NOBODY, Mobs.FOREST),
			new Place("Spirit Cave", Ground.STONE, 18, Skills.FORAGING, 6,
					new Block[]{Blocks.BONE_BLOCK}, NOBODY, Mobs.FOREST),
			new Place("Galatea", Ground.GRASS, 26, Skills.FORAGING, 12,
					new Block[]{Blocks.MANGROVE_LOG, Blocks.MANGROVE_LEAVES},
					new String[]{"Agatha", "Banker Bonsai", "Kiara", "Sawyer"}, null),

			// --- mining ----------------------------------------------------------
			new Place("Mining District", Ground.STONE, 20, "", 0,
					new Block[]{Blocks.STONE, Blocks.COAL_ORE},
					new String[]{"Dusk", "Blacksmith", "Mine Merchant"}, null),
			new Place("Coal Mine", Ground.STONE, 18, "", 0,
					new Block[]{Blocks.COAL_ORE, Blocks.COBBLESTONE}, NOBODY, null),
			// The only public island in SkyBlock with no mobs at all.
			new Place("Gold Mine", Ground.STONE, 22, Skills.MINING, 1,
					new Block[]{Blocks.COBBLESTONE, Blocks.COAL_ORE, Blocks.IRON_ORE,
							Blocks.GOLD_ORE, Blocks.STONE},
					new String[]{"Lazy Miner", "Gold Forger", "Iron Forger", "Rusty"}, null),
			new Place("Deep Caverns", Ground.STONE, 24, Skills.MINING, 5,
					new Block[]{Blocks.COBBLESTONE, Blocks.IRON_ORE, Blocks.GOLD_ORE},
					new String[]{"Lift Operator", "Walter"}, Mobs.DEEP_CAVERNS),
			new Place("Gunpowder Mines", Ground.STONE, 20, Skills.MINING, 5,
					new Block[]{Blocks.COAL_ORE, Blocks.STONE}, NOBODY, Mobs.DEEP_CAVERNS),
			new Place("Lapis Quarry", Ground.STONE, 20, Skills.MINING, 5,
					new Block[]{Blocks.LAPIS_ORE, Blocks.COBBLESTONE}, NOBODY, Mobs.DEEP_CAVERNS),
			new Place("Pigmen's Den", Ground.STONE, 20, Skills.MINING, 6,
					new Block[]{Blocks.REDSTONE_ORE, Blocks.COBBLESTONE}, NOBODY, Mobs.DEEP_CAVERNS),
			new Place("Slimehill", Ground.STONE, 20, Skills.MINING, 7,
					new Block[]{Blocks.EMERALD_ORE, Blocks.COBBLESTONE}, NOBODY, Mobs.DEEP_CAVERNS),
			new Place("Diamond Reserve", Ground.STONE, 20, Skills.MINING, 8,
					new Block[]{Blocks.DIAMOND_ORE, Blocks.COBBLESTONE}, NOBODY, Mobs.DEEP_CAVERNS),
			new Place("Obsidian Sanctuary", Ground.OBSIDIAN, 20, Skills.MINING, 9,
					new Block[]{Blocks.OBSIDIAN, Blocks.DIAMOND_BLOCK}, NOBODY, Mobs.DEEP_CAVERNS),
			new Place("Dwarven Mines", Ground.STONE, 26, Skills.MINING, 12,
					new Block[]{Blocks.STONE, Blocks.IRON_BLOCK},
					new String[]{"King", "Fetchur", "Rhys", "Lift Operator"}, Mobs.DEEP_CAVERNS),
			new Place("Crystal Hollows", Ground.STONE, 26, Skills.MINING, 15,
					new Block[]{Blocks.AMETHYST_BLOCK, Blocks.EMERALD_ORE, Blocks.DIAMOND_ORE},
					new String[]{"Emissary Sisko", "Gemma"}, Mobs.DEEP_CAVERNS),

			// --- combat islands ---------------------------------------------------
			new Place("Spider's Den", Ground.GRASS, 24, Skills.COMBAT, 1,
					new Block[]{Blocks.COBWEB, Blocks.STONE},
					new String[]{"Rick", "Haymitch", "Archaeologist"}, Mobs.SPIDERS_DEN),
			new Place("Spider Mound", Ground.STONE, 18, Skills.COMBAT, 1,
					new Block[]{Blocks.COBWEB}, NOBODY, Mobs.SPIDERS_DEN),
			new Place("Gravel Mines", Ground.STONE, 18, Skills.COMBAT, 1,
					new Block[]{Blocks.GRAVEL}, new String[]{"Rick", "Haymitch"}, null),
			new Place("Arachne's Burrow", Ground.STONE, 20, Skills.COMBAT, 3,
					new Block[]{Blocks.COBWEB}, NOBODY, Mobs.SPIDERS_DEN),
			new Place("Grandma's House", Ground.GRASS, 12, Skills.COMBAT, 1, NOTHING,
					new String[]{"Grandma Wolf", "Shaggy"}, null),
			new Place("The End", Ground.END_STONE, 26, Skills.COMBAT, 12,
					new Block[]{Blocks.END_STONE, Blocks.OBSIDIAN},
					new String[]{"Pearl Dealer", "Guber", "Tyzzo", "Gregory"}, Mobs.END),
			new Place("Dragon's Nest", Ground.END_STONE, 24, Skills.COMBAT, 12,
					new Block[]{Blocks.END_STONE}, NOBODY, Mobs.END),
			new Place("Zealot Bruiser Hideout", Ground.END_STONE, 20, Skills.COMBAT, 20,
					new Block[]{Blocks.END_STONE}, NOBODY, Mobs.END),
			new Place("Void Sepulture", Ground.END_STONE, 22, Skills.COMBAT, 25,
					new Block[]{Blocks.OBSIDIAN, Blocks.END_STONE}, NOBODY, Mobs.END),
			new Place("Crimson Isle", Ground.NETHERRACK, 26, Skills.COMBAT, 22,
					new Block[]{Blocks.NETHERRACK, Blocks.GLOWSTONE, Blocks.NETHER_QUARTZ_ORE},
					new String[]{"Elle", "Aura", "Odger", "Vulcan"}, Mobs.NETHER),
			new Place("Blazing Volcano", Ground.NETHERRACK, 22, Skills.COMBAT, 22,
					new Block[]{Blocks.MAGMA_BLOCK, Blocks.NETHERRACK}, NOBODY, Mobs.NETHER),
			new Place("Stronghold", Ground.NETHERRACK, 22, Skills.COMBAT, 22,
					new Block[]{Blocks.NETHER_BRICKS}, NOBODY, Mobs.NETHER),
			new Place("The Wasteland", Ground.NETHERRACK, 22, Skills.COMBAT, 22,
					new Block[]{Blocks.NETHERRACK, Blocks.SOUL_SAND}, NOBODY, Mobs.NETHER),
			new Place("Dragontail", Ground.NETHERRACK, 22, Skills.COMBAT, 22, NOTHING,
					new String[]{"Kuudra Gatekeeper", "Drakuu"}, null),
			new Place("Scarleton", Ground.NETHERRACK, 22, Skills.COMBAT, 22, NOTHING,
					new String[]{"Mage Council", "Igrupan"}, null),

			// --- farming islands --------------------------------------------------
			new Place("Mushroom Desert", Ground.SAND, 24, Skills.FARMING, 5,
					new Block[]{Blocks.SAND, Blocks.CACTUS, Blocks.SUGAR_CANE},
					new String[]{"Friendly Hiker", "Mason", "Beth"}, null),
			new Place("Oasis", Ground.SAND, 18, Skills.FARMING, 5,
					new Block[]{Blocks.SUGAR_CANE, Blocks.SAND}, NOBODY, null),
			new Place("Shepherd's Keep", Ground.SAND, 16, Skills.FARMING, 5,
					new Block[]{Blocks.CACTUS}, new String[]{"Shepherd"}, null),
			new Place("Trapper's Den", Ground.SAND, 14, Skills.HUNTING, 1, NOTHING,
					new String[]{"Trevor", "Tammy", "Tony"}, null),
			new Place("Jake's House", Ground.SAND, 12, Skills.HUNTING, 1, NOTHING,
					new String[]{"Jake"}, null),
			new Place("Mushroom Gorge", Ground.MYCELIUM, 20, Skills.FARMING, 5,
					new Block[]{Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK},
					new String[]{"Hungry Hiker"}, null),
			new Place("Glowing Mushroom Cave", Ground.MYCELIUM, 18, Skills.FARMING, 5,
					new Block[]{Blocks.RED_MUSHROOM_BLOCK, Blocks.SHROOMLIGHT},
					new String[]{"Moby"}, null),

			// --- fishing -----------------------------------------------------------
			new Place("Fishing Outpost", Ground.WATER, 22, "", 0, NOTHING,
					new String[]{"Angler Angus", "Fisherman Gerald", "Plumber Joe", "Gwynnie"},
					Mobs.SEA),
			new Place("Fisherman's Hut", Ground.WATER, 14, "", 0, NOTHING,
					new String[]{"Captain Baha", "Gavin"}, null),
			// No requirement: your seven skills have no Fishing, since you
			// replaced it with Taming, Hunting and HOTF.
			new Place("Backwater Bayou", Ground.WATER, 22, "", 0, NOTHING,
					new String[]{"Junker Joel", "Hattie", "Roddy"}, Mobs.SEA),
			new Place("Jerry's Workshop", Ground.ICE, 24, "", 0,
					new Block[]{Blocks.ICE, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK},
					new String[]{"Frosty", "Sherry", "Terry", "Banker Barry", "Gary"}, null),

			// --- the rest -----------------------------------------------------------
			new Place("Mountain", Ground.STONE, 20, "", 0,
					new Block[]{Blocks.STONE, Blocks.SNOW_BLOCK},
					new String[]{"Scoop", "Wizard"}, null),
			new Place("Dungeon Hub", Ground.STONE, 24, Skills.COMBAT, 5, NOTHING,
					new String[]{"Mort", "Malik", "Ophelia", "Guildford"}, null),
			new Place("Rift Dimension", Ground.OBSIDIAN, 24, Skills.COMBAT, 10, NOTHING,
					new String[]{"Enigma", "Barry"}, null),
			new Place("Limbo", Ground.OBSIDIAN, 12, "", 0, NOTHING, NOBODY, null),
	};

	/** Find a place by name, ignoring case. Null if there isn't one. */
	public static Place byName(String name) {
		for (Place place : ALL) {
			if (place.name().equalsIgnoreCase(name)) {
				return place;
			}
		}
		return null;
	}
}
