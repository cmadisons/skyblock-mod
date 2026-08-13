package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/**
 * Builds a starting island: the dirt platform, a tree, and the chest that gets
 * you going.
 *
 * The layout is the classic one. Small on purpose -- the whole point is that
 * you have almost nothing and have to stretch it.
 */
public final class Island {
	private Island() {
	}

	/** How far the dirt reaches from the middle, so 2 gives a 5x5 platform. */
	private static final int RADIUS = 2;

	/**
	 * Build an island with its grass surface at {@code centre}.
	 *
	 * @return the position to stand you on, one block above the grass.
	 */
	public static BlockPos build(ServerLevel level, BlockPos centre) {
		// Two layers of dirt with grass on top. Dirt below so a tree has
		// something to sit in once you've dug the grass up.
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				level.setBlockAndUpdate(centre.offset(dx, -2, dz), Blocks.DIRT.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, -1, dz), Blocks.DIRT.defaultBlockState());
				level.setBlockAndUpdate(centre.offset(dx, 0, dz), Blocks.GRASS_BLOCK.defaultBlockState());
			}
		}

		tree(level, centre.offset(-1, 1, -1));
		chest(level, centre.offset(1, 1, 1));

		return centre.above();
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

	/**
	 * The starting chest. Lava and ice are the important pair: put them
	 * together and you get cobblestone, which is the only block you can make
	 * an unlimited amount of.
	 */
	private static void chest(ServerLevel level, BlockPos pos) {
		level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());

		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof ChestBlockEntity box)) {
			return;             // something else got placed there; nothing to fill
		}
		box.setItem(0, new ItemStack(Items.LAVA_BUCKET));
		box.setItem(1, new ItemStack(Items.ICE, 2));
		box.setItem(2, new ItemStack(Items.OAK_SAPLING, 2));
		box.setItem(3, new ItemStack(Items.BONE_MEAL, 8));
		box.setItem(4, new ItemStack(Items.WHEAT_SEEDS, 3));
		box.setItem(5, new ItemStack(Items.PUMPKIN_SEEDS));
		box.setItem(6, new ItemStack(Items.MELON_SEEDS));
		box.setItem(7, new ItemStack(Items.RED_MUSHROOM));
		box.setItem(8, new ItemStack(Items.BROWN_MUSHROOM));
		box.setChanged();
	}
}
