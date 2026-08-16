package com.example;

import java.util.List;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The bank, the bazaar, and the daily reward.
 *
 * Together these are what turn a pile of coins into something worth having:
 * somewhere safe to keep them, somewhere to spend them, and a reason to log in
 * tomorrow.
 */
public final class Shops {
	private Shops() {
	}

	/** What /daily hands out. */
	private static final long DAILY_COINS = 1_000_000L;

	/** How long a Minecraft day is, in ticks. */
	private static final long DAY = 24_000L;

	/**
	 * Sell everything sellable, for the Auctioneer.
	 *
	 * The same thing /sell all does, done on the spot. An NPC whose whole job
	 * is buying your things should buy them, not read you a command.
	 */
	public static void sellEverything(ServerPlayer player) {
		long earned = 0;
		int sold = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			Long price = Economy.sellPrice(stack.getItem());
			if (price == null || stack.isEmpty()) {
				continue;
			}
			earned += price * stack.getCount();
			sold += stack.getCount();
			player.getInventory().setItem(slot, ItemStack.EMPTY);
		}
		if (sold == 0) {
			player.sendSystemMessage(Component.literal(
					"§6" + Npcs.AUCTIONEER + "§7: nothing there I have a price for. "
							+ "Compress it first -- blocks beat what goes into them."));
			return;
		}
		Economy.give(player, earned);
		player.sendSystemMessage(Component.literal(
				"§6" + Npcs.AUCTIONEER + "§7: sold §f" + sold + "§7 for §6"
						+ Economy.pretty(earned) + " coins§7. You have §6"
						+ Economy.pretty(Economy.coins(player)) + "§7."));
	}

	/**
	 * Show what the Bazaar stocks, for the Bazaar Trader.
	 *
	 * A list with prices, so you can see what is worth buying before typing
	 * anything.
	 */
	public static void openBazaar(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(
				"§6" + Npcs.BAZAAR + "§7: here's what I have."));
		Economy.stock().stream()
				.map(item -> BuiltInRegistries.ITEM.getKey(item).getPath())
				.sorted()
				.limit(20)
				.forEach(name -> {
					Item item = BuiltInRegistries.ITEM
							.getOptional(Identifier.withDefaultNamespace(name)).orElse(null);
					Long cost = item == null ? null : Economy.buyPrice(item);
					player.sendSystemMessage(Component.literal(
							"  §f" + name + " §7— §6" + (cost == null ? "?" : cost)
									+ " coins each"));
				});
		player.sendSystemMessage(Component.literal(
				"§7/bazaar buy <item> <amount>"));
	}

	/**
	 * Claim the daily reward, for the Booster Cookie in the menu.
	 *
	 * The same thing /daily does. A button that describes a command is not a
	 * button, so pressing the cookie gives you the cookie.
	 */
	public static void claimDaily(ServerPlayer player) {
		if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
			return;
		}
		long today = level.getGameTime() / DAY;
		if (player.getAttachedOrCreate(Economy.LAST_DAILY, () -> -1L) == today) {
			player.sendSystemMessage(Component.literal(
					"§cAlready claimed today. Come back tomorrow."));
			return;
		}
		player.setAttached(Economy.LAST_DAILY, today);
		Economy.give(player, DAILY_COINS);

		ItemStack surprise = randomTreat(level.getGameTime());
		for (ItemStack prize : List.of(new ItemStack(Items.DIAMOND_BLOCK, 4),
				new ItemStack(Items.NETHERITE_SCRAP, 2), surprise)) {
			if (!player.getInventory().add(prize.copy())) {
				player.drop(prize.copy(), false);
			}
		}
		player.sendSystemMessage(Component.literal(
				"§aDaily claimed! §6" + Economy.pretty(DAILY_COINS) + " coins§a, "
						+ "4 diamond blocks, 2 netherite scrap, and "
						+ surprise.getCount() + "x " + surprise.getHoverName().getString()));
	}

	/**
	 * One of a handful of treats, picked from the world clock.
	 *
	 * Deliberately not truly random: the same tick always gives the same
	 * thing, which keeps the world reproducible and stops save-scumming.
	 */
	private static ItemStack randomTreat(long gameTime) {
		ItemStack[] treats = {
				new ItemStack(Items.GOLDEN_APPLE, 3),
				new ItemStack(Items.ENDER_PEARL, 8),
				new ItemStack(Items.EXPERIENCE_BOTTLE, 16),
				new ItemStack(Items.EMERALD_BLOCK, 2),
				new ItemStack(Items.TNT, 8),
				new ItemStack(Items.OBSIDIAN, 16),
				new ItemStack(Items.BLAZE_ROD, 6),
		};
		return treats[(int) Math.floorMod(gameTime / 7, treats.length)];
	}

	// ------------------------------------------------------------------- bank

	/**
	 * Move coins between pocket and bank.
	 *
	 * What /bank deposit and /bank withdraw used to do, now reachable from the
	 * Personal Bank page. Refuses rather than going negative, and returns
	 * whether it actually moved anything so the page can say so.
	 *
	 * Worth doing before anything dangerous: dying takes half of what you are
	 * carrying and nothing at all of what is banked.
	 */
	public static boolean move(ServerPlayer player, long amount, boolean depositing) {
		if (amount <= 0) {
			return false;
		}
		long pocket = Economy.coins(player);
		long vault = Economy.bank(player);
		if (depositing && amount > pocket) {
			amount = pocket;                     // "deposit all" rather than a refusal
		}
		if (!depositing && amount > vault) {
			amount = vault;
		}
		if (amount <= 0) {
			player.sendSystemMessage(Component.literal(depositing
					? "\u00a7cNothing to deposit."
					: "\u00a7cThe bank is empty."));
			return false;
		}
		long shift = depositing ? amount : -amount;
		player.setAttached(Economy.COINS, pocket - shift);
		player.setAttached(Economy.BANK, vault + shift);
		player.sendSystemMessage(Component.literal(
				"\u00a7a" + (depositing ? "Deposited " : "Withdrew ") + "\u00a76"
						+ Economy.pretty(amount) + "\u00a7a. Bank: \u00a76"
						+ Economy.pretty(Economy.bank(player)) + "\u00a7a, carrying: \u00a76"
						+ Economy.pretty(Economy.coins(player))));
		return true;
	}

	// ----------------------------------------------------------------- bazaar

	/**
	 * Buy from the Bazaar.
	 *
	 * It charges double what it pays, which is what stops you buying and
	 * selling the same item forever to make free money.
	 */
	public static boolean buy(ServerPlayer player, Item item, int amount) {
		Long each = item == null ? null : Economy.buyPrice(item);
		if (each == null) {
			player.sendSystemMessage(Component.literal("\u00a7cThe Bazaar doesn't stock that."));
			return false;
		}
		long cost = each * amount;
		if (cost > Economy.coins(player)) {
			player.sendSystemMessage(Component.literal(
					"\u00a7cThat costs \u00a76" + Economy.pretty(cost) + "\u00a7c and you have \u00a76"
							+ Economy.pretty(Economy.coins(player))
							+ "\u00a7c. Withdraw some from the Bank."));
			return false;
		}
		Economy.give(player, -cost);

		// Hand it over a stack at a time, dropping anything that won't fit.
		int left = amount;
		while (left > 0) {
			ItemStack batch = new ItemStack(item, Math.min(left, item.getDefaultMaxStackSize()));
			left -= batch.getCount();
			if (!player.getInventory().add(batch)) {
				player.drop(batch, false);
			}
		}
		player.sendSystemMessage(Component.literal(
				"\u00a7aBought \u00a7f" + amount + "x " + new ItemStack(item).getHoverName().getString()
						+ "\u00a7a for \u00a76" + Economy.pretty(cost) + "\u00a7a."));
		return true;
	}
}
