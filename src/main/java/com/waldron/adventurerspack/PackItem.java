package com.waldron.adventurerspack;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;
import java.util.List;

public class PackItem extends Item {
 public PackItem(){super(new Properties().stacksTo(1).fireResistant());}
 @Override public boolean canFitInsideContainerItems(){return false;}
 @Override public boolean doesSneakBypassUse(ItemStack s,net.minecraft.world.level.LevelReader l,net.minecraft.core.BlockPos pos,Player p){return l.getBlockState(pos).is(AdventurersPack.DOCK.get());}
 public static final int CURIOS=42;
 public static ItemStack stack(Player p,int slot){if(slot==41)return PackEquipment.stack(p);if(slot==CURIOS)return CuriosPacks.stack(p);return slot>=0&&slot<41?p.getInventory().getItem(slot):ItemStack.EMPTY;}
 public static ItemStack worn(Player p){ItemStack equipped=PackEquipment.stack(p);return equipped.isEmpty()?CuriosPacks.stack(p):equipped;}
 public static int wornSlot(Player p){if(!PackEquipment.stack(p).isEmpty())return 41;return CuriosPacks.stack(p).isEmpty()?41:CURIOS;}
 public static int find(Player p){if(!PackEquipment.stack(p).isEmpty())return 41;if(!CuriosPacks.stack(p).isEmpty())return CURIOS;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).getItem() instanceof PackItem)return i;return p.getOffhandItem().getItem() instanceof PackItem?40:-1;}
 public static PackData data(Player p,ItemStack s){QuiverAccess.flush(p);return p.containerMenu instanceof PackMenu m && m.data.owner==s?m.data:new PackData(s);}
 public static void open(ServerPlayer p,int slot){ToolHand.restore(p);if(slot<0||slot>CURIOS)return;ItemStack s=stack(p,slot);if(slot!=41&&slot!=CURIOS&&!(s.getItem() instanceof PackItem))return;
  PackData d=new PackData(s);
  NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,pl)->new PackMenu(id,inv,slot,d),Component.literal("Adventure's Backpack")),buf->{buf.writeVarInt(slot);buf.writeNbt(d.displayTag());});
 }
 @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){ItemStack s=p.getItemInHand(hand);if(p instanceof ServerPlayer sp)open(sp,hand==InteractionHand.OFF_HAND?40:p.getInventory().selected);return InteractionResultHolder.sidedSuccess(s,l.isClientSide);}
 @Override public InteractionResult useOn(UseOnContext ctx){Player p=ctx.getPlayer();if(p==null)return InteractionResult.PASS;ItemStack pack=ctx.getItemInHand();PackData d=data(p,pack);
  if(p.isShiftKeyDown()||!d.building||!d.has(AdventurersPack.BUILDER.get())){if(p instanceof ServerPlayer sp)open(sp,ctx.getHand()==InteractionHand.OFF_HAND?40:p.getInventory().selected);return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);}
  int i=d.selected;if(d.counts[i]<=0||!(d.bulk[i].getItem() instanceof BlockItem))return InteractionResult.FAIL;
  ItemStack block=d.bulk[i].copyWithCount(1);p.setItemInHand(ctx.getHand(),block);
  try{var hit=new BlockHitResult(ctx.getClickLocation(),ctx.getClickedFace(),ctx.getClickedPos(),ctx.isInside());InteractionResult result=block.useOn(new UseOnContext(p,ctx.getHand(),hit));
   if(!ctx.getLevel().isClientSide&&result.consumesAction()&&block.isEmpty()&&!p.getAbilities().instabuild)d.withdraw(i,1);return result;
  }finally{p.setItemInHand(ctx.getHand(),pack);}
 }
 @Override public Component getName(ItemStack stack){return Component.translatable("item.adventurerspack.tier."+PackData.tierOf(stack));}
 @Override public Rarity getRarity(ItemStack stack){return switch(PackData.tierOf(stack)){case 2->Rarity.UNCOMMON;case 3->Rarity.RARE;case 4->Rarity.EPIC;default->Rarity.COMMON;};}
 @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){PackData d=new PackData(s);lines.add(Component.literal(d.pageSize()+" slots per compartment / "+(d.pageSize()*4)+" total"));lines.add(Component.translatable("item.adventurerspack.sockets",d.openGear(),d.openRunes()));lines.add(Component.translatable("item.adventurerspack.sections",d.openSections()));lines.add(Component.translatable("item.adventurerspack.bulk_line",d.capacity()));lines.add(Component.translatable("item.adventurerspack.help"));}
}
