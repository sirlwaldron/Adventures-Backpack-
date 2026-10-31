package com.waldron.adventurerspack.client;

import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzItemAnimator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CursiumMageElytraAnimator extends AzItemAnimator {
    private static final ResourceLocation ANIMATIONS = new ResourceLocation("adventurerspack", "animations/item/cursium_mage.animation.json");

    @Override
    public void registerControllers(AzAnimationControllerContainer animationControllerContainer) {
        animationControllerContainer.add(AzAnimationController.builder(this, "base_controller").build());
    }

    @NotNull
    @Override
    public ResourceLocation getAnimationLocation(ItemStack animatable) {
        return ANIMATIONS;
    }
}
