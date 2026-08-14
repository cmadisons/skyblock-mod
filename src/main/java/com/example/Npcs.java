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
	 * Right-clicking the Banker opens your bank.
	 *
	 * He tells you the numbers and how to move money; the actual moving is
	 * done by /bank, which works from anywhere.
	 */
	public static void registerInteraction() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (!(player instanceof ServerPlayer serverPlayer)
					|| !(level instanceof ServerLevel serverLevel)
					|| entity.getCustomName() == null
					|| !entity.getCustomName().getString().equals(BANKER)) {
				return InteractionResult.PASS;
			}
			if (!SkyBlocksMod.allowed(serverPlayer, serverLevel)) {
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
