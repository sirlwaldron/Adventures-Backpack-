package com.waldron.adventurerspack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraft.server.level.ServerPlayer;
import org.anti_ad.mc.ipn.api.IPNSlotsIgnoreForInventoryTypes;

@IPNSlotsIgnoreForInventoryTypes(value = {
 "com.waldron.adventurerspack.PackMenu$ToolKitSlot",
 "com.waldron.adventurerspack.PackMenu$UpgradeSocketSlot",
 "com.waldron.adventurerspack.PackMenu$SectionSocketSlot",
 "com.waldron.adventurerspack.PackMenu$PouchSocketSlot",
 "com.waldron.adventurerspack.PackMenu$OffhandSlotItem",
 "com.waldron.adventurerspack.PackMenu$CampSlotItem",
 "com.waldron.adventurerspack.PackMenu$TorchSlotItem",
 "com.waldron.adventurerspack.PackMenu$TrashSlotItem"
})
public class PackMenu extends AbstractContainerMenu {
 public DockEntity dock; public net.minecraft.core.BlockPos dockPos;
 public final PackData data; public final int boundSlot; public final Inventory inventory;
 public final ItemStackHandler ghosts=new ItemStackHandler(18);
 public static final int BULK=123,PLAYER=132,CAROUSEL=173,CRAFT=177,RESULT=186,SECTIONS=187,QUIVER=192,PHILTER=201,BREW=204,ENDER=209;
 public static final int EQUIP_DROP=0;
 public static final int PACK_W=560,PACK_H=368,WIDE_W=560,ENDER_W=560;
 public static final int GRID_X=18,GRID_Y=78,BULK_Y=164,INV_Y=270,HOTBAR_Y=328,TAB_X=14,TAB_Y=54;
 public static final int TOOL_Y=224;
 public static final int SIDE_X=380,SIDE_Y=142,SIDE_W=166,SIDE_H=176;
 public static final int CRAFT_X=392,CRAFT_Y=186;
 public static final int EQUIPMENT=236,EXTRA=237,BULK_EXTRA=381,POUCH_SOCKETS=390,POUCH_CONTENTS=394,CAMP_SLOT=466,FLUID_SLOT=CAMP_SLOT+5,ENERGY_IN=FLUID_SLOT+1,ENERGY_OUT=FLUID_SLOT+2;
 private static final int ARMOR=168,HAND=172;
 public boolean craftingOpen;
 public static final int PLAYER_X=168,PLAYER_STEP=25;
 public int inventoryY(){return 436;}
 public int extraY(){return inventoryY()-INV_Y;}
 public int panelHeight(){return inventoryY()+116;}
 public int bulkIndex(int id){return id>=BULK&&id<PLAYER?id-BULK:id>=BULK_EXTRA&&id<BULK_EXTRA+9?id-BULK_EXTRA+9:-1;}
 public int bulkSlot(int i){return i<9?BULK+i:BULK_EXTRA+i-9;}
 private final TransientCraftingContainer craftGrid=new TransientCraftingContainer(this,3,3);
 private final ResultContainer craftResult=new ResultContainer();
 private int displayHash; private boolean displaySent;
 private record Opening(int bound,net.minecraft.core.BlockPos pos,PackData data){}
 private static Opening read(FriendlyByteBuf b){int bound=b.readVarInt();var pos=bound==-1?b.readBlockPos():null;var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));var tag=b.readNbt();if(tag!=null&&tag.getBoolean("Empty"))d=new PackData(ItemStack.EMPTY);d.readDisplay(tag);return new Opening(bound,pos,d);}
 public PackMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,read(buf));}
 private PackMenu(int id,Inventory inv,Opening o){this(id,inv,o.bound,o.data);dockPos=o.pos;}
 public PackMenu(int id,Inventory inv,DockEntity entity){this(id,inv,-1,entity.data());dock=entity;dockPos=entity.getBlockPos();}

 public final RangedWrapper toolsWrapper;
 public final RangedWrapper modulesWrapper;
 private static final Container SOCKET_CONTAINER=new ResultContainer();

 public static class BaseHandlerSlot extends Slot {
  protected final IItemHandler itemHandler;
  protected final int handlerIndex;
  public BaseHandlerSlot(Container container,IItemHandler itemHandler,int index,int x,int y){
   super(container,index,x,y);
   this.itemHandler=itemHandler;
   this.handlerIndex=index;
  }
  @Override public boolean mayPlace(ItemStack stack){return !stack.isEmpty()&&itemHandler.isItemValid(handlerIndex,stack);}
  @Override public ItemStack getItem(){return itemHandler.getStackInSlot(handlerIndex);}
  @Override public void set(ItemStack stack){if(itemHandler instanceof IItemHandlerModifiable mod)mod.setStackInSlot(handlerIndex,stack);setChanged();}
  @Override public void setByPlayer(ItemStack stack){set(stack);}
  @Override public int getMaxStackSize(){return itemHandler.getSlotLimit(handlerIndex);}
  @Override public int getMaxStackSize(ItemStack stack){
   ItemStack maxAdd=stack.copy();int maxInput=stack.getMaxStackSize();maxAdd.setCount(maxInput);
   ItemStack currentStack=itemHandler.getStackInSlot(handlerIndex);
   if(itemHandler instanceof IItemHandlerModifiable mod){
    mod.setStackInSlot(handlerIndex,ItemStack.EMPTY);
    ItemStack remainder=itemHandler.insertItem(handlerIndex,maxAdd,true);
    mod.setStackInSlot(handlerIndex,currentStack);
    return maxInput-remainder.getCount();
   }else{
    ItemStack remainder=itemHandler.insertItem(handlerIndex,maxAdd,true);
    int current=currentStack.getCount();
    int added=maxInput-remainder.getCount();
    return current+added;
   }
  }
  @Override public boolean mayPickup(Player player){return !itemHandler.extractItem(handlerIndex,1,true).isEmpty();}
  @Override public ItemStack remove(int amount){return itemHandler.extractItem(handlerIndex,amount,false);}
  public IItemHandler getItemHandler(){return itemHandler;}
  @Override public int getSlotIndex(){return handlerIndex;}
  @Override public boolean isSameInventory(Slot other){return other!=null&&other.getClass()==this.getClass();}
 }
 public static class ToolKitSlot extends BaseHandlerSlot {
  public ToolKitSlot(IItemHandler handler,int index,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);}
 }
 public static class UpgradeSocketSlot extends BaseHandlerSlot {
  public final int moduleIndex;
  public UpgradeSocketSlot(IItemHandler handler,int index,int moduleIndex,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);this.moduleIndex=moduleIndex;}
  public int getModuleSlot(){return moduleIndex;}
 }
 public static class SectionSocketSlot extends BaseHandlerSlot {
  public SectionSocketSlot(IItemHandler handler,int index,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);}
 }
 public static class PouchSocketSlot extends BaseHandlerSlot {
  public PouchSocketSlot(IItemHandler handler,int index,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);}
 }
 public static class OffhandSlotItem extends BaseHandlerSlot {
  public OffhandSlotItem(IItemHandler handler,int index,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);}
 }
 public static class TorchSlotItem extends BaseHandlerSlot {
  public TorchSlotItem(IItemHandler handler,int index,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);}
 }
 public static class CampSlotItem extends BaseHandlerSlot {
  public CampSlotItem(IItemHandler handler,int index,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);}
 }
 public static class TrashSlotItem extends BaseHandlerSlot {
  public TrashSlotItem(IItemHandler handler,int index,int x,int y){super(SOCKET_CONTAINER,handler,index,x,y);}
 }

 public PackMenu(int id,Inventory inv,int bound,PackData d){super(AdventurersPack.MENU.get(),id);inventory=inv;boundSlot=bound;data=d;
  toolsWrapper=new RangedWrapper(d.items,PackData.TOOLS,PackData.TOOLS+6);
  modulesWrapper=new RangedWrapper(d.items,PackData.MODULES,PackData.TOTAL);
  for(int i=0;i<108;i++){final int page=i/27;addSlot(new StorageSlot(d.items,i,GRID_X+(i%9)*18,GRID_Y+((i%27)/9)*18){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.tab==page;}});}
  for(int i=0;i<6;i++)addSlot(new ToolKitSlot(toolsWrapper,i,204+i*26,TOOL_Y){@Override public boolean isActive(){return !data.owner.isEmpty();}});
  for(int i=0;i<6;i++)addSlot(new UpgradeSocketSlot(modulesWrapper,i,114+i,SIDE_X+4+(i%3)*(i<3?27:32),SIDE_Y+(i<3?40:78)){@Override public boolean isActive(){return !data.owner.isEmpty()&&!craftingOpen&&(data.addon==0||data.addon==1);}});
  for(int i=0;i<3;i++){final int furnaceSlot=i;addSlot(new SlotItemHandler(d.furnace,i,i==2?SIDE_X+116:SIDE_X+42,i==1?SIDE_Y+94:SIDE_Y+42){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==5&&data.has(AdventurersPack.FURNACE.get());}@Override public boolean mayPlace(ItemStack s){return data.has(AdventurersPack.FURNACE.get())&&super.mayPlace(s);}@Override public void onTake(Player p,ItemStack stack){super.onTake(p,stack);if(furnaceSlot==2)data.grantXp(p,stack.getCount(),getItem().getCount());}});}
  for(int i=0;i<9;i++)addSlot(new SlotItemHandler(ghosts,i,GRID_X+i*18,BULK_Y+data.tier*18){public boolean isActive(){return !data.owner.isEmpty();}@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addPlayer(inv,col+row*9+9,PLAYER_X+col*PLAYER_STEP,inventoryY()+row*PLAYER_STEP);
  for(int i=0;i<9;i++)addPlayer(inv,i,PLAYER_X+i*PLAYER_STEP,inventoryY()+81);
  for(int a=0;a<4;a++){final net.minecraft.world.entity.EquipmentSlot equipment=net.minecraft.world.entity.EquipmentSlot.byTypeAndIndex(net.minecraft.world.entity.EquipmentSlot.Type.ARMOR,a);addSlot(new Slot(inv,36+a,208,78+(3-a)*32){@Override public int getMaxStackSize(){return 1;}@Override public boolean mayPlace(ItemStack stack){return stack.canEquip(equipment,inv.player);}@Override public boolean mayPickup(Player pl){return getItem().isEmpty()||pl.isCreative()||!net.minecraft.world.item.enchantment.EnchantmentHelper.hasBindingCurse(getItem());}});}
  addPlayer(inv,40,338,110);
  for(int i=0;i<4;i++)addSlot(new OffhandSlotItem(d.offhand,i,330+(i%2)*18,142+(i/2)*32){@Override public boolean isActive(){return !data.owner.isEmpty();}});
  for(int i=0;i<9;i++){final int n=i;addSlot(new Slot(craftGrid,n,CRAFT_X+(n%3)*18,CRAFT_Y+(n/3)*18){@Override public boolean isActive(){return !data.owner.isEmpty()&&craftingOpen&&data.has(AdventurersPack.CRAFTING.get());}});}
  addSlot(new ResultSlot(inv.player,craftGrid,craftResult,0,CRAFT_X+88,CRAFT_Y+18){@Override public boolean isActive(){return !data.owner.isEmpty()&&craftingOpen&&data.has(AdventurersPack.CRAFTING.get());}});
  for(int i=0;i<5;i++)addSlot(new SectionSocketSlot(d.sections,i,SIDE_X+4+i*27,SIDE_Y+120){@Override public boolean isActive(){return !data.owner.isEmpty()&&!craftingOpen&&(data.addon==0||data.addon==1);}});
  for(int i=0;i<9;i++)addSlot(new SlotItemHandler(d.quiver,i,SIDE_X+50+(i%3)*20,SIDE_Y+44+(i/3)*20){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==2&&data.has(AdventurersPack.QUIVER.get());}});
  for(int i=0;i<3;i++)addSlot(new SlotItemHandler(d.vials,i,SIDE_X+30+i*44,SIDE_Y+62){@Override public boolean mayPickup(Player p){return false;}@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==3&&data.has(AdventurersPack.PHILTER.get());}});
  for(int i=0;i<3;i++)addSlot(new SlotItemHandler(d.brew,i,SIDE_X+44+i*30,SIDE_Y+96){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==4&&data.has(AdventurersPack.ALEMBIC.get());}});
  addSlot(new SlotItemHandler(d.brew,3,SIDE_X+74,SIDE_Y+38){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==4&&data.has(AdventurersPack.ALEMBIC.get());}});
  addSlot(new SlotItemHandler(d.brew,4,SIDE_X+12,SIDE_Y+38){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==4&&data.has(AdventurersPack.ALEMBIC.get());}});
  for(int i=0;i<27;i++){final int n=i;addSlot(new Slot(inv.player.getEnderChestInventory(),n,SIDE_X+2+(n%9)*18,SIDE_Y+42+(n/9)*18){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==6&&data.has(AdventurersPack.ENDER.get());}@Override public boolean mayPlace(ItemStack s){return !(s.getItem() instanceof PackItem);}});}
  addSlot(new SlotItemHandler(PackEquipment.get(inv.player).items,0,338,78){public boolean mayPickup(Player p){return true;}public boolean mayPlace(ItemStack s){return super.mayPlace(s);}});
  for(int i=0;i<144;i++){final int n=i;addSlot(new StorageSlot(d.extra,i,GRID_X+(i%36%9)*18,GRID_Y+54+(i%36/9)*18){public boolean isActive(){return !data.owner.isEmpty()&&data.tab==n/36&&data.storageOpen(108+n);}});}
  for(int i=9;i<18;i++)addSlot(new SlotItemHandler(ghosts,i,GRID_X+(i-9)*18,BULK_Y+data.tier*18+32){public boolean isActive(){return !data.owner.isEmpty();}public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}});
  for(int i=0;i<4;i++)addSlot(new PouchSocketSlot(d.pouches,i,SIDE_X+8+i*40,SIDE_Y+52){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon>=8&&data.addon<=11;}});
  for(int k=0;k<4;k++)for(int i=0;i<18;i++){final int kind=k;addSlot(new SlotItemHandler(d.pouchStorage[kind],i,SIDE_X+2+(i%9)*18,SIDE_Y+84+(i/9)*18){public boolean isActive(){return !data.owner.isEmpty()&&data.addon==8+kind&&!d.pouchStorage[kind].owner().isEmpty();}});}
  addSlot(new CampSlotItem(d.camp,0,SIDE_X+72,SIDE_Y+36){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==12;}});
  for(int i=0;i<2;i++)addSlot(new UpgradeSocketSlot(modulesWrapper,6+i,120+i,SIDE_X+4+(3+i)*27,SIDE_Y+40){@Override public boolean isActive(){return !data.owner.isEmpty()&&!craftingOpen&&(data.addon==0||data.addon==1);}});
  for(int i=0;i<2;i++)addSlot(new UpgradeSocketSlot(modulesWrapper,8+i,122+i,SIDE_X+4+(3+i)*32,SIDE_Y+78){@Override public boolean isActive(){return !data.owner.isEmpty()&&!craftingOpen&&(data.addon==0||data.addon==1);}});
  addSlot(new SlotItemHandler(d.fluidSlot,0,390,266){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==14&&data.has(AdventurersPack.TANK.get());}});
  addSlot(new SlotItemHandler(d.energySlots,0,414,240){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==16&&data.has(AdventurersPack.BATTERY.get());}});
  addSlot(new SlotItemHandler(d.energySlots,1,494,240){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==16&&data.has(AdventurersPack.BATTERY.get());}});
  addSlot(new SectionSocketSlot(d.sections,5,SIDE_X+4+5*27,SIDE_Y+120){@Override public boolean isActive(){return !data.owner.isEmpty()&&!craftingOpen&&(data.addon==0||data.addon==1);}});
  for(int i=1;i<4;i++)addSlot(new SlotItemHandler(d.fluidSlot,i,390+(i/2)*84+(i%2)*40,266){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==14&&data.has(AdventurersPack.TANK.get());}});
  addSlot(new UpgradeSocketSlot(modulesWrapper,10,124,SIDE_X+4+5*27,SIDE_Y+40){@Override public boolean isActive(){return !data.owner.isEmpty()&&!craftingOpen&&(data.addon==0||data.addon==1);}});
  for(int i=0;i<9;i++)addSlot(new TorchSlotItem(d.torches,i,SIDE_X+50+(i%3)*20,SIDE_Y+44+(i/3)*20){@Override public boolean isActive(){return !data.owner.isEmpty()&&data.addon==17&&data.has(AdventurersPack.TORCH.get());}});
  addSlot(new TrashSlotItem(d.trash,0,18,inventoryY()+85){@Override public boolean isActive(){return !data.owner.isEmpty();}});
  addDataSlot(new DataSlot(){public int get(){return data.tab;}public void set(int v){data.tab=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.selected;}public void set(int v){data.selected=v;}});
  addDataSlot(new DataSlot(){public int get(){return (data.pickup?1:0)|(data.voiding?2:0)|(data.building?4:0)|(data.routing?8:0)|((data.preset&3)<<4);}public void set(int v){data.pickup=(v&1)!=0;data.voiding=(v&2)!=0;data.building=(v&4)!=0;data.routing=(v&8)!=0;data.preset=(v>>4)&3;}});
  addDataSlot(new DataSlot(){public int get(){return data.protectedMask&65535;}public void set(int v){data.protectedMask=(data.protectedMask&0xffff0000)|(v&65535);}});
  addDataSlot(new DataSlot(){public int get(){return data.protectedMask>>>16;}public void set(int v){data.protectedMask=(data.protectedMask&65535)|((v&65535)<<16);}});
  for(int j=0;j<18;j++){final int i=j;addDataSlot(new DataSlot(){public int get(){return data.counts[i]&65535;}public void set(int v){data.counts[i]=(data.counts[i]&0xffff0000)|(v&65535);}});addDataSlot(new DataSlot(){public int get(){return data.counts[i]>>>16;}public void set(int v){data.counts[i]=(data.counts[i]&65535)|((v&65535)<<16);}});}
  addDataSlot(new DataSlot(){public int get(){return data.tier;}public void set(int v){data.tier=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.colors;}public void set(int v){data.colors=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.offhandIndex;}public void set(int v){data.offhandIndex=v;}});
  addDataSlot(new DataSlot(){public int get(){return craftingOpen?1:0;}public void set(int v){craftingOpen=v!=0;}});
  addDataSlot(new DataSlot(){public int get(){return data.addon;}public void set(int v){data.addon=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.vialIndex;}public void set(int v){data.vialIndex=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.brewTime;}public void set(int v){data.brewTime=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.burn;}public void set(int v){data.burn=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.brewFuel;}public void set(int v){data.brewFuel=v;}});
  addDataSlot(new DataSlot(){public int get(){return data.tankIndex;}public void set(int v){data.tankIndex=v==1?1:0;}});
 }
 @Override public void slotsChanged(Container container){if(container==craftGrid)updateCraftResult();}
 private void updateCraftResult(){Player player=inventory.player;Level level=player.level();if(level.isClientSide||!(player instanceof ServerPlayer sp))return;
  ItemStack result=ItemStack.EMPTY;var found=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,craftGrid,level);
  if(found.isPresent()){var recipe=found.get();if(craftResult.setRecipeUsed(level,sp,recipe)){ItemStack assembled=recipe.assemble(craftGrid,level.registryAccess());if(assembled.isItemEnabled(level.enabledFeatures()))result=assembled;}}
  craftResult.setItem(0,result);setRemoteSlot(RESULT,result);sp.connection.send(new ClientboundContainerSetSlotPacket(containerId,incrementStateId(),RESULT,result));
 }
 private void spillCraft(Player p){if(p.level().isClientSide)return;for(int i=0;i<craftGrid.getContainerSize();i++){ItemStack stack=craftGrid.removeItem(i,Integer.MAX_VALUE);if(!stack.isEmpty()&&!p.getInventory().add(stack))p.drop(stack,false);}craftResult.clearContent();if(p instanceof ServerPlayer sp){setRemoteSlot(RESULT,ItemStack.EMPTY);sp.connection.send(new ClientboundContainerSetSlotPacket(containerId,incrementStateId(),RESULT,ItemStack.EMPTY));}}
 @Override public void removed(Player p){super.removed(p);spillCraft(p);}
 public int storageIndex(int id){return id>=0&&id<108?id:id>=EXTRA&&id<EXTRA+144?108+id-EXTRA:-1;}
 private class StorageSlot extends SlotItemHandler {
  StorageSlot(ItemStackHandler handler,int slot,int x,int y){super(handler,slot,x,y);}
  @Override public ItemStack getItem(){ItemStack s=super.getItem();return s.isEmpty()?ItemStack.EMPTY:s.copyWithCount(Math.min(s.getCount(),s.getMaxStackSize()));}
  @Override public int getMaxStackSize(){return 64;}
  @Override public int getMaxStackSize(ItemStack s){return s.getMaxStackSize();}
  @Override public boolean mayPickup(Player p){return data.slotTier==0&&super.mayPickup(p);}
  @Override public boolean mayPlace(ItemStack s){return data.slotTier==0&&super.mayPlace(s);}
  @Override public boolean isSameInventory(Slot other){return other instanceof StorageSlot;}
 }
 private void storageClick(int id,int button,ClickType type,Player player){
  ItemStack stored=data.storage(id),held=getCarried();
  if(type==ClickType.PICKUP){
   if(held.isEmpty())setCarried(data.extractStorage(id,button==1?1:stored.getMaxStackSize(),false));
   else if(stored.isEmpty()||ItemStack.isSameItemSameTags(stored,held)){
    int amount=button==1?1:held.getCount();ItemStack rest=data.insertStorage(id,held.copyWithCount(amount),false);held.shrink(amount-rest.getCount());setCarried(held);
   }else if(stored.getCount()<=stored.getMaxStackSize()&&data.storageHandler(id).isItemValid(data.localSlot(id),held)){
    ItemStack out=data.extractStorage(id,stored.getMaxStackSize(),false);ItemStack rest=data.insertStorage(id,held,false);setCarried(out);if(!rest.isEmpty())player.getInventory().add(rest);
   }
  }else if(type==ClickType.QUICK_MOVE){ItemStack out=data.extractStorage(id,stored.getMaxStackSize(),false);player.getInventory().add(out);if(!out.isEmpty())data.insertStorage(id,out,false);}
  else if(type==ClickType.THROW&&held.isEmpty())player.drop(data.extractStorage(id,button==0?1:stored.getMaxStackSize(),false),true);
  else if(type==ClickType.CLONE&&player.isCreative()&&held.isEmpty()&&!stored.isEmpty())setCarried(stored.copyWithCount(stored.getMaxStackSize()));
  else if(type==ClickType.SWAP&&(button>=0&&button<9||button==40)&&button!=boundSlot){
   ItemStack hot=player.getInventory().getItem(button);
   if(hot.isEmpty())player.getInventory().setItem(button,data.extractStorage(id,stored.getMaxStackSize(),false));
   else if(stored.isEmpty()||ItemStack.isSameItemSameTags(stored,hot))player.getInventory().setItem(button,data.insertStorage(id,hot,false));
  }
  data.save();
 }
 public static final Item[] SLOT_COST_ITEMS={Items.IRON_INGOT,Items.GOLD_INGOT,Items.DIAMOND,Items.EMERALD,Items.NETHERITE_INGOT};
 public static final int[] SLOT_COST_COUNTS={16,16,8,16,4};
 public static final Item[] WARP_COST_ITEMS={Items.NETHER_STAR,Items.NETHERITE_INGOT,Items.ENDER_EYE};
 public static final int[] WARP_COST_COUNTS={2,4,8};
 private boolean upgradeStorage(Player p,int target){
  if(target!=data.slotTier+1||target<1||target>5)return false;
  Item cost=SLOT_COST_ITEMS[target-1];int needed=SLOT_COST_COUNTS[target-1],have=0;
  for(int i=0;i<36;i++){ItemStack stack=p.getInventory().getItem(i);if(stack.is(cost)&&!stack.hasTag())have+=stack.getCount();}
  if(!p.isCreative()&&have<needed)return false;
  if(!p.isCreative())for(int i=0;i<36&&needed>0;i++){ItemStack stack=p.getInventory().getItem(i);if(stack.is(cost)&&!stack.hasTag()){int take=Math.min(needed,stack.getCount());stack.shrink(take);needed-=take;}}
  data.slotTier=target;data.save();broadcastChanges();return true;
 }
 private boolean unlockWarp(Player p){
  if(data.warpUnlocked)return false;
  if(!p.isCreative()){
   for(int c=0;c<WARP_COST_ITEMS.length;c++){
    Item item=WARP_COST_ITEMS[c];int need=WARP_COST_COUNTS[c],have=0;
    for(int i=0;i<36;i++){ItemStack s=p.getInventory().getItem(i);if(s.is(item)&&!s.hasTag())have+=s.getCount();}
    if(have<need)return false;
   }
   for(int c=0;c<WARP_COST_ITEMS.length;c++){
    Item item=WARP_COST_ITEMS[c];int need=WARP_COST_COUNTS[c];
    for(int i=0;i<36&&need>0;i++){ItemStack s=p.getInventory().getItem(i);if(s.is(item)&&!s.hasTag()){int take=Math.min(need,s.getCount());s.shrink(take);need-=take;}}
   }
  }
  data.warpUnlocked=true;data.save();broadcastChanges();return true;
 }
 private void addPlayer(Inventory inv,int i,int x,int y){addSlot(new Slot(inv,i,x,y){@Override public boolean mayPickup(Player p){return i!=boundSlot;}@Override public boolean mayPlace(ItemStack s){return i!=boundSlot;}});}
 @Override public boolean stillValid(Player p){return p.level().isClientSide || (dock!=null ? !dock.isRemoved()&&!dock.pack.isEmpty()&&dock.pack==data.owner&&p.distanceToSqr(dockPos.getX()+.5,dockPos.getY()+.5,dockPos.getZ()+.5)<64 : boundSlot>=0&&PackItem.stack(p,boundSlot)==data.owner);}
 @Override public void broadcastChanges(){if(dock!=null)dock.setChanged();if(!inventory.player.level().isClientSide){PackStorageActions.transferFluidSlots(data,inventory.player);if(craftingOpen&&!data.has(AdventurersPack.CRAFTING.get())){craftingOpen=false;spillCraft(inventory.player);}if(!data.has(AdventurersPack.TANK.get())){for(int n=0;n<4;n++){ItemStack held=data.fluidSlot.getStackInSlot(n).copy();if(!held.isEmpty()){data.fluidSlot.setStackInSlot(n,ItemStack.EMPTY);if(!inventory.player.getInventory().add(held))inventory.player.drop(held,false);}}if(data.addon==14)data.addon=0;}if(!data.has(AdventurersPack.BATTERY.get())){for(int n=0;n<2;n++){ItemStack held=data.energySlots.getStackInSlot(n).copy();if(!held.isEmpty()){data.energySlots.setStackInSlot(n,ItemStack.EMPTY);if(!inventory.player.getInventory().add(held))inventory.player.drop(held,false);}}if(data.addon==16)data.addon=0;}if(data.addon==2&&!data.has(AdventurersPack.QUIVER.get())||data.addon==3&&!data.has(AdventurersPack.PHILTER.get())||data.addon==4&&!data.has(AdventurersPack.ALEMBIC.get())||data.addon==5&&!data.has(AdventurersPack.FURNACE.get())||data.addon==6&&!data.has(AdventurersPack.ENDER.get())||data.addon==17&&!data.has(AdventurersPack.TORCH.get()))data.addon=0;for(int i=0;i<18;i++)ghosts.setStackInSlot(i,data.bulk[i].copy());if(inventory.player instanceof ServerPlayer sp){CompoundTag tag=data.displayTag();int hash=tag.toString().hashCode();if(!displaySent||hash!=displayHash){displaySent=true;displayHash=hash;Network.sendDisplay(sp,containerId,tag);}}}super.broadcastChanges();}
 @Override public void clicked(int id,int button,ClickType type,Player player){
  if(!stillValid(player))return;
  if(data.owner.isEmpty()&&id>=0&&id!=EQUIPMENT&&!(id>=PLAYER&&id<CAROUSEL))return;
  if(type==ClickType.SWAP && button==boundSlot)return;
  if(id>=0&&id<slots.size()&&!slots.get(id).isActive())return;
  if(isSocketIndex(id)&&(type==ClickType.SWAP||type==ClickType.PICKUP_ALL))return;
  if(id==CAMP_SLOT+22&&type==ClickType.PICKUP&&!getCarried().isEmpty()&&data.trash.isItemValid(0,getCarried())){if(!player.level().isClientSide){ItemStack held=getCarried();if(button==1){data.trash.setStackInSlot(0,held.split(1));setCarried(held);}else{data.trash.setStackInSlot(0,held.copy());setCarried(ItemStack.EMPTY);}data.save();broadcastChanges();}return;}
  if(id>=PHILTER&&id<BREW){if(!player.level().isClientSide)vialClick(id-PHILTER,button,type,player);broadcastChanges();return;}
  if(storageIndex(id)>=0&&data.slotTier>0){if(!player.level().isClientSide){storageClick(storageIndex(id),button,type,player);broadcastChanges();}return;}
  if(bulkIndex(id)>=0){if(player.level().isClientSide)return;int b=bulkIndex(id);
   if(type==ClickType.PICKUP){ItemStack held=getCarried();if(!held.isEmpty()){if(button==1){ItemStack one=held.copyWithCount(1);held.shrink(data.deposit(b,one));}else data.deposit(b,held);setCarried(held);}else setCarried(data.withdraw(b,button==1?1:64));}
   else if(type==ClickType.QUICK_MOVE){ItemStack out=data.withdraw(b,64);if(!out.isEmpty()){player.getInventory().add(out);if(!out.isEmpty())data.deposit(b,out);}}
   broadcastChanges();return;
  }
  if(data.owner.isEmpty()&&id>=0&&id!=EQUIPMENT&&!(id>=PLAYER&&id<CAROUSEL))return;
  super.clicked(id,button,type,player);
  if(id==EQUIPMENT&&player.containerMenu==this&&player instanceof ServerPlayer sp){PackEquipment.sync(sp);sp.closeContainer();PackItem.open(sp,41);}
 }
 private boolean wearEmpty(ItemStack stack,int from){
  var equipment=net.minecraft.world.entity.Mob.getEquipmentSlotForItem(stack);
  int dest=equipment.getType()==net.minecraft.world.entity.EquipmentSlot.Type.ARMOR?ARMOR+equipment.getIndex():equipment==net.minecraft.world.entity.EquipmentSlot.OFFHAND?HAND:-1;
  return dest>=0&&from!=dest&&!slots.get(dest).hasItem()&&moveItemStackTo(stack,dest,dest+1,false);
 }
 private boolean toHands(ItemStack stack,boolean hotbarFirst){return moveItemStackTo(stack,PLAYER,ARMOR,hotbarFirst);}
 private ItemStack stashPlayer(ItemStack rest){
  if(rest.is(AdventurersPack.CAMP_KIT.get()))return data.camp.insertItem(0,rest,false);
  if(PouchItem.kind(rest)>=0)return data.pouches.insertItem(PouchItem.kind(rest),rest,false);
  if(rest.getItem() instanceof AdventurersPack.UpgradeItem up){
   if(up.section){for(int i=0;i<data.sections.getSlots()&&!rest.isEmpty();i++)rest=data.sections.insertItem(i,rest,false);return rest;}
   int[] sockets=up.rune?PackData.RUNE_SLOTS:PackData.GEAR_SLOTS;for(int i:sockets){if(rest.isEmpty())break;rest=data.items.insertItem(i,rest,false);}return rest;
  }
  if(data.items.isItemValid(108,rest))for(int tool=108;tool<114&&!rest.isEmpty();tool++)rest=data.items.insertItem(tool,rest,false);
  rest=data.stashTorches(rest);if(rest.isEmpty())return rest;rest=data.stashBrew(rest);return rest.isEmpty()?rest:data.insert(rest,false);
 }
 private ItemStack stashPlayerToOpenTab(ItemStack rest){
  if(rest.is(AdventurersPack.CAMP_KIT.get()))return data.camp.insertItem(0,rest,false);
  if(PouchItem.kind(rest)>=0)return data.pouches.insertItem(PouchItem.kind(rest),rest,false);
  if(rest.getItem() instanceof AdventurersPack.UpgradeItem up){
   if(up.section){for(int i=0;i<data.sections.getSlots()&&!rest.isEmpty();i++)rest=data.sections.insertItem(i,rest,false);return rest;}
   int[] sockets=up.rune?PackData.RUNE_SLOTS:PackData.GEAR_SLOTS;for(int i:sockets){if(rest.isEmpty())break;rest=data.items.insertItem(i,rest,false);}return rest;
  }
  if(data.items.isItemValid(108,rest))for(int tool=108;tool<114&&!rest.isEmpty();tool++)rest=data.items.insertItem(tool,rest,false);
  rest=data.stashTorches(rest);if(rest.isEmpty())return rest;rest=data.stashBrew(rest);if(rest.isEmpty())return rest;
  int b=data.findMatching(rest);if(b>=0&&data.tab==(b<9?0:1)){data.deposit(b,rest);if(rest.isEmpty())return rest;}
  return data.insertIntoTab(data.tab,rest);
 }
 private boolean isSocketIndex(int index){
  if(index<0||index>=slots.size())return false;
  Slot s=slots.get(index);
  if(s instanceof BaseHandlerSlot)return true;
  if(s instanceof SlotItemHandler handler){
   if(handler.getItemHandler()==toolsWrapper||handler.getItemHandler()==modulesWrapper)return true;
   if(handler.getItemHandler()==data.items&&handler.getSlotIndex()>=PackData.TOOLS)return true;
   if(handler.getItemHandler()==data.sections||handler.getItemHandler()==data.pouches||handler.getItemHandler()==data.offhand||handler.getItemHandler()==data.camp||handler.getItemHandler()==data.torches||handler.getItemHandler()==data.trash)return true;
  }
  return false;
 }
 @Override public ItemStack quickMoveStack(Player p,int index){
  if(index<0||index>=slots.size()||!stillValid(p))return ItemStack.EMPTY;Slot slot=slots.get(index);
  if(data.slotTier>0&&storageIndex(index)>=0){if(slot.isActive()&&!p.level().isClientSide)storageClick(storageIndex(index),0,ClickType.QUICK_MOVE,p);return ItemStack.EMPTY;}
  if(!slot.isActive()||!slot.mayPickup(p)||!slot.hasItem()||bulkIndex(index)>=0||isSocketIndex(index))return ItemStack.EMPTY;
  ItemStack stack=slot.getItem(),original=stack.copy();
  if(index==EQUIPMENT){if(!toHands(stack,true))return ItemStack.EMPTY;slot.set(stack.isEmpty()?ItemStack.EMPTY:stack);if(p instanceof ServerPlayer sp){PackEquipment.sync(sp);sp.closeContainer();PackItem.open(sp,41);}return original;}
  if(index>=PLAYER&&index<ARMOR&&stack.getItem() instanceof PackItem&&PackEquipment.stack(p).isEmpty()){PackEquipment.get(p).items.setStackInSlot(0,stack.copyWithCount(1));stack.shrink(1);slot.set(stack.isEmpty()?ItemStack.EMPTY:stack);if(p instanceof ServerPlayer sp){PackEquipment.sync(sp);sp.closeContainer();PackItem.open(sp,41);}return original;}
  if(index==RESULT){int before=stack.getCount();wearEmpty(stack,index);if(!stack.isEmpty())toHands(stack,true);if(stack.getCount()==before)return ItemStack.EMPTY;slot.onQuickCraft(stack,original);slot.onTake(p,stack);if(!stack.isEmpty())p.drop(stack,false);return original;}
  boolean playerSide=index>=PLAYER&&index<CAROUSEL,worn=index>=ARMOR&&index<CAROUSEL;
  if(!worn)wearEmpty(stack,index);
  if(playerSide){
   if(!worn&&data.addon==14&&data.has(AdventurersPack.TANK.get())&&!stack.isEmpty()){
    ItemStack one=stack.copyWithCount(1);
    var fluid=net.minecraftforge.fluids.FluidUtil.getFluidContained(one).orElse(net.minecraftforge.fluids.FluidStack.EMPTY);
    int direction=fluid.isEmpty()?1:0;
    for(int t=0;t<2;t++){
     int target=t*2+direction;var tank=data.tankAt(t);
     if(direction==0&&tank.fill(fluid,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)==0||direction==1&&tank.isEmpty())continue;
     if(data.fluidSlot.getStackInSlot(target).isEmpty()&&data.fluidSlot.isItemValid(target,one)&&data.fluidSlot.insertItem(target,one,false).isEmpty()){stack.shrink(1);break;}
    }
   }
   if(!worn&&data.addon==16&&data.has(AdventurersPack.BATTERY.get())&&!stack.isEmpty()){for(int n=0;n<2&&!stack.isEmpty();n++){if(!data.energySlots.getStackInSlot(n).isEmpty())continue;ItemStack one=stack.copyWithCount(1);if(data.energySlots.isItemValid(n,one)&&data.energySlots.insertItem(n,one,false).isEmpty())stack.shrink(1);}}
   if(!worn&&!data.owner.isEmpty()&&!stack.isEmpty()){
    if(index>=PLAYER+27&&index<ARMOR){
     moveItemStackTo(stack,PLAYER,PLAYER+27,false);
    }else if(index>=PLAYER&&index<PLAYER+27){
     ItemStack rest=stashPlayerToOpenTab(stack.copy());
     if(rest.getCount()!=stack.getCount()){slot.set(rest);stack=slot.getItem();}
     if(!stack.isEmpty()&&stack.getCount()==original.getCount()){moveItemStackTo(stack,PLAYER+27,ARMOR,false);}
    }else{
     ItemStack rest=stashPlayer(stack.copy());
     if(rest.getCount()!=stack.getCount()){slot.set(rest);stack=slot.getItem();}
    }
   }
   if(!stack.isEmpty()&&worn){toHands(stack,false);if(!stack.isEmpty()&&!data.owner.isEmpty()){ItemStack rest=stashPlayer(stack.copy());if(rest.getCount()!=stack.getCount()){slot.set(rest);stack=slot.getItem();}}}
  }else if(!stack.isEmpty())toHands(stack,true);
  if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
  slot.set(stack.isEmpty()?ItemStack.EMPTY:stack);slot.setChanged();int moved=original.getCount()-slot.getItem().getCount();slot.onTake(p,original.copyWithCount(Math.max(0,moved)));data.save();return original;
 }
 @Override public boolean clickMenuButton(Player p,int id){if(!stillValid(p)||data.owner.isEmpty())return false;
  if(id==5400)return unlockWarp(p);
  if(id>=5410&&id<5410+PackData.WARP_COUNT){if(!data.warpUnlocked)return false;data.warpIndex=id-5410;data.save();broadcastChanges();return true;}
  if(id==5420){if(!data.setWarp(p,data.warpIndex))return false;broadcastChanges();return true;}
  if(id==5421){if(!(p instanceof ServerPlayer sp))return data.warpUnlocked&&data.warpSet[data.warpIndex];return data.teleportToWarp(sp,data.warpIndex);}
  if(id==5422){if(!data.clearWarp(data.warpIndex))return false;broadcastChanges();return true;}
  if(id>=5301&&id<=5305)return upgradeStorage(p,id-5300);
  if(id>=5200&&id<=5218||id==5220){
   if(data.addon<8||data.addon>11)return false;
   var pouch=data.pouchStorage[data.addon-8];if(pouch.owner().isEmpty())return false;
   if(id==5220)pouch.cycleFilter();
   else if(id<5209)pouch.setFilter(id-5200,getCarried());
   else if(id>=5210&&id<5219)pouch.setFilter(id-5210,ItemStack.EMPTY);
   else return false;
   broadcastChanges();return true;
  }
  if(id>=5000&&id<=5018){if(id<=5001||id==5012)closeCraft(p);return PackStorageActions.handle(this,p,id);}
  if(id>=4000&&id<4000+slots.size()){
   Slot slot=slots.get(id-4000);
   if(!slot.isActive())return false;
   int targetSlot=-1;
   if(slot instanceof UpgradeSocketSlot us)targetSlot=us.getModuleSlot();
   else if(slot instanceof SlotItemHandler handler&&handler.getItemHandler()==data.items)targetSlot=slot.getSlotIndex();
   if(targetSlot<0||!data.toggleUpgrade(targetSlot))return false;
   broadcastChanges();return true;
  }
  if(id>=0&&id<4){data.tab=id;PackStorageActions.sortCompartment(data);}
  else if(id>=2000&&id<2018)data.selected=id-2000;
  else if(id>=2100&&id<2118)data.protectedMask^=1<<(id-2100);
  else if(id>=1000&&id<1144)data.toggleStorage(id-1000+108);
  else if(id>=3000&&id<3006){if(p instanceof ServerPlayer sp)CampKit.action(sp,data,id-3000);}
  else if(id>=90&&id<=95){closeCraft(p);data.addon=id-82;}
  else if(id==10)data.pickup=!data.pickup;
  else if(id==11)data.voiding=!data.voiding;
  else if(id==12)data.building=!data.building;
  else if(id==13){data.routing=!data.routing;PackStorageActions.autoRoute(data,p,boundSlot);}
  else if(id==14)data.applyPreset(data.preset+1);
  else if(id>=20&&id<29)data.selected=id-20;
  else if(id>=30&&id<39)data.protectedMask^=1<<(id-30);
  else if(id>=60&&id<64)data.cycleColor(id-60);
  else if(id>=64&&id<68)data.cycleRoute(id-64);
  else if(id>=100&&id<208)data.toggleStorage(id-100);
  else if(id==40&&data.has(AdventurersPack.CRAFTING.get())){craftingOpen=!craftingOpen;if(craftingOpen)data.addon=0;if(!craftingOpen)spillCraft(p);}
  else if(id==70){closeCraft(p);data.addon=data.addon==1?0:1;}
  else if(id==71)data.addon=0;
  else if(id==72){closeCraft(p);data.addon=1;}
  else if(id==73&&data.has(AdventurersPack.QUIVER.get())){closeCraft(p);data.addon=data.addon==2?0:2;}
  else if(id==74&&data.has(AdventurersPack.PHILTER.get())){closeCraft(p);data.addon=data.addon==3?0:3;}
  else if(id==75&&data.has(AdventurersPack.ALEMBIC.get())){closeCraft(p);data.addon=data.addon==4?0:4;}
  else if(id==79&&data.has(AdventurersPack.FURNACE.get())){closeCraft(p);data.addon=data.addon==5?0:5;}
  else if(id==80&&data.has(AdventurersPack.ENDER.get())){closeCraft(p);data.addon=data.addon==6?0:6;}
  else if(id==81){closeCraft(p);data.addon=7;}
  else if(id==82&&data.has(AdventurersPack.TORCH.get())){closeCraft(p);data.addon=data.addon==17?0:17;}
  else if(id==76)data.useVial(p);
  else if(id==77)data.vialIndex=Math.floorMod(data.vialIndex+1,3);
  else if(id==78)data.pourFinished();
  else return false;data.save();broadcastChanges();return true;
 }
 private void closeCraft(Player p){if(!craftingOpen)return;craftingOpen=false;spillCraft(p);}
 private void vialClick(int vial,int button,ClickType type,Player player){if(type!=ClickType.PICKUP||!data.has(AdventurersPack.PHILTER.get()))return;ItemStack stored=data.vials.getStackInSlot(vial).copy();ItemStack carried=getCarried();
  if(!carried.isEmpty()){if(!stored.isEmpty()||!PackData.anyPotion(carried))return;data.vials.setStackInSlot(vial,carried.copyWithCount(1));carried.shrink(1);data.vialIndex=vial;if(PackData.drinkable(data.vials.getStackInSlot(vial))){ItemStack bottle=new ItemStack(Items.GLASS_BOTTLE);if(carried.isEmpty())setCarried(bottle);else{setCarried(carried);if(!player.getInventory().add(bottle))player.drop(bottle,false);}return;}setCarried(carried);return;}
  data.vialIndex=vial;if(button!=1||stored.isEmpty())return;if(PackData.drinkable(stored)&&!consumeBottle(player))return;data.vials.setStackInSlot(vial,ItemStack.EMPTY);setCarried(stored);
 }
 private boolean consumeBottle(Player player){var inv=player.getInventory();for(int i=0;i<inv.getContainerSize();i++){ItemStack s=inv.getItem(i);if(s.is(Items.GLASS_BOTTLE)){s.shrink(1);if(s.isEmpty())inv.setItem(i,ItemStack.EMPTY);return true;}}return false;}
}
