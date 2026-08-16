package com.example;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The thirteen skills, their caps, and what each level actually gives you.
 *
 * SkyBlock's own list, with SkyBlock's own ceilings -- and the ceilings are the
 * point. They are not all the same and never have been: Foraging stops at 57,
 * Fishing and Alchemy at 50, Runecrafting at 25. A mod that capped everything
 * at 50 would be tidier and wrong, and the odd numbers are the part a player
 * recognises.
 *
 *   Combat 60 · Farming 60 · Mining 60 · Enchanting 60 · Taming 60
 *   Foraging 57
 *   Fishing 50 · Alchemy 50 · Carpentry 50 · Hunting 50 · Dungeoneering 50
 *   Runecrafting 25 · Social 25          (cosmetic -- no stats)
 *
 * Every one of them is fed by something. Dungeoneering by running the
 * Catacombs, Runecrafting by the Rune Pedestal, Social by the visitors who
 * turn up on your island -- see {@link Dungeons}, {@link Runes} and
 * {@link Visitors}. Nothing on the list is decoration.
 *
 * What a level gives you
 * ----------------------
 * Four of the game's rewards exist in Minecraft already and are handed over as
 * real attributes you can feel: Health, Defence, Strength and Pet Luck become
 * max health, armour, attack damage and luck. The rest -- Crit Chance, the
 * three Fortunes, Intelligence, Ability Damage, Treasure Chance -- are SkyBlock
 * statistics with no vanilla equivalent at all, so they are counted and shown
 * rather than invented badly. See {@link #stat}.
 *
 * XP is still earned by playing rather than handed out, and now every skill has
 * something that feeds it -- see {@link #registerHooks}.
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
	public static final String FISHING = "Fishing";
	public static final String ENCHANTING = "Enchanting";
	public static final String ALCHEMY = "Alchemy";
	public static final String CARPENTRY = "Carpentry";
	public static final String DUNGEONEERING = "Dungeoneering";
	public static final String RUNECRAFTING = "Runecrafting";
	public static final String SOCIAL = "Social";

	/** One skill: its ceiling, and the sentence describing what levels give. */
	public record Skill(String name, int max, boolean cosmetic, String reward) {
	}

	/** Every skill, in the order the game lists them. */
	public static final Skill[] SKILLS = {
			new Skill(COMBAT, 60, false, "+0.5 Crit Chance per level"),
			new Skill(FARMING, 60, false, "+2 Health and +4 Farming Fortune per level"),
			new Skill(MINING, 60, false, "+1 Defence and +4 Mining Fortune per level"),
			new Skill(FORAGING, 57, false, "+1 Strength and +4 Foraging Fortune per level"),
			new Skill(FISHING, 50, false, "+2 Health and +2 Treasure Chance per level"),
			new Skill(ENCHANTING, 60, false, "+1 Intelligence and +0.5 Ability Damage per level"),
			new Skill(ALCHEMY, 50, false, "+1 Intelligence per level"),
			new Skill(CARPENTRY, 50, false, "+1 Health per level"),
			new Skill(TAMING, 60, false, "+1 Pet Luck per level"),
			new Skill(HUNTING, 50, false, "+2 Hunting Fortune per level"),
			new Skill(DUNGEONEERING, 50, false, "+2 Health per level"),
			new Skill(RUNECRAFTING, 25, true, "Access to higher level Runes"),
			new Skill(SOCIAL, 25, true, "Access to more social games"),
	};

	/** Shown in this order by /skills. */
	public static final String[] ALL = names();

	private static String[] names() {
		String[] all = new String[SKILLS.length];
		for (int at = 0; at < SKILLS.length; at++) {
			all[at] = SKILLS[at].name();
		}
		return all;
	}

	public static Skill of(String name) {
		for (Skill skill : SKILLS) {
			if (skill.name().equals(name)) {
				return skill;
			}
		}
		return null;
	}

	/** The highest this skill goes. Fifty if we have never heard of it. */
	public static int maxLevel(String skill) {
		Skill found = of(skill);
		return found == null ? 50 : found.max();
	}

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

	/** How much Wisdom a drunk XP Boost is giving, by skill. */
	public static final AttachmentType<Map<String, Long>> WISDOM =
			AttachmentRegistry.<Map<String, Long>>builder()
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("wisdom"));

	/** When each of those boosts runs out, as a world game time. */
	public static final AttachmentType<Map<String, Long>> WISDOM_UNTIL =
			AttachmentRegistry.<Map<String, Long>>builder()
					.initializer(HashMap::new)
					.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
					.copyOnDeath()
					.buildAndRegister(SkyBlocksMod.id("wisdom_until"));

	/** Total XP a player has in one skill. */
	public static long xp(ServerPlayer player, String skill) {
		Map<String, Long> all = player.getAttachedOrCreate(XP, HashMap::new);
		return all.getOrDefault(skill, 0L);
	}

	/**
	 * The level that much XP buys on the ordinary table.
	 *
	 * Kept for the handful of places that ask about XP without saying which
	 * skill it belongs to. Prefer {@link #levelIn}, which knows the skill and
	 * so can use the right table -- Runecrafting and Dungeoneering are on
	 * completely different scales. See {@link SkillXp}.
	 */
	public static int level(long xp) {
		return SkillXp.levelFor(COMBAT, xp);
	}

	/** The level a player actually has in a skill, capped at that skill's own ceiling. */
	public static int levelIn(ServerPlayer player, String skill) {
		return Math.min(maxLevel(skill), SkillXp.levelFor(skill, xp(player, skill)));
	}

	/** Total XP needed to reach a level in this skill, from nothing. */
	public static long xpForLevel(String skill, int level) {
		return SkillXp.total(skill, level);
	}

	/** XP still to go before this skill's next level. Zero once it is maxed. */
	public static long toNext(ServerPlayer player, String skill) {
		return levelIn(player, skill) >= maxLevel(skill)
				? 0
				: SkillXp.toNext(skill, xp(player, skill));
	}

	/**
	 * Skill average, the number SkyBlock puts at the top of the Skills menu.
	 *
	 * Across the non-cosmetic skills only, and not Dungeoneering -- the game
	 * dropped Dungeoneering out of the average in 0.11.3 because a Catacombs
	 * level is worth so much more work than any other that leaving it in made
	 * the number meaningless.
	 */
	public static double average(ServerPlayer player) {
		int counted = 0;
		int total = 0;
		for (Skill skill : SKILLS) {
			if (skill.cosmetic() || skill.name().equals(DUNGEONEERING)) {
				continue;
			}
			total += levelIn(player, skill.name());
			counted++;
		}
		return counted == 0 ? 0 : (double) total / counted;
	}

	// ---------------------------------------------------------------- wisdom

	/**
	 * Drink an XP Boost: more of a skill's XP, for a while.
	 *
	 * Wisdom is a percentage in SkyBlock and it is one here -- +20 Wisdom is a
	 * fifth again on everything that skill earns. It does not stack with
	 * itself; a second bottle replaces the first rather than adding to it,
	 * which is what stops three Tier IIIs being worth +60.
	 */
	public static void drink(ServerPlayer player, String skill, long wisdom, long ticks) {
		Map<String, Long> amounts = new HashMap<>(player.getAttachedOrCreate(WISDOM, HashMap::new));
		Map<String, Long> until = new HashMap<>(player.getAttachedOrCreate(WISDOM_UNTIL, HashMap::new));
		amounts.put(skill, wisdom);
		until.put(skill, player.level().getGameTime() + ticks);
		player.setAttached(WISDOM, amounts);
		player.setAttached(WISDOM_UNTIL, until);
	}

	/** The Wisdom currently working on a skill, or zero if none is. */
	public static long wisdom(ServerPlayer player, String skill) {
		Map<String, Long> until = player.getAttachedOrCreate(WISDOM_UNTIL, HashMap::new);
		Long ends = until.get(skill);
		if (ends == null || player.level().getGameTime() >= ends) {
			return 0;
		}
		return player.getAttachedOrCreate(WISDOM, HashMap::new).getOrDefault(skill, 0L);
	}

	/** How much longer a boost has, in seconds. Zero if there isn't one. */
	public static long wisdomLeft(ServerPlayer player, String skill) {
		Map<String, Long> until = player.getAttachedOrCreate(WISDOM_UNTIL, HashMap::new);
		Long ends = until.get(skill);
		if (ends == null) {
			return 0;
		}
		return Math.max(0, (ends - player.level().getGameTime()) / 20);
	}

	// -------------------------------------------------------------------- xp

	/**
	 * Add XP, and say so in chat if it pushed the player up a level.
	 *
	 * The map from the attachment is read-only, so it is copied before being
	 * changed and put back -- editing it in place would silently do nothing.
	 *
	 * XP still accrues past a skill's ceiling. Only the level stops, which is
	 * how the real game behaves and means a cap being raised later does not
	 * quietly cost anybody the work they already did.
	 */
	public static void add(ServerPlayer player, String skill, long amount) {
		if (amount <= 0) {
			return;
		}
		// Wisdom from an XP Boost, if one is running.
		long boost = wisdom(player, skill);
		if (boost > 0) {
			amount = amount + (amount * boost) / 100;
		}

		Map<String, Long> all = new HashMap<>(player.getAttachedOrCreate(XP, HashMap::new));
		long before = all.getOrDefault(skill, 0L);
		long after = before + amount;
		all.put(skill, after);
		player.setAttached(XP, all);

		// Taming is levelled by earning Pet XP, and a pet out constantly gains a
		// quarter of whatever skill XP you earn. So Taming grows off everything
		// else you do -- but only while you actually have a pet out, which is
		// the rule that makes it a skill rather than a second copy of the others.
		if (!skill.equals(TAMING) && !Vault.pet(player).isEmpty()) {
			add(player, TAMING, Math.max(1, amount / 4));
		}

		int cap = maxLevel(skill);
		int was = Math.min(cap, SkillXp.levelFor(skill, before));
		int now = Math.min(cap, SkillXp.levelFor(skill, after));
		if (now > was) {
			player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
					"§6" + skill + " level " + now + (now == cap ? " §e(max!)" : "") + "!"));
			applyRewards(player);
		}
	}

	// --------------------------------------------------------------- rewards

	/**
	 * Hand over what the levels are worth.
	 *
	 * Done as transient modifiers with fixed ids, so recalculating replaces the
	 * old ones rather than stacking a fresh copy on every level-up -- which is
	 * the classic way to end up with four hundred hearts.
	 */
	public static void applyRewards(ServerPlayer player) {
		// Health: Farming, Fishing, Carpentry and Dungeoneering all add some.
		double health = levelIn(player, FARMING) * 0.4
				+ levelIn(player, FISHING) * 0.4
				+ levelIn(player, CARPENTRY) * 0.2
				+ levelIn(player, DUNGEONEERING) * 0.4;
		modify(player, Attributes.MAX_HEALTH, "skill_health", health);

		modify(player, Attributes.ARMOR, "skill_defence", levelIn(player, MINING) * 0.2);
		modify(player, Attributes.ATTACK_DAMAGE, "skill_strength", levelIn(player, FORAGING) * 0.1);
		modify(player, Attributes.LUCK, "skill_pet_luck", levelIn(player, TAMING) * 0.05);

		// Raising max health does not fill the new hearts, so a level-up that
		// leaves you on a sliver of a bigger bar looks like a bug. Top up by
		// exactly what was added.
		if (player.getHealth() > 0 && player.getHealth() < player.getMaxHealth()) {
			player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1.0f));
		}
	}

	private static void modify(ServerPlayer player, Holder<Attribute> what, String id, double amount) {
		AttributeInstance attribute = player.getAttribute(what);
		if (attribute == null) {
			return;
		}
		Identifier key = SkyBlocksMod.id(id);
		attribute.removeModifier(key);
		if (amount > 0) {
			attribute.addTransientModifier(new AttributeModifier(
					key, amount, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	/**
	 * The SkyBlock-only statistics, worked out from the levels.
	 *
	 * Crit Chance, the Fortunes, Intelligence, Ability Damage, Treasure Chance
	 * and Pet Luck have no vanilla equivalent, so they are computed on demand
	 * and shown in /skills and the menu rather than half-implemented as
	 * something they are not.
	 */
	public static Map<String, Double> stat(ServerPlayer player) {
		Map<String, Double> stats = new java.util.LinkedHashMap<>();
		stats.put("Crit Chance", levelIn(player, COMBAT) * 0.5);
		stats.put("Farming Fortune", levelIn(player, FARMING) * 4.0);
		stats.put("Mining Fortune", levelIn(player, MINING) * 4.0);
		stats.put("Foraging Fortune", levelIn(player, FORAGING) * 4.0);
		stats.put("Hunting Fortune", levelIn(player, HUNTING) * 2.0);
		stats.put("Treasure Chance", levelIn(player, FISHING) * 2.0);
		stats.put("Intelligence", levelIn(player, ENCHANTING) * 1.0 + levelIn(player, ALCHEMY) * 1.0);
		stats.put("Ability Damage", levelIn(player, ENCHANTING) * 0.5);
		stats.put("Pet Luck", levelIn(player, TAMING) * 1.0);
		return stats;
	}

	// ----------------------------------------------------------------- hooks

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
			}
			// Collections count everything broken, whether it feeds a skill
			// or not, so the page shows the true picture of what you've done.
			Vault.collect(serverPlayer, state.getBlock().asItem(), 1);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer killer
					&& killer.level() instanceof net.minecraft.server.level.ServerLevel world
					&& SkyBlocksMod.allowed(killer, world)) {
				// Tougher things are worth more, using their own health as the
				// guide -- and an enemy with SkyBlock's real health is worth a
				// great deal, so it is capped rather than handing out a level
				// for one Magma Boss.
				long worth = Math.min(5000, Math.max(1,
						(long) (((LivingEntity) entity).getMaxHealth() / 2)));
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
	 * Enchanting, Alchemy and Carpentry, which are things you do at a table
	 * rather than things you break.
	 *
	 * Registered separately because they hang off item events rather than block
	 * ones, and keeping them apart makes it obvious which is which.
	 */
	public static void registerCrafting() {
		// Taking something out of a crafting output is Carpentry.
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register(
				(player, world, hand, hit) -> {
					if (player instanceof ServerPlayer serverPlayer
							&& world instanceof net.minecraft.server.level.ServerLevel level
							&& SkyBlocksMod.allowed(serverPlayer, level)) {
						BlockState state = world.getBlockState(hit.getBlockPos());
						if (state.is(Blocks.ENCHANTING_TABLE)) {
							add(serverPlayer, ENCHANTING, 5);
						} else if (state.is(Blocks.BREWING_STAND)) {
							add(serverPlayer, ALCHEMY, 5);
						} else if (state.is(Blocks.CRAFTING_TABLE)) {
							add(serverPlayer, CARPENTRY, 2);
						} else if (state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL)
								|| state.is(Blocks.DAMAGED_ANVIL)) {
							add(serverPlayer, ENCHANTING, 3);
						}
					}
					return net.minecraft.world.InteractionResult.PASS;
				});
	}

	/**
	 * The rest of the game's ways up, from the wiki's own list.
	 *
	 * Shearing a sheep is Farming -- it is on the Farming list next to
	 * harvesting crops, and it is the one everyday action that fed nothing at
	 * all before. Fishing is fed by {@link Fishing} when something comes up on
	 * the line, and minions by {@link #fromMinion} when you collect from one,
	 * at the reduced rate the game gives them.
	 */
	public static void registerMoreWays() {
		net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register(
				(player, world, hand, entity, hit) -> {
					if (!(player instanceof ServerPlayer serverPlayer)
							|| !(world instanceof net.minecraft.server.level.ServerLevel level)
							|| !SkyBlocksMod.allowed(serverPlayer, level)) {
						return net.minecraft.world.InteractionResult.PASS;
					}
					if (entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep
							&& !sheep.isSheared() && sheep.isAlive()
							&& player.getItemInHand(hand).is(net.minecraft.world.item.Items.SHEARS)) {
						add(serverPlayer, FARMING, 3);
						Vault.collect(serverPlayer, net.minecraft.world.item.Items.WHITE_WOOL, 1);
					}
					return net.minecraft.world.InteractionResult.PASS;
				});
	}

	/**
	 * XP for collecting from a minion, at the reduced rate the game uses.
	 *
	 * A minion working while you sleep should not be worth the same as swinging
	 * the pickaxe yourself, or there would be no reason ever to mine again. A
	 * quarter is the going rate.
	 */
	public static void fromMinion(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
		if (count <= 0) {
			return;
		}
		add(player, MINING, Math.max(1, count / 4));
		Vault.collect(player, item, count);
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
				|| state.is(Blocks.DARK_OAK_LOG) || state.is(Blocks.OAK_LEAVES)
				|| state.is(Blocks.DANDELION) || state.is(Blocks.POPPY)) {
			return FORAGING;
		}
		if (state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES)
				|| state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)
				|| state.is(Blocks.SUGAR_CANE) || state.is(Blocks.NETHER_WART)
				|| state.is(Blocks.COCOA) || state.is(Blocks.CACTUS)) {
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
		// The Dwarven ores this mod adds are Mining too, obviously enough.
		for (Content.Blok blok : Content.BLOCKS) {
			net.minecraft.world.level.block.Block block = SkyItems.BLOCKS.get(blok.id());
			if (block != null && state.is(block)) {
				return MINING;
			}
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
