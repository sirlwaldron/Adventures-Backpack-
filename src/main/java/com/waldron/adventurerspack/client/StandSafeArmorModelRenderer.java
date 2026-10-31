package com.waldron.adventurerspack.client;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.cache.AzBakedModelCache;
import mod.azure.azurelib.render.AzLayerRenderer;
import mod.azure.azurelib.render.AzModelRenderer;
import mod.azure.azurelib.render.AzRendererPipeline;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.armor.AzArmorModelRenderer;
import mod.azure.azurelib.render.armor.AzArmorRendererPipeline;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;

/** Draws armor-stand pieces from the stand's limbs and the model's resting decoration. */
public class StandSafeArmorModelRenderer extends AzArmorModelRenderer {
    private static final Set<String> BODY = Set.of(
            "armorHead", "armorBody", "armorRightArm", "armorLeftArm", "armorRightLeg", "armorLeftLeg",
            "armorRightBoot", "armorLeftBoot", "armorLeggingTorsoLayer", "waist",
            "bipedHead", "bipedBody", "bipedRightArm", "bipedLeftArm", "bipedRightLeg", "bipedLeftLeg");

    public StandSafeArmorModelRenderer(AzArmorRendererPipeline pipeline, AzLayerRenderer<UUID, ItemStack> layer) {
        super(pipeline, layer);
    }

    public static AzModelRenderer<UUID, ItemStack> create(AzRendererPipeline<UUID, ItemStack> pipeline, AzLayerRenderer<UUID, ItemStack> layer) {
        return new StandSafeArmorModelRenderer((AzArmorRendererPipeline) pipeline, layer);
    }

    @Override
    public void render(AzRendererPipelineContext context, boolean isReRender) {
        if (context.currentEntity() instanceof ArmorStand) restDecorations(context);
        super.render(context, isReRender);
    }

    private void restDecorations(AzRendererPipelineContext context) {
        AzBakedModel posed = context.bakedModel();
        if (posed == null) return;
        ResourceLocation geo = armorRendererPipeline.config().modelLocation(context.currentEntity(), (ItemStack) context.animatable());
        AzBakedModel rest = AzBakedModelCache.getInstance().getNullable(geo);
        if (rest == null) return;
        for (AzBone bone : posed.getBonesByName().values()) {
            if (BODY.contains(bone.getName())) continue;
            AzBone source = rest.getBoneOrNull(bone.getName());
            if (source == null || source.getInitialAzSnapshot() == null) continue;
            var snap = source.getInitialAzSnapshot();
            bone.updateRotation(snap.getRotX(), snap.getRotY(), snap.getRotZ());
        }
    }
}

