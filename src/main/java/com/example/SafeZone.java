package com.example;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

/**
 * Keeps the Village free of monsters.
 *
 * The wiki is unambiguous about this: the Village "is entirely safe" and
 * contains no hostile mobs. That is a promise the player can feel -- you should
 * be able to stand at the portal sorting your things out without a creeper
 * arriving.
 *
 * Lighting alone doesn't get you there. A well-lit square still lets things
 * wander in from the dark edges, and a spider can drop in from anywhere. So
 * anything hostile inside the Village is simply removed.
 *
 * Only the Village and the Bank are protected. The Mining District, the Forest,
 * the Combat Settlement, the Graveyard and the Spider's Den are all meant to be
 * dangerous, and are left alone -- the Spider's Den deliberately so.
 */
public final class SafeZone {
	private SafeZone() {
	}

	/**
	 * How far the safety reaches from the middle of the Village.
	 *
	 * Comfortably past the plaza and its buildings, and short of the districts
	 * beyond it, so nowhere that is supposed to be dangerous gets cleaned up.
	 */
	private static final int RADIUS = 40;

	/** Checked twice a second: often enough that nothing lasts long. */
	private static final int EVERY = 10;

	public static void tick(ServerLevel level) {
		if (level.getGameTime() % EVERY != 0) {
			return;
		}
		if (!Hub.exists(level)) {
			return;                          // no Hub yet, nothing to protect
		}

		AABB village = AABB.ofSize(Hub.CENTRE.getCenter(), RADIUS * 2, 40, RADIUS * 2);
		for (Entity entity : level.getEntities((Entity) null, village, SafeZone::hostile)) {
			entity.discard();                // removed quietly, no drops, no death
		}
	}

	/**
	 * Is this something that shouldn't be in a village?
	 *
	 * Enemy is the game's own marker for a monster, so this covers zombies,
	 * skeletons, creepers, spiders and anything else hostile without listing
	 * them one by one -- and it leaves villagers, animals and dropped items
	 * completely alone.
	 */
	private static boolean hostile(Entity entity) {
		return entity instanceof Enemy;
	}
}
