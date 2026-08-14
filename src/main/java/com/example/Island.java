package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * Builds the starting area: your bare little starting island, and -- across a
 * gap of empty void -- a second island holding the portal to the Hub and the
 * cobblestone minion.
 *
 * You begin with almost nothing: 5x5 of grass and a tree. There is no bridge,
 * so to reach the minion (and the way out) you have to dig up your island's
 * dirt and bridge across. Small on purpose -- the whole point is that you have
 * almost nothing and have to stretch it.
 */
public final class Island {
	private Island() {
	}

	/** How far the dirt reaches from the middle, so 2 gives a 5x5 platform. */
	private static final int RADIUS = 2;

	/**
	 * Build the starting island with its grass surface at {@code centre}, plus
	 * the portal island across the void.
	 *
	 * @return the position to stand you on, one block above the grass.
	 */
	public static BlockPos build(ServerLevel level, BlockPos centre) {
		platform(level, centre, RADIUS);
		tree(level, centre.offset(-1, 1, -1));
		buildPortalIsland(level, centre);
		return centre.above();
	}

	/**
	 * The island across the gap: the portal to the Hub at its far edge, and the
	 * cobblestone minion (plus a chest to fill) at the near edge, where you land
	 * after bridging over.
	 *
	 * The minion sits at the near edge on purpose: stepping into the portal
	 * sends you straight to the Hub, so anything behind it would be out of reach.
	 *
	 * Everything here is measured from {@code home} rather than from fixed
	 * coordinates, because on a server this gets built once per player, each at
	 * their own slot along the row. See {@link Islands}.
	 */
	private static void buildPortalIsland(ServerLevel level, BlockPos home) {
		BlockPos portal = Portals.islandPortal(home);   // 14 blocks out, the far edge
		// The platform stays at island height even though the doorway sits a
		// block above it, so take the height from home rather than from portal.
		BlockPos centre = new BlockPos(portal.getX(), home.getY(), portal.getZ() - 2);
		platform(level, centre, RADIUS);
		Portals.frame(level, portal);
		// Minion + chest at the near edge, the side you bridge across to.
		BlockPos near = centre.offset(0, 1, -1);
		level.setBlockAndUpdate(near, Minions.MINIONS[0].defaultBlockState());
		level.setBlockAndUpdate(near.offset(1, 0, 0), Blocks.CHEST.defaultBlockState());
	}

	/**
	 * A (2*radius+1) square of ground with its grass surface at {@code centre}:
	 * two layers of dirt with grass on top, so a tree has something to sit in
	 * once you've dug the grass up.
	 */
	private static void platform(ServerLevel level, BlockPos centre, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -2, dz), Blocks.DIRT.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState());
			}
		}
	}

	/** A small oak, hand-placed rather than grown so it is always the same. */
	private static void tree(ServerLevel level, BlockPos foot) {
		for (int y = 0; y < 4; y++) {
			level.setBlockAndUpdate(foot.above(y), Blocks.OAK_LOG.defaultBlockState());
		}
		// Two wide layers of leaves, then a small cap -- the usual oak shape.
		for (int y = 2; y <= 3; y++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					// Skip the trunk itself and the four far corners.
					if (dx == 0 && dz == 0) continue;
					if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
					leaf(level, foot.offset(dx, y, dz));
				}
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (Math.abs(dx) == 1 && Math.abs(dz) == 1) continue;
				leaf(level, foot.offset(dx, 4, dz));
			}
		}
		leaf(level, foot.above(4));
	}

	/** Only fills empty space, so leaves never eat the trunk. */
	private static void leaf(ServerLevel level, BlockPos pos) {
		if (level.getBlockState(pos).isAir()) {
			level.setBlockAndUpdate(pos, Blocks.OAK_LEAVES.defaultBlockState());
		}
	}

}
