package com.waldron.adventurerspack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The equipped kit tool lives outside the player's 36 inventory slots. */
public final class ToolEquipment {
 private ToolEquipment() {}
 public static boolean active(Player player){
  var equipment=player.getCapability(PackEquipment.CAP).orElse(null);
  if(equipment==null)return false;
  if(player.level().isClientSide)return equipment.toolEnabled&&player.getInventory().selected==equipment.toolHotbarSlot;
  var state=player.getPersistentData().getCompound("AdventurersPackToolHand");
  return state.getBoolean("Virtual")&&player.getInventory().selected==state.getInt("HandSlot");
 }
 public static ItemStack stack(Player player){return PackEquipment.get(player).tool.getStackInSlot(0);}
 public static void set(Player player,ItemStack stack){PackEquipment.get(player).tool.setStackInSlot(0,stack);if(stack.isEmpty()&&ToolHand.active(player))player.getPersistentData().getCompound("AdventurersPackToolHand").remove("Tool");}
}
