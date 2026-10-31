package com.waldron.adventurerspack;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** A foldable bed, lantern, barrel and furnace. Packed inventories are stored on the kit item. */
@Mod.EventBusSubscriber(modid=AdventurersPack.ID)
public class CampKit {
 private static final int[][] OFF={{0,0,0},{0,0,1},{2,0,0},{2,1,0},{2,0,1}};
 private static BlockPos pos(BlockPos base,int i){return base.offset(OFF[i][0],OFF[i][1],OFF[i][2]);}
 private static BlockState state(int i){return switch(i){case 0,1->Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH).setValue(BedBlock.PART,i==0?BedPart.FOOT:BedPart.HEAD);case 2->Blocks.BARREL.defaultBlockState();case 3->Blocks.LANTERN.defaultBlockState();default->Blocks.FURNACE.defaultBlockState();};}
 private static void say(ServerPlayer p,String s){p.displayClientMessage(Component.literal(s),true);}
 public static void action(ServerPlayer p,PackData d,int action){  ItemStack kit=d.camp.getStackInSlot(0);if(kit.isEmpty()){say(p,"Install a Camp Kit in this slot first.");return;}
  var n=kit.getOrCreateTag();var ledger=CampLedger.get(p.server);
  String id=n.hasUUID("CampId")?n.getUUID("CampId").toString():null;
  var record=id!=null?ledger.records.getCompound(id):new CompoundTag();
  if(record.isEmpty()){
   double bestDist=Double.MAX_VALUE;String bestKey=null;CompoundTag bestRec=null;
   for(String key:ledger.records.getAllKeys()){
    CompoundTag rec=ledger.records.getCompound(key);
    if(rec.hasUUID("Owner")&&rec.getUUID("Owner").equals(p.getUUID())){
     double d2=1e9;
     if(rec.getString("Dimension").equals(p.level().dimension().location().toString())){
      BlockPos b=BlockPos.of(rec.getLong("Pos"));d2=p.distanceToSqr(b.getX()+.5,b.getY()+.5,b.getZ()+.5);
     }
     if(bestRec==null||d2<bestDist){bestDist=d2;bestKey=key;bestRec=rec;}
    }
   }
   if(bestKey!=null){id=bestKey;record=bestRec;try{n.putUUID("CampId",UUID.fromString(id));}catch(Exception ignored){}}
  }
  if(id==null){UUID newId=UUID.randomUUID();n.putUUID("CampId",newId);id=newId.toString();}
  if(action==0){if(!record.isEmpty()){say(p,"Your camp is already set up. Return to it to pack up.");return;}deploy(p,d,kit,id,ledger);return;}
  if(record.isEmpty()){say(p,"Set up this camp first.");return;}if(!record.getUUID("Owner").equals(p.getUUID())){say(p,"Only the camper who set this up can fold it.");return;}
  BlockPos base=BlockPos.of(record.getLong("Pos"));if(!record.getString("Dimension").equals(p.level().dimension().location().toString())||p.distanceToSqr(base.getX()+.5,base.getY()+.5,base.getZ()+.5)>100){say(p,"Return within 10 blocks of your camp.");return;}
  for(int i=0;i<5;i++)if(!p.level().hasChunkAt(pos(base,i))){say(p,"Return to your campsite to load the area before folding.");return;}
  if(action==2||action==3){BlockEntity be=p.level().getBlockEntity(pos(base,action==2?2:4));if(be instanceof MenuProvider provider)p.openMenu(provider);else say(p,"Container is missing.");return;}
  if(action!=1)return;
  for(int i:new int[]{2,4}){if(!p.level().getBlockState(pos(base,i)).is(state(i).getBlock()))continue;BlockEntity be=p.level().getBlockEntity(pos(base,i));if(be instanceof net.minecraft.world.Container c)for(int s=0;s<c.getContainerSize();s++){ItemStack stack=c.getItem(s);if(!stack.isEmpty()&&(stack.getItem() instanceof PackItem||!PackData.safe(stack))){say(p,"Remove backpacks and portable containers before folding camp.");return;}}}
  if(p.level().players().stream().anyMatch(q->q.isSleeping()&&q.getSleepingPos().map(b->b.equals(pos(base,0))||b.equals(pos(base,1))).orElse(false))){say(p,"Someone is sleeping here.");return;}
  if(p.server.getPlayerList().getPlayers().stream().anyMatch(q->q!=p&&q.containerMenu!=q.inventoryMenu&&q.level()==p.level()&&q.distanceToSqr(base.getX(),base.getY(),base.getZ())<144)){say(p,"Ask nearby campers to close their containers first.");return;}
  CompoundTag payload=new CompoundTag();for(int i:new int[]{2,4}){if(!p.level().getBlockState(pos(base,i)).is(state(i).getBlock()))continue;BlockEntity be=p.level().getBlockEntity(pos(base,i));if(be!=null)payload.put("Part"+i,be.saveWithoutMetadata());}
  n.put("Packed",payload);n.remove("Deployed");d.save();
  for(int i:new int[]{3,0,1,2,4}){BlockPos at=pos(base,i);if(!p.level().getBlockState(at).is(state(i).getBlock()))continue;BlockEntity be=p.level().getBlockEntity(at);if(be instanceof net.minecraft.world.Container c)c.clearContent();p.level().removeBlockEntity(at);if(p.level().getBlockState(at).is(state(i).getBlock()))p.level().setBlock(at,Blocks.AIR.defaultBlockState(),18);}
  ledger.records.remove(id);ledger.setDirty();say(p,"Camp folded. Your supplies and furnace contents are packed.");
 }
 private static void deploy(ServerPlayer p,PackData d,ItemStack kit,String id,CampLedger ledger){if(!p.level().dimensionType().bedWorks()){say(p,"This dimension is unsafe for a bedroll.");return;}BlockPos base=p.blockPosition().relative(p.getDirection(),3);
  for(int i=0;i<5;i++){BlockPos at=pos(base,i);if(!p.level().hasChunkAt(at)||!p.level().getWorldBorder().isWithinBounds(at)||!p.level().isInWorldBounds(at)||!p.level().isEmptyBlock(at)||!p.level().mayInteract(p,at)||!p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(at)).isEmpty()||(i!=3&&!p.level().getBlockState(at.below()).isFaceSturdy(p.level(),at.below(),Direction.UP))){say(p,"Camp needs clear, level ground: a 3 by 2 area ahead.");return;}}
  CompoundTag payload=kit.getOrCreateTag().getCompound("Packed");
  for(int i=0;i<5;i++){BlockPos at=pos(base,i);var snapshot=net.minecraftforge.common.util.BlockSnapshot.create(p.level().dimension(),p.level(),at);p.level().setBlock(at,state(i),18);if(net.minecraftforge.event.ForgeEventFactory.onBlockPlace(p,snapshot,Direction.UP)){for(int j=i;j>=0;j--){p.level().removeBlockEntity(pos(base,j));p.level().setBlock(pos(base,j),Blocks.AIR.defaultBlockState(),18);}say(p,"You cannot set up a camp in this protected area.");return;}}
  for(int i:new int[]{2,4}){BlockPos at=pos(base,i);BlockEntity be=p.level().getBlockEntity(at);if(be!=null&&payload.contains("Part"+i)){be.load(payload.getCompound("Part"+i));be.setChanged();if(i==4&&payload.getCompound("Part4").getShort("BurnTime")>0)p.level().setBlock(at,state(i).setValue(AbstractFurnaceBlock.LIT,true),18);}}
  CompoundTag record=new CompoundTag();record.putUUID("Owner",p.getUUID());record.putString("Dimension",p.level().dimension().location().toString());record.putLong("Pos",base.asLong());ledger.records.put(id,record);ledger.setDirty();kit.getOrCreateTag().remove("Packed");kit.getOrCreateTag().putBoolean("Deployed",true);d.save();say(p,"Camp ready: bedroll, lantern, supply barrel and furnace. Pack up here when finished.");
 }
 private static boolean protectedAt(net.minecraft.world.level.Level level,BlockPos at){if(level.isClientSide||level.getServer()==null)return false;var records=CampLedger.get(level.getServer()).records;for(String id:records.getAllKeys()){var n=records.getCompound(id);if(!n.getString("Dimension").equals(level.dimension().location().toString()))continue;var base=BlockPos.of(n.getLong("Pos"));for(int i=0;i<5;i++)if(at.equals(pos(base,i))||(i!=3&&at.equals(pos(base,i).below())))return true;}return false;}
 @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e){if(e.getLevel() instanceof net.minecraft.world.level.Level l&&protectedAt(l,e.getPos())){if(e.getPlayer()!=null&&(e.getPlayer().isCreative()||e.getPlayer().hasPermissions(2)))return;e.setCanceled(true);if(e.getPlayer() instanceof ServerPlayer p)say(p,"Use Pack Up in your backpack's Camp panel to fold the camp.");}}
 @SubscribeEvent public static void piston(net.minecraftforge.event.level.PistonEvent.Pre e){if(!(e.getLevel() instanceof net.minecraft.world.level.Level l))return;var helper=e.getStructureHelper();if(helper==null||!helper.resolve())return;if(protectedAt(l,e.getFaceOffsetPos())||helper.getToPush().stream().anyMatch(p->protectedAt(l,p)||protectedAt(l,p.relative(e.getDirection())))||helper.getToDestroy().stream().anyMatch(p->protectedAt(l,p)))e.setCanceled(true);}
 @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e){e.getAffectedBlocks().removeIf(p->protectedAt(e.getLevel(),p));}
}
