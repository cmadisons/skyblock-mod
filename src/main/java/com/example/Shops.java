package com.example;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

	/** Suggests only things the bazaar actually stocks. */
	private static final SuggestionProvider<CommandSourceStack> STOCK = (ctx, builder) ->
			SharedSuggestionProvider.suggest(
					Economy.stock().stream()
							.map(item -> BuiltInRegistries.ITEM.getKey(item).getPath())
							.sorted()
							.toList(),
					builder);

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

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		daily(dispatcher);
		bank(dispatcher);
		bazaar(dispatcher);
	}

	// ------------------------------------------------------------------ daily

	/**
	 * /daily — a million coins and three items, once per Minecraft day.
	 *
	 * The four things are, as asked: the coins, something rare, something you
	 * would only want later on, and one at random.
	 */
	private static void daily(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("daily").executes(ctx -> {
			ServerPlayer player = ctx.getSource().getPlayerOrException();

			long today = ctx.getSource().getLevel().getGameTime() / DAY;
			long last = player.getAttachedOrCreate(Economy.LAST_DAILY, () -> -1L);
			if (last == today) {
				ctx.getSource().sendFailure(Component.literal(
						"Already claimed today. Come back tomorrow."));
				return 0;
			}
			player.setAttached(Economy.LAST_DAILY, today);

			Economy.give(player, DAILY_COINS);

			// Something rare, something for later, and something random.
			ItemStack rare = new ItemStack(Items.DIAMOND_BLOCK, 4);
			ItemStack later = new ItemStack(Items.NETHERITE_SCRAP, 2);
			ItemStack surprise = randomTreat(ctx.getSource().getLevel().getGameTime());

			for (ItemStack prize : List.of(rare, later, surprise)) {
				if (!player.getInventory().add(prize.copy())) {
					player.drop(prize.copy(), false);      // full inventory, drop it
				}
			}

			ctx.getSource().sendSuccess(() -> Component.literal(
					"§aDaily claimed! §6" + Economy.pretty(DAILY_COINS) + " coins§a, "
							+ "4 diamond blocks, 2 netherite scrap, and "
							+ surprise.getCount() + "x " + surprise.getHoverName().getString()), false);
			return 1;
		}));
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
	 * /bank — see it, put coins in, take coins out.
	 *
	 * Worth doing before anything dangerous: dying takes half of what you are
	 * carrying and nothing at all of what is banked.
	 */
	private static void bank(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("bank")
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					ctx.getSource().sendSuccess(() -> Component.literal(
							"§6Bank: " + Economy.pretty(Economy.bank(player))
									+ "§7 · carrying " + Economy.pretty(Economy.coins(player))), false);
					return 1;
				})
				.then(Commands.literal("deposit")
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
								.executes(ctx -> move(ctx, true))))
				.then(Commands.literal("withdraw")
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
								.executes(ctx -> move(ctx, false)))));
	}

	/** Shift coins between pocket and bank, refusing if there aren't enough. */
	private static int move(CommandContext<CommandSourceStack> ctx, boolean depositing)
			throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		long amount = IntegerArgumentType.getInteger(ctx, "amount");

		long pocket = Economy.coins(player);
		long vault = Economy.bank(player);

		if (depositing && amount > pocket) {
			ctx.getSource().sendFailure(Component.literal(
					"You're only carrying " + Economy.pretty(pocket) + "."));
			return 0;
		}
		if (!depositing && amount > vault) {
			ctx.getSource().sendFailure(Component.literal(
					"The bank only holds " + Economy.pretty(vault) + "."));
			return 0;
		}

		long shift = depositing ? amount : -amount;
		player.setAttached(Economy.COINS, pocket - shift);
		player.setAttached(Economy.BANK, vault + shift);

		ctx.getSource().sendSuccess(() -> Component.literal(
				"§a" + (depositing ? "Deposited " : "Withdrew ") + "§6" + Economy.pretty(amount)
						+ "§a. Bank: §6" + Economy.pretty(Economy.bank(player))
						+ "§a, carrying: §6" + Economy.pretty(Economy.coins(player))), false);
		return 1;
	}

    // ----------------------------------------------------------------- bazaar

	/**
	 * /bazaar — buy anything the shop deals in.
	 *
	 * It charges double what it pays, which is what stops you buying and
	 * selling the same item forever to make free money.
	 */
	private static void bazaar(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("bazaar")
				// No arguments: show what's on offer and what it costs.
				.executes(ctx -> {
					ctx.getSource().sendSuccess(() -> Component.literal(
							"§6Bazaar §7— /bazaar buy <item> <amount>"), false);
					Economy.stock().stream()
							.map(item -> BuiltInRegistries.ITEM.getKey(item).getPath())
							.sorted()
							.forEach(name -> ctx.getSource().sendSuccess(() -> Component.literal(
									"  §7" + name), false));
					return 1;
				})
				.then(Commands.literal("buy")
						.then(Commands.argument("item", StringArgumentType.word())
								.suggests(STOCK)
								.then(Commands.argument("amount", IntegerArgumentType.integer(1, 640))
										.executes(Shops::buy)))));
	}

	private static int buy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = StringArgumentType.getString(ctx, "item");
		int amount = IntegerArgumentType.getInteger(ctx, "amount");

		Item item = BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(name))
				.orElse(null);
		Long each = item == null ? null : Economy.buyPrice(item);
		if (each == null) {
			ctx.getSource().sendFailure(Component.literal(
					"The bazaar doesn't stock that. Type /bazaar to see what it has."));
			return 0;
		}

		long cost = each * amount;
		if (cost > Economy.coins(player)) {
			ctx.getSource().sendFailure(Component.literal(
					"That costs " + Economy.pretty(cost) + " and you have "
							+ Economy.pretty(Economy.coins(player))
							+ ". Try /bank withdraw."));
			return 0;
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

		ctx.getSource().sendSuccess(() -> Component.literal(
				"§aBought §f" + amount + "x " + name + "§a for §6" + Economy.pretty(cost)
						+ "§a. Left: §6" + Economy.pretty(Economy.coins(player))), false);
		return 1;
	}
}
