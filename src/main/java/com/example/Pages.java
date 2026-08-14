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

	/** The pets you can have, and the skill that unlocks each at level 5. */
	private static final String[][] PETS = {
			{"Rabbit", Skills.FARMING, "Jump boost"},
			{"Ocelot", Skills.FORAGING, "Faster chopping"},
			{"Mole", Skills.MINING, "Faster mining"},
			{"Wolf", Skills.COMBAT, "Stronger hits"},
			{"Horse", Skills.TAMING, "Faster on foot"},
			{"Lion", Skills.HUNTING, "Stronger hits"},
	};

	private static final int UNLOCK_LEVEL = 5;

	// ------------------------------------------------------------ collections

	/** What you have gathered, most first. */
	public static void collections(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();
		Map<String, Long> got = Vault.collections(player);

		if (got.isEmpty()) {
			page.setItem(Menu.at(5, 4), Menu.entry(Items.PAINTING, "Nothing yet",
					ChatFormatting.GRAY, "Break blocks and gather things.",
					"They will be counted here."));
		} else {
			List<Map.Entry<String, Long>> ranked = got.entrySet().stream()
					.sorted(Comparator.comparingLong((Map.Entry<String, Long> e) -> e.getValue()).reversed())
					.limit(21)
					.toList();

			int slot = 0;
			for (Map.Entry<String, Long> line : ranked) {
				int column = 2 + (slot % 7);
				int row = 4 - (slot / 7);
				page.setItem(Menu.at(column, row), Menu.entry(
						Items.PAPER, tidy(line.getKey()), ChatFormatting.YELLOW,
						Economy.pretty(line.getValue()) + " gathered"));
				slot++;
			}
		}
		back(page);
		Menu.show(player, "Collections", page, Pages::backOnly);
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

	/** Warp between your island and the Hub from anywhere. */
	public static void fastTravel(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();

		page.setItem(Menu.at(4, 4), Menu.entry(Items.GRASS_BLOCK, "Private Island",
				ChatFormatting.GREEN, "Your own island.", "Click to travel."));
		page.setItem(Menu.at(6, 4), Menu.entry(Items.STONE_BRICKS, "Hub",
				ChatFormatting.AQUA, "The Village and its districts.", "Click to travel."));

		back(page);
		Menu.show(player, "Fast Travel", page, Pages::travelClick);
	}

	// ------------------------------------------------------------------- pets

	/** Pick a pet, if a skill has earned you one. */
	public static void pets(ServerPlayer player) {
		SimpleContainer page = Menu.blankPage();
		String active = Vault.pet(player);

		for (int i = 0; i < PETS.length; i++) {
			String name = PETS[i][0];
			String skill = PETS[i][1];
			String perk = PETS[i][2];
			int level = Skills.level(Skills.xp(player, skill));
			boolean unlocked = level >= UNLOCK_LEVEL;

			ItemStack icon;
			if (!unlocked) {
				icon = Menu.locked(Items.BONE, name,
						"Needs " + skill + " level " + UNLOCK_LEVEL,
						"You are level " + level + ".");
			} else if (name.equals(active)) {
				icon = Menu.entry(Items.BONE, name + " (out)", ChatFormatting.GREEN,
						perk, "Click to put away.");
			} else {
				icon = Menu.entry(Items.BONE, name, ChatFormatting.YELLOW,
						perk, "Click to bring out.");
			}
			page.setItem(Menu.at(2 + i, 4), icon);
		}
		back(page);
		Menu.show(player, "Pets", page, Pages::petClick);
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
			switch (Vault.pet(player)) {
				case "Rabbit" -> give(player, MobEffects.JUMP_BOOST);
				case "Ocelot", "Horse" -> give(player, MobEffects.SPEED);
				case "Mole" -> give(player, MobEffects.HASTE);
				case "Wolf", "Lion" -> give(player, MobEffects.STRENGTH);
				default -> {
				}
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

	/** Fast Travel: two destinations, or Back. */
	private static void travelClick(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
		} else if (slot == Menu.at(4, 4)) {
			travel(player, new BlockPos(0, 65, 0), "Travelled to your island.");
		} else if (slot == Menu.at(6, 4)) {
			ServerLevel level = player.level() instanceof ServerLevel world ? world : null;
			if (level != null && !Hub.exists(level)) {
				Hub.build(level);
			}
			travel(player, Hub.arrival(), "Travelled to the Hub.");
		}
	}

	/** Pets: bring one out, put one away, or Back. */
	private static void petClick(ServerPlayer player, int slot) {
		if (slot == Menu.at(5, 1)) {
			Menu.open(player);
			return;
		}
		for (int i = 0; i < PETS.length; i++) {
			if (slot != Menu.at(2 + i, 4)) {
				continue;
			}
			if (Skills.level(Skills.xp(player, PETS[i][1])) < UNLOCK_LEVEL) {
				return;                            // still locked, ignore
			}
			String name = PETS[i][0];
			Vault.setPet(player, Vault.pet(player).equals(name) ? "" : name);
			pets(player);                          // redraw so it shows the change
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
