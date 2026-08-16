package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The quest log.
 *
 * These are the game's own quests, in its own order, with the NPCs who give
 * them and the rewards they pay. Not all of them -- the ones that make sense
 * here, which is most of the early game and the skill-gated ones, and none of
 * the ones that depend on Slayers, Dungeons or the Dark Auction, since none of
 * those exist in this mod.
 *
 * How they finish
 * --------------
 * By watching, not by asking. Each quest names a skill and a level, and it
 * completes the moment you reach it -- so playing the game finishes them and
 * nothing has to be handed in. A quest with no requirement completes as soon as
 * you have seen the log, which is what "Getting Started" is for.
 *
 * That is a real difference from Hypixel, where you talk to an NPC. Doing it
 * this way means the log is never out of step with what you have actually done,
 * and there is no way to be stuck because you missed somebody.
 */
public final class Quests {
	private Quests() {
	}

	/**
	 * One quest.
	 *
	 * @param skill  the skill that finishes it, or empty to finish immediately
	 * @param level  the level of that skill needed
	 * @param coins  what it pays
	 * @param who    the NPC who gives it, for the log to show
	 */
	public record Quest(String name, String skill, int level, long coins, String who,
			String what) {
	}

	/** The quests, roughly in the order you would meet them. */
	public static final Quest[] ALL = {
			new Quest("Getting Started", "", 0, 0, "Jerry",
					"Break a log, make a pickaxe, bridge to the portal."),
			new Quest("Fishing Tutorial", Skills.HUNTING, 1, 1000, "Fisherwoman Enid",
					"Catch a fish."),
			new Quest("Foraging Tutorial", Skills.FORAGING, 1, 1000, "Lumber Jack",
					"Collect 20 logs."),
			new Quest("First Harvest", Skills.FARMING, 1, 0, "Farmer Rigby",
					"Collect 10 wheat, then visit The Barn."),
			new Quest("Saving Up", "", 0, 10, "Banker",
					"Put coins in the bank."),
			new Quest("Time to Mine", Skills.MINING, 1, 0, "Blacksmith",
					"Mine 10 coal, then reach the Gold Mine."),
			new Quest("Time To Strike", Skills.COMBAT, 1, 100, "Bartender",
					"Kill 10 zombies, then reach the Spider's Den."),
			new Quest("The Flint Bros", Skills.COMBAT, 1, 0, "Rick",
					"Bring Rick two iron ingots in the Spider's Den."),
			new Quest("Back at the Barnyard", Skills.FARMING, 1, 0, "Farmhand",
					"Reach The Barn and gather one of each crop."),
			new Quest("Into the Woods", Skills.FORAGING, 5, 0, "Charlie",
					"Chop through the Park to Foraging V."),
			new Quest("Intermediate Farmer", Skills.FARMING, 5, 0, "Beth",
					"Reach the Mushroom Desert."),
			new Quest("Lost and Found", Skills.MINING, 1, 0, "Lazy Miner",
					"Find the Lazy Miner's pickaxe in the Gold Mine."),
			new Quest("Going Deeper", Skills.MINING, 5, 0, "Lift Operator",
					"Reach the Deep Caverns."),
			new Quest("Helpful Miner", Skills.MINING, 5, 0, "Lapis Miner",
					"Bring the Lapis Miner a lapis pickaxe."),
			new Quest("Woods Racing", Skills.FORAGING, 2, 0, "Gustave",
					"Run the Woods Race in Spruce Woods."),
			new Quest("Trial of Fire", Skills.FORAGING, 3, 0, "Ryan",
					"Face the Trial of Fire in the Dark Thicket."),
			new Quest("Melody", Skills.FORAGING, 4, 0, "Melody",
					"Play Melody's songs on the Savanna Woodland."),
			new Quest("Romero and Juliette", Skills.FORAGING, 5, 0, "Juliette",
					"Help Romero and Juliette on Jungle Island."),
			new Quest("There are Dwarves?", Skills.MINING, 12, 0, "Rhys",
					"Reach the Dwarven Mines."),
			new Quest("Warrior's Quest", Skills.COMBAT, 5, 0, "Elle of the Nether",
					"Gather nether wart and a blaze rod on the Crimson Isle."),
			new Quest("Beginning of The End", Skills.COMBAT, 12, 0, "Pearl Dealer",
					"Reach the Dragon's Nest and fight a dragon."),
			new Quest("The End Race", Skills.COMBAT, 12, 0, "Guber",
					"Run the End Race."),
			new Quest("Explorer", "", 0, 50, "",
					"Find every location. Fast Travel is in the menu."),
	};

	/** Which quests a player has finished, by name. */
	public static final AttachmentType<Map<String, Long>> DONE =
			AttachmentRegistry.<Map<String, Long>>builder()
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("quests_done"));

	public static boolean done(ServerPlayer player, Quest quest) {
		return player.getAttachedOrCreate(DONE, HashMap::new).containsKey(quest.name());
	}

	/** How many are finished, for the menu to show. */
	public static int completed(ServerPlayer player) {
		return player.getAttachedOrCreate(DONE, HashMap::new).size();
	}

	/**
	 * Finish anything that has quietly become true.
	 *
	 * Run a couple of times a second rather than every tick: a skill level does
	 * not change often, and this walks every quest for every player.
	 */
	public static void tick(ServerLevel level) {
		if (level.getGameTime() % 40 != 0) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!SkyBlocksMod.allowed(player, level)) {
				continue;
			}
			for (Quest quest : ALL) {
				if (done(player, quest) || !earned(player, quest)) {
					continue;
				}
				complete(player, quest);
			}
		}
	}

	/** Has this player met what the quest asks for? */
	private static boolean earned(ServerPlayer player, Quest quest) {
		if (quest.skill().isEmpty()) {
			// No requirement: finishes once you are actually playing, which is
			// to say once you have any skill experience at all.
			for (String skill : Skills.ALL) {
				if (Skills.xp(player, skill) > 0) {
					return true;
				}
			}
			return false;
		}
		return Skills.levelIn(player, quest.skill()) >= quest.level();
	}

	/** Mark it done, pay it, and make something of it. */
	private static void complete(ServerPlayer player, Quest quest) {
		Map<String, Long> done = new HashMap<>(player.getAttachedOrCreate(DONE, HashMap::new));
		done.put(quest.name(), 1L);
		player.setAttached(DONE, done);

		if (quest.coins() > 0) {
			Economy.give(player, quest.coins());
		}
		player.sendSystemMessage(Component.literal("")
				.append(Component.literal("QUEST COMPLETE! ").withStyle(ChatFormatting.GREEN))
				.append(Component.literal(quest.name()).withStyle(ChatFormatting.WHITE))
				.append(quest.coins() > 0
						? Component.literal(" (+" + Economy.pretty(quest.coins()) + " coins)")
								.withStyle(ChatFormatting.GOLD)
						: Component.empty()));
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
	}

	/** The quest of that name, or null if this mod doesn't have one. */
	public static Quest byName(String name) {
		for (Quest quest : ALL) {
			if (quest.name().equals(name)) {
				return quest;
			}
		}
		return null;
	}

	/**
	 * Somebody has just told you about their quest.
	 *
	 * The NPCs hand out the quests they hand out in the game, and this is what
	 * happens when they do: you are told what it asks for and what it pays.
	 * Nothing is unlocked by it, because nothing needs to be -- a quest here
	 * finishes when you have actually done the thing, whether or not anybody
	 * told you to. Being told just means you know it exists.
	 *
	 * That is why it says COMPLETED for one you have already finished rather
	 * than pretending to offer it again.
	 */
	public static void offer(ServerPlayer player, String name) {
		Quest quest = byName(name);
		if (quest == null) {
			return;
		}
		if (done(player, quest)) {
			player.sendSystemMessage(Component.literal(
					"  §8[§aCOMPLETED§8] §7" + quest.name()));
			return;
		}
		player.sendSystemMessage(Component.literal("")
				.append(Component.literal("  QUEST  ").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(quest.name()).withStyle(ChatFormatting.WHITE)));
		player.sendSystemMessage(Component.literal("  §7" + quest.what()));
		if (!quest.skill().isEmpty()) {
			player.sendSystemMessage(Component.literal(
					"  §8Needs " + quest.skill() + " " + quest.level()));
		}
		if (quest.coins() > 0) {
			player.sendSystemMessage(Component.literal(
					"  §8Reward: §6" + Economy.pretty(quest.coins()) + " coins"));
		}
	}

	/** The quests still to do, in order, for the log and the menu. */
	public static List<Quest> remaining(ServerPlayer player) {
		List<Quest> left = new ArrayList<>();
		for (Quest quest : ALL) {
			if (!done(player, quest)) {
				left.add(quest);
			}
		}
		return left;
	}

	/** /quests — the log. */
}
