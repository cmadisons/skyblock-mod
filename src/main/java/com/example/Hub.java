package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Hub: ten by ten, grey, and empty.
 *
 * It used to be a whole town -- two dozen buildings at their measured
 * coordinates, four districts, a graveyard, a spider's den, and twelve
 * villagers standing where a coordinate guide said they stood. All of it was
 * invented. The positions were real and the buildings were guesses, and a
 * guessed building is worse than no building: it is in the way, it looks wrong,
 * and taking it down is the first thing you have to do before you can put the
 * right one up.
 *
 * So there is nothing here now. A ten by ten square of grey concrete, a portal
 * home in the middle of it, and not one person standing on it. Everything the
 * mod used to place -- the Bank, the Bazaar, the Blacksmith, Melody, the lot --
 * is an item in the Creative inventory instead, and where any of it goes is
 * your decision rather than this file's.
 *
 * What makes that worth doing is {@link HubEdit}: every block you place here in
 * Creative is saved outside the world and painted back on in every world you
 * ever make. So the grey square is a canvas, and the Hub you build on it is the
 * Hub you keep.
 */
public final class Hub {
	private Hub() {
	}

	/** The middle of the square, and where the portal home sits. */
	public static final BlockPos CENTRE = new BlockPos(0, 64, 1000);

	/** Ten by ten, so five each way with one to spare. */
	private static final int WEST = -5;
	private static final int EAST = 4;
	private static final int NORTH = -5;
	private static final int SOUTH = 4;

	/**
	 * Where you land: three blocks south of the portal.
	 *
	 * Off the pad on purpose -- landing on it would send you straight home
	 * again -- and still inside the square, which a ten-wide platform does not
	 * leave much room for.
	 */
	public static BlockPos arrival() {
		return CENTRE.offset(0, 1, 3);
	}

	public static boolean exists(ServerLevel level) {
		return !level.getBlockState(CENTRE.below()).isAir();
	}

	/**
	 * Lay the square down, put the portal in it, and put your own changes back.
	 *
	 * Wrapped in {@link HubEdit#whileBuilding} so the hundred blocks below are
	 * not mistaken for something you built and recorded straight back.
	 */
	public static BlockPos build(ServerLevel level) {
		HubEdit.whileBuilding(() -> {
			BlockState floor = Blocks.GRAY_CONCRETE.defaultBlockState();
			for (int x = WEST; x <= EAST; x++) {
				for (int z = NORTH; z <= SOUTH; z++) {
					level.setBlockAndUpdate(CENTRE.offset(x, -1, z), floor);
					// Anything standing on the square is cleared, so /hub on an
					// old world gives you the same empty platform as a new one.
					for (int y = 0; y < 6; y++) {
						level.setBlockAndUpdate(CENTRE.offset(x, y, z),
								Blocks.AIR.defaultBlockState());
					}
				}
			}
			Portals.pad(level, CENTRE);                       // the way home
		});
		HubEdit.replay(level);
		return arrival();
	}
}
