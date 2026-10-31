package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=AdventurersPack.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public class ClientSetup {
 public static final KeyMapping FORMS=new KeyMapping("key.adventurerspack.forms",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_Z,"key.categories.adventurerspack");
 public static final KeyMapping OPEN=new KeyMapping("key.adventurerspack.open",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_B,"key.categories.adventurerspack");
 public static final KeyMapping TOOL=new KeyMapping("key.adventurerspack.tool",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.adventurerspack");
 public static final KeyMapping CYCLE=new KeyMapping("key.adventurerspack.cycle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_N,"key.categories.adventurerspack");
 public static final KeyMapping WHEEL=new KeyMapping("key.adventurerspack.wheel",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_G,"key.categories.adventurerspack");
 public static final KeyMapping PRESET=new KeyMapping("key.adventurerspack.preset",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_R,"key.categories.adventurerspack");
 public static final KeyMapping OFFHAND=new KeyMapping("key.adventurerspack.offhand",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_Y,"key.categories.adventurerspack");
 public static final KeyMapping VIAL=new KeyMapping("key.adventurerspack.vial",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_U,"key.categories.adventurerspack");
 @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{for(int f=2;f<5;f++){var bow=(RequiemBowItem)Requiem.item(f);net.minecraft.client.renderer.item.ItemProperties.register(bow,new net.minecraft.resources.ResourceLocation("pull"),(stack,level,entity,seed)->entity!=null&&entity.getUseItem()==stack?bow.charge(stack.getUseDuration()-entity.getUseItemRemainingTicks()):0);net.minecraft.client.renderer.item.ItemProperties.register(bow,new net.minecraft.resources.ResourceLocation("pulling"),(stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1:0);}MenuScreens.register(AdventurersPack.MENU.get(),PackScreen::new);net.minecraft.client.renderer.item.ItemProperties.register(AdventurersPack.PACK.get(),new net.minecraft.resources.ResourceLocation(AdventurersPack.ID,"tier"),(stack,level,entity,seed)->PackData.tierOf(stack));AzureLibCompat.initClient();});}
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(AdventurersPack.ENDGAME_PROJECTILE.get(),EndgameRenderer::new);e.registerBlockEntityRenderer(AdventurersPack.DOCK_ENTITY.get(),ctx->PackWornRenderer::renderDock);}
 @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(FORMS);e.register(OPEN);e.register(TOOL);e.register(CYCLE);e.register(WHEEL);e.register(PRESET);e.register(OFFHAND);e.register(VIAL);}
 @SubscribeEvent public static void overlay(RegisterGuiOverlaysEvent e){e.registerAboveAll("tool_hand",(gui,g,partial,w,h)->ToolWheel.hand(g,w,h));}
}