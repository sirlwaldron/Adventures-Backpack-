package com.waldron.adventurerspack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;
public class DockBlock extends BaseEntityBlock {
 public DockBlock(){super(BlockBehaviour.Properties.copy(Blocks.BARREL).strength(2.5f).noOcclusion());}
 @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DockEntity(pos,state);}
 @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,AdventurersPack.DOCK_ENTITY.get(),DockEntity::serverTick);}
 @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){
  if(!(level.getBlockEntity(pos) instanceof DockEntity dock))return InteractionResult.PASS;
  if(dock.pack.isEmpty()){
   ItemStack held=p.getItemInHand(hand);
   if(!(held.getItem() instanceof PackItem))return InteractionResult.PASS;
   if(level.isClientSide){dock.setPack(PackEquipment.visualStack(held));return InteractionResult.SUCCESS;}
   dock.setPack(held.split(1));
   return InteractionResult.CONSUME;
  }
  if(level.isClientSide)return InteractionResult.SUCCESS;
  if(p.isShiftKeyDown()&&p.getItemInHand(hand).isEmpty()){
   if(dock.inUse()){p.displayClientMessage(Component.literal("Close the dock inventory before removing the pack."),true);return InteractionResult.CONSUME;}
   p.setItemInHand(hand,dock.pack);dock.setPack(ItemStack.EMPTY);
  }else if(p instanceof ServerPlayer sp){
   if(dock.inUse()){p.displayClientMessage(Component.literal("This dock is already open."),true);return InteractionResult.CONSUME;}
   PackData d=dock.data();NetworkHooks.openScreen(sp,new SimpleMenuProvider((id,inv,pl)->new PackMenu(id,inv,dock),Component.literal("Expedition Dock")),buf->{buf.writeVarInt(-1);buf.writeBlockPos(pos);buf.writeNbt(d.displayTag());});
  }return InteractionResult.CONSUME;
 }
 @Override public void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){if(old.getBlock()!=next.getBlock()&&!level.isClientSide&&level.getBlockEntity(pos) instanceof DockEntity dock){ItemStack item=dock.pack;dock.setPack(ItemStack.EMPTY);Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,item);}super.onRemove(old,level,pos,next,moving);}
}
