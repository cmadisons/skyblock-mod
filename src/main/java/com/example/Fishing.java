package com.example;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What comes up on the end of the line.
 *
 * The drop weights below are the game's own fishing table, so the odds here are
 * the real odds: a Raw Fish is 55% of the ordinary pool, an Enchanted Diamond
 * is 23% of the rare one, and a Legendary Guardian Pet is a tenth of a percent
 * of a pool you see once in two hundred casts.
 *
 * How a catch is decided
 * ---------------------
 * Three pools, picked before the item is:
 *
 *   95%     ordinary -- vanilla has already given you a fish, so nothing more
 *   4.5%    GOOD CATCH!
 *   0.5%    GREAT CATCH!
 *
 * Those two percentages are worked back from the table itself. Coins appear as
 * 17.0455% of the GOOD pool and 0.767% overall, and one divided by the other is
 * 4.5%; the same sum on the GREAT pool gives 0.5%.
 *
 * Only the good and great pools hand anything over, because vanilla fishing
 * already produced a fish before this runs. Rolling the ordinary pool as well
 * would hand you two fish for one cast.
 */
public final class Fishing {
	private Fishing() {
	}

	/** One possible catch: what it is, how likely, and what it teaches you. */
	private record Catch(Item item, int count, int weight, int xp, String name) {
	}

	/** How often each pool comes up, out of a thousand casts. */
	private static final int GREAT_IN_THOUSAND = 5;      // 0.5%
	private static final int GOOD_IN_THOUSAND = 45;      // 4.5%

	/**
	 * GOOD CATCH! — the everyday surprise.
	 *
	 * Weights are the table's: coins, bait and music discs at 15 each, golden
	 * apples and experience bottles at 10, sea lanterns 8, and the prismarine
	 * and sponge at 5.
	 */
	private static final Catch[] GOOD = {
			new Catch(Items.GOLD_NUGGET, 8, 15, 160, "Coins"),
			new Catch(Items.STRING, 4, 15, 160, "Fishing Bait"),
			new Catch(Items.GOLDEN_APPLE, 1, 10, 160, "Golden Apple"),
			new Catch(Items.EXPERIENCE_BOTTLE, 8, 10, 160, "Grand Experience Bottle"),
			new Catch(Items.MUSIC_DISC_CAT, 1, 15, 160, "Music Disc"),
			new Catch(Items.PRISMARINE_SHARD, 4, 5, 50, "Prismarine Shard"),
			new Catch(Items.PRISMARINE_CRYSTALS, 4, 5, 50, "Prismarine Crystals"),
			new Catch(Items.SEA_LANTERN, 2, 8, 160, "Sea Lantern"),
			new Catch(Items.SPONGE, 1, 5, 160, "Sponge"),
	};

	/**
	 * GREAT CATCH! — the one worth shouting about.
	 *
	 * The Enchanted Diamond is the heaviest at 240, which is why it turns up
	 * more than anything else here despite sounding rarer. The Guardian Pets
	 * run 16, 8, 4, 2, 1 down the rarities, so a Legendary is one in a thousand
	 * of a pool you see once in two hundred casts.
	 */
	private static final Catch[] GREAT = {
			new Catch(Items.DIAMOND, 4, 240, 300, "Enchanted Diamond"),
			new Catch(Items.CLAY_BALL, 32, 160, 300, "Enchanted Clay"),
			new Catch(Items.GOLD_INGOT, 8, 160, 300, "Enchanted Gold"),
			new Catch(Items.ENCHANTED_GOLDEN_APPLE, 1, 160, 300, "Enchanted Golden Apple"),
			new Catch(Items.IRON_INGOT, 8, 160, 300, "Enchanted Iron"),
			new Catch(Items.PUFFERFISH, 16, 80, 300, "Enchanted Pufferfish"),
			new Catch(Items.EXPERIENCE_BOTTLE, 32, 40, 300, "Titanic Experience Bottle"),
			new Catch(Items.GOLD_NUGGET, 16, 8, 300, "Coins"),
			new Catch(Items.PRISMARINE_SHARD, 1, 16, 300, "Common Guardian Pet"),
			new Catch(Items.PRISMARINE_SHARD, 2, 8, 300, "Uncommon Guardian Pet"),
			new Catch(Items.PRISMARINE_CRYSTALS, 1, 4, 300, "Rare Guardian Pet"),
			new Catch(Items.HEART_OF_THE_SEA, 1, 2, 300, "Epic Guardian Pet"),
			new Catch(Items.NAUTILUS_SHELL, 1, 1, 300, "Legendary Guardian Pet"),
			new Catch(Items.NOTE_BLOCK, 1, 3, 300, "Music Rune I"),
	};

	/**
	 * Roll a catch for a player who has just reeled something in.
	 *
	 * Called from the fishing mixin, after vanilla has handed over its fish.
	 */
	public static void reeled(ServerPlayer player) {
		if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)
				|| !SkyBlocksMod.allowed(player, level)) {
			return;
		}

		// The world clock decides, so a cast is not something you can reroll by
		// saving and reloading.
		int roll = (int) Math.floorMod(level.getGameTime() * 7919, 1000);

		if (roll < GREAT_IN_THOUSAND) {
			give(player, pick(GREAT, level.getGameTime()), "GREAT CATCH!", ChatFormatting.GOLD);
		} else if (roll < GREAT_IN_THOUSAND + GOOD_IN_THOUSAND) {
			give(player, pick(GOOD, level.getGameTime()), "GOOD CATCH!", ChatFormatting.AQUA);
		}
		// Otherwise the ordinary pool, which vanilla has already handled.
	}

	/**
	 * Pick from a pool by weight.
	 *
	 * Heavier entries fill more of the range, which is exactly what a drop
	 * weight means: 240 out of a 1,038-point pool is a 23% chance.
	 */
	private static Catch pick(Catch[] pool, long seed) {
		int total = 0;
		for (Catch entry : pool) {
			total += entry.weight();
		}
		int at = (int) Math.floorMod(seed * 31, total);
		for (Catch entry : pool) {
			at -= entry.weight();
			if (at < 0) {
				return entry;
			}
		}
		return pool[0];
	}

	/** Hand it over, say so, and make a noise about it. */
	private static void give(ServerPlayer player, Catch caught, String shout,
			ChatFormatting colour) {
		ItemStack stack = new ItemStack(caught.item(), caught.count());
		stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
				Component.literal(caught.name()).withStyle(colour));
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}

		// Fishing has no skill of its own here -- these seven have Hunting
		// instead, and catching things out of the water is the nearest thing
		// to it, so that is where the experience goes.
		Skills.add(player, Skills.HUNTING, caught.xp());

		player.sendSystemMessage(Component.literal("")
				.append(Component.literal(shout + " ").withStyle(colour))
				.append(Component.literal(caught.name()).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" (+" + caught.xp() + " Hunting)")
						.withStyle(ChatFormatting.GRAY)));

		player.level().playSound(null, player.blockPosition(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f,
				shout.startsWith("GREAT") ? 1.4f : 1.0f);
	}
}
