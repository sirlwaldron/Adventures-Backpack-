package com.waldron.adventurerspack;
import net.minecraft.nbt.CompoundTag;

/** Latest pack display snapshot. Written by the network thread's main-thread task and read by the client screen. */
public final class ClientMirror {
 public static boolean vanillaReady;public static net.minecraft.world.item.ItemStack equipment;
 public static boolean toolActive,toolManual; public static int toolSource=-1,toolSlot=-1; public static net.minecraft.world.item.ItemStack originalHand=net.minecraft.world.item.ItemStack.EMPTY;
 /** The hotbar highlight belongs on the side tool slot while a manual draw is out. */
 public static boolean toolCursor(net.minecraft.world.entity.player.Player player){return player!=null&&toolManual&&toolActive&&player.getInventory().selected==toolSlot;}
 public static boolean endgameCrafted=false;
 private ClientMirror(){}
 public static int containerId=Integer.MIN_VALUE;
 public static CompoundTag tag;
}
