package com.example;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

/**
 * The Combat Arena's monsters.
 *
 * A mostly-empty void world barely spawns anything on its own, so rather than
 * lean on vanilla's rules this hand-spawns a steady trickle while a survival
 * player is inside the {@link Arena}. Zombies, skeletons and spiders -- enough
 * to fight, capped so it never becomes a lag pit.
 *
 * Killing them already feeds the Combat skill: see {@link Skills} where any
 * Enemy that dies to a player hands out Combat XP.
 */
public final class Mobs {
	private Mobs() {
	}

	/** At most this many alive in the arena at once. */
	private static final int CAP = 8;
	/** One spawn attempt this often, so they arrive a few at a time. */
	private static final int EVERY_TICKS = 40;

	/** Called every server tick for the overworld. Costs nothing while empty. */
	public static void tick(ServerLevel level) {
		if (level.getGameTime() % EVERY_TICKS != 0 || !Arena.exists(level)) {
			return;
		}

		AABB bounds = Arena.bounds();
		if (!hasFighter(level, bounds)) {
			return;                                    // no one to fight, so no spawns
		}

		List<Monster> alive = level.getEntitiesOfClass(Monster.class, bounds);
		if (alive.size() >= CAP) {
			return;
		}

		RandomSource rng = level.getRandom();
		BlockPos spot = Arena.randomFloor(rng);
		EntityType<?> kind = switch (rng.nextInt(3)) {
			case 0 -> EntityType.ZOMBIE;
			case 1 -> EntityType.SKELETON;
			default -> EntityType.SPIDER;
		};
		kind.spawn(level, spot, EntitySpawnReason.EVENT);
	}

	/** Is a survival player actually standing in the arena? */
	private static boolean hasFighter(ServerLevel level, AABB bounds) {
		for (ServerPlayer player : level.players()) {
			if (SkyBlocksMod.allowed(player, level)
					&& bounds.contains(player.getX(), player.getY(), player.getZ())) {
				return true;
			}
		}
		return false;
	}
}
