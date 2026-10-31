package com.waldron.adventurerspack.mixin;

import com.waldron.adventurerspack.client.PackScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep vanilla stack rendering and drag previews, enlarging only the player's 36 slots. */
@Mixin(value=AbstractContainerScreen.class,remap=false)
public abstract class PackInventoryScaleMixin {
 @Inject(method={"renderSlot","m_280092_"},at=@At("HEAD"),cancellable=true,remap=false)
 private void beginSlot(GuiGraphics g,Slot slot,CallbackInfo ci){
  if((Object)this instanceof PackScreen screen&&screen.usesModCount(slot)){screen.renderCustomCountSlot(g,slot);ci.cancel();return;}
  if((Object)this instanceof PackScreen&&PackScreen.isPlayerInventory(slot)){
   g.pose().pushPose();
   g.pose().translate(slot.x,slot.y,0);
   g.pose().scale(1.375f,1.375f,1);
   g.pose().translate(-slot.x,-slot.y,0);
  }
 }
 @Inject(method={"renderSlot","m_280092_"},at=@At("RETURN"),remap=false)
 private void endSlot(GuiGraphics g,Slot slot,CallbackInfo ci){
  if((Object)this instanceof PackScreen&&PackScreen.isPlayerInventory(slot))g.pose().popPose();
 }
 @Inject(method={"isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z","m_97774_(Lnet/minecraft/world/inventory/Slot;DD)Z"},at=@At("HEAD"),cancellable=true,remap=false)
 private void playerHitbox(Slot slot,double x,double y,CallbackInfoReturnable<Boolean> cir){
  if((Object)this instanceof PackScreen screen&&PackScreen.isPlayerInventory(slot)){
   double sx=screen.getGuiLeft()+slot.x,sy=screen.getGuiTop()+slot.y;
   cir.setReturnValue(x>=sx-1&&x<sx+23&&y>=sy-1&&y<sy+23);
  }
 }
}
