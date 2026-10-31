package com.waldron.adventurerspack.client;

import mod.azure.azurelib.render.armor.AzArmorRendererConfig;
import mod.azure.azurelib.render.layer.AzAutoGlowingLayer;
import net.minecraft.resources.ResourceLocation;

public class CursiumMageElytraArmorRenderer extends PackArmorRenderer {
    private static final ResourceLocation GEO = new ResourceLocation("adventurerspack", "geo/cursium_mage_elytra.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation("adventurerspack", "textures/models/armor/cursium_mage_armor_elytra.png");

    public CursiumMageElytraArmorRenderer() {
        super(AzArmorRendererConfig.builder(GEO, TEX)
                .setAnimatorProvider(CursiumMageElytraAnimator::new)
                .addRenderLayer(new AzAutoGlowingLayer())
                .setPipelineContext(AzArmorLeggingTorsoLayerPipeline::new)
                .setModelRenderer(StandSafeArmorModelRenderer::create)
                .build());
    }
}
