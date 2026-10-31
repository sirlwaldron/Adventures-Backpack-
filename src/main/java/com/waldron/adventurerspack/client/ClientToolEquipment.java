package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.PackEquipment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
public final class ClientToolEquipment {
 private ClientToolEquipment() {}
 public static void update(boolean active,int slot,ItemStack stack){
  var player=Minecraft.getInstance().player;
  if(player==null)return;
  var equipment=PackEquipment.get(player);
  equipment.toolEnabled=active;equipment.toolHotbarSlot=slot;equipment.tool.setStackInSlot(0,stack);
 }
}
