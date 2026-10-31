package com.waldron.adventurerspack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.waldron.adventurerspack.AdventurersPack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The blade model is registered and rendered cleanly with standard display transforms. */
@Mod.EventBusSubscriber(modid=AdventurersPack.ID, value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class RequiemBladeRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation BLADE = new ResourceLocation(AdventurersPack.ID, "item/requiem_blade");

    private RequiemBladeRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static IClientItemExtensions extension() {
        return new IClientItemExtensions() {
            private RequiemBladeRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new RequiemBladeRenderer();
                return renderer;
            }
        };
    }

    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) { event.register(BLADE); }

    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc=Minecraft.getInstance();
        pose.pushPose();
        try {
            // Compensate the inner renderer's corner offset so the item display transforms center correctly.
            pose.translate(.5,.5,.5);
            mc.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, pose, buffers,
                    light, overlay, mc.getModelManager().getModel(BLADE));
        } finally { pose.popPose(); }
    }
}
