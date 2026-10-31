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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = AdventurersPack.ID)
public class CursiumArmorItem extends ArmorItem {
 private static final String FLIGHT = "EndgameFlight";
 public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
  private static final int[] HEALTH = new int[]{13, 15, 16, 11};
  // Helmet, chest, legs, boots. Three times netherite: 9, 24, 18, 9.
  private static final int[] DEFENSE = new int[]{9, 24, 18, 9};

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
   return 45;
  }

  @Override
  public SoundEvent getEquipSound() {
   return SoundEvents.ARMOR_EQUIP_NETHERITE;
  }

  @Override
  public Ingredient getRepairIngredient() {
   return Ingredient.of(AdventurersPack.ENDGAME_INGOT.get());
  }

  @Override
  public String getName() {
   return "adventurerspack:cursium_mage_armor";
  }

  @Override
  public float getToughness() {
   return 9.0F;
  }

  @Override
  public float getKnockbackResistance() {
   return 0.3F;
  }
 };

 private final boolean winged;

 public CursiumArmorItem(Type type, boolean winged) {
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
          ? "adventurerspack:textures/models/armor/cursium_mage_armor_elytra.png"
          : "adventurerspack:textures/models/armor/cursium_mage_armor.png";
 }

 @Override
 public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
  if (winged) {
   lines.add(Component.literal("Winged armor with Elytra flight capabilities.").withStyle(ChatFormatting.DARK_GREEN));
  }
  if (getType() == Type.CHESTPLATE) {
   lines.add(Component.literal("Grants flight while worn.").withStyle(ChatFormatting.AQUA));
  }
  lines.add(Component.literal("Forged from End Game Ingots. 200% stronger than netherite. Indestructible.").withStyle(ChatFormatting.GOLD));
 }

 @SubscribeEvent public static void flight(TickEvent.PlayerTickEvent e) {
  if (e.phase != TickEvent.Phase.END || e.player.level().isClientSide) return;
  Player player = e.player;
  boolean wearing = player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CursiumArmorItem;
  boolean vanillaFlight = player.isCreative() || player.isSpectator();
  var tag = player.getPersistentData();
  if (wearing && !vanillaFlight) {
   if (!player.getAbilities().mayfly || !tag.getBoolean(FLIGHT)) {
    tag.putBoolean(FLIGHT, true);
    player.getAbilities().mayfly = true;
    player.onUpdateAbilities();
   }
  } else if (tag.getBoolean(FLIGHT) && !wearing) {
   tag.remove(FLIGHT);
   if (!vanillaFlight) {
    player.getAbilities().mayfly = false;
    player.getAbilities().flying = false;
    player.onUpdateAbilities();
   }
  }
 }
}
