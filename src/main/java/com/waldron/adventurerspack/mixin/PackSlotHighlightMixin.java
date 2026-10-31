package com.waldron.adventurerspack.mixin;

import com.waldron.adventurerspack.client.PackScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opt pack slots out of the native overlay without replacing other mods' render calls. */
@Mixin(value=Slot.class,remap=false)
public abstract class PackSlotHighlightMixin {
 @Inject(method={"isHighlightable","m_280329_"},at=@At("HEAD"),cancellable=true,remap=false)
 private void packOwnsHighlight(CallbackInfoReturnable<Boolean> cir){
  if(Minecraft.getInstance().screen instanceof PackScreen screen){
   Slot slot=(Slot)(Object)this;
   var slots=screen.getMenu().slots;
   if(slot.index>=0&&slot.index<slots.size()&&slots.get(slot.index)==slot)cir.setReturnValue(false);
  }
 }
}
