package com.example;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Coins, and the shop that pays them.
 *
 * The one rule worth knowing: never sell raw. A block is always worth more
 * than the nine things that went into it, so compressing before selling is
 * how you actually make money -- the same trick that runs the real game's
 * economy. Nine coal sells for 36; a block of coal sells for 45.
 *
 * Coins live on the player and are saved with them, and survive dying.
 */
public final class Economy {
	private Economy() {
	}

	/** How many coins a player has. */
	public static final AttachmentType<Long> COINS =
			AttachmentRegistry.<Long>builder()
					.initializer(() -> 0L)
					.persistent(Codec.LONG)
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("coins"));

	/**
	 * Coins kept in the bank.
	 *
	 * This is the whole point of a bank: {@link #onDeath} takes half of what
	 * you are carrying and never touches what is in here.
	 */
	public static final AttachmentType<Long> BANK =
			AttachmentRegistry.<Long>builder()
					.initializer(() -> 0L)
					.persistent(Codec.LONG)
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("bank"));

	/**
	 * The day number of the last /daily claim, counted from the world's own
	 * clock rather than real time, so it can't be gamed by changing the
	 * computer's date.
	 */
	public static final AttachmentType<Long> LAST_DAILY =
			AttachmentRegistry.<Long>builder()
					.initializer(() -> -1L)
					.persistent(Codec.LONG)
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("last_daily"));

	/**
	 * What the shop pays, per item.
	 *
	 * Anything not listed can't be sold at all, which keeps junk out of the
	 * economy and stops dirt from being a career.
	 */
	private static final Map<Item, Long> PRICES = new HashMap<>();

	static {
		// --- raw materials, deliberately poor ---------------------------------
		PRICES.put(Items.COBBLESTONE, 1L);
		PRICES.put(Items.STONE, 2L);
		PRICES.put(Items.COAL, 4L);
		PRICES.put(Items.RAW_IRON, 6L);
		PRICES.put(Items.IRON_INGOT, 8L);
		PRICES.put(Items.RAW_GOLD, 9L);
		PRICES.put(Items.GOLD_INGOT, 12L);
		PRICES.put(Items.REDSTONE, 2L);
		PRICES.put(Items.LAPIS_LAZULI, 2L);
		PRICES.put(Items.QUARTZ, 5L);
		PRICES.put(Items.DIAMOND, 40L);
		PRICES.put(Items.EMERALD, 40L);
		PRICES.put(Items.NETHERITE_INGOT, 900L);

		// --- compressed, always better than the sum of its parts --------------
		PRICES.put(Items.COAL_BLOCK, 45L);          // 9 coal would be 36
		PRICES.put(Items.IRON_BLOCK, 85L);          // 9 ingots would be 72
		PRICES.put(Items.GOLD_BLOCK, 130L);
		PRICES.put(Items.REDSTONE_BLOCK, 22L);
		PRICES.put(Items.LAPIS_BLOCK, 22L);
		PRICES.put(Items.DIAMOND_BLOCK, 420L);      // 9 diamonds would be 360
		PRICES.put(Items.EMERALD_BLOCK, 420L);
		PRICES.put(Items.NETHERITE_BLOCK, 9500L);   // the best thing you can sell

		// --- wood and farming --------------------------------------------------
		PRICES.put(Items.OAK_LOG, 3L);
		PRICES.put(Items.OAK_PLANKS, 1L);
		PRICES.put(Items.APPLE, 5L);
		PRICES.put(Items.WHEAT, 3L);
		PRICES.put(Items.CARROT, 3L);
		PRICES.put(Items.POTATO, 3L);
		PRICES.put(Items.PUMPKIN, 6L);
		PRICES.put(Items.MELON, 4L);
		PRICES.put(Items.SUGAR_CANE, 2L);
		PRICES.put(Items.HAY_BLOCK, 30L);           // 9 wheat would be 27
	}

	public static long coins(ServerPlayer player) {
		return player.getAttachedOrCreate(COINS, () -> 0L);
	}

	public static long bank(ServerPlayer player) {
		return player.getAttachedOrCreate(BANK, () -> 0L);
	}

	/** What the shop pays for one of these, or null if it won't buy them. */
	public static Long sellPrice(net.minecraft.world.item.Item item) {
		return PRICES.get(item);
	}

	/** What the bazaar charges: double what it pays, as a shop does. */
	public static Long buyPrice(net.minecraft.world.item.Item item) {
		Long sell = PRICES.get(item);
		return sell == null ? null : sell * 2;
	}

	/** Everything the shop deals in, for command suggestions. */
	public static java.util.Set<net.minecraft.world.item.Item> stock() {
		return PRICES.keySet();
	}

	public static void give(ServerPlayer player, long amount) {
		player.setAttached(COINS, coins(player) + amount);
	}

	/**
	 * Dying costs you half your coins.
	 *
	 * Rounded down, so one coin becomes none and you can't sit on an odd coin
	 * forever. This is the whole reason a bank is worth building later: coins
	 * you are carrying are at risk, coins you have put away are not.
	 */
	public static void onDeath(ServerPlayer player) {
		long had = coins(player);
		if (had <= 0) {
			return;
		}
		long lost = had / 2;
		player.setAttached(COINS, had - lost);
		player.sendSystemMessage(Component.literal(
				"§cYou died and lost §6" + pretty(lost) + " coins§c. "
						+ "§7(" + pretty(had - lost) + " left)"));
	}

	/** Nicely spaced number, so 1234567 reads as 1,234,567. */
	public static String pretty(long n) {
		return String.format("%,d", n);
	}
	/**
	 * Sell whatever the player is holding.
	 *
	 * The shopkeeper's half of what /sell used to do. Unsellable things are
	 * left exactly where they are rather than quietly eaten, so this can never
	 * cost you a tool.
	 */
	public static void sellHeld(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();
		Long price = PRICES.get(held.getItem());
		if (price == null || held.isEmpty()) {
			player.sendSystemMessage(Component.literal(
					"\u00a7cNothing there I have a price for. Compress it first \u2014 "
							+ "blocks beat what goes into them."));
			return;
		}
		long earned = price * held.getCount();
		int sold = held.getCount();
		held.setCount(0);
		give(player, earned);
		player.sendSystemMessage(Component.literal(
				"\u00a7aSold " + sold + " for \u00a76" + pretty(earned) + " coins\u00a7a. "
						+ "You now have \u00a76" + pretty(coins(player)) + "\u00a7a."));
	}
}
