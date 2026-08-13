package com.example.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A mixin is how a mod edits Minecraft's own code, by splicing a method into a
 * vanilla class at load time. It is the tool of last resort, for behaviour
 * Mojang left no hook for.
 *
 * Sky Blocks doesn't need one yet: the world type is data, and the island is
 * built from an ordinary Fabric join event. Kept as a working example and as
 * the landing spot for anything that does need to change vanilla behaviour.
 */
@Mixin(MinecraftServer.class)
public class SkyBlocksServerMixin {
	/** Runs just before the world loads. Empty on purpose. */
	@Inject(at = @At("HEAD"), method = "loadLevel")
	private void init(CallbackInfo info) {
	}
}
