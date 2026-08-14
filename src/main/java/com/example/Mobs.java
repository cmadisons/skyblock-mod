package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;

/**
 * Every named enemy in the game, with its real numbers.
 *
 * Health and damage below are Hypixel's own, taken from each mob's wiki page.
 * Where a page gives no numbers they are estimated from the mob's level, and
 * the table says which is which -- so it is always clear what is measured and
 * what is a guess.
 *
 * Why the real numbers are divided down
 * ------------------------------------
 * A Hypixel player has thousands of health. A vanilla player has twenty. A Crypt
 * Ghoul really does hit for 350, which there is a hard fight and here is death
 * through full netherite before you see it coming -- not the same fight at all,
 * just a wall.
 *
 * So the real figures are the source of truth and are scaled on the way in. The
 * ratios are what matter and they survive: a Crypt Ghoul stays roughly twelve
 * times the threat a Splitter Spider is. Change the two divisors to make the
 * whole game harder or softer at once.
 */
public final class Mobs {
	private Mobs() {
	}

	/**
	 * How far to divide Hypixel's numbers.
	 *
	 * A vanilla player has 20 health and full netherite stops about 80% of a
	 * hit, so much past 10 damage is a one-shot. These put the Broodmother at
	 * 200 health and 10 damage: a fight you can lose rather than one you
	 * cannot win.
	 */
	public static final double HEALTH_SCALE = 15.0;
	public static final double DAMAGE_SCALE = 12.5;

	/** What kind of thing to spawn, since not every enemy is a spider. */
	public enum Shape {
		SPIDER, ZOMBIE, ZOMBIE_VILLAGER, SKELETON
	}

	/**
	 * One enemy.
	 *
	 * @param real true when health and damage come from the wiki, false when
	 *             they are estimated from the level.
	 */
	public record Kind(String name, Shape shape, int level, double health, double damage,
			double speed, boolean real) {
	}

	// ------------------------------------------------------------ the spiders

	public static final Kind[] SPIDERS_DEN = {
			new Kind("Splitter Spider", Shape.SPIDER, 2, 180, 30, 0.30, true),
			new Kind("Spider Jockey", Shape.SPIDER, 3, 120, 40, 0.32, false),
			new Kind("Weaver Spider", Shape.SPIDER, 3, 130, 40, 0.30, false),
			new Kind("Dasher Spider", Shape.SPIDER, 4, 170, 55, 0.42, true),
			new Kind("Voracious Spider", Shape.SPIDER, 10, 300, 80, 0.34, true),
	};

	/** The Den's boss. Real: level 12, 3,000 health, 125 damage. */
	public static final Kind BROODMOTHER =
			new Kind("Broodmother", Shape.SPIDER, 12, 3000, 125, 0.30, true);

	// ---------------------------------------------------------- the graveyard

	public static final Kind[] GRAVEYARD = {
			new Kind("Graveyard Zombie", Shape.ZOMBIE, 1, 100, 25, 0.23, false),
			new Kind("Zombie Villager", Shape.ZOMBIE_VILLAGER, 3, 160, 35, 0.23, false),
			// Real: level 30, 2,000 health, 350 damage. Easily the nastiest
			// thing in the Hub, and the numbers say so.
			new Kind("Crypt Ghoul", Shape.ZOMBIE, 30, 2000, 350, 0.25, true),
			// Real: level 60, 45,000 health, 800 damage. By far the worst thing
			// in the game so far -- 3,000 health here even after scaling, which
			// is a boss fight rather than a wandering zombie.
			new Kind("Golden Ghoul", Shape.ZOMBIE, 60, 45000, 800, 0.25, true),
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
			case ZOMBIE -> EntityType.ZOMBIE;
			case ZOMBIE_VILLAGER -> EntityType.ZOMBIE_VILLAGER;
			case SKELETON -> EntityType.SKELETON;
		};

		Entity entity = type.create(level, EntitySpawnReason.COMMAND);
		if (!(entity instanceof LivingEntity mob)) {
			return;
		}
		mob.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 0.0f, 0.0f);

		// "[Lv30] Crypt Ghoul", the way the game writes it. Red once a mob is
		// genuinely dangerous, so you can tell before it reaches you.
		mob.setCustomName(Component.literal("[Lv" + kind.level() + "] " + kind.name())
				.withStyle(kind.level() >= 10 ? ChatFormatting.RED : ChatFormatting.GRAY));
		mob.setCustomNameVisible(true);

		double health = Math.max(4, kind.health() / HEALTH_SCALE);
		double damage = Math.max(1, kind.damage() / DAMAGE_SCALE);

		set(mob, Attributes.MAX_HEALTH, health);
		mob.setHealth((float) health);
		set(mob, Attributes.ATTACK_DAMAGE, damage);
		set(mob, Attributes.MOVEMENT_SPEED, kind.speed());

		// Persistent, so an area keeps its enemies instead of quietly emptying
		// once you walk away.
		if (mob instanceof net.minecraft.world.entity.Mob asMob) {
			asMob.setPersistenceRequired();
		}
		level.addFreshEntity(mob);
	}

	private static void set(LivingEntity entity, Holder<Attribute> what, double value) {
		AttributeInstance attribute = entity.getAttribute(what);
		if (attribute != null) {
			attribute.setBaseValue(value);
		}
	}
}
