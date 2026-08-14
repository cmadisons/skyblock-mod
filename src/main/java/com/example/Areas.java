package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Places you cannot go yet.
 *
 * Hypixel gates its islands behind skills, and the requirements here are the
 * real ones: the Spider's Den needs Combat 1, the Gold Mine needs Mining 1, the
 * Deep Caverns need Mining 5. That is what stops a brand new player wandering
 * into something that will kill them, and it is why those skills are worth
 * levelling in the first place.
 *
 * How it stops you
 * ---------------
 * Not with a wall. Walk in without the level and you are put back where you
 * came from with a line saying what you need -- so it reads as "not yet"
 * rather than as the game being broken. Levelling the skill is the only thing
 * that opens it.
 *
 * Everywhere not listed is open from the start, including the whole Village.
 */
public final class Areas {
	private Areas() {
	}

	/**
	 * One gated area.
	 *
	 * @param real true when the requirement is Hypixel's own, false when it is
	 *             a sensible guess because nothing publishes it.
	 */
	public record Gate(String name, BlockPos where, int radius, String skill, int level,
			boolean real) {
	}

	/**
	 * The gates, positioned to match where {@link Hub} builds each district.
	 *
	 * Radii are generous: the point is to turn you back before you are among
	 * the mobs, not at the exact moment one bites you.
	 */
	public static Gate[] gates() {
		BlockPos hub = Hub.CENTRE;
		BlockPos graveyard = hub.offset(-110, 0, -150).offset(0, 0, -26);
		return new Gate[]{
				// Real: the Spider's Den opens at Combat 1.
				new Gate("Spider's Den", graveyard.offset(0, 0, -60), 30,
						Skills.COMBAT, 1, true),
				// Real: the Gold Mine opens at Mining 1.
				new Gate("Mining District", hub.offset(0, 0, HubMap.northEdge() - 40), 26,
						Skills.MINING, 1, true),
				// The Park's requirement isn't published, so this is a guess --
				// Foraging 1, matching how the other first islands work.
				new Gate("Forest", hub.offset(HubMap.westEdge() - 40, 0, -40), 26,
						Skills.FORAGING, 1, false),
		};
	}

	/**
	 * Turn back anyone who shouldn't be here yet.
	 *
	 * Checked a few times a second rather than every tick: walking is slow
	 * enough that a quarter-second is plenty, and this runs for every player.
	 */
	public static void tick(ServerLevel level) {
		if (level.getGameTime() % 5 != 0 || !Hub.exists(level)) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!SkyBlocksMod.allowed(player, level)) {
				continue;
			}
			// Building mode goes everywhere -- you cannot lay out an area you
			// are not allowed to stand in.
			if (SkyBlocksMod.buildMode()) {
				continue;
			}
			for (Gate gate : gates()) {
				if (!inside(player, gate)) {
					continue;
				}
				int has = Skills.level(Skills.xp(player, gate.skill()));
				if (has >= gate.level()) {
					continue;
				}
				turnBack(player, gate, has);
				break;
			}
		}
	}

	private static boolean inside(ServerPlayer player, Gate gate) {
		return player.blockPosition().closerThan(gate.where(), gate.radius());
	}

	/**
	 * Put them back at the Hub, and say why.
	 *
	 * The message names the skill and the level, because "you can't go here" on
	 * its own is the most annoying thing a game can say.
	 */
	private static void turnBack(ServerPlayer player, Gate gate, int has) {
		BlockPos home = Hub.arrival();
		player.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
		player.sendSystemMessage(Component.literal(
				"§c" + gate.name() + " needs §e" + gate.skill() + " level " + gate.level()
						+ "§c. You are level " + has + "."));
		player.sendSystemMessage(Component.literal(
				"§7" + howTo(gate.skill())));
	}

	/** A hint that actually tells you what to go and do. */
	private static String howTo(String skill) {
		if (skill.equals(Skills.COMBAT)) {
			return "Kill monsters to raise Combat.";
		}
		if (skill.equals(Skills.MINING)) {
			return "Break stone and ore to raise Mining.";
		}
		if (skill.equals(Skills.FORAGING)) {
			return "Chop wood to raise Foraging.";
		}
		return "Raise the skill by using it.";
	}
}
