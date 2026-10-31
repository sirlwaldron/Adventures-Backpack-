package com.waldron.adventurerspack;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

final class CuriosIntegration {
 static void init() { MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, CuriosIntegration::attach); }
 private static void attach(AttachCapabilitiesEvent<ItemStack> event) {
  ItemStack stack = event.getObject();
  if (!(stack.getItem() instanceof PackItem)) return;
  LazyOptional<ICurio> curio = LazyOptional.of(() -> new ICurio() {
   public ItemStack getStack() { return stack; }
   public boolean canUnequip(SlotContext context) {
    return !(context.entity() instanceof Player player && player.containerMenu instanceof PackMenu menu
        && menu.boundSlot == PackItem.CURIOS && menu.data.owner == stack);
   }
  });
  event.addCapability(new ResourceLocation(AdventurersPack.ID, "curio"), new ICapabilityProvider() {
   public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
    return CuriosCapability.ITEM.orEmpty(capability, curio);
   }
  });
  event.addListener(curio::invalidate);
 }
 static ItemStack stack(Player player, boolean visual) {
  return CuriosApi.getCuriosInventory(player).map(inventory -> {
   for (String id : inventory.getCurios().keySet().stream().sorted().toList()) {
    var handler = inventory.getCurios().get(id);
    for (int i = 0; i < handler.getSlots(); i++) {
     ItemStack equipped = handler.getStacks().getStackInSlot(i);
     if (visual) {
      if (i < handler.getRenders().size() && !handler.getRenders().get(i)) continue;
      ItemStack cosmetic = i < handler.getCosmeticStacks().getSlots() ? handler.getCosmeticStacks().getStackInSlot(i) : ItemStack.EMPTY;
      if (!cosmetic.isEmpty()) {
       if (cosmetic.getItem() instanceof PackItem) return cosmetic;
       continue;
      }
     }
     if (equipped.getItem() instanceof PackItem) return equipped;
    }
   }
   return ItemStack.EMPTY;
  }).orElse(ItemStack.EMPTY);
 }
}
