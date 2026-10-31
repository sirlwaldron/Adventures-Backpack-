package com.waldron.adventurerspack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.waldron.adventurerspack.AdventurersPack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class StaffAnimation implements IClientItemExtensions {
    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partial, float equip, float swing) {
        if (stack.is(AdventurersPack.REQUIEM_SPEAR.get()) && swing > 0.0F && !player.isUsingItem()) {
            float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            float t = Math.max(0.0F, Math.min(1.0F, swing));
            float ease = t * t * (3.0F - 2.0F * t);
            float sweep = (float) Math.sin(Math.PI * t);
            pose.translate(side * 0.56D, -0.52D - equip * 0.6D, -0.72D);
            pose.translate(-side * 0.27D * sweep, 0.1D * sweep, -0.18D * sweep);
            pose.mulPose(Axis.YP.rotationDegrees(side * 52.0F * sweep));
            pose.mulPose(Axis.XP.rotationDegrees(-18.0F * sweep));
            pose.mulPose(Axis.ZP.rotationDegrees(side * 360.0F * ease));
            return true;
        }
        return false;
    }
}
