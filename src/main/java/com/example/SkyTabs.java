package com.example;

import java.util.List;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Seven tabs in the creative inventory, holding everything the mod adds.
 *
 * This is the answer to "where is any of it". Five hundred items scattered
 * through vanilla's own tabs would be unusable, and hidden behind a command
 * they may as well not exist -- so they are grouped the way SkyBlock groups
 * them, and press E to find them:
 *
 *   Enchanted     the compressed materials, all of them, glinting
 *   Materials     drops, upgrades, minion parts, and the sixty gemstones
 *   Weapons       swords, bows, tools
 *   Armor         every set, four pieces each
 *   Accessories   talismans, rings, artifacts
 *   Blocks        Dwarven ores, gemstone crystals, and the minions
 *   NPCs          everybody, ready to be put down somewhere
 */
public final class SkyTabs {
	private SkyTabs() {
	}

	public static void register() {
		tab("enchanted", "enchanted_diamond", SkyItems.BY_TAB.get("enchanted"));
		tab("materials", "perfect_ruby_gem", join(
				SkyItems.BY_TAB.get("material"), SkyItems.BY_TAB.get("gemstone")));
		tab("gear", "aspect_of_the_dragons", SkyItems.BY_TAB.get("gear"));
		tab("armor", "superior_dragon_chestplate", SkyItems.BY_TAB.get("armor"));
		tab("accessories", "speed_artifact", SkyItems.BY_TAB.get("accessory"));
		// The XP Boosts go in with the materials, which is where you look for a
		// consumable you drink before a farming session.
		potions();
		tab("blocks", "mithril_block", withMinions(SkyItems.BY_TAB.get("blocks")));
		people();
		enemies();
	}

	/** The seven skills' XP Boosts, three tiers each. */
	private static void potions() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.skyblocks.boosts"))
				.icon(() -> {
					ItemStack stack = XpBoosts.stack("combat_xp_boost_iii");
					return stack.isEmpty() ? new ItemStack(Items.POTION) : stack;
				})
				.displayItems((parameters, output) -> {
					for (Item item : XpBoosts.BOOSTS.values()) {
						output.accept(item);
					}
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SkyBlocksMod.id("boosts"), tab);
	}

	/** And the enemies, likewise -- a token each, in level order. */
	private static void enemies() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.skyblocks.mobs"))
				.icon(() -> {
					ItemStack stack = MobTokens.token("crypt_ghoul");
					return stack.isEmpty() ? new ItemStack(Items.ZOMBIE_HEAD) : stack;
				})
				.displayItems((parameters, output) -> {
					for (Item item : MobTokens.TOKENS.values()) {
						output.accept(item);
					}
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SkyBlocksMod.id("mobs"), tab);
	}

	/**
	 * One tab.
	 *
	 * The items go in registration order, which is the order they are written
	 * in tools/make_content.py -- mining, then farming, then foraging, then
	 * combat. That is deliberate: it means the tab reads like the game's own
	 * collections rather than an alphabetical jumble.
	 */
	private static void tab(String name, String iconId, List<Item> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.skyblocks." + name))
				.icon(() -> icon(iconId))
				.displayItems((parameters, output) -> {
					for (Item item : items) {
						output.accept(item);
					}
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SkyBlocksMod.id(name), tab);
	}

	/** The people get their own tab, since they aren't items in the same sense. */
	private static void people() {
		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.skyblocks.npcs"))
				.icon(() -> {
					ItemStack stack = NpcTokens.token("jerry");
					return stack.isEmpty() ? new ItemStack(Items.EMERALD) : stack;
				})
				.displayItems((parameters, output) -> {
					for (Item item : NpcTokens.TOKENS.values()) {
						output.accept(item);
					}
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SkyBlocksMod.id("npcs"), tab);
	}

	/** The minions belong with the blocks, since that is what they are. */
	private static List<Item> withMinions(List<Item> blocks) {
		List<Item> all = new java.util.ArrayList<>(blocks == null ? List.of() : blocks);
		for (net.minecraft.world.level.block.Block minion : Minions.MINIONS) {
			all.add(minion.asItem());
		}
		return all;
	}

	private static List<Item> join(List<Item> first, List<Item> second) {
		List<Item> all = new java.util.ArrayList<>();
		if (first != null) {
			all.addAll(first);
		}
		if (second != null) {
			all.addAll(second);
		}
		return all;
	}

	/** The picture on the tab itself. Falls back if an id is ever renamed. */
	private static ItemStack icon(String id) {
		ItemStack stack = SkyItems.stack(id);
		return stack.isEmpty() ? new ItemStack(Items.NETHER_STAR) : stack;
	}
}
