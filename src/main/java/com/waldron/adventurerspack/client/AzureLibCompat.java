package com.waldron.adventurerspack.client;

import com.waldron.adventurerspack.AdventurersPack;
import mod.azure.azurelib.animation.cache.AzIdentityRegistry;
import mod.azure.azurelib.render.armor.AzArmorRendererRegistry;
import net.minecraftforge.fml.ModList;

public class AzureLibCompat {
    public static void initClient() {
        if (!ModList.get().isLoaded("azurelib")) return;

        mod.azure.azurelib.model.factory.registry.AzBakedModelFactoryRegistry.register(
                "adventurerspack", new ArmorShapeFactory());

        AzArmorRendererRegistry.register(
            IgnisWizardArmorRenderer::new,
            AdventurersPack.IGNIS_HELMET.get(),
            AdventurersPack.IGNIS_CHESTPLATE.get(),
            AdventurersPack.IGNIS_LEGGINGS.get(),
            AdventurersPack.IGNIS_BOOTS.get()
        );
        AzArmorRendererRegistry.register(
            IgnisWizardElytraArmorRenderer::new,
            AdventurersPack.IGNIS_CHESTPLATE_ELYTRA.get()
        );
        AzArmorRendererRegistry.register(
            CursiumMageArmorRenderer::new,
            AdventurersPack.CURSIUM_CIRCLET.get(),
            AdventurersPack.CURSIUM_CHESTPLATE.get(),
            AdventurersPack.CURSIUM_SKIRT.get(),
            AdventurersPack.CURSIUM_BOOTS.get()
        );
        AzArmorRendererRegistry.register(
            CursiumMageElytraArmorRenderer::new,
            AdventurersPack.CURSIUM_CHESTPLATE_ELYTRA.get()
        );
    }

    public static void initCommon() {
        if (!ModList.get().isLoaded("azurelib")) return;

        AzIdentityRegistry.register(
            AdventurersPack.IGNIS_CHESTPLATE_ELYTRA.get()
        );
        AzIdentityRegistry.register(
            AdventurersPack.CURSIUM_CHESTPLATE_ELYTRA.get()
        );
    }
}
