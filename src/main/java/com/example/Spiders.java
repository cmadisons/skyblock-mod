package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The Spider's Den's own spiders.
 *
 * The wiki names five kinds and a boss, each at a level, and vanilla spiders
 * are not them: a Voracious Spider is meant to be a serious fight and a
 * Splitter is meant to be a nuisance. So each is a spider underneath with its
 * name, health and speed set to match what it is supposed to be.
 *
 * Levels are the wiki's. Health and speed are not recorded anywhere, so they
 * are scaled from the level -- a level 10 spider being a real threat and a
 * level 2 one barely slowing you down.
 */
public final class Spiders {
	private Spiders() {
	}

	/** name, level, health, and how much faster than a normal spider. */
	private record Kind(String name, int level, double health, double speed) {
	}

	/**
	 * The Den's residents, as the wiki lists them.
	 *
	 * A vanilla spider has 16 health and moves at 0.3, which is the yardstick
	 * everything here is set against.
	 */
	private static final Kind[] KINDS = {
			new Kind("Splitter Spider", 2, 12, 0.30),
			new Kind("Spider Jockey", 3, 16, 0.32),
			new Kind("Weaver Spider", 3, 18, 0.30),
			new Kind("Dasher Spider", 4, 14, 0.42),   // the fast one
			new Kind("Voracious Spider", 10, 60, 0.34),
	};

	/** The boss, alone at the top of the mound. */
	private static final Kind BROODMOTHER = new Kind("Broodmother", 15, 220, 0.30);

	/**
	 * Fill the Den.
	 *
	 * Spawned once when the Hub is built rather than left to natural spawning,
	 * so the named ones are actually there the first time you walk in instead
	 * of whatever the dark happens to produce.
	 */
	public static void populate(ServerLevel level, BlockPos middle) {
		int at = 0;
		for (Kind kind : KINDS) {
			// Two of each, spread around the mound on a rough ring.
			for (int copy = 0; copy < 2; copy++) {
				double angle = (at * 2.4) + copy * Math.PI;
				int x = (int) Math.round(Math.cos(angle) * 10);
				int z = (int) Math.round(Math.sin(angle) * 10);
				spawn(level, middle.offset(x, 2, z), kind);
			}
			at++;
		}
		spawn(level, middle.offset(0, 3, 0), BROODMOTHER);
	}

	/** One spider, named and tuned. */
	private static void spawn(ServerLevel level, BlockPos where, Kind kind) {
		var spider = EntityType.SPIDER.create(level, EntitySpawnReason.COMMAND);
		if (spider == null) {
			return;
		}
		spider.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 0.0f, 0.0f);

		// "[Lv4] Dasher Spider", the way the game writes it.
		spider.setCustomName(Component.literal("[Lv" + kind.level() + "] " + kind.name())
				.withStyle(kind.level() >= 10 ? ChatFormatting.RED : ChatFormatting.GRAY));
		spider.setCustomNameVisible(true);

		set(spider, Attributes.MAX_HEALTH, kind.health());
		spider.setHealth((float) kind.health());
		set(spider, Attributes.MOVEMENT_SPEED, kind.speed());

		// Persistent, so the Den still has its spiders after you walk away and
		// come back rather than quietly emptying itself.
		spider.setPersistenceRequired();
		level.addFreshEntity(spider);
	}

	private static void set(LivingEntity entity,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> what,
			double value) {
		AttributeInstance attribute = entity.getAttribute(what);
		if (attribute != null) {
			attribute.setBaseValue(value);
		}
	}
}
