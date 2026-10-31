package com.waldron.adventurerspack.client;

import java.util.UUID;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipeline;
import mod.azure.azurelib.render.armor.AzArmorRendererPipelineContext;
import mod.azure.azurelib.render.armor.bone.AzArmorBoneContext;
import mod.azure.azurelib.render.armor.bone.AzArmorBoneProvider;
import mod.azure.azurelib.util.client.RenderUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class AzArmorLeggingTorsoLayerPipeline extends AzArmorRendererPipelineContext {
    public AzArmorLeggingTorsoLayerPipeline(AzRendererPipeline<UUID, ItemStack> rendererPipeline) {
        super(rendererPipeline);
    }

    @Override
    public AzArmorBoneContext boneContext() {
        return new AzArmorBoneContext() {
            protected AzBone armorLeggingTorsoBone;

            public AzBone getArmorLeggingTorsoBone(AzBakedModel model) {
                return model.getBone("armorLeggingTorsoLayer").orElse(null);
            }

            @Override
            public void grabRelevantBones(AzBakedModel model, AzArmorBoneProvider boneProvider) {
                super.grabRelevantBones(model, boneProvider);
                this.armorLeggingTorsoBone = this.getArmorLeggingTorsoBone(model);
            }

            @Override
            public void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
                this.setAllVisible(false);
                this.setBoneVisible(this.armorLeggingTorsoBone, false);
                switch (currentSlot) {
                    case HEAD -> this.setBoneVisible(this.head, true);
                    case CHEST -> {
                        this.setBoneVisible(this.body, true);
                        this.setBoneVisible(this.rightArm, true);
                        this.setBoneVisible(this.leftArm, true);
                    }
                    case LEGS -> {
                        this.setBoneVisible(this.armorLeggingTorsoBone, true);
                        this.setBoneVisible(this.rightLeg, true);
                        this.setBoneVisible(this.leftLeg, true);
                    }
                    case FEET -> {
                        this.setBoneVisible(this.rightBoot, true);
                        this.setBoneVisible(this.leftBoot, true);
                    }
                    default -> {}
                }
            }

            @Override
            public void applyBaseTransformations(HumanoidModel<?> baseModel) {
                super.applyBaseTransformations(baseModel);
                if (this.armorLeggingTorsoBone != null) {
                    ModelPart modelPart = baseModel.body;
                    RenderUtils.matchModelPartRot(modelPart, this.armorLeggingTorsoBone);
                    this.armorLeggingTorsoBone.updatePosition(modelPart.x, -modelPart.y, modelPart.z);
                }
            }
        };
    }
}
