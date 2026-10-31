package com.waldron.adventurerspack.client;

import mod.azure.azurelib.render.armor.AzArmorRendererConfig;
import mod.azure.azurelib.render.layer.AzAutoGlowingLayer;
import net.minecraft.resources.ResourceLocation;

public class IgnisWizardElytraArmorRenderer extends PackArmorRenderer {
    private static final ResourceLocation GEO = new ResourceLocation("adventurerspack", "geo/ignis_armor_winged.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation("adventurerspack", "textures/models/armor/ignis_armor_winged.png");

    public IgnisWizardElytraArmorRenderer() {
        super(AzArmorRendererConfig.builder(GEO, TEX)
                .setAnimatorProvider(IgnisWizardElytraAnimator::new)
                .addRenderLayer(new AzAutoGlowingLayer())
                .setPipelineContext(AzArmorLeggingTorsoLayerPipeline::new)
                .setModelRenderer(StandSafeArmorModelRenderer::create)
                .build());
    }
}
