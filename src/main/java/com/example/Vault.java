package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * The things a player owns beyond their pockets: Storage, the Wardrobe, their
 * Collections and their chosen Pet.
 *
 * All of it hangs off the player and is saved with them, and all of it survives
 * death. Storage in particular is the safe place for items, the way the Bank is
 * the safe place for coins.
 */
public final class Vault {
	private Vault() {
	}

	/**
	 * The Personal Vault: 27 slots, as the wiki gives it.
	 *
	 * Unlocked once for a fee and then yours forever, and unlike your pockets
	 * nothing in here is ever at risk.
	 */
	public static final int STORAGE_SLOTS = 27;

	/** What unlocking the vault costs, from the wiki. */
	public static final long VAULT_COST = 10_000L;

	/** Whether this player has paid to open their vault. */
	public static final AttachmentType<Boolean> VAULT_OPEN =
			AttachmentRegistry.<Boolean>builder()
					.initializer(() -> false)
					.persistent(Codec.BOOL)
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("vault_open"));

	public static boolean vaultOpen(ServerPlayer player) {
		return player.getAttachedOrCreate(VAULT_OPEN, () -> false);
	}

	public static void openVault(ServerPlayer player) {
		player.setAttached(VAULT_OPEN, true);
	}

	/** Wardrobe: helmet, chestplate, leggings, boots. */
	public static final int WARDROBE_SLOTS = 4;

	public static final AttachmentType<List<ItemStack>> STORAGE =
			AttachmentRegistry.<List<ItemStack>>builder()
					.initializer(() -> empty(STORAGE_SLOTS))
					.persistent(Codec.list(ItemStack.OPTIONAL_CODEC))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("storage"));

	public static final AttachmentType<List<ItemStack>> WARDROBE =
			AttachmentRegistry.<List<ItemStack>>builder()
					.initializer(() -> empty(WARDROBE_SLOTS))
					.persistent(Codec.list(ItemStack.OPTIONAL_CODEC))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("wardrobe"));

	/**
	 * Collections: how many of each thing you have ever gathered.
	 *
	 * Counted by the item's registry name, so it survives the mod changing and
	 * reads sensibly if anyone ever looks at the save file.
	 */
	public static final AttachmentType<Map<String, Long>> COLLECTIONS =
			AttachmentRegistry.<Map<String, Long>>builder()
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("collections"));

	/** Which pet is out, by name, or empty for none. */
	public static final AttachmentType<String> PET =
			AttachmentRegistry.<String>builder()
					.initializer(() -> "")
					.persistent(Codec.STRING)
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("pet"));

	private static List<ItemStack> empty(int size) {
		List<ItemStack> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(ItemStack.EMPTY);
		}
		return list;
	}

	// ---------------------------------------------------------------- storage

	/**
	 * Load a saved list into a container the player can actually use.
	 *
	 * The container is a live copy: {@link #save} puts it back when they close
	 * the screen.
	 */
	public static SimpleContainer container(ServerPlayer player,
			AttachmentType<List<ItemStack>> which, int size) {
		List<ItemStack> saved = player.getAttachedOrCreate(which, () -> empty(size));
		SimpleContainer box = new SimpleContainer(size);
		for (int slot = 0; slot < Math.min(size, saved.size()); slot++) {
			box.setItem(slot, saved.get(slot).copy());
		}
		return box;
	}

	/** Write a container back to the player. */
	public static void save(ServerPlayer player, AttachmentType<List<ItemStack>> which,
			SimpleContainer box) {
		List<ItemStack> out = new ArrayList<>(box.getContainerSize());
		for (int slot = 0; slot < box.getContainerSize(); slot++) {
			out.add(box.getItem(slot).copy());
		}
		player.setAttached(which, out);
	}

	// ------------------------------------------------------------ collections

	/**
	 * Record that a player gathered some of something.
	 *
	 * The count is only half of it: crossing 50, 100, 250 and so on is a
	 * Collection tier, and a tier pays. See {@link Collections#check}, which is
	 * called straight afterwards so the reward lands on the block that earned
	 * it rather than the next time a menu is opened.
	 */
	public static void collect(ServerPlayer player, net.minecraft.world.item.Item item, long count) {
		String name = BuiltInRegistries.ITEM.getKey(item).getPath();
		Map<String, Long> all = new HashMap<>(player.getAttachedOrCreate(COLLECTIONS, HashMap::new));
		all.merge(name, count, Long::sum);
		player.setAttached(COLLECTIONS, all);
		Collections.check(player, item);
	}

	public static Map<String, Long> collections(ServerPlayer player) {
		return player.getAttachedOrCreate(COLLECTIONS, HashMap::new);
	}

	// ------------------------------------------------------------------- pets

	/**
	 * Which pet is following you, by name.
	 *
	 * Each is unlocked by getting the matching skill to level 5, so having one
	 * is proof you played rather than something handed over. What each does is
	 * defined in {@link Pages}.
	 */
	public static String pet(ServerPlayer player) {
		return player.getAttachedOrCreate(PET, () -> "");
	}

	public static void setPet(ServerPlayer player, String name) {
		player.setAttached(PET, name);
	}
}
