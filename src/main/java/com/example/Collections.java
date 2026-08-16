package com.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Collections: the count of everything you have ever gathered, and the tiers
 * that count unlocks.
 *
 * {@link Vault} has always kept the numbers. What it did not have was the point
 * of them. In SkyBlock a Collection is not a tally, it is a ladder -- 50 cobble
 * gets you a tier, 100 gets you another, and each one hands over a recipe or a
 * bump to something. The number on its own is a scoreboard; the ladder is why
 * anybody mines the fiftieth cobblestone.
 *
 * The tiers below are the game's own thresholds. They are steep on purpose:
 * tier I is almost immediate, tier IX is a project. Five categories, matching
 * the five the game sorts them into, so the page reads the way the real one
 * does.
 *
 * What a tier gives you here
 * -------------------------
 * Coins, and a line saying what it was for. SkyBlock's collection rewards are
 * mostly recipes for items that do not exist in this mod, and inventing a
 * reward would be worse than paying out honestly -- so the milestone is real,
 * the money is real, and nothing pretends to be a recipe it hasn't got.
 */
public final class Collections {
	private Collections() {
	}

	/** The thresholds a collection passes through, as the game sets them. */
	private static final long[] TIERS = {50, 100, 250, 1_000, 2_500, 5_000, 10_000, 25_000, 50_000};

	/** Tier numbers as SkyBlock writes them. */
	private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

	/** One collection: what it counts, and which of the five lists it is on. */
	public record Entry(String name, Item item, String category) {
	}

	public static final String FARMING = "Farming";
	public static final String MINING = "Mining";
	public static final String COMBAT = "Combat";
	public static final String FORAGING = "Foraging";
	public static final String FISHING = "Fishing";

	/**
	 * Every collection, in the game's own five categories.
	 *
	 * These are the ones whose material actually exists in Minecraft, which is
	 * the honest limit: a Mithril collection with no mithril to gather would be
	 * a row in a menu and nothing else.
	 */
	public static final Entry[] ALL = {
			// --- Farming ---------------------------------------------------------
			new Entry("Wheat", Items.WHEAT, FARMING),
			new Entry("Carrot", Items.CARROT, FARMING),
			new Entry("Potato", Items.POTATO, FARMING),
			new Entry("Pumpkin", Items.PUMPKIN, FARMING),
			new Entry("Melon", Items.MELON_SLICE, FARMING),
			new Entry("Seeds", Items.WHEAT_SEEDS, FARMING),
			new Entry("Sugar Cane", Items.SUGAR_CANE, FARMING),
			new Entry("Nether Wart", Items.NETHER_WART, FARMING),
			new Entry("Cactus", Items.CACTUS, FARMING),
			new Entry("Cocoa Beans", Items.COCOA_BEANS, FARMING),
			new Entry("Mushroom", Items.RED_MUSHROOM, FARMING),
			new Entry("Feather", Items.FEATHER, FARMING),
			new Entry("Leather", Items.LEATHER, FARMING),
			new Entry("Porkchop", Items.PORKCHOP, FARMING),
			new Entry("Chicken", Items.CHICKEN, FARMING),
			new Entry("Mutton", Items.MUTTON, FARMING),
			new Entry("Rabbit", Items.RABBIT, FARMING),
			new Entry("Wool", Items.WHITE_WOOL, FARMING),

			// --- Mining ----------------------------------------------------------
			new Entry("Cobblestone", Items.COBBLESTONE, MINING),
			new Entry("Coal", Items.COAL, MINING),
			new Entry("Iron Ingot", Items.IRON_INGOT, MINING),
			new Entry("Gold Ingot", Items.GOLD_INGOT, MINING),
			new Entry("Diamond", Items.DIAMOND, MINING),
			new Entry("Lapis Lazuli", Items.LAPIS_LAZULI, MINING),
			new Entry("Emerald", Items.EMERALD, MINING),
			new Entry("Redstone", Items.REDSTONE, MINING),
			new Entry("Quartz", Items.QUARTZ, MINING),
			new Entry("Obsidian", Items.OBSIDIAN, MINING),
			new Entry("Glowstone", Items.GLOWSTONE_DUST, MINING),
			new Entry("Gravel", Items.GRAVEL, MINING),
			new Entry("Ice", Items.ICE, MINING),
			new Entry("Netherrack", Items.NETHERRACK, MINING),
			new Entry("Sand", Items.SAND, MINING),
			new Entry("End Stone", Items.END_STONE, MINING),

			// --- Combat ----------------------------------------------------------
			new Entry("Rotten Flesh", Items.ROTTEN_FLESH, COMBAT),
			new Entry("Bone", Items.BONE, COMBAT),
			new Entry("String", Items.STRING, COMBAT),
			new Entry("Spider Eye", Items.SPIDER_EYE, COMBAT),
			new Entry("Gunpowder", Items.GUNPOWDER, COMBAT),
			new Entry("Ender Pearl", Items.ENDER_PEARL, COMBAT),
			new Entry("Ghast Tear", Items.GHAST_TEAR, COMBAT),
			new Entry("Slime Ball", Items.SLIME_BALL, COMBAT),
			new Entry("Blaze Rod", Items.BLAZE_ROD, COMBAT),
			new Entry("Magma Cream", Items.MAGMA_CREAM, COMBAT),

			// --- Foraging --------------------------------------------------------
			new Entry("Oak Wood", Items.OAK_LOG, FORAGING),
			new Entry("Spruce Wood", Items.SPRUCE_LOG, FORAGING),
			new Entry("Birch Wood", Items.BIRCH_LOG, FORAGING),
			new Entry("Dark Oak Wood", Items.DARK_OAK_LOG, FORAGING),
			new Entry("Acacia Wood", Items.ACACIA_LOG, FORAGING),
			new Entry("Jungle Wood", Items.JUNGLE_LOG, FORAGING),

			// --- Fishing ---------------------------------------------------------
			new Entry("Raw Fish", Items.COD, FISHING),
			new Entry("Raw Salmon", Items.SALMON, FISHING),
			new Entry("Clownfish", Items.TROPICAL_FISH, FISHING),
			new Entry("Pufferfish", Items.PUFFERFISH, FISHING),
			new Entry("Prismarine Shard", Items.PRISMARINE_SHARD, FISHING),
			new Entry("Prismarine Crystals", Items.PRISMARINE_CRYSTALS, FISHING),
			new Entry("Clay", Items.CLAY_BALL, FISHING),
			new Entry("Lily Pad", Items.LILY_PAD, FISHING),
			new Entry("Ink Sac", Items.INK_SAC, FISHING),
			new Entry("Sponge", Items.SPONGE, FISHING),
	};

	/** The five categories, in the order the game lists them. */
	public static final String[] CATEGORIES = {FARMING, MINING, COMBAT, FORAGING, FISHING};

	// ------------------------------------------------------------------ asking

	/** The collections in one category. */
	public static List<Entry> inCategory(String category) {
		List<Entry> found = new ArrayList<>();
		for (Entry entry : ALL) {
			if (entry.category().equals(category)) {
				found.add(entry);
			}
		}
		return found;
	}

	/** The tier that many of something earns: 0 to 9. */
	public static int tierFor(long amount) {
		int tier = 0;
		for (long threshold : TIERS) {
			if (amount >= threshold) {
				tier++;
			}
		}
		return tier;
	}

	/** "IV", or "" for tier zero. */
	public static String roman(int tier) {
		return tier <= 0 ? "" : ROMAN[Math.min(tier, ROMAN.length) - 1];
	}

	/** How many are needed for the next tier, or 0 if the ladder is finished. */
	public static long nextAt(long amount) {
		for (long threshold : TIERS) {
			if (amount < threshold) {
				return threshold;
			}
		}
		return 0;
	}

	public static int maxTier() {
		return TIERS.length;
	}

	/** How many of this collection the player has gathered. */
	public static long amount(ServerPlayer player, Entry entry) {
		String key = net.minecraft.core.registries.BuiltInRegistries.ITEM
				.getKey(entry.item()).getPath();
		return Vault.collections(player).getOrDefault(key, 0L);
	}

	/** Every tier the player has earned, added up across everything. */
	public static int totalTiers(ServerPlayer player) {
		int total = 0;
		for (Entry entry : ALL) {
			total += tierFor(amount(player, entry));
		}
		return total;
	}

	/** How many collections have been started at all. */
	public static int started(ServerPlayer player) {
		int started = 0;
		for (Entry entry : ALL) {
			if (amount(player, entry) > 0) {
				started++;
			}
		}
		return started;
	}

	// ---------------------------------------------------------------- unlocking

	/**
	 * Which tiers a player has already been paid for.
	 *
	 * Kept so a tier pays once. Without it, every block broken after the
	 * fiftieth would pay the tier I reward again.
	 */
	private static final Map<String, Integer> NOTHING = new LinkedHashMap<>();

	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Map<String, Long>> PAID =
			net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
					.<Map<String, Long>>builder()
					.initializer(LinkedHashMap::new)
					.persistent(com.mojang.serialization.Codec.unboundedMap(
							com.mojang.serialization.Codec.STRING,
							com.mojang.serialization.Codec.LONG))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("collection_tiers"));

	/**
	 * Check whether gathering this just crossed a threshold, and pay if so.
	 *
	 * Called from {@link Vault#collect}, so it runs on every single block break.
	 * The early return is doing the work: almost every call finds the tier
	 * unchanged and stops after two map lookups.
	 */
	public static void check(ServerPlayer player, Item item) {
		Entry entry = null;
		for (Entry candidate : ALL) {
			if (candidate.item() == item) {
				entry = candidate;
				break;
			}
		}
		if (entry == null) {
			return;
		}

		long have = amount(player, entry);
		int tier = tierFor(have);
		if (tier == 0) {
			return;
		}

		Map<String, Long> paid = player.getAttachedOrCreate(PAID, LinkedHashMap::new);
		long already = paid.getOrDefault(entry.name(), 0L);
		if (already >= tier) {
			return;
		}

		Map<String, Long> updated = new LinkedHashMap<>(paid);
		updated.put(entry.name(), (long) tier);
		player.setAttached(PAID, updated);

		// Later tiers are worth a great deal more, which is what makes the long
		// ones worth finishing rather than abandoning at IV.
		long reward = 100L * tier * tier;
		Economy.give(player, reward);
		player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"§6" + entry.name() + " Collection " + roman(tier) + "§7 — §6"
						+ Economy.pretty(reward) + " coins"));
	}
}
