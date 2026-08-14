package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The portals between your island and the Hub.
 *
 * These aren't real Minecraft portals. A nether portal would send you to the
 * Nether and there is no way to retarget one without rewriting vanilla, so
 * instead these are obsidian doorways with empty middles, and the mod watches
 * for somebody standing in the gap. Simpler, and it behaves exactly the same
 * from the player's side.
 */
public final class Portals {
	private Portals() {
	}

	/**
	 * How far out from an island's middle its portal sits, across the gap.
	 *
	 * An offset rather than a fixed position, because every player has their own
	 * island and so their own portal. See {@link Islands}.
	 */
	private static final int PORTAL_DISTANCE = 14;

	/** The way back, on the far side of the Hub plaza. Shared by everyone. */
	private static final BlockPos HUB_PORTAL = Hub.CENTRE.offset(0, 0, 8);

	/** On the Hub, opposite the way home: into the Combat Arena. */
	private static final BlockPos HUB_ARENA_GATE = Hub.CENTRE.offset(0, 0, -8);

	/** Inside the Arena: the way back to the Hub. */
	private static final BlockPos ARENA_GATE = Arena.CENTRE.offset(0, 0, 8);

	/**
	 * Who has just teleported, and until when.
	 *
	 * Without this you would arrive inside the far portal, be detected again
	 * immediately, and ping-pong between the two forever.
	 */
	private static final Map<UUID, Long> cooldown = new HashMap<>();
	private static final long COOLDOWN_TICKS = 60;          // three seconds

	/** The portal belonging to the island centred at {@code home}. */
	public static BlockPos islandPortal(BlockPos home) {
		return home.offset(0, 0, PORTAL_DISTANCE);
	}

	public static BlockPos hubPortal() {
		return HUB_PORTAL;
	}

	public static BlockPos hubArenaGate() {
		return HUB_ARENA_GATE;
	}

	public static BlockPos arenaGate() {
		return ARENA_GATE;
	}

	/**
	 * Build a doorway with its bottom edge at {@code base}.
	 *
	 * Four wide and five tall on the outside, leaving a 2x3 hole to walk
	 * through — the same shape as a nether portal, so it reads as one.
	 */
	public static void frame(ServerLevel level, BlockPos base) {
		BlockState edge = Blocks.OBSIDIAN.defaultBlockState();
		BlockState lit = Blocks.CRYING_OBSIDIAN.defaultBlockState();

		for (int dx = -1; dx <= 2; dx++) {
			for (int y = -1; y <= 3; y++) {
				boolean isEdge = dx == -1 || dx == 2 || y == -1 || y == 3;
				if (!isEdge) {
					// The gap you walk into: cleared, so an old build doesn't
					// leave blocks in the doorway.
					level.setBlockAndUpdate(base.offset(dx, y, 0), Blocks.AIR.defaultBlockState());
					continue;
				}
				// Crying obsidian on the uprights gives it the purple glow.
				boolean upright = (dx == -1 || dx == 2) && y >= 0 && y <= 2;
				level.setBlockAndUpdate(base.offset(dx, y, 0), upright ? lit : edge);
			}
		}
		// A step in front so you can walk in from ground level.
		for (int dx = -1; dx <= 2; dx++) {
			level.setBlockAndUpdate(base.offset(dx, -1, 1), Blocks.POLISHED_ANDESITE.defaultBlockState());
			level.setBlockAndUpdate(base.offset(dx, -1, -1), Blocks.POLISHED_ANDESITE.defaultBlockState());
		}
	}

	/** The space a player has to be standing in to be sent somewhere. */
	private static AABB doorway(BlockPos base) {
		return new AABB(base.getX() - 0.4, base.getY(), base.getZ() - 0.4,
				base.getX() + 1.4, base.getY() + 3.0, base.getZ() + 1.4);
	}

	/**
	 * Called every tick. Sends anyone standing in a doorway to the other end.
	 *
	 * Deliberately cheap: it only looks at players, and only compares two
	 * boxes per player, so it costs nothing on a normal world.
	 */
	public static void tick(ServerLevel level) {
		long now = level.getGameTime();

		for (ServerPlayer player : level.players()) {
			if (!SkyBlocksMod.allowed(player, level)) {
				continue;                                  // survival only
			}
			Long until = cooldown.get(player.getUUID());
			if (until != null && now < until) {
				continue;
			}

			AABB box = player.getBoundingBox();

			// Whichever island the player is standing on, not necessarily their
			// own -- stepping into a friend's portal should work like your own.
			BlockPos nearbyIsland = Islands.centreOf(Islands.nearestSlot(player.getX()));

			if (box.intersects(doorway(islandPortal(nearbyIsland)))) {
				if (!Hub.exists(level)) {
					Hub.build(level);                      // built on first visit
					SkyBlocksMod.LOGGER.info("Built the Hub at {}", Hub.CENTRE);
				}
				send(player, level, Hub.CENTRE.above(), "Welcome to the Hub.");
			} else if (box.intersects(doorway(HUB_PORTAL))) {
				BlockPos home = Islands.homeOf(player);
				if (home == null) {
					continue;                              // no island of their own yet
				}
				send(player, level, home, "Back to your island.");
			} else if (box.intersects(doorway(HUB_ARENA_GATE))) {
				if (!Arena.exists(level)) {
					Arena.build(level);                    // built on first visit
					SkyBlocksMod.LOGGER.info("Built the Combat Arena at {}", Arena.CENTRE);
				}
				send(player, level, Arena.CENTRE.above(), "The Combat Arena — mind yourself.");
			} else if (box.intersects(doorway(ARENA_GATE))) {
				send(player, level, Hub.CENTRE.above(), "Back to the Hub.");
			}
		}
	}

	private static void send(ServerPlayer player, ServerLevel level, BlockPos to, String message) {
		player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
		player.sendSystemMessage(Component.literal(message));
		cooldown.put(player.getUUID(), level.getGameTime() + COOLDOWN_TICKS);
	}
}
