package com.waldron.adventurerspack.mixin;

import com.waldron.adventurerspack.client.PackScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** IPN's fixed-size green focus overlay does not fit the pack's scaled slots. */
@Pseudo
@Mixin(targets="org.anti_ad.mc.ipnext.event.SlotHighlightHandler",remap=false)
public abstract class PackIpnHighlightMixin {
 @Inject(method="getEnabled",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private void skipPackForeground(CallbackInfoReturnable<Boolean> cir){
  if(Minecraft.getInstance().screen instanceof PackScreen)cir.setReturnValue(false);
 }
 @Inject(method="onBackgroundRender",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private void skipPackBackground(CallbackInfo ci){
  if(Minecraft.getInstance().screen instanceof PackScreen)ci.cancel();
 }
}
