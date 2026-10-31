package com.waldron.adventurerspack;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.network.chat.Component;
import java.util.List;
public final class EndgamePickaxeItem extends PickaxeItem {
 public EndgamePickaxeItem(){super(Endgame.TIER,23,-2.8f,new Item.Properties().fireResistant().rarity(Rarity.EPIC));}
 public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){if(!p.isShiftKeyDown())return InteractionResultHolder.pass(p.getItemInHand(hand));if(!level.isClientSide)Endgame.transform(p,hand);return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);}
 @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx){if(ctx.getPlayer()!=null&&ctx.getPlayer().isShiftKeyDown()){if(!ctx.getLevel().isClientSide)Endgame.transform(ctx.getPlayer(),ctx.getHand());return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);}return super.useOn(ctx);}
 public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){Endgame.tooltip(s,lines,true);}
 @Override public boolean isDamageable(ItemStack stack){return false;}
 @Override public boolean isBarVisible(ItemStack stack){return false;}
}

