package com.example.mixin;

import com.example.Fishing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Catches the moment a fishing rod is reeled in.
 *
 * This is what a mixin is for. Mojang provides no event for "a player just
 * caught something", so the only way to know is to splice into the method that
 * does it -- FishingHook.retrieve, which runs once per reel and returns how
 * much to wear the rod down by.
 *
 * Injected at RETURN rather than HEAD on purpose: by then vanilla has already
 * decided whether anything was caught and handed over its fish, so anything
 * added here lands on top of a real catch instead of every idle click. See
 * {@link Fishing} for what it rolls.
 */
@Mixin(FishingHook.class)
public class SkyBlocksServerMixin {
	@Inject(method = "retrieve", at = @At("RETURN"))
	private void skyblocksCatch(ItemStack rod, CallbackInfoReturnable<Integer> info) {
		// A return of zero means nothing was reeled in -- no catch, no roll.
		if (info.getReturnValue() == null || info.getReturnValue() <= 0) {
			return;
		}
		FishingHook hook = (FishingHook) (Object) this;
		if (hook.getPlayerOwner() instanceof ServerPlayer player) {
			Fishing.reeled(player);
		}
	}
}
