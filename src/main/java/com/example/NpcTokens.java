package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The people, as something you can carry and put down.
 *
 * Every named person in SkyBlock -- the Blacksmith, Jerry, the Lazy Miner,
 * Melody -- is an item here. Right-click the ground with one and they are
 * standing there, named, facing you, and they will not wander off. Right-click
 * them and they say what they say in the game and hand you their quest.
 *
 * Why an item rather than a command
 * ---------------------------------
 * Because you can hold it. Building a Hub of your own means putting people in
 * it, and picking Melody out of a creative tab and placing her by the trees is
 * the same motion as placing a block. A command would mean knowing the name and
 * spelling it right, and the list is sixty long.
 *
 * The Hub still stands its own people up when it is built. These are for
 * everywhere else.
 */
public final class NpcTokens {
	private NpcTokens() {
	}

	/** Every token, by the id of the person it stands up. */
	public static final Map<String, Item> TOKENS = new java.util.LinkedHashMap<>();

	/** The people, looked up by the name that floats over their head. */
	private static final Map<String, Content.Npc> BY_NAME = new HashMap<>();

	public static void register() {
		for (Content.Npc npc : Content.NPCS) {
			BY_NAME.put(npc.name(), npc);

			String id = "npc_" + npc.id();
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SkyBlocksMod.id(id));

			List<Component> lines = new ArrayList<>();
			lines.add(grey(npc.where()));
			lines.add(Component.literal(""));
			lines.add(grey("Place to stand them somewhere."));
			if (!npc.quest().isEmpty()) {
				lines.add(Component.literal("Quest: " + npc.quest())
						.withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)));
			}
			lines.add(Component.literal("NPC")
					.withStyle(style -> style.withColor(ChatFormatting.GREEN)
							.withBold(true).withItalic(false)));

			Item item = Registry.register(BuiltInRegistries.ITEM, key,
					new Token(npc, new Item.Properties()
							.setId(key)
							.component(DataComponents.ITEM_NAME, Component.literal(npc.name())
									.withStyle(style -> style.withColor(ChatFormatting.GREEN)
											.withItalic(false)))
							.component(DataComponents.LORE, new ItemLore(lines))));
			TOKENS.put(npc.id(), item);
		}
		SkyBlocksMod.LOGGER.info("Sky Blocks added {} people you can place.", TOKENS.size());
	}

	private static Component grey(String text) {
		return Component.literal(text)
				.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	/** The item. Using it on a block stands that person on top of the block. */
	public static class Token extends Item {
		private final Content.Npc who;

		Token(Content.Npc who, Properties properties) {
			super(properties);
			this.who = who;
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			if (!(context.getLevel() instanceof ServerLevel level)
					|| !(context.getPlayer() instanceof ServerPlayer player)) {
				// Let the client play the swing animation and wait for the server.
				return InteractionResult.SUCCESS;
			}
			BlockPos on = context.getClickedPos().relative(context.getClickedFace());
			Npcs.spawnVillager(level, on, who.name());
			level.playSound(null, on, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.7f, 1.0f);
			player.sendSystemMessage(Component.literal("§a" + who.name() + "§7 is here now. "
					+ "Right-click to talk."));

			if (!player.hasInfiniteMaterials()) {
				context.getItemInHand().shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
	}

	/**
	 * Somebody has right-clicked a villager. Say their piece.
	 *
	 * Returns false when the name is not one of ours, so the Banker, the
	 * Auctioneer and the Bazaar Trader keep the shops they open instead of
	 * standing there reciting.
	 */
	public static boolean talk(ServerPlayer player, String name) {
		Content.Npc npc = BY_NAME.get(name);
		if (npc == null) {
			return false;
		}
		player.sendSystemMessage(Component.literal("")
				.append(Component.literal("[NPC] ").withStyle(ChatFormatting.DARK_GREEN))
				.append(Component.literal(npc.name()).withStyle(ChatFormatting.GREEN)));
		for (String line : npc.lines()) {
			player.sendSystemMessage(Component.literal("  §f" + line));
		}
		if (!npc.quest().isEmpty()) {
			Quests.offer(player, npc.quest());
		}
		player.level().playSound(null, player.blockPosition(),
				SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.5f, 1.1f);
		return true;
	}

	/** A token for the given person, for the creative tab and for /npc. */
	public static ItemStack token(String id) {
		Item item = TOKENS.get(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}
}
