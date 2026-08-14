package com.example;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * Places areas that were built by hand in Minecraft rather than written out in
 * Java.
 *
 * Code is the right tool for the starting island: it is small, and it has to be
 * buildable anywhere, which is what {@code /skyblock island} needs. It is the
 * wrong tool for scenery. Every lamp and every stair in the Hub is a line of
 * Java that you cannot see without launching the game.
 *
 * So the Hub can come from a structure file instead. You build it in Creative
 * with your own hands, save it with a Structure Block, and drop the file into
 * the mod. Nothing here is required: when there is no file, the caller falls
 * back to its built-in version, so the mod works exactly as before.
 *
 * <h2>Exporting one</h2>
 *
 * <ol>
 * <li>Make an ordinary Creative superflat world. Sky Blocks switches itself off
 *     in Creative, so it will not interfere.</li>
 * <li>Build the thing.</li>
 * <li>Place a Structure Block ({@code /give @s structure_block}), set it to
 *     SAVE, name it {@code skyblocks:hub}, size the box around your build and
 *     press SAVE.</li>
 * <li>It lands in {@code <that world>/generated/skyblocks/structure/hub.nbt}.
 *     Copy it to {@code src/main/resources/data/skyblocks/structure/hub.nbt}
 *     and rebuild.</li>
 * </ol>
 *
 * Both {@code .nbt} and the text {@code .snbt} form are read from a data pack,
 * so either is fine to ship. Keep a structure under 48 blocks in each direction
 * -- that is as much as one Structure Block can hold.
 */
public final class Structures {
	private Structures() {
	}

	/**
	 * Place a structure with its bottom layer at {@code floor} and its footprint
	 * centred on that column.
	 *
	 * Centring is what makes a structure a drop-in replacement for code: the
	 * middle of whatever you built lands on the middle the code already uses, so
	 * the portals and the teleport targets stay where they are and you can
	 * resize the build freely.
	 *
	 * @param name  the file's name, so {@code "hub"} for
	 *              {@code data/skyblocks/structure/hub.nbt}
	 * @param floor where the lowest layer of the structure goes -- for the Hub,
	 *              the plaza floor
	 * @return whether a structure was found and placed; false means the caller
	 *         should build its own
	 */
	public static boolean placeCentred(ServerLevel level, String name, BlockPos floor) {
		StructureTemplateManager manager = level.getStructureManager();
		Optional<StructureTemplate> found = manager.get(SkyBlocksMod.id(name));
		if (found.isEmpty()) {
			return false;                          // no file shipped: not an error
		}

		StructureTemplate template = found.get();
		Vec3i size = template.getSize();
		if (size.getX() == 0 || size.getY() == 0 || size.getZ() == 0) {
			// An empty Structure Block save. Better to build the code version
			// than to drop nothing at all and leave a portal into the void.
			SkyBlocksMod.LOGGER.warn("Structure {} is empty -- using the built-in build instead.", name);
			return false;
		}

		// placeInWorld anchors at the structure's lowest corner, not its middle.
		BlockPos origin = floor.offset(-size.getX() / 2, 0, -size.getZ() / 2);
		template.placeInWorld(level, origin, origin, new StructurePlaceSettings(),
				level.getRandom(), Block.UPDATE_ALL);

		SkyBlocksMod.LOGGER.info("Placed structure {} ({}x{}x{}) from {}",
				name, size.getX(), size.getY(), size.getZ(), origin);
		return true;
	}
}
