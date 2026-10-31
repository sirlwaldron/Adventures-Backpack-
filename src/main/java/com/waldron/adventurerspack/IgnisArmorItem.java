package com.waldron.adventurerspack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.List;

public class IgnisArmorItem extends ArmorItem {
 public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
  private static final int[] HEALTH = new int[]{13, 15, 16, 11};
  // Helmet, chest, legs, boots. Netherite is 3, 8, 6, 3; each piece is 50% higher.
  private static final int[] DEFENSE = new int[]{5, 12, 9, 5};

  @Override
  public int getDurabilityForType(Type type) {
   return HEALTH[type.ordinal()] * 45;
  }

  @Override
  public int getDefenseForType(Type type) {
   return DEFENSE[type.ordinal()];
  }

  @Override
  public int getEnchantmentValue() {
   return 22;
  }

  @Override
  public SoundEvent getEquipSound() {
   return SoundEvents.ARMOR_EQUIP_NETHERITE;
  }

  @Override
  public Ingredient getRepairIngredient() {
   return Ingredient.of(AdventurersPack.CRIMSON_INGOT.get());
  }

  @Override
  public String getName() {
   return "adventurerspack:ignis_armor";
  }

  @Override
  public float getToughness() {
   return 4.5F;
  }

  @Override
  public float getKnockbackResistance() {
   return 0.15F;
  }
 };

 private final boolean winged;

 public IgnisArmorItem(Type type, boolean winged) {
  super(MATERIAL, type, new Item.Properties().rarity(Rarity.EPIC).fireResistant());
  this.winged = winged;
 }

 @Override
 public boolean isDamageable(ItemStack stack) {
  return false;
 }

 @Override
 public boolean isBarVisible(ItemStack stack) {
  return false;
 }

 @Override
 public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
  return winged;
 }

 @Override
 public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
  if (!winged) return false;
  if (!entity.level().isClientSide) {
   int nextFlightTick = flightTicks + 1;
   if (nextFlightTick % 10 == 0) {
    entity.gameEvent(GameEvent.ELYTRA_GLIDE);
   }
  }
  return true;
 }

 @Override
 public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
  super.inventoryTick(stack, level, entity, slotId, isSelected);
  if (winged && !level.isClientSide && entity instanceof Player player) {
   if (net.minecraftforge.fml.ModList.get().isLoaded("azurelib")) {
    AzureLibDispatcher.tickWingedArmor(player, stack);
   }
  }
 }

 @Override
 public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
  return winged
          ? "adventurerspack:textures/models/armor/ignis_armor_winged.png"
          : "adventurerspack:textures/models/armor/ignis_armor.png";
 }

 @Override
 public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
  if (winged) {
   lines.add(Component.literal("Winged armor with Elytra flight capabilities.").withStyle(ChatFormatting.DARK_GREEN));
  }
  lines.add(Component.literal("Forged from Crimson Ingots. 50% stronger than netherite. Indestructible.").withStyle(ChatFormatting.DARK_RED));
 }
}
