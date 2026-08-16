package com.example;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/**
 * The XP Boost potions: seven skills, three tiers each.
 *
 * Drinking one gives that skill Wisdom for an hour, and Wisdom is a straight
 * percentage on everything the skill earns -- Tier I is +5, Tier II is +10,
 * Tier III is +20, which are the game's own numbers.
 *
 * They do not stack with themselves. A second bottle replaces the first rather
 * than adding to it, so a stack of twenty-one Tier IIIs is twenty-one hours of
 * +20 and not one minute of +420. That is the real game's rule and it is also
 * the only version of the rule that leaves the skill curve meaning anything.
 *
 * Only the seven skills the game gives boosts for have them. There is no Taming
 * XP Boost in SkyBlock and there is not one here.
 */
public final class XpBoosts {
	private XpBoosts() {
	}

	/** How long a boost lasts: one hour of play. */
	private static final long DURATION = 20L * 60 * 60;

	/** Every boost potion, by its id. */
	public static final Map<String, Item> BOOSTS = new LinkedHashMap<>();

	public static void register() {
		for (Content.Boost boost : Content.BOOSTS) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SkyBlocksMod.id(boost.id()));

			ItemLore lore = new ItemLore(List.of(
					Component.literal("Gain " + boost.skill() + " Wisdom")
							.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)),
					Component.literal(""),
					Component.literal("+" + boost.wisdom() + " " + boost.skill() + " Wisdom")
							.withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)),
					Component.literal("for 1 hour")
							.withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY).withItalic(false)),
					Component.literal(""),
					Component.literal(boost.rarity() + " POTION")
							.withStyle(style -> style.withColor(SkyItems.colour(boost.rarity()))
									.withBold(true).withItalic(false))));

			Item item = Registry.register(BuiltInRegistries.ITEM, key,
					new Bottle(boost, new Item.Properties()
							.setId(key)
							.stacksTo(16)
							.component(DataComponents.ITEM_NAME,
									Component.literal(boost.name())
											.withStyle(style -> style
													.withColor(SkyItems.colour(boost.rarity()))
													.withItalic(false)))
							.component(DataComponents.LORE, lore)));
			BOOSTS.put(boost.id(), item);
		}
		SkyBlocksMod.LOGGER.info("Sky Blocks added {} XP Boost potions.", BOOSTS.size());
	}

	/**
	 * The bottle. Right-click anywhere to drink it.
	 *
	 * Drunk on use rather than held down like a vanilla potion, because there
	 * is nothing to animate and holding right-click for a second and a half to
	 * start an hour-long buff is only ever an annoyance.
	 */
	public static class Bottle extends Item {
		private final Content.Boost boost;

		Bottle(Content.Boost boost, Properties properties) {
			super(properties);
			this.boost = boost;
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (!(player instanceof ServerPlayer serverPlayer)
					|| !(level instanceof ServerLevel world)) {
				return InteractionResult.SUCCESS;
			}
			if (!SkyBlocksMod.allowed(serverPlayer, world)) {
				return InteractionResult.PASS;
			}

			Skills.drink(serverPlayer, boost.skill(), boost.wisdom(), DURATION);
			serverPlayer.sendSystemMessage(Component.literal("")
					.append(Component.literal("BOOST! ").withStyle(ChatFormatting.AQUA))
					.append(Component.literal("+" + boost.wisdom() + " " + boost.skill()
							+ " Wisdom for one hour.").withStyle(ChatFormatting.GRAY)));
			world.playSound(null, serverPlayer.blockPosition(),
					SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.7f, 1.3f);

			if (!player.hasInfiniteMaterials()) {
				player.getItemInHand(hand).shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
	}

	public static ItemStack stack(String id) {
		Item item = BOOSTS.get(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}
}
