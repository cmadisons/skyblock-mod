package com.example;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Who owns which island.
 *
 * On a server everybody needs an island of their own, so islands are laid out
 * along a row heading east from spawn: slot 0 at x=0, slot 1 at x=1000, and so
 * on. A player is given the next unused slot the first time they join, and it
 * is saved on them, so they come back to the same island forever.
 *
 * <h2>Why a row rather than a grid</h2>
 *
 * Because the Hub sits at z=+1000 and the Arena at z=-1000. Growing the islands
 * along z would eventually drop one on top of them; growing along x alone
 * cannot. It also keeps slot 0 at (0, 64, 0), exactly where the single island
 * used to be, so a world made before any of this still works and its owner
 * keeps their island.
 *
 * <h2>Why not one world per player</h2>
 *
 * Every loaded dimension costs chunk storage and tick time whether or not
 * anybody is on it. One world with islands spread across it lets Minecraft
 * unload the ones nobody is visiting, which is how a server survives more than
 * a handful of players.
 */
public final class Islands {
	private Islands() {
	}

	/**
	 * Blocks between one island and the next.
	 *
	 * Far enough that you cannot see your neighbour, and cannot casually bridge
	 * to them either -- reaching someone else's island should take real effort.
	 */
	public static final int SPACING = 1000;

	/** The height every island's grass sits at. */
	public static final int Y = 64;

	/**
	 * Which island this player owns.
	 *
	 * No initializer on purpose: unset means "has never been given one", which
	 * is how {@link #slot} tells a new player from a returning one.
	 *
	 * copyOnDeath is essential -- respawning builds a fresh player entity, and
	 * without it dying would lose you your island and hand you a new one.
	 */
	public static final AttachmentType<Integer> SLOT =
			AttachmentRegistry.<Integer>builder()
					.persistent(Codec.INT)
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("island_slot"));

	/**
	 * The lowest slot nobody has claimed yet, kept on the world rather than on
	 * any one player -- it is the server's counter, not anybody's property.
	 */
	public static final AttachmentType<Integer> NEXT_SLOT =
			AttachmentRegistry.<Integer>builder()
					.initializer(() -> 0)
					.persistent(Codec.INT)
					.buildAndRegister(SkyBlocksMod.id("next_island_slot"));

	/** Middle of the island in a given slot, at grass height. */
	public static BlockPos centreOf(int slot) {
		return new BlockPos(slot * SPACING, Y, 0);
	}

	/** Which slot this player owns, or null if they have never been given one. */
	public static Integer slot(ServerPlayer player) {
		return player.getAttached(SLOT);
	}

	/**
	 * The slot a player would get if they claimed one now.
	 *
	 * Kept separate from {@link #claim} so a join can look at where an island
	 * would go, decide the world isn't a Sky Blocks world after all, and walk
	 * away without having burnt a slot.
	 */
	public static int peekNext(ServerLevel level) {
		return level.getAttachedOrCreate(NEXT_SLOT, () -> 0);
	}

	/** Hand a player the given slot and move the counter past it. */
	public static void claim(ServerLevel level, ServerPlayer player, int slot) {
		player.setAttached(SLOT, slot);
		level.setAttached(NEXT_SLOT, Math.max(peekNext(level), slot + 1));
		SkyBlocksMod.LOGGER.info("{} claimed island slot {} at {}",
				player.getGameProfile().name(), slot, centreOf(slot));
	}

	/** Where to stand this player when they come home, or null if they have no island. */
	public static BlockPos homeOf(ServerPlayer player) {
		Integer slot = slot(player);
		return slot == null ? null : centreOf(slot).above();
	}

	/**
	 * Which island's row position an x coordinate falls nearest to.
	 *
	 * Used so that standing in a portal works on anybody's island, not just
	 * your own -- visiting a friend and stepping into their portal should take
	 * you to the Hub rather than doing nothing.
	 */
	public static int nearestSlot(double x) {
		return Math.max(0, (int) Math.round(x / SPACING));
	}
}
