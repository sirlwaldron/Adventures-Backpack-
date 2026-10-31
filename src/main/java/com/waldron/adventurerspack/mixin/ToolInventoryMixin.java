package com.waldron.adventurerspack.mixin;

import com.waldron.adventurerspack.ToolEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Inventory.class,remap=false)
public abstract class ToolInventoryMixin {
 @Inject(method={"getSelected","m_36056_"},at=@At("HEAD"),cancellable=true,remap=false)
 private void equippedTool(CallbackInfoReturnable<ItemStack> cir){
  var player=((Inventory)(Object)this).player;
  if(ToolEquipment.active(player))cir.setReturnValue(ToolEquipment.stack(player));
 }
 @Inject(method={"removeFromSelected","m_182403_"},at=@At("HEAD"),cancellable=true,remap=false)
 private void dropEquippedTool(boolean all,CallbackInfoReturnable<ItemStack> cir){
  var player=((Inventory)(Object)this).player;
  if(ToolEquipment.active(player)){
   // Dropping a borrowed kit tool stows it, leaving the hotbar item untouched.
   if(!player.level().isClientSide)com.waldron.adventurerspack.ToolHand.restore(player);
   cir.setReturnValue(ItemStack.EMPTY);
  }
 }
}
