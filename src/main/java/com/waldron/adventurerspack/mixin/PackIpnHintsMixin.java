package com.waldron.adventurerspack.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tell IPN that the pack screen is playerSideOnly, so IPN sorts the player inventory instead of the backpack and never removes sockets/attachments. */
@Pseudo
@Mixin(targets="org.anti_ad.mc.ipnext.integration.HintsManagerNG",remap=false)
public abstract class PackIpnHintsMixin {
 @Inject(method="isPlayerSideOnly",at=@At("HEAD"),cancellable=true,require=0,remap=false)
 private void packPlayerSideOnly(Class<?> clazz,CallbackInfoReturnable<Boolean> cir){
  if(clazz!=null&&(clazz.getName().startsWith("com.waldron.adventurerspack.")||clazz.getName().contains("PackScreen")||clazz.getName().contains("PackMenu"))){
   cir.setReturnValue(true);
  }
 }
}

