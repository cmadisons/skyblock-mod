package com.example;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The pages behind the SkyBlock Menu's buttons.
 *
 * Two different kinds live here, and it matters which is which:
 *
 *   read-only pages   Collections, Recipe Book, Fast Travel and Pets. Built
 *                     out of icons, clicks are button presses, nothing can be
 *                     taken out.
 *
 *   real containers   Storage and the Wardrobe. These hold your actual items,
 *                     so they are ordinary chest screens and are written back
 *                     to you when closed.
 */
public final class Pages {
	private Pages() {
	}


	// ------------------------------------------------------------ collections

	/** Which category each player last had open. */
	private static final java.util.Map<java.util.UUID, String> collectionTab = new java.util.HashMap<>();

	/**
	 * What you have gathered, and the tier it has reached.
	 *
	 * This used to be a top-21 list of raw counts, which told you what you had
	 * been doing and nothing about what it was worth. Collections in SkyBlock
	 * are a ladder -- 50, 100, 250, 1,000 and up -- so the page now shows where
	 * on that ladder each one is and how many more it wants. See
	 * {@link Collections}.
	 */
	public static void collections(ServerPlayer player) {
		collections(player, collectionTab.getOrDefault(player.getUUID(), Collections.FARMING));
	}

	public static void collections(ServerPlayer player, String category) {
		collectionTab.put(player.getUUID(), category);
		SimpleContainer page = Menu.blankPage();

		// Row 6: the five categories the game sorts collections into.
		for (int i = 0; i < Collections.CATEGORIES.length; i++) {
			String tab = Collections.CATEGORIES[i];
			page.setItem(Menu.at(2 + i, 6), Menu.entry(
					tab.equals(category) ? Items.LIME_STAINED_GLASS_PANE : Items.GRAY_STAINED_GLASS_PANE,
					tab + " Collection",
					tab.equals(category) ? ChatFormatting.GREEN : ChatFormatting.GRAY,
					Collections.inCategory(tab).size() + " to gather."));
		}

		int slot = 0;
		for (Collections.Entry entry : Collections.inCategory(category)) {
			int column = 2 + (slot % 7);
			int row = 4 - (slot / 7);
			if (row < 2) {
				break;
			}
			slot++;

			long have = Collections.amount(player, entry);
			int tier = Collections.tierFor(have);
			long next = Collections.nextAt(have);

			if (have == 0) {
				page.setItem(Menu.at(column, row), Menu.locked(entry.item(), entry.name(),
						"Gather one to begin.",
						"Tier I at 50."));
				continue;
			}
			page.setItem(Menu.at(column, row), Menu.entry(entry.item(),
					entry.name() + (tier > 0 ? " " + Collections.roman(tier) : ""),
					tier >= Collections.maxTier() ? ChatFormatting.GOLD : ChatFormatting.YELLOW,
					Economy.pretty(have) + " gathered",
					next == 0
							? "Every tier earned."
							: "Tier " + Collections.roman(tier + 1) + " at " + Economy.pretty(next)
									+ " — " + Economy.pretty(next - have) + " to go"));
		}

		page.setItem(Menu.at(5, 5), Menu.entry(Items.PAINTING, "Collections", ChatFormatting.GREEN,
				Collections.started(player) + " of " + Collections.ALL.length + " started",
				Collections.totalTiers(player) + " tiers earned in all",
				"Each tier pays coins."));

		back(page);
		Menu.show(player, "Collections — " + category, page, Pages::collectionClick);
	}

	private static void collectionClick(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
			return;
		}
		for (int i = 0; i < Collections.CATEGORIES.length; i++) {
			if (slot == Menu.at(2 + i, 6)) {
				collections(player, Collections.CATEGORIES[i]);
				return;
			}
		}
	}

	/** "oak_log" reads better as "Oak Log". */
	private static String tidy(String key) {
		StringBuilder out = new StringBuilder();
		for (String word : key.split("_")) {
			if (word.isEmpty()) {
				continue;
			}
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
		}
		return out.toString().trim();
	}

	// ------------------------------------------------------------ recipe book

	/** What this mod adds that you can craft. */
	public static void recipes(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();

		page.setItem(Menu.at(5, 5), Menu.entry(Items.CRAFTING_TABLE, "Minions",
				ChatFormatting.GREEN,
				"Eight cobblestone around an axe.",
				"The axe decides the speed."));

		String[] tiers = Minions.TIERS;
		for (int i = 0; i < tiers.length; i++) {
			double seconds = new double[]{3.0, 2.5, 2.0, 1.5, 1.0, 0.5}[i];
			page.setItem(Menu.at(2 + i, 3), Menu.entry(
					Minions.MINIONS[i].asItem(),
					tidy(tiers[i]) + " Cobblestone Minion", ChatFormatting.YELLOW,
					"Eight cobblestone around",
					"a " + tiers[i] + " axe.",
					"Acts every " + seconds + " seconds."));
		}
		back(page);
		Menu.show(player, "Recipe Book", page, Pages::backOnly);
	}

	// ------------------------------------------------------------ fast travel

	/** Which page of places each player is on. */
	private static final java.util.Map<java.util.UUID, Integer> travelPage = new java.util.HashMap<>();

	/** How many places fit on one screen. */
	private static final int PER_PAGE = 28;

	/**
	 * Fast Travel, which used to be /warp and /hub.
	 *
	 * Every place in {@link Locations}, not just the island and the Hub, with
	 * the ones you have not earned greyed out and saying what they want. Paged,
	 * because there are far more than fifty-four of them.
	 */
	public static void fastTravel(ServerPlayer player) {
		fastTravel(player, travelPage.getOrDefault(player.getUUID(), 0));
	}

	public static void fastTravel(ServerPlayer player, int pageNumber) {
		SimpleContainer page = Menu.blankPage();
		Locations.Place[] places = Locations.ALL;
		int pages = Math.max(1, (places.length + PER_PAGE - 1) / PER_PAGE);
		pageNumber = Math.max(0, Math.min(pages - 1, pageNumber));
		travelPage.put(player.getUUID(), pageNumber);

		page.setItem(Menu.at(2, 6), Menu.entry(Items.GRASS_BLOCK, "Private Island",
				ChatFormatting.GREEN, "Your own island.", "Click to travel."));
		page.setItem(Menu.at(8, 6), Menu.entry(Items.STONE_BRICKS, "Hub",
				ChatFormatting.AQUA, "The Village and its districts.", "Click to travel."));

		int first = pageNumber * PER_PAGE;
		for (int i = 0; i < PER_PAGE && first + i < places.length; i++) {
			Locations.Place place = places[first + i];
			int column = 2 + (i % 7);
			int row = 5 - (i / 7);
			boolean open = Warps.open(player, place);
			page.setItem(Menu.at(column, row), open
					? Menu.entry(Items.ENDER_PEARL, place.name(), ChatFormatting.GREEN,
							"Click to travel.")
					: Menu.locked(Items.ENDER_PEARL, place.name(),
							"Needs " + place.skill() + " level " + place.level() + ".",
							"You are level " + Skills.levelIn(player, place.skill()) + "."));
		}

		if (pageNumber > 0) {
			page.setItem(Menu.at(2, 1), Menu.entry(Items.ARROW, "Previous page",
					ChatFormatting.WHITE, "Page " + pageNumber + " of " + pages));
		}
		if (pageNumber < pages - 1) {
			page.setItem(Menu.at(8, 1), Menu.entry(Items.ARROW, "Next page",
					ChatFormatting.WHITE, "Page " + (pageNumber + 2) + " of " + pages));
		}
		page.setItem(Menu.at(4, 1), Menu.entry(Items.COMPASS, "Places",
				ChatFormatting.AQUA, Warps.openTo(player) + " of " + Warps.count() + " open to you"));

		back(page);
		Menu.show(player, "Fast Travel", page, Pages::travelClick);
	}

	// ------------------------------------------------------------------- pets

	/**
	 * Pick a pet.
	 *
	 * One page per skill rather than one page of everything: there are forty
	 * of them now, a chest screen holds fifty-four slots, and jamming the lot
	 * in would leave no room for the headings that make it readable. The skill
	 * you are looking at is remembered per player so clicking back and forth
	 * doesn't dump you at the start each time.
	 */
	public static void pets(ServerPlayer player) {
		pets(player, petTab.getOrDefault(player.getUUID(), Skills.FARMING));
	}

	/** Which skill's pets each player last had open. */
	private static final java.util.Map<java.util.UUID, String> petTab = new java.util.HashMap<>();

	/** The skills that actually have pets, in menu order. */
	private static final String[] PET_TABS = {
			Skills.FARMING, Skills.MINING, Skills.COMBAT, Skills.FORAGING,
			Skills.FISHING, Skills.TAMING, Skills.ALCHEMY, Skills.HUNTING,
	};

	public static void pets(ServerPlayer player, String skill) {
		petTab.put(player.getUUID(), skill);
		SimpleContainer page = Menu.blankPage();
		String active = Vault.pet(player);

		// Row 6: one tab per skill, the open one lit up.
		for (int i = 0; i < PET_TABS.length; i++) {
			String tab = PET_TABS[i];
			int level = Skills.levelIn(player, tab);
			page.setItem(Menu.at(1 + i, 6), Menu.entry(
					tab.equals(skill) ? Items.LIME_STAINED_GLASS_PANE : Items.GRAY_STAINED_GLASS_PANE,
					tab + " pets",
					tab.equals(skill) ? ChatFormatting.GREEN : ChatFormatting.GRAY,
					"You are " + tab + " level " + level + ".",
					Pets.forSkill(tab).size() + " pets here."));
		}

		java.util.List<Pets.Pet> pets = Pets.forSkill(skill);
		int slot = 0;
		for (Pets.Pet pet : pets) {
			int column = 2 + (slot % 7);
			int row = 4 - (slot / 7);
			if (row < 2) {
				break;                             // page full; the tabs split them
			}
			slot++;

			ItemStack icon;
			if (!Pets.unlocked(player, pet)) {
				icon = Menu.locked(pet.icon(), pet.rarity().pretty() + " " + pet.name(),
						pet.perk(),
						"Needs " + pet.skill() + " level " + pet.rarity().needs() + ".",
						"You are level " + Skills.levelIn(player, pet.skill()) + ".");
			} else if (pet.name().equals(active)) {
				icon = Menu.entry(pet.icon(), pet.name() + " (out)", ChatFormatting.GREEN,
						pet.rarity().pretty(), pet.perk(), "Click to put away.");
			} else {
				icon = Menu.entry(pet.icon(), pet.name(), pet.rarity().colour(),
						pet.rarity().pretty(), pet.perk(), "Click to bring out.");
			}
			page.setItem(Menu.at(column, row), icon);
		}

		page.setItem(Menu.at(5, 5), Menu.entry(Items.BONE, "Your Pets", ChatFormatting.GREEN,
				Pets.unlockedCount(player) + " of " + Pets.ALL.length + " unlocked",
				active.isEmpty() ? "None out." : active + " is out.",
				"A pet out earns Taming XP as you play."));

		back(page);
		Menu.show(player, "Pets — " + skill, page, Pages::petClick);
	}

	/**
	 * Give the effect of whichever pet is out.
	 *
	 * Refreshed every few seconds rather than granted once, so it lasts as long
	 * as the pet does and stops the moment you put it away.
	 */
	public static void tickPets(ServerLevel level) {
		if (level.getGameTime() % 40 != 0) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			if (!SkyBlocksMod.allowed(player, level)) {
				continue;
			}
			Pets.Pet pet = Pets.byName(Vault.pet(player));
			// A pet you no longer qualify for stops working rather than being
			// taken off you -- levels can only go up, so this is really about a
			// save made before the pet tables changed.
			if (pet != null && Pets.unlocked(player, pet)) {
				give(player, pet.effect());
			}
		}
	}

	private static void give(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
		// 100 ticks, refreshed every 40, so it never visibly flickers out.
		player.addEffect(new MobEffectInstance(effect, 100, 0, true, false, false));
	}

	// -------------------------------------------------------- real containers

	/**
	 * The Personal Vault: 27 slots that follow you, safe from everything.
	 *
	 * Costs coins to open the first time, as in the real game. Charged rather
	 * than given so it is something to work towards early on.
	 */
	public static void storage(ServerPlayer player) {
		if (!Vault.vaultOpen(player)) {
			if (Economy.coins(player) < Vault.VAULT_COST) {
				player.sendSystemMessage(Component.literal(
						"§cThe Personal Vault costs §6" + Economy.pretty(Vault.VAULT_COST)
								+ " coins§c to open. You have §6"
								+ Economy.pretty(Economy.coins(player)) + "§c."));
				player.closeContainer();
				return;
			}
			Economy.give(player, -Vault.VAULT_COST);
			Vault.openVault(player);
			player.sendSystemMessage(Component.literal(
					"§aPersonal Vault unlocked. It's yours for good now."));
		}
		SimpleContainer box = Vault.container(player, Vault.STORAGE, Vault.STORAGE_SLOTS);
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new SavingMenu(id, inventory, box,
						(ServerPlayer) who, Vault.STORAGE),
				Component.literal("Storage")));
	}

	/** Wardrobe: four slots for an armour set, kept for later. */
	public static void wardrobe(ServerPlayer player) {
		SimpleContainer box = Vault.container(player, Vault.WARDROBE, Vault.WARDROBE_SLOTS);
		player.openMenu(new SimpleMenuProvider(
				(id, inventory, who) -> new SavingMenu(id, inventory, box,
						(ServerPlayer) who, Vault.WARDROBE),
				Component.literal("Wardrobe")));
	}

	// ------------------------------------------------------------------ click

	/**
	 * Each page gets its own click handler.
	 *
	 * They used to share one, which was a bug waiting to happen: Fast Travel's
	 * two destinations sit in the same row as the six pets, so a click meant
	 * for one could trigger the other. Separate handlers make that impossible
	 * rather than relying on the order of a chain of ifs.
	 */

	/** Pages with nothing to press but Back. */
	private static void backOnly(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
		}
	}

	/** Fast Travel: a place, a page, or Back. */
	private static void travelClick(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
			return;
		}
		int pageNumber = travelPage.getOrDefault(player.getUUID(), 0);
		if (slot == Menu.at(2, 1)) {
			fastTravel(player, pageNumber - 1);
			return;
		}
		if (slot == Menu.at(8, 1)) {
			fastTravel(player, pageNumber + 1);
			return;
		}
		if (slot == Menu.at(2, 6)) {
			travel(player, new BlockPos(0, 65, 0), "Travelled to your island.");
			return;
		}
		if (slot == Menu.at(8, 6)) {
			ServerLevel level = player.level() instanceof ServerLevel world ? world : null;
			if (level != null && !Hub.exists(level)) {
				Hub.build(level);
			}
			travel(player, Hub.arrival(), "Travelled to the Hub.");
			return;
		}

		Locations.Place[] places = Locations.ALL;
		int first = pageNumber * PER_PAGE;
		for (int i = 0; i < PER_PAGE && first + i < places.length; i++) {
			int column = 2 + (i % 7);
			int row = 5 - (i / 7);
			if (slot == Menu.at(column, row)) {
				Warps.travel(player, places[first + i]);
				return;
			}
		}
	}

	/** Pets: switch skill tab, bring one out, put one away, or Back. */
	private static void petClick(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
			return;
		}
		for (int i = 0; i < PET_TABS.length; i++) {
			if (slot == Menu.at(1 + i, 6)) {
				pets(player, PET_TABS[i]);
				return;
			}
		}

		String skill = petTab.getOrDefault(player.getUUID(), Skills.FARMING);
		java.util.List<Pets.Pet> pets = Pets.forSkill(skill);
		for (int at = 0; at < pets.size(); at++) {
			int column = 2 + (at % 7);
			int row = 4 - (at / 7);
			if (row < 2 || slot != Menu.at(column, row)) {
				continue;
			}
			Pets.Pet pet = pets.get(at);
			if (!Pets.unlocked(player, pet)) {
				return;                            // still locked, ignore
			}
			Vault.setPet(player, Vault.pet(player).equals(pet.name()) ? "" : pet.name());
			pets(player, skill);                   // redraw so it shows the change
			return;
		}
	}

	private static void travel(ServerPlayer player, BlockPos to, String message) {
		player.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
		player.sendSystemMessage(Component.literal("§a" + message));
		player.closeContainer();
	}

	/** The Back button, in the same place on every page. */
	private static void back(SimpleContainer page) {
		page.setItem(Menu.at(5, 1), Menu.entry(Items.ARROW, "Back",
				ChatFormatting.WHITE, "To the SkyBlock Menu."));
	}

	// --------------------------------------------------------------- skills

	/**
	 * Your Skills, which used to be /skills.
	 *
	 * Thirteen skills, each with its level, its own ceiling and how far through
	 * the current level you are -- and the SkyBlock statistics they add up to,
	 * which have no vanilla equivalent and so exist nowhere else.
	 */
	public static void skills(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();

		for (int i = 0; i < Skills.SKILLS.length; i++) {
			Skills.Skill skill = Skills.SKILLS[i];
			int level = Skills.levelIn(player, skill.name());
			boolean maxed = level >= skill.max();
			long left = Skills.toNext(player, skill.name());
			long boost = Skills.wisdom(player, skill.name());

			java.util.List<String> lines = new java.util.ArrayList<>();
			lines.add("Level " + level + " of " + skill.max());
			lines.add(maxed ? "Maxed." : Economy.pretty(left) + " XP to the next");
			if (!skill.cosmetic()) {
				lines.add(skill.reward());
			}
			if (boost > 0) {
				lines.add("+" + boost + " Wisdom for "
						+ Skills.wisdomLeft(player, skill.name()) + "s");
			}

			int column = 2 + (i % 7);
			int row = 4 - (i / 7);
			page.setItem(Menu.at(column, row), Menu.entry(
					iconFor(skill.name()), skill.name(),
					maxed ? ChatFormatting.GOLD
							: skill.cosmetic() ? ChatFormatting.GRAY : ChatFormatting.GREEN,
					lines.toArray(new String[0])));
		}

		java.util.List<String> stats = new java.util.ArrayList<>();
		for (var stat : Skills.stat(player).entrySet()) {
			if (stat.getValue() > 0) {
				stats.add(stat.getKey() + ": +"
						+ (stat.getValue() % 1 == 0
								? String.valueOf(stat.getValue().longValue())
								: String.valueOf(stat.getValue())));
			}
		}
		if (stats.isEmpty()) {
			stats.add("Level a skill to earn some.");
		}
		page.setItem(Menu.at(5, 5), Menu.entry(Items.DIAMOND_SWORD, "Your Skills",
				ChatFormatting.GREEN,
				String.format("Skill average %.1f", Skills.average(player))));
		page.setItem(Menu.at(3, 5), Menu.entry(Items.NETHER_STAR, "Stats",
				ChatFormatting.AQUA, stats.toArray(new String[0])));

		back(page);
		Menu.show(player, "Your Skills", page, Pages::backOnly);
	}

	/** Something recognisable for each skill. */
	private static net.minecraft.world.item.Item iconFor(String skill) {
		return switch (skill) {
			case Skills.FARMING -> Items.GOLDEN_HOE;
			case Skills.MINING -> Items.IRON_PICKAXE;
			case Skills.COMBAT -> Items.IRON_SWORD;
			case Skills.FORAGING -> Items.OAK_SAPLING;
			case Skills.FISHING -> Items.FISHING_ROD;
			case Skills.ENCHANTING -> Items.ENCHANTING_TABLE;
			case Skills.ALCHEMY -> Items.BREWING_STAND;
			case Skills.CARPENTRY -> Items.CRAFTING_TABLE;
			case Skills.TAMING -> Items.LEAD;
			case Skills.HUNTING -> Items.BOW;
			case Skills.DUNGEONEERING -> Items.SKELETON_SKULL;
			case Skills.RUNECRAFTING -> Items.MAGMA_CREAM;
			default -> Items.EMERALD;
		};
	}

	// ----------------------------------------------------------------- bank

	/**
	 * The Personal Bank, which used to be /bank and /coins.
	 *
	 * Fixed amounts rather than a typed number, because a chest screen has no
	 * way to type one. The four buttons cover what anybody actually does:
	 * a bit, a lot, and everything.
	 */
	public static void bank(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();

		page.setItem(Menu.at(5, 5), Menu.entry(Items.GOLD_INGOT, "Personal Bank",
				ChatFormatting.GOLD,
				"Carrying " + Economy.pretty(Economy.coins(player)),
				"Banked " + Economy.pretty(Economy.bank(player)),
				"Dying costs half of what you carry.",
				"It never touches what is banked."));

		long[] amounts = {1_000, 10_000, 100_000};
		for (int i = 0; i < amounts.length; i++) {
			page.setItem(Menu.at(3 + i, 4), Menu.entry(Items.HOPPER,
					"Deposit " + Economy.pretty(amounts[i]), ChatFormatting.GREEN,
					"Into the bank, where it is safe."));
			page.setItem(Menu.at(3 + i, 2), Menu.entry(Items.DROPPER,
					"Withdraw " + Economy.pretty(amounts[i]), ChatFormatting.YELLOW,
					"Out of the bank, into your pocket."));
		}
		page.setItem(Menu.at(6, 4), Menu.entry(Items.CHEST, "Deposit everything",
				ChatFormatting.GREEN, "All " + Economy.pretty(Economy.coins(player)) + "."));
		page.setItem(Menu.at(6, 2), Menu.entry(Items.ENDER_CHEST, "Withdraw everything",
				ChatFormatting.YELLOW, "All " + Economy.pretty(Economy.bank(player)) + "."));

		back(page);
		Menu.show(player, "Personal Bank", page, Pages::bankClick);
	}

	private static void bankClick(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
			return;
		}
		long[] amounts = {1_000, 10_000, 100_000};
		for (int i = 0; i < amounts.length; i++) {
			if (slot == Menu.at(3 + i, 4)) {
				Shops.move(player, amounts[i], true);
				bank(player);
				return;
			}
			if (slot == Menu.at(3 + i, 2)) {
				Shops.move(player, amounts[i], false);
				bank(player);
				return;
			}
		}
		if (slot == Menu.at(6, 4)) {
			Shops.move(player, Economy.coins(player), true);
			bank(player);
		} else if (slot == Menu.at(6, 2)) {
			Shops.move(player, Economy.bank(player), false);
			bank(player);
		}
	}

	// ---------------------------------------------------------------- bazaar

	/** What each slot on the Bazaar page is selling. */
	private static final java.util.Map<java.util.UUID, java.util.List<net.minecraft.world.item.Item>>
			bazaarSlots = new java.util.HashMap<>();

	/**
	 * The Bazaar, which used to be /bazaar buy.
	 *
	 * Everything the shop deals in, with what a stack costs. Clicking buys
	 * sixty-four, because one at a time through a menu would be unbearable and
	 * a stack is the unit anybody actually wants.
	 */
	public static void bazaar(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();

		java.util.List<net.minecraft.world.item.Item> stock =
				new java.util.ArrayList<>(Economy.stock());
		stock.sort(java.util.Comparator.comparing(item ->
				net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath()));
		bazaarSlots.put(player.getUUID(), stock);

		for (int i = 0; i < stock.size(); i++) {
			int column = 2 + (i % 7);
			int row = 5 - (i / 7);
			if (row < 2) {
				break;
			}
			net.minecraft.world.item.Item item = stock.get(i);
			Long each = Economy.buyPrice(item);
			page.setItem(Menu.at(column, row), Menu.entry(item,
					new ItemStack(item).getHoverName().getString(), ChatFormatting.YELLOW,
					each == null ? "Not for sale" : Economy.pretty(each) + " coins each",
					each == null ? "" : "A stack of 64: " + Economy.pretty(each * 64),
					"Click to buy 64."));
		}

		page.setItem(Menu.at(1, 1), Menu.entry(Items.GOLD_NUGGET, "Your coins",
				ChatFormatting.GOLD, Economy.pretty(Economy.coins(player)) + " carried",
				"The Bazaar charges double what it pays."));
		back(page);
		Menu.show(player, "Bazaar", page, Pages::bazaarClick);
	}

	private static void bazaarClick(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
			return;
		}
		java.util.List<net.minecraft.world.item.Item> stock = bazaarSlots.get(player.getUUID());
		if (stock == null) {
			return;
		}
		for (int i = 0; i < stock.size(); i++) {
			int column = 2 + (i % 7);
			int row = 5 - (i / 7);
			if (row >= 2 && slot == Menu.at(column, row)) {
				Shops.buy(player, stock.get(i), 64);
				bazaar(player);
				return;
			}
		}
	}

	// ---------------------------------------------------------------- quests

	/** The quest log, which used to be /quests. */
	public static void quests(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();

		java.util.List<Quests.Quest> left = Quests.remaining(player);
		page.setItem(Menu.at(5, 5), Menu.entry(Items.WRITABLE_BOOK, "Quests",
				ChatFormatting.YELLOW,
				Quests.completed(player) + " of " + Quests.ALL.length + " done",
				left.isEmpty() ? "All of them. Well done." : left.size() + " to go."));

		int slot = 0;
		for (Quests.Quest quest : Quests.ALL) {
			int column = 2 + (slot % 7);
			int row = 4 - (slot / 7);
			if (row < 1) {
				break;
			}
			slot++;
			boolean done = !left.contains(quest);
			page.setItem(Menu.at(column, row), done
					? Menu.entry(Items.LIME_DYE, quest.name(), ChatFormatting.GREEN, "Done.")
					: Menu.entry(Items.PAPER, quest.name(), ChatFormatting.YELLOW,
							"Needs " + quest.skill() + " level " + quest.level(),
							"You are level " + Skills.levelIn(player, quest.skill()) + "."));
		}

		back(page);
		Menu.show(player, "Quests", page, Pages::backOnly);
	}

	/**
	 * A chest screen whose contents belong to the player and are written back
	 * when they close it.
	 */
	private static class SavingMenu extends net.minecraft.world.inventory.ChestMenu {
		private final SimpleContainer box;
		private final ServerPlayer owner;
		private final net.fabricmc.fabric.api.attachment.v1.AttachmentType<List<ItemStack>> where;

		SavingMenu(int id, net.minecraft.world.entity.player.Inventory inventory, SimpleContainer box,
				ServerPlayer owner,
				net.fabricmc.fabric.api.attachment.v1.AttachmentType<List<ItemStack>> where) {
			super(box.getContainerSize() > 9
							? net.minecraft.world.inventory.MenuType.GENERIC_9x3
							: net.minecraft.world.inventory.MenuType.GENERIC_9x1,
					id, inventory, box, box.getContainerSize() > 9 ? 3 : 1);
			this.box = box;
			this.owner = owner;
			this.where = where;
		}

		@Override
		public void removed(net.minecraft.world.entity.player.Player player) {
			super.removed(player);
			Vault.save(owner, where, box);
		}
	}
}
