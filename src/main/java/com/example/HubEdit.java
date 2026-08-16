package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Change the Hub in Creative and it stays changed -- in every world, forever.
 *
 * The Hub this mod builds is a guess. It is the right shape in the right
 * places, because the districts and the coordinates are real, but the buildings
 * are invented and they are not what you remember. The obvious fix is to go and
 * fix them yourself, and the obvious problem with that is that a world is one
 * world: rebuild the Bank beautifully, start a new save, and it is the ugly one
 * again.
 *
 * So every block you change in the Hub while in Creative is written down. Not
 * the Hub -- the changes to it. The next time a Hub is built anywhere, they are
 * played back over the top, so the Bank you rebuilt is the Bank you get, in
 * every world you ever make, and the mod's own version is only ever what is
 * underneath.
 *
 * Why the changes and not the whole thing
 * ---------------------------------------
 * The Hub is about five hundred blocks across and sixty tall, which is upwards
 * of a million blocks. Copying that every time you place a torch would be
 * unusable. A list of changes is a few hundred entries after an afternoon's
 * building, costs nothing to save, and has a property a snapshot does not: put
 * a block back the way it was and the two entries collapse into one, so
 * changing your mind leaves no trace.
 *
 * Survival is left alone. Mining a Hub block while playing normally is playing,
 * not editing, and it would be a nasty surprise if it followed you into the
 * next world -- so only a Creative player's changes are recorded. See
 * {@link #creativeNearby}.
 *
 *   /hub edits    how many changes are saved
 *   /hub apply    play them over the Hub in this world now
 *   /hub reset    throw them away and go back to the built-in Hub
 */
public final class HubEdit {
	private HubEdit() {
	}

	/**
	 * How far out from the Hub's middle counts as the Hub.
	 *
	 * Generous on purpose. The districts sit a long way out -- the Spider's Den
	 * is more than two hundred blocks north -- and a box that stopped at the
	 * Village would quietly fail to record half of what you built.
	 */
	private static final int REACH_X = 280;
	private static final int REACH_NORTH = -420;
	private static final int REACH_SOUTH = 100;
	private static final int REACH_DOWN = -24;
	private static final int REACH_UP = 80;

	/** How near a Creative player has to be for a change to count as editing. */
	private static final int NEARBY = 48;

	/** Every change, by its position relative to {@link Hub#CENTRE}. */
	private static final Map<BlockPos, BlockState> EDITS = new LinkedHashMap<>();

	/** Set while the mod is building or replaying, so it never records itself. */
	private static boolean building;

	private static boolean loaded;
	private static boolean dirty;

	// ------------------------------------------------------------- recording

	/**
	 * Called for every block change in the world. Deliberately cheap.
	 *
	 * This runs on the hot path -- every block placed, broken, grown, burnt or
	 * pushed -- so the three tests are ordered by how quickly they say no: the
	 * building flag first, then arithmetic on the position, and only then the
	 * loop over players.
	 */
	public static void changed(ServerLevel level, BlockPos pos, BlockState state) {
		if (building || !inHub(pos)) {
			return;
		}
		if (!creativeNearby(level, pos)) {
			return;
		}
		BlockPos where = pos.subtract(Hub.CENTRE);
		EDITS.put(where, state);
		dirty = true;
	}

	private static boolean inHub(BlockPos pos) {
		int dx = pos.getX() - Hub.CENTRE.getX();
		int dy = pos.getY() - Hub.CENTRE.getY();
		int dz = pos.getZ() - Hub.CENTRE.getZ();
		return Math.abs(dx) <= REACH_X
				&& dz >= REACH_NORTH && dz <= REACH_SOUTH
				&& dy >= REACH_DOWN && dy <= REACH_UP;
	}

	/**
	 * Is somebody in Creative close enough for this to be their doing?
	 *
	 * A blunt test, and the right one. It cannot tell a player's edit from a
	 * fire spreading beside them, but the alternative -- tracing every block
	 * change back to whatever caused it -- means a hook in every one of the
	 * dozen places Minecraft changes a block, and gets it wrong anyway.
	 */
	private static boolean creativeNearby(ServerLevel level, BlockPos pos) {
		for (ServerPlayer player : level.players()) {
			if (player.gameMode() == GameType.CREATIVE
					&& player.blockPosition().closerThan(pos, NEARBY)) {
				return true;
			}
		}
		return false;
	}

	// -------------------------------------------------------------- playback

	/**
	 * Put every saved change back, over a Hub that has just been built.
	 *
	 * Order matters and is kept: changes are replayed in the order they were
	 * made, so a wall built and then knocked a hole in ends up with the hole.
	 */
	public static int replay(ServerLevel level) {
		load(level);
		if (EDITS.isEmpty()) {
			return 0;
		}
		building = true;
		try {
			for (Map.Entry<BlockPos, BlockState> edit : EDITS.entrySet()) {
				level.setBlockAndUpdate(Hub.CENTRE.offset(edit.getKey()), edit.getValue());
			}
		} finally {
			building = false;
		}
		SkyBlocksMod.LOGGER.info("Put back {} changes you made to the Hub.", EDITS.size());
		return EDITS.size();
	}

	/** Wrap the Hub being built, so the mod's own blocks are not recorded. */
	public static void whileBuilding(Runnable work) {
		building = true;
		try {
			work.run();
		} finally {
			building = false;
		}
	}

	// ------------------------------------------------------------ the file

	/**
	 * Where the changes live.
	 *
	 * In the config folder rather than inside a world, which is the whole
	 * point: a world folder would make them belong to one world, and they are
	 * meant to belong to you.
	 */
	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("skyblocks").resolve("hub_edits.nbt");
	}

	public static void load(ServerLevel level) {
		if (loaded) {
			return;
		}
		loaded = true;
		Path path = file();
		if (!Files.isRegularFile(path)) {
			return;
		}
		try {
			CompoundTag root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
			HolderGetter<Block> lookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
			for (Tag tag : root.getListOrEmpty("edits")) {
				if (!(tag instanceof CompoundTag entry)) {
					continue;
				}
				BlockPos at = new BlockPos(
						entry.getIntOr("x", 0), entry.getIntOr("y", 0), entry.getIntOr("z", 0));
				CompoundTag state = entry.getCompoundOrEmpty("state");
				EDITS.put(at, NbtUtils.readBlockState(lookup, state));
			}
			SkyBlocksMod.LOGGER.info("Loaded {} changes you made to the Hub.", EDITS.size());
		} catch (IOException e) {
			SkyBlocksMod.LOGGER.warn("Couldn't read your Hub changes: {}", e.getMessage());
		}
	}

	/** Write them out, if anything has changed since last time. */
	public static void save() {
		if (!dirty) {
			return;
		}
		dirty = false;
		CompoundTag root = new CompoundTag();
		ListTag edits = new ListTag();
		for (Map.Entry<BlockPos, BlockState> edit : EDITS.entrySet()) {
			CompoundTag entry = new CompoundTag();
			entry.putInt("x", edit.getKey().getX());
			entry.putInt("y", edit.getKey().getY());
			entry.putInt("z", edit.getKey().getZ());
			entry.put("state", NbtUtils.writeBlockState(edit.getValue()));
			edits.add(entry);
		}
		root.put("edits", edits);
		try {
			Path path = file();
			Files.createDirectories(path.getParent());
			NbtIo.writeCompressed(root, path);
		} catch (IOException e) {
			SkyBlocksMod.LOGGER.warn("Couldn't save your Hub changes: {}", e.getMessage());
		}
	}

	/**
	 * Save now and then, rather than on every block.
	 *
	 * Placing blocks in Creative happens faster than a disk write wants to, so
	 * the writing is done every five seconds and only when something actually
	 * changed. Nothing is lost on a crash beyond those five seconds, and the
	 * server also saves on the way out.
	 */
	public static void tick(ServerLevel level) {
		if (level.getGameTime() % 100 == 0) {
			save();
		}
	}

	public static int count() {
		return EDITS.size();
	}

	/** Forget everything, and put the file back to empty. */
	public static void reset() {
		EDITS.clear();
		dirty = true;
		save();
	}

	// -------------------------------------------------------------- commands

}
