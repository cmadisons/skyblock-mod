package com.example.mixin;

import com.example.HubEdit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Notices every block that changes anywhere, so the Hub can remember its own.
 *
 * There is no event for "a block was placed". Fabric has one for breaking and
 * none for the other twenty ways a block can change -- placing, buckets, fire,
 * pistons, a command block. Rather than hook each of them and still miss some,
 * this splices into the one method they all end up calling.
 *
 * That is a busy method, so the work done here is a flag test and some
 * arithmetic; see {@link HubEdit#changed}, which is written to say no quickly.
 */
@Mixin(Level.class)
public class HubEditMixin {
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
			at = @At("HEAD"))
	private void skyblocksRecordHubEdit(BlockPos pos, BlockState state, int flags, int limit,
			CallbackInfoReturnable<Boolean> info) {
		Level self = (Level) (Object) this;
		if (self instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
			HubEdit.changed(level, pos, state);
		}
	}
}
