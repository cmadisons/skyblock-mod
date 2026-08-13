package com.example;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The block you place to get a minion.
 *
 * All the actual work happens in {@link MinionBlockEntity} -- this class only
 * exists to give the block somewhere to keep it, to run its clock, and to open
 * its inventory when you right-click.
 *
 * There is one of these per axe tier. A minion is crafted from eight
 * cobblestone around an axe, and the axe decides how fast it works: wooden is
 * slowest, netherite is six times quicker.
 */
public class MinionBlock extends Block implements EntityBlock {
	/** Ticks between actions. Lower is faster. Set by which axe built it. */
	private final int period;

	public MinionBlock(Properties properties, int period) {
		super(properties);
		this.period = period;
	}

	/** How often this minion acts, in ticks. */
	public int period() {
		return period;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MinionBlockEntity(pos, state);
	}

	/**
	 * Hand the game the method to call each tick.
	 *
	 * Only on the server: the client has no business deciding when cobblestone
	 * appears, and running it in both places would produce twice as much.
	 */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
			Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != Minions.MINION_ENTITY) {
			return null;
		}
		return (world, pos, blockState, be) -> MinionBlockEntity.tick(
				world, pos, blockState, (MinionBlockEntity) be);
	}

	/**
	 * Right-click to see what it has collected.
	 *
	 * It borrows the ordinary chest screen rather than having one of its own,
	 * which is why the inventory is 27 slots -- exactly a chest's worth.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (level.getBlockEntity(pos) instanceof MinionBlockEntity minion) {
			player.openMenu(new SimpleMenuProvider(
					(id, inventory, who) -> ChestMenu.threeRows(id, inventory, minion),
					MinionBlockEntity.title()));
		}
		return InteractionResult.SUCCESS;
	}
}
