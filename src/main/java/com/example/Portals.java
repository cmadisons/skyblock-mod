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

	/** On the far edge of the portal island, out across the gap from your start. */
	private static final BlockPos ISLAND_PORTAL = new BlockPos(0, 64, 14);

	/** The way back, on the far side of the Hub plaza. */
	private static BlockPos hubPortal = Hub.CENTRE.offset(0, 0, 8);

	/**
	 * Who has just teleported, and until when.
	 *
	 * Without this you would arrive inside the far portal, be detected again
	 * immediately, and ping-pong between the two forever.
	 */
	private static final Map<UUID, Long> cooldown = new HashMap<>();
	private static final long COOLDOWN_TICKS = 60;          // three seconds

	public static BlockPos islandPortal() {
		return ISLAND_PORTAL;
	}

	public static BlockPos hubPortal() {
		return hubPortal;
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

			if (box.intersects(doorway(ISLAND_PORTAL))) {
				if (!Hub.exists(level)) {
					Hub.build(level);                      // built on first visit
					SkyBlocksMod.LOGGER.info("Built the Hub at {}", Hub.CENTRE);
				}
				send(player, level, Hub.CENTRE.above(), "Welcome to the Hub.");
			} else if (box.intersects(doorway(hubPortal))) {
				send(player, level, new BlockPos(0, 65, 0), "Back to your island.");
			}
		}
	}

	private static void send(ServerPlayer player, ServerLevel level, BlockPos to, String message) {
		player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
		player.sendSystemMessage(Component.literal(message));
		cooldown.put(player.getUUID(), level.getGameTime() + COOLDOWN_TICKS);
	}
}
