package com.waldron.adventurerspack.mixin;

import com.waldron.adventurerspack.ToolEquipment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Player.class,remap=false)
public abstract class ToolPlayerMixin {
 @Inject(method={"setItemSlot","m_8061_"},at=@At("HEAD"),cancellable=true,remap=false)
 private void setEquippedTool(EquipmentSlot slot,ItemStack stack,CallbackInfo ci){
  var player=(Player)(Object)this;
  if(slot==EquipmentSlot.MAINHAND&&ToolEquipment.active(player)){
   ToolEquipment.set(player,stack);ci.cancel();
  }
 }
}
