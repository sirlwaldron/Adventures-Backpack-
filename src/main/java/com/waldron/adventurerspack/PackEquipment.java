package com.waldron.adventurerspack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;
@Mod.EventBusSubscriber(modid=AdventurersPack.ID)
public final class PackEquipment {
 public static final int SLOT=41;
 public static final Capability<PackEquipment> CAP=CapabilityManager.get(new CapabilityToken<>(){});
 public final ItemStackHandler items=new ItemStackHandler(1){
  public boolean isItemValid(int i,ItemStack s){return s.getItem() instanceof PackItem;}
  public int getSlotLimit(int i){return 1;}
 };
 public static PackEquipment get(Player p){return p.getCapability(CAP).orElseThrow(()->new IllegalStateException("Missing backpack equipment"));}
 /** Survives the capability optional being invalidated when the old player is removed during respawn. */
 private static final java.util.Map<Player,PackEquipment> ATTACHED=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
 public final ItemStackHandler tool=new ItemStackHandler(1);
 public boolean toolEnabled;
 public int toolHotbarSlot=-1;
 public static ItemStack stack(Player p){return get(p).items.getStackInSlot(0);}
 private static final java.util.Map<Player,ItemStack> SENT=new java.util.WeakHashMap<>();
 public static ItemStack visualStack(ItemStack full){
  if(full.isEmpty())return ItemStack.EMPTY;
  ItemStack visual=new ItemStack(full.getItem());
  if(!full.hasTag())return visual;
  CompoundTag srcPack=full.getTag().getCompound("Pack");
  CompoundTag pack=new CompoundTag();
  pack.putInt("Tier",srcPack.getInt("Tier"));
  if(srcPack.contains("CampKit"))pack.put("CampKit",srcPack.getCompound("CampKit"));
  if(srcPack.contains("Pouches"))pack.put("Pouches",srcPack.getCompound("Pouches"));
  if(srcPack.contains("Sections"))pack.put("Sections",srcPack.getCompound("Sections"));
  if(srcPack.contains("Items")){
   CompoundTag srcItems=srcPack.getCompound("Items");
   CompoundTag dstItems=new CompoundTag();
   if(srcItems.contains("Size"))dstItems.putInt("Size",srcItems.getInt("Size"));
   ListTag srcList=srcItems.getList("Items",10);
   ListTag dstList=new ListTag();
   boolean hadTool=false;
   for(int i=0;i<srcList.size();i++){
    CompoundTag itemTag=srcList.getCompound(i);
    int slot=itemTag.getByte("Slot")&255;
    if(slot>=114&&slot<PackData.TOTAL){dstList.add(itemTag.copy());}
    else if(slot>=108&&slot<114&&!hadTool){
     CompoundTag dummy=new CompoundTag();
     dummy.putByte("Slot",(byte)108);dummy.putString("id","minecraft:iron_sword");dummy.putByte("Count",(byte)1);
     dstList.add(dummy);hadTool=true;
    }
   }
   dstItems.put("Items",dstList);
   pack.put("Items",dstItems);
  }
  visual.getOrCreateTag().put("Pack",pack);
  return visual;
 }
 public static void sync(ServerPlayer p){
  ItemStack current=stack(p),old=SENT.get(p);
  if(old!=null&&ItemStack.matches(old,current))return;
  SENT.put(p,current.copy());
  Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.Equipment(p.getId(),current.copy()));
  Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(()->p),new Network.Equipment(p.getId(),visualStack(current)));
 }
 @SubscribeEvent public static void startTracking(PlayerEvent.StartTracking e){
  if(e.getTarget() instanceof ServerPlayer tracked&&e.getEntity() instanceof ServerPlayer tracker){
   ItemStack current=stack(tracked);
   if(!current.isEmpty()){
    Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->tracker),new Network.Equipment(tracked.getId(),visualStack(current)));
   }
  }
 }
 @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> e){if(!(e.getObject() instanceof Player player))return;var value=new PackEquipment();ATTACHED.put(player,value);var optional=LazyOptional.of(()->value);e.addCapability(new ResourceLocation(AdventurersPack.ID,"equipment"),new ICapabilitySerializable<CompoundTag>(){
  public <T> LazyOptional<T> getCapability(Capability<T> c,net.minecraft.core.Direction side){return c==CAP?optional.cast():LazyOptional.empty();}
  public CompoundTag serializeNBT(){var n=value.items.serializeNBT();n.put("EquippedTool",value.tool.serializeNBT());return n;}
  public void deserializeNBT(CompoundTag n){value.items.deserializeNBT(n);if(n.contains("EquippedTool"))value.tool.deserializeNBT(n.getCompound("EquippedTool"));}
 });e.addListener(optional::invalidate);}
 @SubscribeEvent public static void clone(PlayerEvent.Clone e){PackEquipment from=ATTACHED.get(e.getOriginal()),to=ATTACHED.get(e.getEntity());if(from!=null&&to!=null){to.items.deserializeNBT(from.items.serializeNBT());to.tool.deserializeNBT(from.tool.serializeNBT());}}
 @SubscribeEvent public static void drops(LivingDropsEvent e){if(e.getEntity() instanceof ServerPlayer p&&!p.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)){ItemStack s=stack(p);if(!s.isEmpty()){get(p).items.setStackInSlot(0,ItemStack.EMPTY);e.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),s));}}}
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)sync(p);}
 @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p){SENT.remove(p);sync(p);}}
 @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p){SENT.remove(p);sync(p);}}
}
