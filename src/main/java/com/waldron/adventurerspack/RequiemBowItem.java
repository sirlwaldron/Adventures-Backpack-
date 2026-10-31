package com.waldron.adventurerspack;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import java.util.List;
public class RequiemBowItem extends BowItem {
 public enum Style { SHORTBOW, RECURVE, GREATBOW }
 public final Style style;
 public final int drawTicks;public final float velocity;public final double damage;
 public RequiemBowItem(Style style,int drawTicks,float velocity,double damage){super(new Item.Properties().durability(4096).rarity(Rarity.EPIC).fireResistant());this.style=style;this.drawTicks=drawTicks;this.velocity=velocity;this.damage=damage;}
 public float charge(int elapsed){float t=Math.max(0,elapsed)/(float)drawTicks;return Math.min(1,(t*t+2*t)/3);}
 @Override public int getEnchantmentValue(){return 22;}
 @Override public boolean isValidRepairItem(ItemStack s,ItemStack ingredient){return ingredient.is(Items.NETHERITE_INGOT)||super.isValidRepairItem(s,ingredient);}
 @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity living,int remaining){
  if(!(living instanceof Player p))return;
  ItemStack ammo=p.getProjectile(stack);boolean free=p.getAbilities().instabuild||EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS,stack)>0;
  int elapsed=net.minecraftforge.event.ForgeEventFactory.onArrowLoose(stack,level,p,getUseDuration(stack)-remaining,!ammo.isEmpty()||free);
  if(elapsed<0||ammo.isEmpty()&&!free)return;float power=charge(elapsed);if(power<.1f)return;
  if(ammo.isEmpty())ammo=new ItemStack(Items.ARROW);
  boolean infinite=p.getAbilities().instabuild||(ammo.getItem() instanceof ArrowItem a&&a.isInfinite(ammo,stack,p));
  if(!level.isClientSide){
   AbstractArrow arrow=createShot(level,p,stack,ammo,power,0,infinite,false);
   if(!level.addFreshEntity(arrow))return;
   if(style==Style.SHORTBOW&&power>=1){level.addFreshEntity(createShot(level,p,stack,ammo,power,-9,infinite,true));level.addFreshEntity(createShot(level,p,stack,ammo,power,9,infinite,true));}
   if(!infinite){ammo.shrink(1);if(ammo.isEmpty())p.getInventory().removeItem(ammo);}
   level.playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1/(level.random.nextFloat()*.4f+1.2f)+power*.5f);
   p.awardStat(Stats.ITEM_USED.get(this));
  }
 }
 public AbstractArrow createShot(Level level,Player p,ItemStack stack,ItemStack ammo,float power,float yawOffset,boolean infinite,boolean sideShot){
  ArrowItem arrowItem=ammo.getItem() instanceof ArrowItem a?a:(ArrowItem)Items.ARROW;
  AbstractArrow arrow=customArrow(arrowItem.createArrow(level,ammo,p));
  arrow.shootFromRotation(p,p.getXRot(),p.getYRot()+yawOffset,0,power*velocity,style==Style.RECURVE?.15f:.5f);
  arrow.setCritArrow(power>=1);int ench=EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS,stack);
  arrow.setBaseDamage(damage+(ench>0?ench*.5+.5:0));
  arrow.setKnockback(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS,stack)+(style==Style.GREATBOW&&power>=1?3:0));
  if(style==Style.RECURVE&&power>=1)arrow.setPierceLevel((byte)2);
  if(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS,stack)>0)arrow.setSecondsOnFire(100);
  arrow.pickup=sideShot?AbstractArrow.Pickup.DISALLOWED:infinite?AbstractArrow.Pickup.CREATIVE_ONLY:AbstractArrow.Pickup.ALLOWED;return arrow;
 }
 public String ability(){return switch(style){case SHORTBOW->"Full draw: 3-arrow spread (one arrow consumed)";case RECURVE->"Full draw: accurate piercing through up to 3 enemies";case GREATBOW->"Full draw: heavy impact and powerful knockback";};}
 @Override public void appendHoverText(ItemStack s,Level l,List<Component> out,TooltipFlag f){out.add(Component.literal(ability()));out.add(Component.literal(drawTicks+" tick draw | Uses arrows | Supports bow enchantments"));out.add(Component.literal("Endgame Forms key: choose any of ten forms"));out.add(Component.literal("Unbreakable | All ten forms share enchantments"));}
 @Override public boolean isDamageable(ItemStack stack){return false;}
 @Override public boolean isBarVisible(ItemStack stack){return false;}
}
