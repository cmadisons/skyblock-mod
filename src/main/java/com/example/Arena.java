package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Combat Arena -- a walled, roofed room off the Hub where monsters spawn to
 * be fought. Reached by the portal on the far side of the Hub plaza, opposite
 * the way home.
 *
 * The Hub is deliberately lit so nothing spawns on it; the Arena is the
 * opposite. Built the first time somebody goes through, like the Hub, so a world
 * you never fight in stays empty. See {@link Mobs} for what turns up inside.
 */
public final class Arena {
	private Arena() {
	}

	/** Middle of the arena floor. As far the other way from the Hub as it is from spawn. */
	public static final BlockPos CENTRE = new BlockPos(0, 64, -1000);

	/** Half-width of the floor, so 10 gives a 21x21 room. */
	private static final int RADIUS = 10;
	/** Air height between floor and ceiling. */
	private static final int HEIGHT = 6;

	/** Has the Arena been built yet? Checked by looking for its floor. */
	public static boolean exists(ServerLevel level) {
		return !level.getBlockState(CENTRE.below()).isAir();
	}

	/** The box monsters spawn inside, and that a player is counted against. */
	public static AABB bounds() {
		return new AABB(
				CENTRE.getX() - RADIUS, CENTRE.getY(), CENTRE.getZ() - RADIUS,
				CENTRE.getX() + RADIUS + 1, CENTRE.getY() + HEIGHT, CENTRE.getZ() + RADIUS + 1);
	}

	/** A random standing spot on the floor, kept a block clear of the walls. */
	public static BlockPos randomFloor(RandomSource rng) {
		int x = CENTRE.getX() + rng.nextIntBetweenInclusive(-(RADIUS - 1), RADIUS - 1);
		int z = CENTRE.getZ() + rng.nextIntBetweenInclusive(-(RADIUS - 1), RADIUS - 1);
		return new BlockPos(x, CENTRE.getY(), z);
	}

	/**
	 * Build the room -- floor, four walls and a ceiling -- and the portal home.
	 *
	 * @return where to stand a player arriving from the Hub.
	 */
	public static BlockPos build(ServerLevel level) {
		BlockState floor = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState wall = Blocks.COBBLESTONE.defaultBlockState();

		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				level.setBlockAndUpdate(CENTRE.offset(dx, -1, dz), floor);       // floor below standing level
				level.setBlockAndUpdate(CENTRE.offset(dx, HEIGHT, dz), floor);   // ceiling, keeps it enclosed
				if (Math.abs(dx) == RADIUS || Math.abs(dz) == RADIUS) {
					for (int y = 0; y < HEIGHT; y++) {
						level.setBlockAndUpdate(CENTRE.offset(dx, y, dz), wall);
					}
				}
			}
		}

		// The way back to the Hub, set inside the room near the far wall.
		Portals.frame(level, Portals.arenaGate());
		return CENTRE.above();
	}
}
