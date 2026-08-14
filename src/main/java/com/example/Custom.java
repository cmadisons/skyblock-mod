package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Buildings you made yourself, used in place of the ones this mod invents.
 *
 * The Hub's buildings are guesses. They are the wrong size, in the wrong place,
 * and missing things, because whoever is playing knows what the real ones look
 * like and this mod does not. Rather than keep guessing, the Blueprint mod lets
 * you build one properly and save it -- and this reads those saves.
 *
 * Drop a blueprint named after a building into config/blueprints and the Hub
 * uses yours instead. Nothing else changes; if the file isn't there, the built
 * in version is used exactly as before, so this is entirely optional.
 *
 *   bank.nbt · community_center.nbt · auction_house.nbt · bazaar_alley.nbt
 *   museum.nbt · pet_care.nbt · builders_house.nbt
 *
 * The format is Blueprint's: compressed NBT holding a size, a palette of block
 * states, and one palette index per position.
 */
public final class Custom {
	private Custom() {
	}

	private static Path fileFor(String name) {
		return FabricLoader.getInstance().getConfigDir()
				.resolve("blueprints").resolve(name + ".nbt");
	}

	/** Has the player saved a building under this name? */
	public static boolean exists(String name) {
		return Files.isRegularFile(fileFor(name));
	}

	/**
	 * Place a saved building, centred on {@code centre} rather than cornered
	 * there, so it lands where the Hub expects the building to be regardless of
	 * how big you made it.
	 *
	 * @return true if it was placed; false if there is no such file, in which
	 *         case the caller should build its own version.
	 */
	public static boolean place(ServerLevel level, BlockPos centre, String name) {
		Path file = fileFor(name);
		if (!Files.isRegularFile(file)) {
			return false;
		}
		try {
			CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			int[] size = root.getIntArray("size").orElse(null);
			if (size == null || size.length != 3) {
				return false;
			}
			int width = size[0];
			int height = size[1];
			int depth = size[2];

			HolderGetter<Block> lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
			List<BlockState> palette = new ArrayList<>();
			for (Tag tag : root.getListOrEmpty("palette")) {
				palette.add(tag instanceof CompoundTag entry
						? NbtUtils.readBlockState(lookup, entry)
						: Blocks.AIR.defaultBlockState());
			}

			// Centred: half the footprint back in x and z, sitting on the ground.
			BlockPos corner = centre.offset(-width / 2, 0, -depth / 2);

			int[] blocks = root.getIntArray("blocks").orElse(new int[0]);
			int at = 0;
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < depth; z++) {
					for (int x = 0; x < width; x++) {
						if (at >= blocks.length) {
							return true;
						}
						int index = blocks[at++];
						if (index >= 0 && index < palette.size()) {
							level.setBlockAndUpdate(corner.offset(x, y, z), palette.get(index));
						}
					}
				}
			}
			SkyBlocksMod.LOGGER.info("Used your saved '{}' instead of the built-in one", name);
			return true;
		} catch (IOException e) {
			SkyBlocksMod.LOGGER.warn("Couldn't read blueprint '{}': {}", name, e.getMessage());
			return false;
		}
	}
}
