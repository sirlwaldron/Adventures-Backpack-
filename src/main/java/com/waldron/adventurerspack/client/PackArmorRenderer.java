package com.waldron.adventurerspack.client;

import mod.azure.azurelib.animation.cache.AzIdentityRegistry;
import mod.azure.azurelib.animation.impl.AzItemAnimator;
import mod.azure.azurelib.render.armor.AzArmorRenderer;
import mod.azure.azurelib.render.armor.AzArmorRendererConfig;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Armor renderers that keep an armor stand on its own pose. */
public class PackArmorRenderer extends AzArmorRenderer {
    protected PackArmorRenderer(AzArmorRendererConfig config) {
        super(config);
    }

    @Override
    public void prepForRender(Entity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> model) {
        if (entity instanceof ArmorStand && AzIdentityRegistry.hasIdentity(stack.getItem())) separate(stack, entity, slot);
        super.prepForRender(entity, stack, slot, model);
    }

    @Override
    public AzItemAnimator animator() {
        if (rendererPipeline().context().currentEntity() instanceof ArmorStand) return null;
        return super.animator();
    }

    /** A copied stack keeps the wearer's animation id. Give the stand its own. */
    private static void separate(ItemStack stack, Entity stand, EquipmentSlot slot) {
        UUID id = UUID.nameUUIDFromBytes(("adventurerspack-stand:" + stand.getId() + ":" + slot.getName()).getBytes(StandardCharsets.UTF_8));
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID("az_id") || !id.equals(tag.getUUID("az_id"))) tag.putUUID("az_id", id);
    }
}
