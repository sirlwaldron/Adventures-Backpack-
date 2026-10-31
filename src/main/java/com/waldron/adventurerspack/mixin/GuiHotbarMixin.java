package com.waldron.adventurerspack.mixin;

import com.waldron.adventurerspack.ClientMirror;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Moves the hotbar selection frame off the numbered slot while the tool kit slot is selected. */
@Mixin(value=Gui.class,remap=false)
public abstract class GuiHotbarMixin {
 @Redirect(method={"renderHotbar","m_280518_"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),require=0,remap=false)
 private void hideOfficialSelector(GuiGraphics graphics,ResourceLocation texture,int x,int y,int u,int v,int w,int h){paint(graphics,texture,x,y,u,v,w,h);}
 @Redirect(method={"renderHotbar","m_280518_"},at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;m_280218_(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),require=0,remap=false)
 private void hideSrgSelector(GuiGraphics graphics,ResourceLocation texture,int x,int y,int u,int v,int w,int h){paint(graphics,texture,x,y,u,v,w,h);}
 private static void paint(GuiGraphics graphics,ResourceLocation texture,int x,int y,int u,int v,int w,int h){
  // The selection frame is the 24x22 widget at UV (0, 22). Leave the hotbar and offhand blits alone.
  if(u==0&&v==22&&w==24&&h==22&&ClientMirror.toolCursor(Minecraft.getInstance().player))return;
  graphics.blit(texture,x,y,u,v,w,h);
 }
}
