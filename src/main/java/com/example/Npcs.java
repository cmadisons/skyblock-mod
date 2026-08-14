package com.example;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * The people who live in the Hub.
 *
 * Only one so far: Banker Broadjaw, who the wiki puts inside the Bank and who
 * you right-click to see your money. He is an ordinary villager underneath,
 * frozen in place and made invulnerable so he can't wander off, be killed, or
 * turn into a zombie in the night.
 */
public final class Npcs {
	private Npcs() {
	}

	public static final String BANKER = "Banker Broadjaw";

	/** The Auction House's own NPC: sells everything you're carrying. */
	public static final String AUCTIONEER = "Auctioneer";

	/** Bazaar Alley's: opens the buying list. */
	public static final String BAZAAR = "Bazaar Trader";

	/**
	 * Put the Banker in the Bank.
	 *
	 * Called once, while the Hub is being built.
	 */
	public static void spawnBanker(ServerLevel level, BlockPos where) {
		Villager banker = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
		if (banker == null) {
			return;
		}
		banker.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 180.0f, 0.0f);
		banker.setCustomName(Component.literal(BANKER).withStyle(ChatFormatting.GOLD));
		banker.setCustomNameVisible(true);
		banker.setInvulnerable(true);
		banker.setPersistenceRequired();
		banker.setNoAi(true);                  // stands still, doesn't trade or flee
		banker.setSilent(true);
		level.addFreshEntity(banker);
	}

	/**
	 * Put a named villager somewhere and freeze them there.
	 *
	 * Used for the Hub's residents, whose names and positions come from a
	 * community coordinate guide rather than being made up.
	 */
	public static void spawnVillager(ServerLevel level, BlockPos where, String name) {
		Villager npc = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
		if (npc == null) {
			return;
		}
		npc.snapTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5, 180.0f, 0.0f);
		npc.setCustomName(Component.literal(name).withStyle(ChatFormatting.YELLOW));
		npc.setCustomNameVisible(true);
		npc.setInvulnerable(true);
		npc.setPersistenceRequired();
		npc.setNoAi(true);
		npc.setSilent(true);
		level.addFreshEntity(npc);
	}

	/**
	 * Right-clicking the Banker opens your bank.
	 *
	 * He tells you the numbers and how to move money; the actual moving is
	 * done by /bank, which works from anywhere.
	 */
	public static void registerInteraction() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!(player instanceof ServerPlayer serverPlayer)
					|| !(level instanceof ServerLevel serverLevel)
					|| entity.getCustomName() == null) {
				return InteractionResult.PASS;
			}
			if (!SkyBlocksMod.allowed(serverPlayer, serverLevel)) {
				return InteractionResult.PASS;
			}

			String who = entity.getCustomName().getString();

			if (who.equals(AUCTIONEER)) {
				// Selling is the Auction House's whole job, so he does it --
				// right there, rather than telling you a command to go and type.
				Shops.sellEverything(serverPlayer);
				return InteractionResult.SUCCESS;
			}
			if (who.equals(BAZAAR)) {
				// Opens the stock list as a page you can look at.
				Shops.openBazaar(serverPlayer);
				return InteractionResult.SUCCESS;
			}
			if (!who.equals(BANKER)) {
				return InteractionResult.PASS;
			}

			long carried = Economy.coins(serverPlayer);
			long banked = Economy.bank(serverPlayer);
			serverPlayer.sendSystemMessage(Component.literal(
					"§6" + BANKER + "§7: you're carrying §6" + Economy.pretty(carried)
							+ "§7 and have §6" + Economy.pretty(banked) + "§7 with me."));
			serverPlayer.sendSystemMessage(Component.literal(
					"§7Use §f/bank deposit <amount>§7 — dying costs half of what you carry, "
							+ "and none of what I hold."));
			return InteractionResult.SUCCESS;
		});
	}
}
