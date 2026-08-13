package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A working cobblestone minion.
 *
 * It behaves the way one does on Hypixel: it lays cobblestone into the eight
 * spaces around itself and breaks them back up again, keeping what it gets. It
 * never conjures items out of nothing, which is why you can stand and watch it
 * work.
 *
 * How fast depends on the axe it was crafted around -- see {@link MinionBlock}.
 *
 * What it collects goes into its own inventory. Only once that is completely
 * full does it start feeding a chest placed against it, which is what a chest
 * is for: it's the overflow, not the destination.
 *
 * When the inventory is full AND there is no chest, it stops. A stopped minion
 * is doing exactly what Hypixel's does when you have ignored it too long.
 */
public class MinionBlockEntity extends BlockEntity implements Container {
	/** Matches a chest, so the whole lot fits the chest screen it opens in. */
	public static final int SLOTS = 27;

	/**
	 * The eight blocks around the minion, at its own height -- the full ring,
	 * including the diagonals. That is its whole working area: it fills all
	 * eight with cobblestone and then breaks them one at a time.
	 */
	private static final int[][] RING = {
			{-1, -1}, {0, -1}, {1, -1},
			{-1, 0},           {1, 0},
			{-1, 1},  {0, 1},  {1, 1},
	};

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

	public MinionBlockEntity(BlockPos pos, BlockState state) {
		super(Minions.MINION_ENTITY, pos, state);
	}

	/** Shown at the top of the screen when you open it. */
	public static Component title() {
		return Component.literal("Cobblestone Minion");
	}

	/**
	 * One step of the place-then-break cycle, called by the game every tick.
	 *
	 * Breaking is checked before placing, so a block that is already down gets
	 * picked up before another goes anywhere. That is what keeps it to one
	 * block at a time instead of slowly walling itself in.
	 */
	public static void tick(Level level, BlockPos pos, BlockState state, MinionBlockEntity minion) {
		// How often it acts comes from the axe it was built around: a netherite
		// minion works six times as fast as a wooden one.
		int period = state.getBlock() instanceof MinionBlock block ? block.period() : 40;
		if (level.isClientSide() || level.getGameTime() % period != 0) {
			return;
		}

		// Break first, so the ring gets cleared rather than only ever filled.
		for (int[] step : RING) {
			BlockPos target = pos.offset(step[0], 0, step[1]);
			if (level.getBlockState(target).is(Blocks.COBBLESTONE)) {
				if (minion.store(new ItemStack(Items.COBBLESTONE))) {
					level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
				}
				return;                       // full and no chest: leave it be
			}
		}

		// Ring is clear, so lay a fresh block in the first empty spot.
		for (int[] step : RING) {
			BlockPos target = pos.offset(step[0], 0, step[1]);
			if (level.getBlockState(target).isAir()) {
				level.setBlockAndUpdate(target, Blocks.COBBLESTONE.defaultBlockState());
				return;
			}
		}
	}

	/**
	 * Keep an item: own inventory first, then any chest touching the minion.
	 *
	 * @return false if there was nowhere at all to put it.
	 */
	private boolean store(ItemStack stack) {
		if (insert(this, stack)) {
			return true;
		}
		for (Direction side : Direction.values()) {
			BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(side));
			if (neighbour instanceof Container chest && insert(chest, stack)) {
				return true;
			}
		}
		return false;
	}

	/** Drop an item into the first slot that will take it. */
	private static boolean insert(Container box, ItemStack stack) {
		for (int slot = 0; slot < box.getContainerSize(); slot++) {
			ItemStack there = box.getItem(slot);
			if (there.isEmpty()) {
				box.setItem(slot, stack.copy());
				box.setChanged();
				return true;
			}
			if (ItemStack.isSameItemSameComponents(there, stack)
					&& there.getCount() < there.getMaxStackSize()) {
				there.grow(stack.getCount());
				box.setChanged();
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		ContainerHelper.saveAllItems(out, items);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		items.clear();
		ContainerHelper.loadAllItems(in, items);
	}

	// ------------------------------------------------------------- container

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, count);
		if (!taken.isEmpty()) {
			setChanged();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		items.clear();
	}
}
