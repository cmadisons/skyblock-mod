package com.example;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * The SkyBlock Menu — the nether star that lives in your ninth hotbar slot.
 *
 * On Hypixel that slot always holds the menu, and you can't lose it. Same here:
 * the star is put back the moment it goes missing, so dying, dropping it or
 * burning it never costs you the menu.
 *
 * Right-clicking it opens a page showing your skills, your coins and what you
 * have in the bank.
 */
public final class Menu {
	private Menu() {
	}

	/** The ninth hotbar slot, counting from zero. */
	private static final int SLOT = 8;

	/** How often to check the star is still there. Once a second is plenty. */
	private static final int CHECK_EVERY = 20;

	/** The star itself, named and marked so it is recognisable. */
	public static ItemStack star() {
		ItemStack star = new ItemStack(Items.NETHER_STAR);
		star.set(DataComponents.CUSTOM_NAME,
				Component.literal("SkyBlock Menu").withStyle(ChatFormatting.GREEN));
		return star;
	}

	/** Is this the menu star rather than an ordinary nether star? */
	public static boolean isStar(ItemStack stack) {
		if (!stack.is(Items.NETHER_STAR)) {
			return false;
		}
		Component name = stack.get(DataComponents.CUSTOM_NAME);
		return name != null && name.getString().equals("SkyBlock Menu");
	}

	/**
	 * Keep slot nine stocked.
	 *
	 * Anything else sitting in that slot gets moved aside rather than
	 * destroyed, so this can never eat something you were carrying.
	 */
	public static void registerKeepInSlot() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_EVERY != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				for (ServerPlayer player : level.players()) {
					if (!SkyBlocksMod.allowed(player, level)) {
						continue;
					}
					ItemStack there = player.getInventory().getItem(SLOT);
					if (isStar(there)) {
						continue;
					}
					if (!there.isEmpty()) {
						// Give whatever was there somewhere else to live.
						if (!player.getInventory().add(there.copy())) {
							player.drop(there.copy(), false);
						}
					}
					player.getInventory().setItem(SLOT, star());
				}
			}
		});
	}

	/**
	 * Open the menu.
	 *
	 * The layout is the real one, taken from the SkyBlock wiki rather than
	 * guessed: 54 slots, light grey glass behind, and every button in the slot
	 * it actually occupies. The wiki numbers slots from the bottom-left as
	 * (column 1, row 1) up to (9, 6), so {@link #at} converts that into
	 * Minecraft's own numbering, which counts from the top-left.
	 *
	 * Not everything behind these buttons exists yet. Rather than fake them,
	 * the ones that aren't built say so in red -- the buttons are all in the
	 * right places regardless.
	 *
	 * Nothing in the page can be taken or swapped -- see {@link ReadOnlyMenu}.
	 */
	public static void open(ServerPlayer player) {
		SimpleContainer page = new SimpleContainer(54);

		// Light grey glass behind everything, as the real menu has.
		ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
		filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}

		long carried = Economy.coins(player);
		long banked = Economy.bank(player);
		int combined = 0;
		for (String skill : Skills.ALL) {
			combined += Skills.level(Skills.xp(player, skill));
		}

		// --- row 5: your stats ------------------------------------------------
		page.setItem(at(5, 5), entry(Items.PLAYER_HEAD, "Stats & Equipment", ChatFormatting.GREEN,
				player.getName().getString(),
				"Coins: " + Economy.pretty(carried),
				"Bank: " + Economy.pretty(banked)));

		// --- row 4: the main row ----------------------------------------------
		page.setItem(at(2, 4), entry(Items.DIAMOND_SWORD, "Your Skills", ChatFormatting.GREEN,
				"Combined level " + combined,
				"Type /skills for the full list."));
		page.setItem(at(3, 4), entry(Items.PAINTING, "Collections", ChatFormatting.GREEN,
				"Everything you have gathered."));
		page.setItem(at(4, 4), entry(Items.BOOK, "Recipe Book", ChatFormatting.GREEN,
				"What this mod adds that you can craft."));
		page.setItem(at(5, 4), entry(Items.EXPERIENCE_BOTTLE, "SkyBlock Leveling",
				ChatFormatting.GREEN,
				"Your combined skill level: " + combined,
				"Every skill level counts once.",
				"Raise any skill to raise this."));
		// The real quest log: how many are done, and the next few to do.
		java.util.List<Quests.Quest> left = Quests.remaining(player);
		java.util.List<String> lines = new java.util.ArrayList<>();
		lines.add(Quests.completed(player) + " of " + Quests.ALL.length + " done");
		for (int i = 0; i < Math.min(4, left.size()); i++) {
			lines.add("☐ " + left.get(i).name());
		}
		if (left.isEmpty()) {
			lines.add("All of them. Well done.");
		}
		page.setItem(at(6, 4), entry(Items.WRITABLE_BOOK, "Quests & Chapters",
				ChatFormatting.YELLOW, lines.toArray(new String[0])));
		page.setItem(at(7, 4), entry(Items.CLOCK, "Calendar and Events", ChatFormatting.YELLOW,
				"Daily reward is ready once",
				"per Minecraft day.",
				"Type /daily."));
		page.setItem(at(8, 4), entry(Items.CHEST, "Storage", ChatFormatting.GREEN,
				Vault.vaultOpen(player)
						? "Your Personal Vault. 27 slots."
						: "Opens for " + Economy.pretty(Vault.VAULT_COST) + " coins.",
				"Kept in the Bank. Always safe."));

		// --- row 3: your things -----------------------------------------------
		page.setItem(at(3, 3), soon(Items.BUNDLE, "Your Bags"));
		page.setItem(at(4, 3), entry(Items.BONE, "Pets", ChatFormatting.GREEN,
				Vault.pet(player).isEmpty() ? "None out." : Vault.pet(player) + " is out.",
				"Unlocked at skill level 5."));
		page.setItem(at(5, 3), entry(Items.CRAFTING_TABLE, "Crafting Table", ChatFormatting.WHITE,
				"Minions are eight cobblestone",
				"around any axe. Better axe,",
				"faster minion."));
		page.setItem(at(6, 3), entry(Items.LEATHER_CHESTPLATE, "Wardrobe", ChatFormatting.GREEN,
				"Keep an armour set for later."));
		page.setItem(at(7, 3), entry(Items.GOLD_INGOT, "Personal Bank", ChatFormatting.GOLD,
				"Carrying " + Economy.pretty(carried),
				"Banked " + Economy.pretty(banked),
				"Dying costs half of what you carry.",
				"/bank deposit and /bank withdraw"));

		// --- row 1: the bottom row --------------------------------------------
		page.setItem(at(3, 1), entry(Items.ENDER_PEARL, "Fast Travel", ChatFormatting.GREEN,
				"Warp to your island or the Hub."));
		page.setItem(at(4, 1), entry(Items.NAME_TAG, "Profile Management",
				ChatFormatting.GREEN,
				player.getName().getString(),
				"One profile per world.",
				"Make a new world for a new profile."));
		page.setItem(at(5, 1), entry(Items.BARRIER, "Close", ChatFormatting.RED,
				"Press Escape."));
		page.setItem(at(6, 1), entry(Items.REDSTONE_TORCH, "Settings",
				ChatFormatting.GREEN,
				"Keep inventory on death: yes",
				"Death costs half your coins.",
				"The Village is always safe."));
		page.setItem(at(7, 1), entry(Items.COOKIE, "Booster Cookie",
				ChatFormatting.YELLOW,
				"Claim one free every day.",
				"A million coins and three items.",
				"Click, or type /daily."));

		// ReadOnlyMenu, not a plain chest: the icons are real items, so without
		// it you could walk off with the diamond sword behind "Your Skills".
		show(player, "SkyBlock Menu", page, Menu::pressed);
	}

	/** What each button on the main menu does. */
	private static void pressed(ServerPlayer player, int slot) {
		if (slot == at(3, 4)) {
			Pages.collections(player);
		} else if (slot == at(4, 4)) {
			Pages.recipes(player);
		} else if (slot == at(8, 4)) {
			Pages.storage(player);
		} else if (slot == at(4, 3)) {
			Pages.pets(player);
		} else if (slot == at(6, 3)) {
			Pages.wardrobe(player);
		} else if (slot == at(3, 1)) {
			Pages.fastTravel(player);
		} else if (slot == at(7, 1)) {
			// The Booster Cookie is the daily reward, so pressing it claims it
			// rather than telling you a command to go and type.
			Shops.claimDaily(player);
			player.closeContainer();
		} else if (slot == at(5, 1)) {
			player.closeContainer();
		}
	}

	/** A page with the light grey glass background and nothing else. */
	public static SimpleContainer blankPage() {
		SimpleContainer page = new SimpleContainer(54);
		ItemStack filler = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
		filler.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
		for (int slot = 0; slot < 54; slot++) {
			page.setItem(slot, filler.copy());
		}
		return page;
	}

	/** Show a page. Clicks are button presses, never item moves. */
	public static void show(ServerPlayer player, String title, SimpleContainer page,
			ReadOnlyMenu.OnClick handler) {
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new ReadOnlyMenu(id, inventory, page, handler),
				Component.literal(title)));
	}

	/**
	 * Turn a wiki slot reference into a Minecraft one.
	 *
	 * The wiki counts columns 1-9 left to right and rows 1-6 from the BOTTOM.
	 * Minecraft counts slots 0-53 from the top-left, so the row flips.
	 */
	public static int at(int column, int rowFromBottom) {
		return (6 - rowFromBottom) * 9 + (column - 1);
	}

	/** One menu entry: an icon, a coloured name, and grey lines under it. */
	public static ItemStack entry(net.minecraft.world.item.Item icon, String name,
			ChatFormatting colour, String... lines) {
		ItemStack stack = new ItemStack(icon);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(colour));
		stack.set(DataComponents.LORE, new ItemLore(
				java.util.Arrays.stream(lines)
						.map(line -> (Component) Component.literal(line).withStyle(ChatFormatting.GRAY))
						.toList()));
		return stack;
	}

	/** An entry for something you haven't unlocked, saying what it needs. */
	public static ItemStack locked(net.minecraft.world.item.Item icon, String name, String... why) {
		ItemStack stack = new ItemStack(icon);
		stack.set(DataComponents.CUSTOM_NAME,
				Component.literal(name).withStyle(ChatFormatting.DARK_GRAY));
		java.util.List<Component> lines = new java.util.ArrayList<>();
		lines.add(Component.literal("Locked").withStyle(ChatFormatting.RED));
		for (String line : why) {
			lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
		}
		stack.set(DataComponents.LORE, new ItemLore(lines));
		return stack;
	}

	/** An entry for something that doesn't exist yet, and says so. */
	public static ItemStack soon(net.minecraft.world.item.Item icon, String name) {
		ItemStack stack = new ItemStack(icon);
		stack.set(DataComponents.CUSTOM_NAME,
				Component.literal(name).withStyle(ChatFormatting.DARK_GRAY));
		stack.set(DataComponents.LORE, new ItemLore(java.util.List.of(
				Component.literal("Not built yet.").withStyle(ChatFormatting.RED))));
		return stack;
	}
}
