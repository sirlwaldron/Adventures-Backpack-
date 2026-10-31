package com.waldron.adventurerspack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
public final class PackStorageActions {
 private PackStorageActions(){}
 public static int points(Player p){int l=p.experienceLevel;return (l<=16?l*l+6*l:l<=31?(5*l*l-81*l+720)/2:(9*l*l-325*l+4440)/2)+Math.round(p.experienceProgress*p.getXpNeededForNextLevel());}
 public static int transferXp(PackData d,Player p,boolean deposit,int requested){
  if(!d.has(AdventurersPack.XP_BANK.get())||requested<=0)return 0;
  int amount=deposit?Math.min(requested,Math.min(points(p),PackData.XP_CAPACITY-d.xpBank)):Math.min(requested,d.xpBank);
  if(amount<=0)return 0;d.xpBank+=deposit?amount:-amount;p.giveExperiencePoints(deposit?-amount:amount);d.save();return amount;
 }
 /** Moves FE into the battery. Limited to {@link PackData#ENERGY_RATE} per call, matching a pipe transfer. */
 public static int receiveEnergy(PackData d,int max,boolean simulate){
  if(!d.has(AdventurersPack.BATTERY.get())||max<=0||d.energy>=PackData.ENERGY_CAPACITY)return 0;
  int moved=Math.min(max,Math.min(PackData.ENERGY_RATE,PackData.ENERGY_CAPACITY-d.energy));
  if(!simulate&&moved>0){d.energy+=moved;d.save();}
  return moved;
 }
 /** Moves FE out of the battery. Limited to {@link PackData#ENERGY_RATE} per call. */
 public static int extractEnergy(PackData d,int max,boolean simulate){
  if(!d.has(AdventurersPack.BATTERY.get())||max<=0||d.energy<=0)return 0;
  int moved=Math.min(max,Math.min(PackData.ENERGY_RATE,d.energy));
  if(!simulate&&moved>0){d.energy-=moved;d.save();}
  return moved;
 }
 /** Input slot drains an item into the battery. Output slot charges an item from the battery. */
 public static void tickEnergy(PackData d){
  if(!d.has(AdventurersPack.BATTERY.get()))return;
  boolean changed=false;
  ItemStack inbound=d.energySlots.getStackInSlot(0);
  if(!inbound.isEmpty()){
   var optional=inbound.getCapability(ForgeCapabilities.ENERGY);
   if(optional.isPresent()){
    int room=Math.min(PackData.ENERGY_RATE,PackData.ENERGY_CAPACITY-d.energy);
    int moved=room<=0?0:optional.orElseThrow(IllegalStateException::new).extractEnergy(room,false);
    if(moved>0){d.energy+=moved;changed=true;}
   }
  }
  ItemStack outbound=d.energySlots.getStackInSlot(1);
  if(!outbound.isEmpty()){
   var optional=outbound.getCapability(ForgeCapabilities.ENERGY);
   if(optional.isPresent()){
    int offer=Math.min(PackData.ENERGY_RATE,d.energy);
    int moved=offer<=0?0:optional.orElseThrow(IllegalStateException::new).receiveEnergy(offer,false);
    if(moved>0){d.energy-=moved;changed=true;}
   }
  }
  if(changed)d.save();
 }
 /** Each tank has a dedicated deposit and withdrawal container. */
 public static void transferFluidSlots(PackData d,Player player){
  if(!d.has(AdventurersPack.TANK.get()))return;
  for(int i=0;i<4;i++){
   ItemStack container=d.fluidSlot.getStackInSlot(i).copy();
   if(container.getCount()!=1)continue;
   var tank=d.tankAt(i/2);
   var result=i%2==0?FluidUtil.tryEmptyContainer(container,tank,Integer.MAX_VALUE,player,true):FluidUtil.tryFillContainer(container,tank,Integer.MAX_VALUE,player,true);
   if(result.isSuccess())d.fluidSlot.setStackInSlot(i,result.getResult());
  }
 }
 public static boolean handle(PackMenu menu,Player player,int id){
  PackData d=menu.data;
  if(id==5000&&d.has(AdventurersPack.TANK.get())||id==5001&&d.has(AdventurersPack.XP_BANK.get())||id==5012&&d.has(AdventurersPack.BATTERY.get())){menu.craftingOpen=false;d.addon=id==5000?14:id==5001?15:16;menu.broadcastChanges();return true;}
  if((id==5010||id==5011)&&d.addon==14&&d.has(AdventurersPack.TANK.get())){d.tankIndex=id==5011?1:0;d.save();menu.broadcastChanges();return true;}
  if((id==5002||id==5003)&&d.addon==14&&d.has(AdventurersPack.TANK.get())){
   boolean fromSlot=!d.fluidSlot.getStackInSlot(0).isEmpty();
   ItemStack cursor=fromSlot?d.fluidSlot.getStackInSlot(0).copy():menu.getCarried();if(cursor.getCount()!=1)return false;
   var tank=d.tankAt(d.tankIndex);
   var result=id==5002?FluidUtil.tryEmptyContainer(cursor,tank,Integer.MAX_VALUE,player,true):FluidUtil.tryFillContainer(cursor,tank,Integer.MAX_VALUE,player,true);
   if(!result.isSuccess())return false;if(fromSlot)d.fluidSlot.setStackInSlot(0,result.getResult());else menu.setCarried(result.getResult());d.save();menu.broadcastChanges();return true;
  }
  if(id>=5004&&id<=5007&&d.addon==15&&d.has(AdventurersPack.XP_BANK.get())){
   transferXp(d,player,id==5004||id==5006,id>=5006?PackData.XP_CAPACITY:100);menu.broadcastChanges();return true;
  }
  if(id==5014){sortCompartment(d);menu.broadcastChanges();return true;}
  if(id==5015){quickStack(menu,player);menu.broadcastChanges();return true;}
  if(id==5016){restockHotbar(menu,player);menu.broadcastChanges();return true;}
  if(id==5017){depositInventory(menu,player);menu.broadcastChanges();return true;}
  if(id==5018){d.trash.setStackInSlot(0,ItemStack.EMPTY);d.save();menu.broadcastChanges();return true;}
  return false;
 }
 public static void sortCompartment(PackData d){
  java.util.List<Integer> openSlots=new java.util.ArrayList<>();
  java.util.List<ItemStack> stacks=new java.util.ArrayList<>();
  for(int j=0;j<d.pageSize();j++){
   int id=d.storageId(d.tab,j);
   if(!d.storageOpen(id)||d.protectedStorage(id))continue;
   openSlots.add(id);
   ItemStack cur=d.storage(id);
   if(!cur.isEmpty()){
    d.storageHandler(id).setStackInSlot(d.localSlot(id),ItemStack.EMPTY);
    int rem=cur.getCount(),cap=cur.getMaxStackSize()==1?1:d.slotCapacity();
    for(ItemStack ex:stacks)if(ex.getCount()<cap&&ItemStack.isSameItemSameTags(ex,cur)){
     int add=Math.min(rem,cap-ex.getCount());ex.grow(add);rem-=add;if(rem<=0)break;
    }
    while(rem>0){int take=Math.min(rem,cap);stacks.add(cur.copyWithCount(take));rem-=take;}
   }
  }
  stacks.sort((a,b)->{
   int countCmp=Integer.compare(b.getCount(),a.getCount());
   if(countCmp!=0)return countCmp;
   int ca=PackData.category(a),cb=PackData.category(b);
   if(ca!=cb)return Integer.compare(cb,ca);
   return a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString());
  });
  for(int i=0;i<Math.min(openSlots.size(),stacks.size());i++){
   int id=openSlots.get(i);
   d.storageHandler(id).setStackInSlot(d.localSlot(id),stacks.get(i));
  }
  d.save();
 }
 private static boolean hasMatchingInPack(PackData d,ItemStack s){
  if(!PackData.safe(s))return false;
  if(d.findMatching(s)>=0)return true;
  if(d.has(AdventurersPack.QUIVER.get())&&PackData.ammo(s))for(int i=0;i<d.quiver.getSlots();i++)if(ItemStack.isSameItemSameTags(d.quiver.getStackInSlot(i),s))return true;
  if(d.has(AdventurersPack.TORCH.get())&&PackData.torch(s))for(int i=0;i<d.torches.getSlots();i++)if(ItemStack.isSameItemSameTags(d.torches.getStackInSlot(i),s))return true;
  for(int t=0;t<PackData.TABS;t++)for(int j=0;j<d.pageSize();j++){int id=d.storageId(t,j);if(ItemStack.isSameItemSameTags(d.storage(id),s))return true;}
  return false;
 }
 public static void quickStack(PackMenu menu,Player player){
  depositMatching(menu.data,player,menu.boundSlot);
 }
 public static void autoRoute(PackData d,Player player,int boundSlot){
  if(d.routing&&!d.owner.isEmpty())depositMatching(d,player,boundSlot);
 }
 private static void depositMatching(PackData d,Player player,int boundSlot){
  var inv=player.getInventory();boolean changed=false;
  for(int i=9;i<36;i++){
   if(i==boundSlot)continue;
   ItemStack s=inv.getItem(i);
   if(s.isEmpty()||!hasMatchingInPack(d,s))continue;
   ItemStack rest=d.insertMatching(s);
   if(rest.getCount()!=s.getCount()){inv.setItem(i,rest.isEmpty()?ItemStack.EMPTY:rest);changed=true;}
  }
  if(changed){inv.setChanged();d.save();}
 }
 public static void depositInventory(PackMenu menu,Player player){
  PackData d=menu.data;var inv=player.getInventory();boolean changed=false;
  for(int i=9;i<36;i++){
   if(i==menu.boundSlot)continue;
   ItemStack s=inv.getItem(i);
   if(s.isEmpty()||!PackData.safe(s)||s.getItem() instanceof AdventurersPack.UpgradeItem)continue;
   ItemStack rest=d.insert(s,false);
   if(rest.getCount()!=s.getCount()){inv.setItem(i,rest.isEmpty()?ItemStack.EMPTY:rest);changed=true;}
  }
  if(changed){inv.setChanged();d.save();}
 }
 public static void restockHotbar(PackMenu menu,Player player){
  PackData d=menu.data;var inv=player.getInventory();boolean changed=false;
  for(int i=0;i<9;i++){
   if(i==menu.boundSlot)continue;
   ItemStack hot=inv.getItem(i);
   if(hot.isEmpty()||!hot.isStackable()||hot.getCount()>=hot.getMaxStackSize())continue;
   int need=hot.getMaxStackSize()-hot.getCount();
   int b=d.findMatching(hot);
   if(b>=0&&!d.protectedSlot(b)&&d.counts[b]>0){
    ItemStack pulled=d.withdraw(b,need);
    if(!pulled.isEmpty()){hot.grow(pulled.getCount());need-=pulled.getCount();changed=true;}
   }
   for(int t=0;t<PackData.TABS&&need>0;t++)for(int j=0;j<d.pageSize()&&need>0;j++){
    int id=d.storageId(t,j);
    if(d.protectedStorage(id))continue;
    ItemStack st=d.storage(id);
    if(!st.isEmpty()&&ItemStack.isSameItemSameTags(st,hot)){
     ItemStack pulled=d.extractStorage(id,need,false);
     if(!pulled.isEmpty()){hot.grow(pulled.getCount());need-=pulled.getCount();changed=true;}
    }
   }
  }
  if(changed){inv.setChanged();d.save();}
 }
}
