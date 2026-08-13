package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hub — the shared area the portal on your island takes you to.
 *
 * It sits a long way from your island rather than in a dimension of its own,
 * which keeps the whole thing to one world file and means a portal is just a
 * teleport. A thousand blocks is far enough that you will never wander into it
 * by accident from spawn.
 *
 * Built the first time somebody goes through, not when the world is made, so a
 * world you never use the portal in stays completely empty.
 */
public final class Hub {
	private Hub() {
	}

	/** Middle of the Hub plaza. Far from the island, same world. */
	public static final BlockPos CENTRE = new BlockPos(0, 64, 1000);

	/** Half-width of the stone plaza, so 10 gives a 21x21 square. */
	private static final int RADIUS = 10;

	/** Has the Hub been built yet? Checked by looking for its floor. */
	public static boolean exists(ServerLevel level) {
		return !level.getBlockState(CENTRE.below()).isAir();
	}

	/**
	 * Build the plaza, its lamps, the little market stalls and the portal home.
	 *
	 * @return where to stand a player arriving from an island.
	 */
	public static BlockPos build(ServerLevel level) {
		BlockState floor = Blocks.STONE_BRICKS.defaultBlockState();
		BlockState path = Blocks.POLISHED_ANDESITE.defaultBlockState();

		// A round-ish plaza: square corners trimmed off so it reads as a place
		// rather than a slab.
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				if (Math.abs(dx) + Math.abs(dz) > RADIUS + 5) {
					continue;
				}
				// A cross of lighter stone through the middle, as walkways.
				boolean walkway = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
				level.setBlockAndUpdate(CENTRE.offset(dx, -1, dz), walkway ? path : floor);
			}
		}

		// Lamps around the edge so the Hub is lit and nothing spawns on it.
		for (int dx = -RADIUS; dx <= RADIUS; dx += 5) {
			for (int dz = -RADIUS; dz <= RADIUS; dz += 5) {
				if (Math.abs(dx) != RADIUS && Math.abs(dz) != RADIUS) {
					continue;                       // edge only, not the middle
				}
				level.setBlockAndUpdate(CENTRE.offset(dx, 0, dz), Blocks.SEA_LANTERN.defaultBlockState());
			}
		}

		// Four small stalls, one per corner, for shops to live in later.
		stall(level, CENTRE.offset(-6, 0, -6));
		stall(level, CENTRE.offset(6, 0, -6));
		stall(level, CENTRE.offset(-6, 0, 6));
		stall(level, CENTRE.offset(6, 0, 6));

		// The way home, on the far side of the plaza from where you arrive.
		Portals.frame(level, Portals.hubPortal());

		return CENTRE.above();
	}

	/** A 3x3 shelter: four corner posts, a roof, and a lamp underneath. */
	private static void stall(ServerLevel level, BlockPos corner) {
		BlockState post = Blocks.OAK_LOG.defaultBlockState();
		BlockState roof = Blocks.OAK_PLANKS.defaultBlockState();

		for (int dx = 0; dx <= 2; dx += 2) {
			for (int dz = 0; dz <= 2; dz += 2) {
				for (int y = 0; y < 3; y++) {
					level.setBlockAndUpdate(corner.offset(dx, y, dz), post);
				}
			}
		}
		for (int dx = -1; dx <= 3; dx++) {
			for (int dz = -1; dz <= 3; dz++) {
				level.setBlockAndUpdate(corner.offset(dx, 3, dz), roof);
			}
		}
		level.setBlockAndUpdate(corner.offset(1, 2, 1), Blocks.GLOWSTONE.defaultBlockState());
	}
}
