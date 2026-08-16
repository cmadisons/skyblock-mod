package com.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Runecrafting: the runes, and the pedestal you combine them on.
 *
 * Twenty-eight runes in three tiers each. A rune is a look rather than a stat
 * -- Bloody makes a weapon drip, Music makes it play, Smoky makes it smoke --
 * which is why the only thing that differs between them here is the colour cut
 * into the tablet.
 *
 * How the pedestal works
 * ----------------------
 * Stand three of the same rune at the same tier in your hand and right-click
 * the pedestal. They fuse into one of the next tier up, and you get
 * Runecrafting XP whether or not it worked -- which is the real game's rule and
 * the reason the skill levels at all. Combining is the only source of
 * Runecrafting XP there is.
 *
 * Tier III is the top. Three of those do nothing but tell you so.
 */
public final class Runes {
	private Runes() {
	}

	/** How many of a rune it takes to make the next one up. */
	private static final int RECIPE = 3;

	/** Every rune, by its id. */
	public static final Map<String, Item> RUNES = new LinkedHashMap<>();

	/** The pedestal, and the item you place it with. */
	public static Block PEDESTAL;

	public static void register() {
		for (Content.Rune rune : Content.RUNES) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SkyBlocksMod.id(rune.id()));
			List<Component> lore = new ArrayList<>();
			lore.add(grey("Applied to a weapon or a piece of armour."));
			lore.add(Component.literal(""));
			if (rune.tier() < 3) {
				lore.add(grey("Combine " + RECIPE + " on a Rune Pedestal to upgrade."));
			} else {
				lore.add(grey("The highest tier this rune goes."));
			}
			lore.add(Component.literal(""));
			lore.add(Component.literal(rune.rarity() + " RUNE")
					.withStyle(style -> style.withColor(SkyItems.colour(rune.rarity()))
							.withBold(true).withItalic(false)));

			Item item = Registry.register(BuiltInRegistries.ITEM, key,
					new Item(new Item.Properties()
							.setId(key)
							.component(DataComponents.ITEM_NAME, Component.literal(rune.name())
									.withStyle(style -> style
											.withColor(SkyItems.colour(rune.rarity())).withItalic(false)))
							.component(DataComponents.LORE, new ItemLore(lore))));
			RUNES.put(rune.id(), item);
		}

		// --- the pedestal ------------------------------------------------------
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK,
				SkyBlocksMod.id("rune_pedestal"));
		PEDESTAL = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new Pedestal(BlockBehaviour.Properties.of()
						.strength(3.0f)
						.sound(SoundType.STONE)
						.noOcclusion()
						.setId(blockKey)));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
				SkyBlocksMod.id("rune_pedestal"));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(PEDESTAL, new Item.Properties()
						.setId(itemKey)
						.useBlockDescriptionPrefix()
						.component(DataComponents.ITEM_NAME, Component.literal("Rune Pedestal")
								.withStyle(style -> style.withColor(ChatFormatting.BLUE)
										.withItalic(false)))
						.component(DataComponents.LORE, new ItemLore(List.of(
								grey("Right-click holding " + RECIPE + " matching runes"),
								grey("to fuse them into the next tier."),
								Component.literal(""),
								Component.literal("RARE BLOCK")
										.withStyle(style -> style.withColor(ChatFormatting.BLUE)
												.withBold(true).withItalic(false)))))));

		SkyBlocksMod.LOGGER.info("Sky Blocks added {} runes and the pedestal.", RUNES.size());
	}

	private static Component grey(String text) {
		return Component.literal(text)
				.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	/**
	 * The pedestal itself.
	 *
	 * Waist-high rather than a full cube, because you stand things on a
	 * pedestal and a metre-tall block you cannot see over is a plinth.
	 */
	public static class Pedestal extends Block {
		private static final VoxelShape SHAPE = box(2, 0, 2, 14, 12, 14);

		public Pedestal(Properties properties) {
			super(properties);
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
				CollisionContext context) {
			return SHAPE;
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
				Player player, BlockHitResult hit) {
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendSystemMessage(Component.literal(
						"§9Rune Pedestal§7: hold " + RECIPE + " matching runes and right-click."));
			}
			return InteractionResult.SUCCESS;
		}

		@Override
		protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level,
				BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
				BlockHitResult hit) {
			if (!(player instanceof ServerPlayer serverPlayer)
					|| !(level instanceof ServerLevel world)) {
				return InteractionResult.SUCCESS;
			}
			return combine(serverPlayer, world, pos, held);
		}
	}

	/**
	 * Fuse three matching runes into one of the next tier.
	 *
	 * XP is paid for the attempt rather than the success. That is deliberate
	 * and it is the game's own rule: a Tier III rune has nothing above it, so
	 * a Runecrafting skill that only paid on success would stop dead the moment
	 * you finished a rune line.
	 */
	private static InteractionResult combine(ServerPlayer player, ServerLevel level, BlockPos pos,
			ItemStack held) {
		Content.Rune rune = find(held.getItem());
		if (rune == null) {
			player.sendSystemMessage(Component.literal(
					"§9Rune Pedestal§7: that isn't a rune."));
			return InteractionResult.PASS;
		}
		if (held.getCount() < RECIPE) {
			player.sendSystemMessage(Component.literal("§9Rune Pedestal§7: you need §f"
					+ RECIPE + "§7 of them. You have §f" + held.getCount() + "§7."));
			return InteractionResult.PASS;
		}

		// Paid either way -- see the note above.
		Skills.add(player, Skills.RUNECRAFTING, 15L * rune.tier());
		level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.4f);

		if (rune.tier() >= 3) {
			player.sendSystemMessage(Component.literal("§9Rune Pedestal§7: "
					+ rune.name() + " is already at its highest tier."));
			return InteractionResult.SUCCESS;
		}

		Content.Rune next = at(rune.kind(), rune.tier() + 1);
		if (next == null) {
			return InteractionResult.SUCCESS;
		}
		held.shrink(RECIPE);
		Item made = RUNES.get(next.id());
		if (made != null) {
			player.getInventory().placeItemBackInInventory(new ItemStack(made));
		}
		player.sendSystemMessage(Component.literal("")
				.append(Component.literal("RUNE! ").withStyle(ChatFormatting.LIGHT_PURPLE))
				.append(Component.literal("Made " + next.name()).withStyle(ChatFormatting.WHITE)));
		return InteractionResult.SUCCESS;
	}

	/** Which rune this item is, or null if it isn't one. */
	private static Content.Rune find(Item item) {
		for (Content.Rune rune : Content.RUNES) {
			if (RUNES.get(rune.id()) == item) {
				return rune;
			}
		}
		return null;
	}

	/** That rune, at that tier. */
	private static Content.Rune at(String kind, int tier) {
		for (Content.Rune rune : Content.RUNES) {
			if (rune.kind().equals(kind) && rune.tier() == tier) {
				return rune;
			}
		}
		return null;
	}

	public static ItemStack stack(String id) {
		Item item = RUNES.get(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}
}
