package com.waldron.adventurerspack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
/** Keeps optional Curios classes unloaded when the mod is absent. */
public final class CuriosPacks {
 private CuriosPacks() {}
 public static void init() { if (ModList.get().isLoaded("curios")) CuriosIntegration.init(); }
 public static ItemStack stack(Player player) { return player != null && ModList.get().isLoaded("curios") ? CuriosIntegration.stack(player, false) : ItemStack.EMPTY; }
 public static ItemStack visualStack(Player player) { return player != null && ModList.get().isLoaded("curios") ? CuriosIntegration.stack(player, true) : ItemStack.EMPTY; }
}
