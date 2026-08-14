package com.example;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The seven skills, and the experience that feeds them.
 *
 * XP is earned by playing rather than handed out: break stone and Mining goes
 * up, chop wood and Foraging does, kill a monster and Combat does. Nothing here
 * gives a reward yet beyond the number itself -- levels are the scoreboard, and
 * what they unlock comes later.
 *
 * All seven earn XP now:
 *
 *   Farming, Foraging, Mining   breaking the right blocks
 *   Combat                      killing monsters
 *   Hunting                     killing animals
 *   Taming                      breeding animals, so it grows from looking
 *                               after things rather than killing them
 *   HOTF (Heart of the Forest)  a tenth of all Foraging XP, so it climbs
 *                               quietly in the background as you chop
 *
 * Levels use a squared curve: level 1 costs 50 XP, level 2 costs 200, level 10
 * costs 5,000. Early levels come quickly and later ones take real work, which
 * is what keeps a skill worth pushing.
 */
public final class Skills {
	private Skills() {
	}

	public static final String FARMING = "Farming";
	public static final String FORAGING = "Foraging";
	public static final String MINING = "Mining";
	public static final String COMBAT = "Combat";
	public static final String TAMING = "Taming";
	public static final String HUNTING = "Hunting";

	/**
	 * Heart of the Forest. Not a skill in the real game -- this one is ours.
	 * It has no source of XP yet, so it sits at zero until we decide what
	 * feeds it.
	 */
	public static final String HOTF = "HOTF";

	/** Shown in this order by /skills. */
	public static final String[] ALL = {FARMING, FORAGING, MINING, COMBAT, TAMING, HUNTING, HOTF};

	/**
	 * Every skill's XP for one player, saved with them.
	 *
	 * copyOnDeath matters: dying in a game about a tiny island is common, and
	 * losing every skill level on a bad jump would be miserable.
	 */
	public static final AttachmentType<Map<String, Long>> XP =
			AttachmentRegistry.<Map<String, Long>>builder()
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("skill_xp"));

	/** Total XP a player has in one skill. */
	public static long xp(ServerPlayer player, String skill) {
		Map<String, Long> all = player.getAttachedOrCreate(XP, HashMap::new);
		return all.getOrDefault(skill, 0L);
	}

	/** The level that much XP buys. */
	public static int level(long xp) {
		return (int) Math.floor(Math.sqrt(xp / 50.0));
	}

	/** Total XP needed to reach a level. */
	public static long xpForLevel(int level) {
		return 50L * level * level;
	}

	/**
	 * Add XP, and say so in chat if it pushed the player up a level.
	 *
	 * The map from the attachment is read-only, so it is copied before being
	 * changed and put back -- editing it in place would silently do nothing.
	 */
	public static void add(ServerPlayer player, String skill, long amount) {
		if (amount <= 0) {
			return;
		}
		Map<String, Long> all = new HashMap<>(player.getAttachedOrCreate(XP, HashMap::new));
		long before = all.getOrDefault(skill, 0L);
		long after = before + amount;
		all.put(skill, after);
		player.setAttached(XP, all);

		if (level(after) > level(before)) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
					"§6" + skill + " level " + level(after) + "!"));
		}
	}

	/**
	 * Wire XP up to actually playing.
	 *
	 * Which skill a block feeds is decided by what it is: ores and stone are
	 * Mining, wood and leaves are Foraging, crops are Farming. Anything else
	 * gives nothing, so cobblestone from your own minion isn't a free ride.
	 */
	public static void registerHooks() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return;
			}
			if (!(serverPlayer.level() instanceof net.minecraft.server.level.ServerLevel world)
					|| !SkyBlocksMod.allowed(serverPlayer, world)) {
				return;
			}
			String skill = skillFor(state);
			if (skill != null) {
				add(serverPlayer, skill, xpFor(state));
				// Heart of the Forest grows off Foraging rather than its own
				// blocks -- it is the slow background half of chopping.
				if (skill.equals(FORAGING)) {
					add(serverPlayer, HOTF, Math.max(1, xpFor(state) / 10));
				}
			}
			// Collections count everything broken, whether it feeds a skill
			// or not, so the page shows the true picture of what you've done.
			Vault.collect(serverPlayer, state.getBlock().asItem(), 1);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer killer
					&& killer.level() instanceof net.minecraft.server.level.ServerLevel world
					&& SkyBlocksMod.allowed(killer, world)) {
				// Tougher things are worth more, using their own health as the guide.
				long worth = Math.max(1, (long) (((LivingEntity) entity).getMaxHealth() / 2));
				// Monsters feed Combat; animals feed Hunting. Killing a chicken
				// shouldn't make you a better fighter.
				add(killer, entity instanceof net.minecraft.world.entity.monster.Enemy
						? COMBAT : HUNTING, worth);
			}

			// Dying costs you half of what you're carrying. Your items are
			// safe -- keepInventory is switched on for Sky Blocks worlds.
			if (entity instanceof ServerPlayer dead) {
				Economy.onDeath(dead);
			}
		});
	}

	/**
	 * Taming XP, for breeding animals.
	 *
	 * A newly born animal means somebody just fed two of them, so the nearest
	 * player gets the credit. Looking after animals is what Taming is for --
	 * killing them is Hunting, and the two shouldn't be the same thing.
	 */
	public static void registerBreeding() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!(entity instanceof net.minecraft.world.entity.animal.Animal baby) || !baby.isBaby()) {
				return;
			}
			net.minecraft.world.entity.player.Player near =
					world.getNearestPlayer(entity, 12.0);
			if (near instanceof ServerPlayer player && SkyBlocksMod.allowed(player, world)) {
				add(player, TAMING, 10);
			}
		});
	}

	/** Which skill this block belongs to, or null if it isn't worth any. */
	private static String skillFor(BlockState state) {
		if (state.is(Blocks.OAK_LOG) || state.is(Blocks.BIRCH_LOG) || state.is(Blocks.SPRUCE_LOG)
				|| state.is(Blocks.JUNGLE_LOG) || state.is(Blocks.ACACIA_LOG)
				|| state.is(Blocks.DARK_OAK_LOG) || state.is(Blocks.OAK_LEAVES)) {
			return FORAGING;
		}
		if (state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES)
				|| state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)
				|| state.is(Blocks.SUGAR_CANE) || state.is(Blocks.NETHER_WART)) {
			return FARMING;
		}
		if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
				|| state.is(Blocks.COAL_ORE) || state.is(Blocks.IRON_ORE) || state.is(Blocks.GOLD_ORE)
				|| state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.EMERALD_ORE)
				|| state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.LAPIS_ORE)
				|| state.is(Blocks.DEEPSLATE_DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)
				|| state.is(Blocks.DEEPSLATE_COAL_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)
				|| state.is(Blocks.NETHER_QUARTZ_ORE) || state.is(Blocks.ANCIENT_DEBRIS)) {
			return MINING;
		}
		return null;
	}

	/** Rarer blocks are worth more XP than common ones. */
	private static long xpFor(BlockState state) {
		if (state.is(Blocks.ANCIENT_DEBRIS)) {
			return 50;
		}
		if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
				|| state.is(Blocks.EMERALD_ORE)) {
			return 20;
		}
		if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.IRON_ORE) || state.is(Blocks.LAPIS_ORE)
				|| state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) {
			return 8;
		}
		if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)
				|| state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.NETHER_QUARTZ_ORE)) {
			return 5;
		}
		return 1;
	}
}
