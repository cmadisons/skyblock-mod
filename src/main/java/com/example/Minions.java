package com.example;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers the minion: the block, the item you hold, and the block entity
 * that does the work.
 *
 * A minion is what makes Sky Blocks a game rather than a dare. You start with
 * no chest and no bridge, so the cobblestone it makes is the only way to build
 * across to the portal. See {@link MinionBlockEntity} for how it behaves.
 */
public final class Minions {
	private Minions() {
	}

	/**
	 * The six tiers, slowest first. The number is ticks between actions, so
	 * wooden acts every three seconds and netherite twice a second.
	 */
	public static final String[] TIERS = {"wooden", "stone", "golden", "iron", "diamond", "netherite"};
	private static final int[] PERIODS = {60, 50, 40, 30, 20, 10};

	/** The placed blocks, in the same order as {@link #TIERS}. */
	public static final Block[] MINIONS = new Block[TIERS.length];

	/** The thinking part, attached to each placed block. */
	public static BlockEntityType<MinionBlockEntity> MINION_ENTITY;

	/** Called once at start-up, before any world exists. */
	public static void register() {
		for (int tier = 0; tier < TIERS.length; tier++) {
			String name = TIERS[tier] + "_cobblestone_minion";
			ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, SkyBlocksMod.id(name));
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, SkyBlocksMod.id(name));

			MINIONS[tier] = Registry.register(BuiltInRegistries.BLOCK, blockKey,
					new MinionBlock(BlockBehaviour.Properties.of()
							.strength(2.0f)
							.sound(SoundType.STONE)
							.setId(blockKey), PERIODS[tier]));

			Registry.register(BuiltInRegistries.ITEM, itemKey,
					new BlockItem(MINIONS[tier], new Item.Properties().setId(itemKey)));
		}

		// One block entity type shared by every tier -- they behave identically
		// apart from their clock, which comes from the block itself.
		MINION_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
				SkyBlocksMod.id("cobblestone_minion"),
				FabricBlockEntityTypeBuilder.create(MinionBlockEntity::new, MINIONS).build());
	}
}
