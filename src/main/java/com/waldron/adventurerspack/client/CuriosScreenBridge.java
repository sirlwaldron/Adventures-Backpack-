package com.waldron.adventurerspack.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.common.network.NetworkHandler;
import top.theillusivec4.curios.common.network.client.CPacketOpenCurios;

/** Loaded only when Curios is installed; uses its normal inventory-opening packet. */
final class CuriosScreenBridge {
 private CuriosScreenBridge() {}
 static void open() {
  var mc=Minecraft.getInstance();
  if(mc.player==null||!(mc.screen instanceof PackScreen))return;
  var menu=mc.player.containerMenu;
  ItemStack carried=menu.getCarried();
  menu.setCarried(ItemStack.EMPTY);
  NetworkHandler.INSTANCE.sendToServer(new CPacketOpenCurios(carried));
 }
}
