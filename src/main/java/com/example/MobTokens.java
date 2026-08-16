package com.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;

/**
 * The enemies, as something you can carry and put down.
 *
 * Every enemy SkyBlock has that Minecraft does not -- a Crypt Ghoul, a Zealot,
 * a Soul of the Alpha, the Magma Boss -- is an item here. Right-click the
 * ground with one and it is standing there, named "[Lv30] Crypt Ghoul" the way
 * the game writes it, with the level, health and damage the game gives it.
 *
 * They are vanilla mobs underneath, which is what they are on Hypixel too: a
 * Crypt Ghoul is a zombie with a name and a great deal more health. What makes
 * it a Crypt Ghoul is the numbers, and the numbers are the game's own, used
 * exactly as they are -- see tools/enemies.py for the list and
 * {@link com.example.mixin.BigNumbersMixin} for what it takes to get two
 * hundred million health past Minecraft's own ceiling.
 */
public final class MobTokens {
	private MobTokens() {
	}

	/** Every enemy token, by the id of the enemy it puts down. */
	public static final Map<String, Item> TOKENS = new LinkedHashMap<>();

	public static void register() {
		for (Content.Mob mob : Content.MOBS) {
			String id = "mob_" + mob.id();
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SkyBlocksMod.id(id));

			// Hypixel's own figures on the tooltip, not the scaled-down ones --
			// so the item says what the wiki says, and the scaling stays an
			// implementation detail of putting it in a world.
			ItemLore lore = new ItemLore(java.util.List.of(
					grey(mob.where()),
					Component.literal(""),
					Component.literal("Health: " + pretty(mob.health()))
							.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false)),
					Component.literal("Damage: " + pretty(mob.damage()))
							.withStyle(style -> style.withColor(ChatFormatting.RED).withItalic(false)),
					Component.literal(""),
					Component.literal("ENEMY")
							.withStyle(style -> style.withColor(ChatFormatting.DARK_RED)
									.withBold(true).withItalic(false))));

			Item item = Registry.register(BuiltInRegistries.ITEM, key,
					new Token(mob, new Item.Properties()
							.setId(key)
							.component(DataComponents.ITEM_NAME,
									Component.literal("[Lv" + mob.level() + "] " + mob.name())
											.withStyle(style -> style
													.withColor(colour(mob.level())).withItalic(false)))
							.component(DataComponents.LORE, lore)));
			TOKENS.put(mob.id(), item);
		}
		SkyBlocksMod.LOGGER.info("Sky Blocks added {} enemies you can place.", TOKENS.size());
	}

	/** Grey for harmless, yellow for real, red for dangerous, dark red for worse. */
	private static ChatFormatting colour(int level) {
		if (level >= 50) {
			return ChatFormatting.DARK_RED;
		}
		if (level >= 25) {
			return ChatFormatting.RED;
		}
		if (level >= 8) {
			return ChatFormatting.YELLOW;
		}
		return ChatFormatting.GRAY;
	}

	/** 200,000,000 rather than 2.0E8, which is what a raw double would print. */
	private static String pretty(double value) {
		return String.format("%,d", (long) value);
	}

	private static Component grey(String text) {
		return Component.literal(text)
				.withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false));
	}

	/** The item. Using it on a block stands that enemy on top of the block. */
	public static class Token extends Item {
		private final Content.Mob mob;

		Token(Content.Mob mob, Properties properties) {
			super(properties);
			this.mob = mob;
		}

		@Override
		public InteractionResult useOn(UseOnContext context) {
			if (!(context.getLevel() instanceof ServerLevel level)
					|| !(context.getPlayer() instanceof ServerPlayer player)) {
				return InteractionResult.SUCCESS;
			}
			BlockPos on = context.getClickedPos().relative(context.getClickedFace());
			if (!spawn(level, on, mob)) {
				player.sendSystemMessage(Component.literal(
						"§cCouldn't put down " + mob.name() + " — no such mob in this version."));
				return InteractionResult.FAIL;
			}
			player.sendSystemMessage(Component.literal("§7Put down §c[Lv" + mob.level() + "] "
					+ mob.name() + "§7."));
			if (!player.hasInfiniteMaterials()) {
				context.getItemInHand().shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
	}

	/**
	 * Put one enemy in the world, named and tuned.
	 *
	 * The vanilla mob is looked up by name rather than through a switch over
	 * every entity in the game, so adding an enemy to tools/enemies.py is one
	 * line and no Java at all. An unknown name returns false instead of
	 * throwing, which is what would happen if Mojang ever renamed one.
	 */
	public static boolean spawn(ServerLevel level, BlockPos where, Content.Mob mob) {
		Optional<EntityType<?>> type = EntityType.byString(mob.entity());
		if (type.isEmpty()) {
			return false;
		}
		Entity entity = type.get().create(level, EntitySpawnReason.COMMAND);
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		living.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 0.0f, 0.0f);
		living.setCustomName(Component.literal("[Lv" + mob.level() + "] " + mob.name())
				.withStyle(colour(mob.level())));
		living.setCustomNameVisible(true);

		// Hypixel's own figures, used as they are. A Magma Boss really does
		// have two hundred million health and a Crypt Ghoul really does hit for
		// 350, and an enemy that is not those numbers is not that enemy. Getting
		// them past Minecraft's own ceiling takes BigNumbersMixin.
		double health = Math.max(1, mob.health());
		set(living, Attributes.MAX_HEALTH, health);
		living.setHealth((float) health);
		if (mob.damage() > 0) {
			set(living, Attributes.ATTACK_DAMAGE, mob.damage());
		}
		if (living instanceof net.minecraft.world.entity.Mob asMob) {
			asMob.setPersistenceRequired();          // stays where you put it
		}
		level.addFreshEntity(living);
		return true;
	}

	private static void set(LivingEntity entity,
			net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> what,
			double value) {
		net.minecraft.world.entity.ai.attributes.AttributeInstance attribute = entity.getAttribute(what);
		if (attribute != null) {
			attribute.setBaseValue(value);
		}
	}

	public static ItemStack token(String id) {
		Item item = TOKENS.get(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}
}
