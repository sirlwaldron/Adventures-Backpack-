package com.waldron.adventurerspack;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import com.google.common.collect.*;
import java.util.*;
public final class EndgameLanceItem extends SwordItem {
 public EndgameLanceItem(){super(Endgame.TIER,41,-2.9f,new Item.Properties().fireResistant().rarity(Rarity.EPIC));}
 public Multimap<Attribute,AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){if(slot!=EquipmentSlot.MAINHAND)return super.getDefaultAttributeModifiers(slot);var b=ImmutableMultimap.<Attribute,AttributeModifier>builder();b.putAll(super.getDefaultAttributeModifiers(slot));b.put(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(),new AttributeModifier(UUID.fromString("f63dcf56-248e-4480-a5be-9933f52346da"),"Endgame reach",1.5,AttributeModifier.Operation.ADDITION));return b.build();}
 public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){ItemStack s=p.getItemInHand(hand);if(p.isShiftKeyDown()){if(!level.isClientSide)Endgame.transform(p,hand);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);}p.startUsingItem(hand);return InteractionResultHolder.consume(s);}
 public UseAnim getUseAnimation(ItemStack s){return UseAnim.SPEAR;}
 public int getUseDuration(ItemStack s){return 72000;}
 public void releaseUsing(ItemStack s,Level level,LivingEntity entity,int left){if(level.isClientSide||!(entity instanceof Player p)||getUseDuration(s)-left<10)return;InteractionHand hand=p.getUsedItemHand();ItemStack thrown=s.copyWithCount(1);boolean creative=p.getAbilities().instabuild;
  EndgameProjectile projectile=new EndgameProjectile(level,p,thrown);projectile.creativeCopy=creative;projectile.returnSlot=hand==InteractionHand.OFF_HAND?40:p.getInventory().selected;
  if(!creative&&ToolHand.isLeased(p,s)){var lease=p.getPersistentData().getCompound("AdventurersPackToolHand");if(lease.hasUUID("Pack")){projectile.homePack=lease.getUUID("Pack");projectile.homeSlot=lease.getInt("Home");}if(thrown.hasTag())thrown.getTag().remove("AdventurersPackLease");projectile.setItem(thrown);}
  projectile.shootFromRotation(p,p.getXRot(),p.getYRot(),0,3f,0.5f);
  if(!level.addFreshEntity(projectile))return;
  if(!creative){p.setItemInHand(hand,ItemStack.EMPTY);ToolHand.restore(p);}
  p.getCooldowns().addCooldown(this,20);p.awardStat(Stats.ITEM_USED.get(this));level.playSound(null,p.blockPosition(),SoundEvents.TRIDENT_THROW,SoundSource.PLAYERS,1f,0.8f);
 }
 public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){Endgame.tooltip(s,lines,false);}
 @Override public boolean isDamageable(ItemStack stack){return false;}
 @Override public boolean isBarVisible(ItemStack stack){return false;}
}
