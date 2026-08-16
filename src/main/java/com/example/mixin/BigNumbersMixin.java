package com.example.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a mob actually have SkyBlock's health and damage.
 *
 * Minecraft caps Max Health at 1,024 and Attack Damage at 2,048, which is
 * plenty for a game where the player has twenty. SkyBlock is not that game: a
 * Crypt Ghoul has 2,000 health, a Zealot has 13,000, and the Magma Boss has two
 * hundred million. Set those through the ordinary attribute and every one of
 * them comes back as 1,024 -- so every enemy past the Spider's Den would be
 * exactly as tough as every other, which is worse than being wrong.
 *
 * So the ceiling is lifted, and only on those two. Everything else keeps its
 * limits, because nothing else needs raising and a cap is usually there for a
 * reason.
 *
 * The floor is left alone. Health below zero is not a large enemy, it is a
 * crash.
 */
@Mixin(RangedAttribute.class)
public class BigNumbersMixin {
	@Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
	private void skyblocksLiftTheCeiling(double value, CallbackInfoReturnable<Double> info) {
		RangedAttribute self = (RangedAttribute) (Object) this;
		String what = self.getDescriptionId();
		if (!what.endsWith("max_health") && !what.endsWith("attack_damage")) {
			return;
		}
		if (Double.isNaN(value)) {
			return;
		}
		info.setReturnValue(Math.max(value, self.getMinValue()));
	}
}
