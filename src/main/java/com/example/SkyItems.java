package com.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Turns the lists in {@link Content} into real items and blocks.
 *
 * Everything SkyBlock has that Minecraft does not: the enchanted materials,
 * the gemstones, the swords and bows, the armour sets, the talismans, the
 * Dwarven ores, and a token for each of the people who live in the world.
 * Roughly five hundred things, all registered from tables rather than written
 * out one at a time.
 *
 * Two decisions worth knowing about
 * ---------------------------------
 * Items borrow vanilla textures wherever there is a sensible one to borrow.
 * Enchanted Coal is coal with a glint on it. That is not a shortcut around
 * drawing them: it is what the real game does, and it is the reason an
 * Enchanted Diamond is recognisable at a glance. Things with no counterpart at
 * all -- the sixty gemstones, the Dwarven ores -- have textures drawn for them
 * in tools/make_content.py.
 *
 * Names and rarity come from data components rather than the language file, so
 * that a Legendary shows up gold and a Common shows up white, which the
 * language file has no way to say. The language file still carries every name
 * as a plain string, so nothing is nameless if a component is ever lost.
 */
public final class SkyItems {
	private SkyItems() {
	}

	/** Every item this mod adds, by its id, in the order they were made. */
	public static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	/** Every block this mod adds, by its id. */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

	/** The same items, grouped for the creative tabs to show. */
	public static final Map<String, List<Item>> BY_TAB = new LinkedHashMap<>();

	/** Called once at start-up, before any world exists. */
	public static void register() {
		for (Content.Mat mat : Content.MATERIALS) {
			material(mat);
		}
		for (Content.Mat acc : Content.ACCESSORIES) {
			material(acc);
		}
		for (Content.Gear gear : Content.GEAR) {
			gear(gear);
		}
		for (Content.Armour piece : Content.ARMOUR) {
			armour(piece);
		}
		for (Content.Blok block : Content.BLOCKS) {
			block(block);
		}
		SkyBlocksMod.LOGGER.info("Sky Blocks added {} items and {} blocks.",
				ITEMS.size(), BLOCKS.size());
	}

	// ------------------------------------------------------------- the kinds

	private static void material(Content.Mat mat) {
		String noun = switch (mat.tab()) {
			case "accessory" -> "ACCESSORY";
			case "gemstone" -> "GEMSTONE";
			default -> "MATERIAL";
		};
		Item.Properties props = props(mat.id(), mat.rarity())
				.component(DataComponents.ITEM_NAME, name(mat.name(), mat.rarity()))
				.component(DataComponents.LORE, lore(mat.desc(), mat.rarity(), noun));
		if (mat.glint()) {
			props = props.component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		add(mat.tab(), mat.id(), new Item(props));
	}

	/**
	 * A weapon, a bow or a tool.
	 *
	 * Which vanilla behaviour it gets is read off the texture it borrows: a
	 * thing that looks like a pickaxe mines like one. That sounds backwards,
	 * but the look and the job always agree in SkyBlock -- the Stonk looks like
	 * a wooden pickaxe because it is one, underneath -- so one field says both
	 * and they cannot drift apart.
	 */
	private static void gear(Content.Gear gear) {
		String noun = switch (gear.kind()) {
			case "bow" -> "BOW";
			case "tool" -> "TOOL";
			case "trinket" -> "ITEM";
			default -> "SWORD";
		};
		Item.Properties props = props(gear.id(), gear.rarity())
				.stacksTo(1)
				.enchantable(15)
				.component(DataComponents.ITEM_NAME, name(gear.name(), gear.rarity()))
				.component(DataComponents.LORE, lore(gear.desc(), gear.rarity(), noun));

		ToolMaterial metal = metalFor(gear.look());
		Item item;
		if (gear.kind().equals("bow")) {
			item = new BowItem(props.durability(384));
		} else if (gear.look().contains("pickaxe")) {
			item = new Item(props.pickaxe(metal, gear.damage(), gear.speed()));
		} else if (gear.look().contains("axe")) {
			item = new Item(props.axe(metal, gear.damage(), gear.speed()));
		} else if (gear.look().contains("hoe")) {
			item = new Item(props.hoe(metal, gear.damage(), gear.speed()));
		} else if (gear.kind().equals("trinket")) {
			item = new Item(props.durability(256));
		} else {
			item = new Item(props.sword(metal, gear.damage(), gear.speed()));
		}
		add("gear", gear.id(), item);
	}

	/**
	 * One piece of armour, wearable and worth wearing.
	 *
	 * It is drawn on you as the vanilla armour it resembles -- Ender Armor
	 * shows as diamond, Farm Suit as leather. Painting twenty-eight new sets
	 * onto the player model would mean twenty-eight more textures and a lot of
	 * guessing at what they look like; borrowing keeps every set visible and
	 * distinct in the inventory, which is where you actually read them.
	 */
	private static void armour(Content.Armour piece) {
		ArmorType type = ArmorType.valueOf(piece.type());
		Item.Properties props = props(piece.id(), piece.rarity())
				.component(DataComponents.ITEM_NAME, name(piece.name(), piece.rarity()))
				.component(DataComponents.LORE, lore(piece.desc(), piece.rarity(), piece.type()))
				.humanoidArmor(armourMaterial(piece), type);
		add("armor", piece.id(), new Item(props));
	}

	/** A placeable block, and the item you hold to place it. */
	private static void block(Content.Blok blok) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, SkyBlocksMod.id(blok.id()));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new Block(BlockBehaviour.Properties.of()
						.strength(blok.hardness())
						.requiresCorrectToolForDrops()
						.sound(SoundType.STONE)
						.setId(blockKey)));
		BLOCKS.put(blok.id(), block);

		Item.Properties props = props(blok.id(), blok.rarity())
				.useBlockDescriptionPrefix()
				.component(DataComponents.ITEM_NAME, name(blok.name(), blok.rarity()))
				.component(DataComponents.LORE, lore("", blok.rarity(), "BLOCK"));
		add("blocks", blok.id(), new BlockItem(block, props));
	}

	// ------------------------------------------------------------- the parts

	private static Item.Properties props(String id, String rarity) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SkyBlocksMod.id(id));
		return new Item.Properties().setId(key).rarity(vanillaRarity(rarity));
	}

	private static Item add(String tab, String id, Item item) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SkyBlocksMod.id(id));
		Item registered = Registry.register(BuiltInRegistries.ITEM, key, item);
		ITEMS.put(id, registered);
		BY_TAB.computeIfAbsent(tab, unused -> new ArrayList<>()).add(registered);
		return registered;
	}

	/**
	 * The name, in the colour its rarity says it should be.
	 *
	 * Italics are turned off explicitly. A name set this way is treated as a
	 * rename and shown in italics by default, which looks like somebody has
	 * been at it with an anvil.
	 */
	private static Component name(String text, String rarity) {
		return Component.literal(text)
				.withStyle(style -> style.withColor(colour(rarity)).withItalic(false));
	}

	/**
	 * The tooltip: what it does, then what it is.
	 *
	 * The last line is the rarity in capitals, which is how SkyBlock says it
	 * and the fastest way to read a chest full of things you have never seen.
	 */
	private static ItemLore lore(String desc, String rarity, String noun) {
		List<Component> lines = new ArrayList<>();
		if (!desc.isEmpty()) {
			for (String line : wrap(desc)) {
				lines.add(Component.literal(line)
						.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)));
			}
			lines.add(Component.literal(""));
		}
		lines.add(Component.literal(rarity + " " + noun)
				.withStyle(style -> style.withColor(colour(rarity)).withBold(true).withItalic(false)));
		return new ItemLore(lines);
	}

	/** Break a description at spaces so it doesn't run off the screen. */
	private static List<String> wrap(String text) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (line.length() + word.length() > 40) {
				lines.add(line.toString());
				line.setLength(0);
			}
			line.append(line.isEmpty() ? "" : " ").append(word);
		}
		if (!line.isEmpty()) {
			lines.add(line.toString());
		}
		return lines;
	}

	/**
	 * SkyBlock's rarity colours, which are not Minecraft's.
	 *
	 * Vanilla has four rarities and stops at light purple. SkyBlock has seven
	 * and its Legendary is gold, so the colour is set by hand rather than left
	 * to {@link Rarity}.
	 */
	public static ChatFormatting colour(String rarity) {
		return switch (rarity) {
			case "UNCOMMON" -> ChatFormatting.GREEN;
			case "RARE" -> ChatFormatting.BLUE;
			case "EPIC" -> ChatFormatting.DARK_PURPLE;
			case "LEGENDARY" -> ChatFormatting.GOLD;
			case "MYTHIC" -> ChatFormatting.LIGHT_PURPLE;
			case "SPECIAL" -> ChatFormatting.RED;
			default -> ChatFormatting.WHITE;
		};
	}

	/** The nearest vanilla rarity, so sorting and search still behave. */
	private static Rarity vanillaRarity(String rarity) {
		return switch (rarity) {
			case "UNCOMMON" -> Rarity.UNCOMMON;
			case "RARE" -> Rarity.RARE;
			case "COMMON" -> Rarity.COMMON;
			default -> Rarity.EPIC;
		};
	}

	private static ToolMaterial metalFor(String look) {
		if (look.startsWith("wooden")) {
			return ToolMaterial.WOOD;
		}
		if (look.startsWith("stone")) {
			return ToolMaterial.STONE;
		}
		if (look.startsWith("golden")) {
			return ToolMaterial.GOLD;
		}
		if (look.startsWith("iron")) {
			return ToolMaterial.IRON;
		}
		if (look.startsWith("netherite")) {
			return ToolMaterial.NETHERITE;
		}
		return ToolMaterial.DIAMOND;
	}

	/**
	 * The armour material for a set.
	 *
	 * Defence is the number from the table, on every piece -- SkyBlock gives
	 * each piece its own Defence stat rather than splitting a set total the way
	 * Minecraft does, so a helmet and a chestplate from the same set protect
	 * you equally.
	 */
	private static ArmorMaterial armourMaterial(Content.Armour piece) {
		Map<ArmorType, Integer> defence = new LinkedHashMap<>();
		for (ArmorType type : ArmorType.values()) {
			defence.put(type, piece.defence());
		}
		return new ArmorMaterial(
				37,                                          // durability factor
				defence,
				12,                                          // enchantment value
				equipSound(piece.look()),
				piece.rarity().equals("LEGENDARY") ? 3.0f : 1.0f,
				0.0f,
				repairs(piece.look()),
				equipmentAsset(piece.look()));
	}

	private static net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> equipSound(String look) {
		return switch (look) {
			case "LEATHER" -> SoundEvents.ARMOR_EQUIP_LEATHER;
			case "CHAINMAIL" -> SoundEvents.ARMOR_EQUIP_CHAIN;
			case "GOLD" -> SoundEvents.ARMOR_EQUIP_GOLD;
			case "DIAMOND" -> SoundEvents.ARMOR_EQUIP_DIAMOND;
			default -> SoundEvents.ARMOR_EQUIP_IRON;
		};
	}

	private static net.minecraft.tags.TagKey<Item> repairs(String look) {
		return switch (look) {
			case "LEATHER" -> ItemTags.REPAIRS_LEATHER_ARMOR;
			case "CHAINMAIL" -> ItemTags.REPAIRS_CHAIN_ARMOR;
			case "GOLD" -> ItemTags.REPAIRS_GOLD_ARMOR;
			case "DIAMOND" -> ItemTags.REPAIRS_DIAMOND_ARMOR;
			default -> ItemTags.REPAIRS_IRON_ARMOR;
		};
	}

	private static ResourceKey<EquipmentAsset> equipmentAsset(String look) {
		return switch (look) {
			case "LEATHER" -> EquipmentAssets.LEATHER;
			case "CHAINMAIL" -> EquipmentAssets.CHAINMAIL;
			case "GOLD" -> EquipmentAssets.GOLD;
			case "DIAMOND" -> EquipmentAssets.DIAMOND;
			default -> EquipmentAssets.IRON;
		};
	}

	// -------------------------------------------------------------- lookups

	/** One of this mod's items by id, or an empty stack if there is no such thing. */
	public static ItemStack stack(String id) {
		Item item = ITEMS.get(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}
}
