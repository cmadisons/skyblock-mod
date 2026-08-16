package com.example;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The pets, as SkyBlock has them.
 *
 * There used to be six, one per skill, all unlocked at level five and all doing
 * roughly the same thing. That was a placeholder wearing a pet's name. The real
 * game has dozens, sorted by the skill they belong to and graded by rarity, and
 * the rarity is what makes them worth chasing -- a Common Rabbit and a
 * Legendary Golden Dragon are not the same kind of thing at all.
 *
 * How you get one
 * --------------
 * In the real game pets drop from mobs and get traded on the Auction House.
 * Neither exists here, so they are earned from the skill they belong to, and
 * how deep into that skill decides which rarity you can hold:
 *
 *   Common 5 · Uncommon 10 · Rare 15 · Epic 20 · Legendary 25 · Mythic 30
 *
 * That keeps the ladder the rarities imply -- a Legendary is a serious way into
 * a skill, a Common is an afternoon -- without inventing a drop table for mobs
 * this mod does not have.
 *
 * What they do
 * -----------
 * A held effect, refreshed while the pet is out. Deliberately modest: a pet in
 * SkyBlock nudges a number, it does not play the game for you. Which effect
 * follows what the pet is actually for, so the Mole digs faster and the Lion
 * hits harder.
 */
public final class Pets {
	private Pets() {
	}

	/** The six rarities, and the skill level each needs. */
	public enum Rarity {
		COMMON(5, ChatFormatting.WHITE),
		UNCOMMON(10, ChatFormatting.GREEN),
		RARE(15, ChatFormatting.BLUE),
		EPIC(20, ChatFormatting.DARK_PURPLE),
		LEGENDARY(25, ChatFormatting.GOLD),
		MYTHIC(30, ChatFormatting.LIGHT_PURPLE);

		private final int needs;
		private final ChatFormatting colour;

		Rarity(int needs, ChatFormatting colour) {
			this.needs = needs;
			this.colour = colour;
		}

		public int needs() {
			return needs;
		}

		public ChatFormatting colour() {
			return colour;
		}

		/** "LEGENDARY" reads better as "Legendary". */
		public String pretty() {
			String lower = name().toLowerCase();
			return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
		}
	}

	/** One pet: what it is, whose skill it belongs to, and what holding it does. */
	public record Pet(String name, String skill, Rarity rarity, Item icon,
			Holder<MobEffect> effect, String perk) {
	}

	/**
	 * Every pet, grouped by the skill that earns it.
	 *
	 * The skill each belongs to is the game's own grouping -- which is why the
	 * Lion is Foraging and not Combat, and why the Wither Skeleton is a Mining
	 * pet despite being made of bones. Both surprise people and both are right.
	 */
	public static final Pet[] ALL = {
			// --- Farming ---------------------------------------------------------
			new Pet("Chicken", Skills.FARMING, Rarity.COMMON, Items.EGG,
					MobEffects.JUMP_BOOST, "Lighter on your feet"),
			new Pet("Rabbit", Skills.FARMING, Rarity.COMMON, Items.RABBIT_FOOT,
					MobEffects.JUMP_BOOST, "Jump boost"),
			new Pet("Pig", Skills.FARMING, Rarity.COMMON, Items.CARROT_ON_A_STICK,
					MobEffects.SPEED, "Faster on foot"),
			new Pet("Slug", Skills.FARMING, Rarity.UNCOMMON, Items.SLIME_BALL,
					MobEffects.REGENERATION, "Slowly mends you"),
			new Pet("Hedgehog", Skills.FARMING, Rarity.RARE, Items.BROWN_MUSHROOM,
					MobEffects.RESISTANCE, "Harder to hurt"),
			new Pet("Bee", Skills.FARMING, Rarity.EPIC, Items.HONEYCOMB,
					MobEffects.SPEED, "Faster, and better at farming"),
			new Pet("Mooshroom Cow", Skills.FARMING, Rarity.LEGENDARY, Items.RED_MUSHROOM,
					MobEffects.SATURATION, "Never goes hungry"),
			new Pet("Elephant", Skills.FARMING, Rarity.LEGENDARY, Items.HAY_BLOCK,
					MobEffects.HEALTH_BOOST, "A great deal more health"),

			// --- Mining ----------------------------------------------------------
			new Pet("Silverfish", Skills.MINING, Rarity.COMMON, Items.STONE,
					MobEffects.HASTE, "Faster mining"),
			new Pet("Rock", Skills.MINING, Rarity.COMMON, Items.COBBLESTONE,
					MobEffects.RESISTANCE, "Harder to hurt"),
			new Pet("Armadillo", Skills.MINING, Rarity.UNCOMMON, Items.ARMADILLO_SCUTE,
					MobEffects.RESISTANCE, "Rolls with the hits"),
			new Pet("Mole", Skills.MINING, Rarity.RARE, Items.IRON_SHOVEL,
					MobEffects.HASTE, "Digs a great deal faster"),
			new Pet("Wither Skeleton", Skills.MINING, Rarity.EPIC, Items.WITHER_SKELETON_SKULL,
					MobEffects.FIRE_RESISTANCE, "Walks through fire"),
			new Pet("Goblin", Skills.MINING, Rarity.EPIC, Items.GOLD_NUGGET,
					MobEffects.HASTE, "Knows where the gold is"),
			new Pet("Bal", Skills.MINING, Rarity.LEGENDARY, Items.MAGMA_CREAM,
					MobEffects.FIRE_RESISTANCE, "At home in the magma"),

			// --- Combat ----------------------------------------------------------
			new Pet("Zombie", Skills.COMBAT, Rarity.COMMON, Items.ROTTEN_FLESH,
					MobEffects.STRENGTH, "Stronger hits"),
			new Pet("Skeleton", Skills.COMBAT, Rarity.COMMON, Items.BONE,
					MobEffects.SPEED, "Quicker on the draw"),
			new Pet("Spider", Skills.COMBAT, Rarity.UNCOMMON, Items.STRING,
					MobEffects.SPEED, "Faster, and climbs better"),
			new Pet("Endermite", Skills.COMBAT, Rarity.UNCOMMON, Items.END_STONE,
					MobEffects.SPEED, "Quick and small"),
			new Pet("Wolf", Skills.COMBAT, Rarity.RARE, Items.BONE_MEAL,
					MobEffects.STRENGTH, "Stronger hits"),
			new Pet("Ghoul", Skills.COMBAT, Rarity.EPIC, Items.ZOMBIE_HEAD,
					MobEffects.REGENERATION, "Heals as you fight"),
			new Pet("Enderman", Skills.COMBAT, Rarity.EPIC, Items.ENDER_PEARL,
					MobEffects.NIGHT_VISION, "Sees in the dark"),
			new Pet("Tiger", Skills.COMBAT, Rarity.LEGENDARY, Items.LEATHER,
					MobEffects.STRENGTH, "A great deal stronger"),
			new Pet("Ender Dragon", Skills.COMBAT, Rarity.MYTHIC, Items.DRAGON_HEAD,
					MobEffects.STRENGTH, "The best there is"),

			// --- Foraging --------------------------------------------------------
			new Pet("Ocelot", Skills.FORAGING, Rarity.COMMON, Items.OAK_SAPLING,
					MobEffects.HASTE, "Faster chopping"),
			new Pet("Monkey", Skills.FORAGING, Rarity.UNCOMMON, Items.JUNGLE_SAPLING,
					MobEffects.HASTE, "Faster chopping, and climbs"),
			new Pet("Lion", Skills.FORAGING, Rarity.EPIC, Items.GOLDEN_APPLE,
					MobEffects.STRENGTH, "Stronger hits"),
			new Pet("Giraffe", Skills.FORAGING, Rarity.LEGENDARY, Items.ACACIA_SAPLING,
					MobEffects.HEALTH_BOOST, "More health"),

			// --- Fishing ---------------------------------------------------------
			new Pet("Squid", Skills.FISHING, Rarity.COMMON, Items.INK_SAC,
					MobEffects.WATER_BREATHING, "Breathes underwater"),
			new Pet("Flying Fish", Skills.FISHING, Rarity.RARE, Items.PRISMARINE_SHARD,
					MobEffects.DOLPHINS_GRACE, "Swims far faster"),
			new Pet("Dolphin", Skills.FISHING, Rarity.EPIC, Items.COD,
					MobEffects.DOLPHINS_GRACE, "Swims far faster"),
			new Pet("Ammonite", Skills.FISHING, Rarity.EPIC, Items.NAUTILUS_SHELL,
					MobEffects.WATER_BREATHING, "Never drowns"),
			new Pet("Baby Yeti", Skills.FISHING, Rarity.LEGENDARY, Items.SNOWBALL,
					MobEffects.RESISTANCE, "Shrugs off the cold"),
			new Pet("Blue Whale", Skills.FISHING, Rarity.LEGENDARY, Items.HEART_OF_THE_SEA,
					MobEffects.HEALTH_BOOST, "A great deal more health"),
			new Pet("Megalodon", Skills.FISHING, Rarity.MYTHIC, Items.PRISMARINE_CRYSTALS,
					MobEffects.STRENGTH, "The sea's own"),

			// --- Taming ----------------------------------------------------------
			new Pet("Horse", Skills.TAMING, Rarity.RARE, Items.SADDLE,
					MobEffects.SPEED, "Faster on foot"),
			new Pet("Black Cat", Skills.TAMING, Rarity.LEGENDARY, Items.BLACK_DYE,
					MobEffects.LUCK, "Luckier than you were"),
			new Pet("Golden Dragon", Skills.TAMING, Rarity.MYTHIC, Items.GOLD_BLOCK,
					MobEffects.STRENGTH, "The rarest pet in the game"),

			// --- Alchemy and Enchanting -------------------------------------------
			new Pet("Sheep", Skills.ALCHEMY, Rarity.COMMON, Items.WHITE_WOOL,
					MobEffects.REGENERATION, "Mends you slowly"),
			new Pet("Parrot", Skills.ALCHEMY, Rarity.EPIC, Items.FEATHER,
					MobEffects.REGENERATION, "Longer potions"),
			new Pet("Guardian", Skills.ENCHANTING, Rarity.EPIC, Items.PRISMARINE,
					MobEffects.RESISTANCE, "Harder to hurt"),

			// --- Hunting ----------------------------------------------------------
			new Pet("Jellyfish", Skills.HUNTING, Rarity.RARE, Items.GLOW_INK_SAC,
					MobEffects.REGENERATION, "Mends you as you hunt"),
			new Pet("Blaze", Skills.HUNTING, Rarity.LEGENDARY, Items.BLAZE_POWDER,
					MobEffects.FIRE_RESISTANCE, "Walks through fire"),
	};

	/** Find a pet by name, or null. */
	public static Pet byName(String name) {
		for (Pet pet : ALL) {
			if (pet.name().equals(name)) {
				return pet;
			}
		}
		return null;
	}

	/** The pets belonging to one skill, rarest last. */
	public static List<Pet> forSkill(String skill) {
		List<Pet> found = new ArrayList<>();
		for (Pet pet : ALL) {
			if (pet.skill().equals(skill)) {
				found.add(pet);
			}
		}
		return found;
	}

	/** Has this player earned the right to hold this one? */
	public static boolean unlocked(net.minecraft.server.level.ServerPlayer player, Pet pet) {
		return Skills.levelIn(player, pet.skill()) >= pet.rarity().needs();
	}

	/** How many of the lot are unlocked. */
	public static int unlockedCount(net.minecraft.server.level.ServerPlayer player) {
		int count = 0;
		for (Pet pet : ALL) {
			if (unlocked(player, pet)) {
				count++;
			}
		}
		return count;
	}
}
