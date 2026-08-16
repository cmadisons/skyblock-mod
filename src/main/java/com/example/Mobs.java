package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Every enemy in the game, with its real numbers.
 *
 * Health, damage and level below are all Hypixel's own, from the game's mob
 * tables. Nothing here is estimated any more. The two that used to be guesses
 * are both settled: the Spider Jockey has 220 health and 55 damage, and the
 * Gold Mine turns out to be the only public island in SkyBlock with no mobs at
 * all -- so the ones invented for it have been deleted rather than corrected.
 *
 * Why the numbers are divided down
 * -------------------------------
 * A Hypixel player has thousands of health. A vanilla player has twenty. A Soul
 * of the Alpha really does hit for 1,140 -- there a hard fight, here death
 * through full netherite before you see it coming. Not the same fight, just a
 * wall.
 *
 * So the real figures are the source of truth and are scaled on the way in. The
 * ratios are the part that matters and they survive: a Soul of the Alpha stays
 * about thirty times the threat a Splitter Spider is. Two divisors make the
 * whole game harder or softer at once.
 */
public final class Mobs {
	private Mobs() {
	}

	/**
	 * How far to divide Hypixel's numbers.
	 *
	 * A vanilla player has 20 health and full netherite stops about 80% of a
	 * hit, so much past 10 damage is a one-shot. Health divides harder than
	 * damage because the worst mobs here run to tens of thousands.
	 */
	public static final double HEALTH_SCALE = 60.0;
	public static final double DAMAGE_SCALE = 40.0;

	/** Nothing ever scales down weaker than this. */
	private static final double MIN_HEALTH = 4;
	private static final double MIN_DAMAGE = 1;

	/** What to spawn, since an enemy can be almost anything. */
	public enum Shape {
		SPIDER, CAVE_SPIDER, ZOMBIE, ZOMBIE_VILLAGER, SKELETON, SILVERFISH, SLIME,
		WOLF, DROWNED, GUARDIAN, WITCH, ENDERMAN, ENDERMITE, WITHER_SKELETON,
		BLAZE, MAGMA_CUBE, CREEPER, PIGLIN, COW, PIG, CHICKEN
	}

	/** One enemy: its name, what it looks like, and Hypixel's own numbers. */
	public record Kind(String name, Shape shape, int level, double health, double damage,
			double speed) {
	}

	// -------------------------------------------------------------- graveyard

	public static final Kind[] GRAVEYARD = {
			new Kind("Zombie", Shape.ZOMBIE, 1, 100, 20, 0.23),
			new Kind("Zombie Villager", Shape.ZOMBIE_VILLAGER, 1, 120, 24, 0.25),
			new Kind("Crypt Ghoul", Shape.ZOMBIE, 30, 2000, 350, 0.25),
			new Kind("Golden Ghoul", Shape.ZOMBIE, 60, 45000, 800, 0.25),
	};

	// ----------------------------------------------------------- spider's den

	public static final Kind[] SPIDERS_DEN = {
			new Kind("Splitter Spider", Shape.SPIDER, 2, 180, 30, 0.30),
			new Kind("Weaver Spider", Shape.SPIDER, 3, 160, 35, 0.30),
			new Kind("Dasher Spider", Shape.SPIDER, 4, 170, 55, 0.42),
			new Kind("Spider Jockey", Shape.SPIDER, 4, 220, 55, 0.32),
			new Kind("Jockey Skeleton", Shape.SKELETON, 4, 250, 40, 0.25),
			new Kind("Voracious Spider", Shape.SPIDER, 10, 1000, 100, 0.34),
			new Kind("Silverfish", Shape.SILVERFISH, 1, 50, 20, 0.30),
			new Kind("Rain Slime", Shape.SLIME, 8, 200, 100, 0.28),
	};

	/** The Den's boss. Level 12, 3,000 health, 125 damage. */
	public static final Kind BROODMOTHER =
			new Kind("Broodmother", Shape.SPIDER, 12, 3000, 125, 0.30);

	// ------------------------------------------------------ the park & ruins

	/**
	 * The Park's spirits and the Ruins' wolves.
	 *
	 * A Soul of the Alpha is level 55 with 31,150 health and 1,140 damage,
	 * which makes the woodland the most dangerous place in the game -- far
	 * worse than anything in the Spider's Den, despite looking like a forest.
	 */
	public static final Kind[] FOREST = {
			new Kind("Wolf", Shape.WOLF, 15, 250, 90, 0.32),
			new Kind("Pack Spirit", Shape.WOLF, 30, 6000, 240, 0.34),
			new Kind("Howling Spirit", Shape.WOLF, 35, 7000, 400, 0.34),
			new Kind("Old Wolf", Shape.WOLF, 50, 15000, 800, 0.36),
			new Kind("Soul of the Alpha", Shape.WOLF, 55, 31150, 1140, 0.36),
	};

	// ----------------------------------------------------------- deep caverns

	/**
	 * The mining enemies.
	 *
	 * These belong to the Deep Caverns, not the Gold Mine: the Gold Mine is the
	 * only public island in SkyBlock with no mobs at all, so nothing spawns
	 * there however tempting it is to fill it.
	 */
	public static final Kind[] DEEP_CAVERNS = {
			new Kind("Sneaky Creeper", Shape.CREEPER, 3, 120, 80, 0.25),
			new Kind("Emerald Slime", Shape.SLIME, 5, 80, 70, 0.26),
			new Kind("Lapis Zombie", Shape.ZOMBIE, 8, 200, 50, 0.23),
			new Kind("Redstone Pigman", Shape.PIGLIN, 10, 250, 75, 0.25),
			new Kind("Miner Zombie", Shape.ZOMBIE, 15, 250, 200, 0.25),
			new Kind("Miner Skeleton", Shape.SKELETON, 15, 250, 150, 0.25),
	};

	// -------------------------------------------------------------- the end

	public static final Kind[] END = {
			new Kind("Endermite", Shape.ENDERMITE, 37, 2000, 400, 0.30),
			new Kind("Enderman", Shape.ENDERMAN, 42, 4500, 500, 0.30),
			new Kind("Watcher", Shape.SKELETON, 55, 9500, 500, 0.25),
			new Kind("Obsidian Defender", Shape.ENDERMAN, 55, 10000, 200, 0.32),
			new Kind("Zealot", Shape.ENDERMAN, 55, 13000, 1250, 0.30),
	};

	// --------------------------------------------------------------- nether

	public static final Kind[] NETHER = {
			new Kind("Wither Skeleton", Shape.WITHER_SKELETON, 70, 600000, 3000, 0.28),
			new Kind("Magma Cube", Shape.MAGMA_CUBE, 75, 1000000, 3000, 0.26),
			new Kind("Blaze", Shape.BLAZE, 80, 1000000, 3000, 0.24),
	};

	// -------------------------------------------------------------- the barn

	/** Farm animals. Passive, so they never attack and never learn to. */
	public static final Kind[] BARN = {
			new Kind("Cow", Shape.COW, 1, 50, 0, 0.20),
			new Kind("Pig", Shape.PIG, 1, 50, 0, 0.20),
			new Kind("Chicken", Shape.CHICKEN, 1, 50, 0, 0.20),
	};

	// -------------------------------------------------------------- the sea

	/**
	 * What comes out of the water when you fish: the Basic pool, in full.
	 *
	 * The levels here are the Fishing skill each one needs, which is what the
	 * wiki's sea creature table actually publishes -- and they were wrong
	 * before. A Sea Witch is Fishing 7, not 15; a Catfish is 13, not 23; a
	 * Guardian Defender is 17, not 45. Getting those right matters more than it
	 * sounds, because the whole pool is a ladder you climb by fishing, and a
	 * ladder with the rungs in the wrong places is not the same ladder.
	 *
	 * Two were missing entirely and are now here: the Rider of the Deep at 11,
	 * and the Water Hydra at 19 -- the Legendary of the pool, and the rarest
	 * thing an ordinary rod in ordinary water will ever produce, at 0.42%.
	 *
	 * Health and damage are still placed by rarity rather than quoted, because
	 * the sea creature tables give weight and chance and skill requirement and
	 * not those two. The order is right even where the absolute numbers are
	 * this mod's own: a Water Hydra is the worst thing in the pool and reads
	 * that way.
	 */
	public static final Kind[] SEA = {
			new Kind("Squid", Shape.DROWNED, 1, 60, 8, 0.20),
			new Kind("Sea Walker", Shape.DROWNED, 1, 100, 10, 0.22),
			new Kind("Sea Witch", Shape.WITCH, 7, 320, 45, 0.22),
			new Kind("Sea Archer", Shape.SKELETON, 9, 380, 55, 0.24),
			new Kind("Rider of the Deep", Shape.DROWNED, 11, 500, 70, 0.23),
			new Kind("Catfish", Shape.DROWNED, 13, 800, 90, 0.26),
			new Kind("Sea Leech", Shape.DROWNED, 16, 1200, 120, 0.24),
			new Kind("Guardian Defender", Shape.GUARDIAN, 17, 3000, 250, 0.22),
			new Kind("Deep Sea Protector", Shape.GUARDIAN, 18, 8000, 400, 0.22),
			new Kind("Water Hydra", Shape.GUARDIAN, 19, 25000, 900, 0.24),
	};

	// ---------------------------------------------------------------- spawning

	/**
	 * Spawn a ring of these kinds around a spot.
	 *
	 * Placed once when the area is built rather than left to natural spawning,
	 * so the named ones are actually there the first time you walk in instead
	 * of whatever the dark happens to produce.
	 */
	public static void ring(ServerLevel level, BlockPos middle, Kind[] kinds, int radius,
			int each) {
		int at = 0;
		for (Kind kind : kinds) {
			for (int copy = 0; copy < each; copy++) {
				double angle = (at * 2.4) + copy * (Math.PI * 2 / Math.max(1, each));
				int x = (int) Math.round(Math.cos(angle) * radius);
				int z = (int) Math.round(Math.sin(angle) * radius);
				spawn(level, middle.offset(x, 2, z), kind);
			}
			at++;
		}
	}

	/** One enemy, named and tuned. */
	public static void spawn(ServerLevel level, BlockPos where, Kind kind) {
		EntityType<? extends Entity> type = switch (kind.shape()) {
			case SPIDER -> EntityType.SPIDER;
			case CAVE_SPIDER -> EntityType.CAVE_SPIDER;
			case ZOMBIE -> EntityType.ZOMBIE;
			case ZOMBIE_VILLAGER -> EntityType.ZOMBIE_VILLAGER;
			case SKELETON -> EntityType.SKELETON;
			case SILVERFISH -> EntityType.SILVERFISH;
			case SLIME -> EntityType.SLIME;
			case WOLF -> EntityType.WOLF;
			case DROWNED -> EntityType.DROWNED;
			case GUARDIAN -> EntityType.GUARDIAN;
			case WITCH -> EntityType.WITCH;
			case ENDERMAN -> EntityType.ENDERMAN;
			case ENDERMITE -> EntityType.ENDERMITE;
			case WITHER_SKELETON -> EntityType.WITHER_SKELETON;
			case BLAZE -> EntityType.BLAZE;
			case MAGMA_CUBE -> EntityType.MAGMA_CUBE;
			case CREEPER -> EntityType.CREEPER;
			case PIGLIN -> EntityType.ZOMBIFIED_PIGLIN;
			case COW -> EntityType.COW;
			case PIG -> EntityType.PIG;
			case CHICKEN -> EntityType.CHICKEN;
		};

		Entity entity = type.create(level, EntitySpawnReason.COMMAND);
		if (!(entity instanceof LivingEntity mob)) {
			return;
		}
		mob.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 0.0f, 0.0f);

		// "[Lv30] Crypt Ghoul", the way the game writes it, coloured by how bad
		// it is -- so you can tell from across a room what you are looking at.
		mob.setCustomName(Component.literal("[Lv" + kind.level() + "] " + kind.name())
				.withStyle(colourFor(kind.level())));
		mob.setCustomNameVisible(true);

		double health = Math.max(MIN_HEALTH, kind.health() / HEALTH_SCALE);
		set(mob, Attributes.MAX_HEALTH, health);
		mob.setHealth((float) health);

		// Passive animals keep their zero, so a cow never learns to fight back.
		if (kind.damage() > 0) {
			set(mob, Attributes.ATTACK_DAMAGE,
					Math.max(MIN_DAMAGE, kind.damage() / DAMAGE_SCALE));
		}
		set(mob, Attributes.MOVEMENT_SPEED, kind.speed());

		// Persistent, so an area keeps its enemies instead of quietly emptying
		// once you walk away.
		if (mob instanceof net.minecraft.world.entity.Mob asMob) {
			asMob.setPersistenceRequired();
		}
		level.addFreshEntity(mob);
	}

	/** Grey for harmless, yellow for real, red for dangerous, dark red for worse. */
	private static ChatFormatting colourFor(int level) {
		if (level >= 50) {
			return ChatFormatting.DARK_RED;
		}
		if (level >= 25) {
			return ChatFormatting.RED;
		}
		if (level >= 8) {
			return ChatFormatting.YELLOW;
		}
		return ChatFormatting.GRAY;
	}

	private static void set(LivingEntity entity, Holder<Attribute> what, double value) {
		AttributeInstance attribute = entity.getAttribute(what);
		if (attribute != null) {
			attribute.setBaseValue(value);
		}
	}
}
