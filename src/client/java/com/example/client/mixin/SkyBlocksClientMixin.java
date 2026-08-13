package com.example.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The client-side counterpart to {@link com.example.mixin.SkyBlocksServerMixin}
 * -- same idea, injected into the game client rather than the server.
 *
 * Empty for the same reason, and the obvious future use is drawing a coin
 * counter or skill bar onto the screen.
 */
@Mixin(Minecraft.class)
public class SkyBlocksClientMixin {
	/** Runs as the game boots. Empty on purpose. */
	@Inject(at = @At("HEAD"), method = "run")
	private void init(CallbackInfo info) {
	}
}
