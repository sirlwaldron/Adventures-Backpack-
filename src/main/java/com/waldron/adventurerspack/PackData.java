package com.waldron.adventurerspack;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.Tags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidUtil;
import java.util.Arrays;
import java.util.function.Predicate;

/** Bulk entries use a count-one template and a separate integer, never oversized vanilla stacks. */
public class PackData {
 public static final int TABS=4, PAGE=27, STORAGE=108, TOOLS=108, MODULES=114, TOTAL=125;
 public static final int[] GEAR_SLOTS={114,115,116,120,121,124};
 public static final int[] RUNE_SLOTS={117,118,119,122,123};
 public static final int TRAVEL=0, MINING=1, BUILDING=2, COMBAT=3;
 public static final int ROUTE_ORE=1, ROUTE_BLOCK=2, ROUTE_GEAR=4, ROUTE_LOOT=8;
 public static final int DEFAULT_ROUTES=1|(2<<4)|(8<<8)|(4<<12);
 public static final int DEFAULT_COLORS=0|(1<<3)|(5<<6)|(3<<9);
 public static final String[] DEFAULT_NAMES={"Mine","Build","Loot","Tools"};
 public static final String[] PRESET_NAMES={"Travel","Mining","Building","Tools"};
 public static final int[] PALETTE={0x8B5A2B,0xC4A574,0x6B7C3A,0x7A2E2E,0x3E5C76,0xD4AF37,0x6B4C7A,0xE7D1A4};
 private static final int[] ROUTE_CYCLE={1,2,4,8,3,15,0};
 private static final int[] GOAL_STEPS={64,256,1000,2000,4096,0};
 public final ItemStack owner;
 public final ItemStack[] bulk=new ItemStack[18];
 public final int[] counts=new int[18];
 public final String[] tabNames=DEFAULT_NAMES.clone();
 public final Item[] projectItems=new Item[4];
 public final int[] projectGoals=new int[4];
 public static final int WARP_COUNT=10,WARP_XP_COST=100;
 public boolean warpUnlocked;
 public int warpIndex;
 public final boolean[] warpSet=new boolean[WARP_COUNT];
 public final String[] warpNames=new String[WARP_COUNT],warpDims=new String[WARP_COUNT];
 public final int[] warpX=new int[WARP_COUNT],warpY=new int[WARP_COUNT],warpZ=new int[WARP_COUNT];
 public final float[] warpYaw=new float[WARP_COUNT],warpPitch=new float[WARP_COUNT];
 public int slotTier;
 public static int slotCapacityForTier(int tier){int t=Math.max(0,Math.min(5,tier));return t==0?64:3*(64<<t);}
 public int slotCapacity(){return slotCapacityForTier(slotTier);}
 public final int[] displayStorageCounts=new int[252];
 public int displayQuiverCount,displayTorchCount;
 public int tab,selected,burn,cook,tier,preset,routes=DEFAULT_ROUTES,colors=DEFAULT_COLORS,offhandIndex,addon,brewTime,brewFuel,vialIndex;
 public boolean pickup=true,voiding=false,building=false,routing=false;
 public int cookTotal=200,burnTotal=0,xpBank=0;
 public static final int FLUID_CAPACITY=128000,XP_CAPACITY=200000,ENERGY_CAPACITY=1000000,ENERGY_RATE=1000;
 public final net.minecraftforge.fluids.capability.templates.FluidTank tank=new net.minecraftforge.fluids.capability.templates.FluidTank(FLUID_CAPACITY){
  @Override protected void onContentsChanged(){if(!loading)save();}
 };
 public final net.minecraftforge.fluids.capability.templates.FluidTank tankTwo=new net.minecraftforge.fluids.capability.templates.FluidTank(FLUID_CAPACITY){
  @Override protected void onContentsChanged(){if(!loading)save();}
 };
 public int tankIndex,energy;
 public net.minecraftforge.fluids.capability.templates.FluidTank tankAt(int index){return index==1?tankTwo:tank;}
 public int protectedMask=0; public long protectLo,protectHi; public float storedXp;
 private boolean loading=true;
 public final ItemStackHandler items=new StackStorageHandler(this,TOTAL,108){
  @Override public void deserializeNBT(CompoundTag nbt){super.deserializeNBT(nbt);if(getSlots()==TOTAL)return;ItemStack[] kept=new ItemStack[Math.min(getSlots(),TOTAL)];for(int i=0;i<kept.length;i++)kept[i]=getStackInSlot(i);setSize(TOTAL);for(int i=0;i<kept.length;i++)setStackInSlot(i,kept[i]==null?ItemStack.EMPTY:kept[i]);}
  @Override protected void onContentsChanged(int slot){if(!loading)save();}
  @Override public boolean isItemValid(int slot,ItemStack s){
   if(!safe(s))return false;
   if(slot>=MODULES)return moduleAllowed(slot,s);
   if(slot>=TOOLS)return s.getItem() instanceof TieredItem || s.getItem() instanceof ShearsItem || s.getItem() instanceof TridentItem || Requiem.form(s) >= 0;
   return true;
  }
  @Override public int getSlotLimit(int s){return s>=TOOLS?1:slotCapacity();}
 };
 public final ItemStackHandler extra=new StackStorageHandler(this,144,144){
  protected void onContentsChanged(int i){if(!loading)save();}
  public boolean isItemValid(int i,ItemStack s){return safe(s)&&i%36<(pageSize()-27);}
 };
 public long[] extraProtect=new long[3];
 public int pageSize(){return 27+18*(tier/2)+9*(tier%2);}
 public int storageId(int page,int cell){return cell<27?page*27+cell:108+page*36+cell-27;}
 public boolean storageOpen(int id){return id>=0&&id<252&&(id<108||(id-108)%36<pageSize()-27);}
 public net.minecraftforge.items.ItemStackHandler storageHandler(int id){return id<108?items:extra;}
 public int localSlot(int id){return id<108?id:id-108;}
 public ItemStack storage(int id){return storageHandler(id).getStackInSlot(localSlot(id));}
 public ItemStack insertStorage(int id,ItemStack s,boolean simulate){return storageOpen(id)?storageHandler(id).insertItem(localSlot(id),s,simulate):s;}
 public ItemStack extractStorage(int id,int amount,boolean simulate){return storageHandler(id).extractItem(localSlot(id),amount,simulate);}
 public final ItemStackHandler pouches=new ItemStackHandler(4){
  protected void onContentsChanged(int i){if(!loading)save();}
  public int getSlotLimit(int i){return 1;}
  public boolean isItemValid(int i,ItemStack s){return PouchItem.kind(s)==i;}
 };
 public final PouchInventory[] pouchStorage={new PouchInventory(this,0),new PouchInventory(this,1),new PouchInventory(this,2),new PouchInventory(this,3)};
 public final ItemStackHandler camp=new ItemStackHandler(1){
  protected void onContentsChanged(int i){if(!loading)save();}
  public int getSlotLimit(int i){return 1;}
  public boolean isItemValid(int i,ItemStack s){return s.is(AdventurersPack.CAMP_KIT.get());}
 };
 public final ItemStackHandler fluidSlot=new ItemStackHandler(4){
  protected void onContentsChanged(int i){if(!loading)save();}
  public int getSlotLimit(int i){return 1;}
  public boolean isItemValid(int i,ItemStack s){return fluidContainer(s);}
 };
 public final ItemStackHandler energySlots=new ItemStackHandler(2){
  protected void onContentsChanged(int i){if(!loading)save();}
  public int getSlotLimit(int i){return 1;}
  public boolean isItemValid(int i,ItemStack s){return energyItem(s);}
 };
 public static boolean fluidContainer(ItemStack s){return !s.isEmpty()&&!(s.getItem() instanceof PackItem)&&!hasNestedPack(s)&&FluidUtil.getFluidHandler(s).isPresent();}
 public static boolean energyItem(ItemStack s){return !s.isEmpty()&&!(s.getItem() instanceof PackItem)&&!hasNestedPack(s)&&s.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent();}
 public ItemStack stashPouches(ItemStack input){ItemStack rest=input;for(var h:pouchStorage)if(h.isItemValid(0,rest))for(int pass=0;pass<2;pass++)for(int i=0;i<18;i++){if((pass==0)==!h.getStackInSlot(i).isEmpty())rest=h.insertItem(i,rest,false);if(rest.isEmpty())return rest;}return rest;}
 public final ItemStackHandler furnace=new ItemStackHandler(3){
  @Override protected void onContentsChanged(int slot){if(!loading){if(slot==0)cook=0;save();}}
  @Override public boolean isItemValid(int slot,ItemStack s){return slot!=2&&safe(s);}
 };
 public final ItemStackHandler offhand=new ItemStackHandler(4){
  @Override protected void onContentsChanged(int slot){if(!loading)save();}
  @Override public boolean isItemValid(int slot,ItemStack s){return safe(s);}
 };
 public final ItemStackHandler sections=new ItemStackHandler(6){
  @Override public void deserializeNBT(CompoundTag nbt){super.deserializeNBT(nbt);if(getSlots()==6)return;ItemStack[] kept=new ItemStack[Math.min(getSlots(),6)];for(int i=0;i<kept.length;i++)kept[i]=getStackInSlot(i);setSize(6);for(int i=0;i<kept.length;i++)setStackInSlot(i,kept[i]==null?ItemStack.EMPTY:kept[i]);}
  @Override protected void onContentsChanged(int slot){if(!loading)save();}
  @Override public boolean isItemValid(int slot,ItemStack s){return sectionAllowed(slot,s);}
  @Override public int getSlotLimit(int slot){return 1;}
 };
 public final ItemStackHandler quiver=new ItemStackHandler(9){
  @Override protected void onContentsChanged(int slot){if(!loading)save();}
  @Override public boolean isItemValid(int slot,ItemStack s){return ammo(s);}
 };
 public final ItemStackHandler torches=new ItemStackHandler(9){@Override protected void onContentsChanged(int slot){if(!loading)save();}@Override public boolean isItemValid(int slot,ItemStack s){return torch(s);}};
 public final ItemStackHandler trash=new ItemStackHandler(1){@Override protected void onContentsChanged(int slot){if(!loading)save();}@Override public boolean isItemValid(int slot,ItemStack s){return trashable(s);}};
 public final ItemStackHandler vials=new ItemStackHandler(3){
  @Override protected void onContentsChanged(int slot){if(!loading)save();}
  @Override public boolean isItemValid(int slot,ItemStack s){return anyPotion(s);}
  @Override public int getSlotLimit(int slot){return 1;}
 };
 public final ItemStackHandler brew=new ItemStackHandler(5){
  @Override protected void onContentsChanged(int slot){if(!loading){if(slot<4)brewTime=0;save();}}
  @Override public boolean isItemValid(int slot,ItemStack s){return brewSlot(slot,s);}
 };
 public PackData(ItemStack stack){owner=stack;Arrays.fill(bulk,ItemStack.EMPTY);load(stack.hasTag()?stack.getTag().getCompound("Pack"):new CompoundTag());loading=false;}
 public static boolean safe(ItemStack s){return !s.isEmpty()&&!(s.getItem() instanceof PackItem)&&s.getItem().canFitInsideContainerItems()&&!s.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()&&!hasNestedPack(s);}
 public static boolean hasNestedPack(ItemStack s){if(!s.hasTag()||!s.getTag().contains("Packed",10))return false;CompoundTag packed=s.getTag().getCompound("Packed");for(String partKey:new String[]{"Part2","Part4"})if(packed.contains(partKey,10)){CompoundTag part=packed.getCompound(partKey);if(part.contains("Items",9)){ListTag list=part.getList("Items",10);for(int i=0;i<list.size();i++){ItemStack nested=ItemStack.of(list.getCompound(i));if(nested.getItem() instanceof PackItem||!safe(nested))return true;}}}return false;}
 public static boolean bulkable(ItemStack s){return safe(s)&&s.isStackable()&&!s.hasTag()&&!(s.getItem() instanceof AdventurersPack.UpgradeItem);}
 public static boolean trashable(ItemStack s){return safe(s)&&!(s.getItem() instanceof AdventurersPack.UpgradeItem)&&!s.is(AdventurersPack.CAMP_KIT.get())&&PouchItem.kind(s)<0&&Requiem.form(s)<0;}
 public static int tierOf(ItemStack s){if(s==null||!s.hasTag())return 0;return Math.max(0,Math.min(4,s.getTag().getCompound("Pack").getInt("Tier")));}
 public static String cleanName(String value,String fallback){if(value==null)return fallback;String s=value.replaceAll("[\\p{Cntrl}]","").trim();if(s.isEmpty())return fallback;if("Combat".equalsIgnoreCase(s))return "Tools";return s.length()>12?s.substring(0,12):s;}
 public static int category(ItemStack s){if(s.isEmpty())return 0;Item item=s.getItem();
  if(item instanceof SwordItem||item instanceof AxeItem||item instanceof BowItem||item instanceof CrossbowItem||item instanceof TridentItem||item instanceof ArmorItem||item instanceof ShieldItem||item instanceof ArrowItem||item instanceof TieredItem||item instanceof ShearsItem||item instanceof FishingRodItem)return ROUTE_GEAR;
  if(s.isEdible())return ROUTE_LOOT;
  if(s.is(Tags.Items.ORES)||s.is(Tags.Items.RAW_MATERIALS)||item==Items.COBBLESTONE||item==Items.COBBLED_DEEPSLATE||item==Items.STONE||item==Items.DEEPSLATE||item==Items.NETHERRACK||item==Items.OBSIDIAN)return ROUTE_ORE;
  if(item==Items.DIAMOND||item==Items.EMERALD||item==Items.ANCIENT_DEBRIS||item==Items.NETHERITE_INGOT||item==Items.NETHERITE_SCRAP||item==Items.GOLD_INGOT||item==Items.IRON_INGOT||item==Items.ENCHANTED_BOOK||(s.hasTag()&&!(item instanceof BlockItem)))return ROUTE_LOOT;
  if(item instanceof BlockItem)return ROUTE_BLOCK;
  return 0;
 }
 public static String routeName(int mask){if(mask==0)return "Selected";if(mask==15)return "Any";String text="";if((mask&ROUTE_ORE)!=0)text+="Ore ";if((mask&ROUTE_BLOCK)!=0)text+="Blocks ";if((mask&ROUTE_GEAR)!=0)text+="Gear ";if((mask&ROUTE_LOOT)!=0)text+="Loot ";return text.trim();}
 public static boolean moduleSlot(int slot){return gearSlot(slot)||runeSlot(slot);}
 public static boolean gearSlot(int slot){for(int i:GEAR_SLOTS)if(i==slot)return true;return false;}
 public static boolean runeSlot(int slot){for(int i:RUNE_SLOTS)if(i==slot)return true;return false;}
 private boolean moduleAllowed(int slot,ItemStack s){if(!(s.getItem() instanceof AdventurersPack.UpgradeItem up)||up.section||up.rune!=runeSlot(slot)||!moduleSlot(slot)||!socketOpen(slot))return false;for(int i=MODULES;i<TOTAL;i++)if(i!=slot&&items.getStackInSlot(i).is(s.getItem()))return false;return true;}
 private boolean sectionAllowed(int slot,ItemStack s){if(s.getItem() instanceof PouchItem)return false;if(!(s.getItem() instanceof AdventurersPack.UpgradeItem up)||!up.section||!sectionOpen(slot))return false;for(int i=0;i<sections.getSlots();i++)if(i!=slot&&sections.getStackInSlot(i).is(s.getItem()))return false;return true;}
 public int openGear(){return GEAR_SLOTS.length;}
 public int openRunes(){return RUNE_SLOTS.length;}
 public int openSections(){return 6;}
 public boolean sectionOpen(int slot){if(slot<0||slot>=sections.getSlots())return false;int used=0;for(int i=0;i<sections.getSlots();i++)if(i!=slot&&!sections.getStackInSlot(i).isEmpty())used++;return used<openSections();}
 public static boolean ammo(ItemStack s){return safe(s)&&(s.getItem() instanceof ArrowItem||s.is(Items.FIREWORK_ROCKET));}
 public static boolean torch(ItemStack s){return safe(s)&&(s.is(Items.TORCH)||s.is(Items.SOUL_TORCH)||(s.getItem() instanceof BlockItem bi&&bi.getBlock() instanceof net.minecraft.world.level.block.TorchBlock&&!(bi.getBlock() instanceof net.minecraft.world.level.block.RedstoneTorchBlock)));}
 public static boolean anyPotion(ItemStack s){return safe(s)&&s.getItem() instanceof PotionItem;}
 public static boolean drinkable(ItemStack s){return anyPotion(s)&&!(s.getItem() instanceof ThrowablePotionItem);}
 public static boolean finishedPotion(ItemStack s){if(!anyPotion(s))return false;Potion potion=PotionUtils.getPotion(s);return potion!=Potions.EMPTY&&potion!=Potions.WATER&&potion!=Potions.AWKWARD&&potion!=Potions.THICK&&potion!=Potions.MUNDANE;}
 private static boolean brewSlot(int slot,ItemStack s){if(!safe(s))return false;if(slot<3)return s.getItem() instanceof PotionItem||s.is(Items.GLASS_BOTTLE);if(slot==3)return PotionBrewing.isIngredient(s);return s.is(Items.BLAZE_POWDER);}
 public boolean socketOpen(int slot){int[] group=gearSlot(slot)?GEAR_SLOTS:runeSlot(slot)?RUNE_SLOTS:null;if(group==null)return false;int limit=group==GEAR_SLOTS?openGear():openRunes(),used=0;for(int i:group)if(i!=slot&&!items.getStackInSlot(i).isEmpty())used++;return used<limit;}
 public int colorIndex(int t){return (colors>>>(t*3))&7;}
 public int colorRgb(int t){return PALETTE[colorIndex(t)];}
 public void cycleColor(int t){int next=(colorIndex(t)+1)&7;int shift=t*3;colors=(colors&~(7<<shift))|(next<<shift);}
 public int routeOf(int t){return (routes>>>(t*4))&15;}
 public void cycleRoute(int t){int cur=routeOf(t),next=ROUTE_CYCLE[0];for(int i=0;i<ROUTE_CYCLE.length;i++)if(ROUTE_CYCLE[i]==cur){next=ROUTE_CYCLE[(i+1)%ROUTE_CYCLE.length];break;}int shift=t*4;routes=(routes&~(15<<shift))|((next&15)<<shift);}
 public boolean tabHasItem(int t,ItemStack s){if(t<0||t>=TABS||s.isEmpty())return false;for(int j=0;j<pageSize();j++){int id=storageId(t,j);ItemStack existing=storage(id);if(!existing.isEmpty()&&ItemStack.isSameItemSameTags(existing,s))return true;}return false;}
 public ItemStack insertIntoTab(int t,ItemStack input){ItemStack rest=input;for(int pass=0;pass<2&&!rest.isEmpty();pass++)for(int j=0;j<pageSize()&&!rest.isEmpty();j++){int i=storageId(t,j);if((pass==0)==!storage(i).isEmpty())rest=insertStorage(i,rest,false);}return rest;}
 public int destination(ItemStack s){if(!routing)return tab;for(int t=0;t<TABS;t++)if(tabHasItem(t,s))return t;return tab;}
 public void applyPreset(int id){preset=Math.floorMod(id,4);if(preset==MINING||preset==BUILDING)pickup=true;if(preset!=BUILDING)building=false;if(preset==BUILDING)building=true;save();}
 public boolean protectedStorage(int slot){if(slot<0||slot>=252)return false;if(slot>=108)return (extraProtect[(slot-108)/64]&(1L<<((slot-108)%64)))!=0;return slot<64?(protectLo&(1L<<slot))!=0:(protectHi&(1L<<(slot-64)))!=0;}
 public void toggleStorage(int slot){if(slot<0||slot>=252)return;if(slot>=108){extraProtect[(slot-108)/64]^=1L<<((slot-108)%64);save();return;}if(slot<64)protectLo^=1L<<slot;else protectHi^=1L<<(slot-64);save();}
 private void load(CompoundTag n){slotTier=Math.max(0,Math.min(5,n.getInt("SlotTier")));tank.readFromNBT(n.getCompound("FluidTank"));if(n.contains("FluidTank2"))tankTwo.readFromNBT(n.getCompound("FluidTank2"));tankIndex=n.getInt("TankIndex")==1?1:0;energy=Math.max(0,Math.min(ENERGY_CAPACITY,n.getInt("Energy")));if(n.contains("EnergySlots"))energySlots.deserializeNBT(n.getCompound("EnergySlots"));if(n.contains("FluidSlot")){CompoundTag slots=n.getCompound("FluidSlot").copy();slots.putInt("Size",4);fluidSlot.deserializeNBT(slots);}xpBank=Math.max(0,Math.min(XP_CAPACITY,n.getInt("XpBank")));cookTotal=Math.max(1,n.contains("CookTotal")?n.getInt("CookTotal"):200);burnTotal=Math.max(0,n.getInt("BurnTotal"));if(n.contains("Pouches"))pouches.deserializeNBT(n.getCompound("Pouches"));if(n.contains("CampKit"))camp.deserializeNBT(n.getCompound("CampKit"));if(n.contains("Extra"))extra.deserializeNBT(n.getCompound("Extra"));long[] ep=n.getLongArray("ExtraProtect");System.arraycopy(ep,0,extraProtect,0,Math.min(3,ep.length));if(n.contains("Furnace"))furnace.deserializeNBT(n.getCompound("Furnace"));if(n.contains("Items"))items.deserializeNBT(n.getCompound("Items"));if(n.contains("Offhand"))offhand.deserializeNBT(n.getCompound("Offhand"));if(n.contains("Sections"))sections.deserializeNBT(n.getCompound("Sections"));if(n.contains("Quiver"))quiver.deserializeNBT(n.getCompound("Quiver"));if(n.contains("Torches"))torches.deserializeNBT(n.getCompound("Torches"));if(n.contains("Trash"))trash.deserializeNBT(n.getCompound("Trash"));if(n.contains("Vials"))vials.deserializeNBT(n.getCompound("Vials"));if(n.contains("Brew"))brew.deserializeNBT(n.getCompound("Brew"));brewTime=Math.max(0,n.getInt("BrewTime"));brewFuel=Math.max(0,n.getInt("BrewFuel"));vialIndex=Math.floorMod(n.getInt("Vial"),3);
  tab=Math.max(0,Math.min(3,n.getInt("Tab")));selected=Math.floorMod(n.getInt("Selected"),18);tier=Math.max(0,Math.min(4,n.getInt("Tier")));preset=Math.floorMod(n.getInt("Preset"),4);offhandIndex=Math.floorMod(n.getInt("OffIndex"),4);
  pickup=!n.contains("Pickup")||n.getBoolean("Pickup");voiding=n.getBoolean("Voiding");building=n.getBoolean("Building");routing=n.getBoolean("Routing");protectedMask=n.getInt("Protected");protectLo=n.getLong("ProtectLo");protectHi=n.getLong("ProtectHi");burn=n.getInt("Burn");cook=n.getInt("Cook");storedXp=n.getFloat("Xp");
  routes=n.contains("Routes")?n.getInt("Routes"):DEFAULT_ROUTES;colors=n.contains("Colors")?n.getInt("Colors"):DEFAULT_COLORS;
  ListTag names=n.getList("Names",Tag.TAG_STRING);for(int i=0;i<Math.min(TABS,names.size());i++)tabNames[i]=cleanName(names.getString(i),DEFAULT_NAMES[i]);
  ListTag projects=n.getList("Projects",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(4,projects.size());i++){CompoundTag p=projects.getCompound(i);projectGoals[i]=Math.max(0,Math.min(1000000,p.getInt("Goal")));if(p.contains("Id")){ResourceLocation key=ResourceLocation.tryParse(p.getString("Id"));Item item=key==null?null:ForgeRegistries.ITEMS.getValue(key);projectItems[i]=item==null||item==Items.AIR?null:item;}}
  ListTag list=n.getList("Bulk",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(18,list.size());i++){var b=list.getCompound(i);ItemStack s=ItemStack.of(b.getCompound("Item"));int c=Math.max(0,Math.min(1000000,b.getInt("Count")));if(!s.isEmpty()&&c>0){s.setCount(1);bulk[i]=s;counts[i]=c;}}
  normalizeTabOrder();
  loadWarps(n);
 }
 private void normalizeTabOrder(){
  for(int i=0;i<TABS;i++)if("Combat".equalsIgnoreCase(tabNames[i]))tabNames[i]="Tools";
  if("Tools".equalsIgnoreCase(tabNames[2])&&"Loot".equalsIgnoreCase(tabNames[3])){tabNames[2]="Loot";tabNames[3]="Tools";}
 }
 private void loadWarps(CompoundTag n){
  warpUnlocked=n.getBoolean("WarpUnlocked");
  warpIndex=Math.max(0,Math.min(WARP_COUNT-1,n.getInt("WarpIndex")));
  for(int i=0;i<WARP_COUNT;i++){warpSet[i]=false;warpNames[i]="Warp "+(i+1);warpDims[i]="";warpX[i]=warpY[i]=warpZ[i]=0;warpYaw[i]=warpPitch[i]=0;}
  ListTag warps=n.getList("Warps",Tag.TAG_COMPOUND);
  for(int i=0;i<Math.min(WARP_COUNT,warps.size());i++){
   CompoundTag w=warps.getCompound(i);
   warpNames[i]=cleanName(w.getString("Name"),"Warp "+(i+1));
   if(w.getBoolean("Set")&&!w.getString("Dim").isEmpty()){
    warpSet[i]=true;warpDims[i]=w.getString("Dim");
    warpX[i]=w.getInt("X");warpY[i]=w.getInt("Y");warpZ[i]=w.getInt("Z");
    warpYaw[i]=w.getFloat("Yaw");warpPitch[i]=w.getFloat("Pitch");
   }
  }
 }
 private void saveWarps(CompoundTag n){
  n.putBoolean("WarpUnlocked",warpUnlocked);
  n.putInt("WarpIndex",warpIndex);
  ListTag warps=new ListTag();
  for(int i=0;i<WARP_COUNT;i++){
   CompoundTag w=new CompoundTag();
   w.putString("Name",cleanName(warpNames[i],"Warp "+(i+1)));
   w.putBoolean("Set",warpSet[i]);
   if(warpSet[i]){
    w.putString("Dim",warpDims[i]==null?"":warpDims[i]);
    w.putInt("X",warpX[i]);w.putInt("Y",warpY[i]);w.putInt("Z",warpZ[i]);
    w.putFloat("Yaw",warpYaw[i]);w.putFloat("Pitch",warpPitch[i]);
   }
   warps.add(w);
  }
  n.put("Warps",warps);
 }
 public void save(){if(loading||owner.isEmpty())return;CompoundTag n=new CompoundTag();n.put("Extra",extra.serializeNBT());n.putLongArray("ExtraProtect",extraProtect);n.putInt("Schema",4);n.put("FluidTank",tank.writeToNBT(new CompoundTag()));n.put("FluidTank2",tankTwo.writeToNBT(new CompoundTag()));n.putInt("TankIndex",tankIndex);n.putInt("Energy",energy);n.put("EnergySlots",energySlots.serializeNBT());n.put("FluidSlot",fluidSlot.serializeNBT());n.putInt("XpBank",xpBank);n.putInt("CookTotal",cookTotal);n.putInt("BurnTotal",burnTotal);n.put("Pouches",pouches.serializeNBT());n.put("CampKit",camp.serializeNBT());n.put("Furnace",furnace.serializeNBT());n.put("Items",items.serializeNBT());n.put("Offhand",offhand.serializeNBT());n.put("Sections",sections.serializeNBT());n.put("Quiver",quiver.serializeNBT());n.put("Torches",torches.serializeNBT());n.put("Trash",trash.serializeNBT());n.put("Vials",vials.serializeNBT());n.put("Brew",brew.serializeNBT());n.putInt("Tab",tab);n.putInt("Selected",selected);n.putLongArray("ExtraProtect",extraProtect);n.putInt("Tier",tier);n.putInt("SlotTier",slotTier);n.putInt("Preset",preset);n.putInt("OffIndex",offhandIndex);n.putBoolean("Pickup",pickup);n.putBoolean("Voiding",voiding);n.putBoolean("Building",building);n.putBoolean("Routing",routing);n.putInt("Protected",protectedMask);n.putLong("ProtectLo",protectLo);n.putLong("ProtectHi",protectHi);n.putInt("Burn",burn);n.putInt("Cook",cook);n.putFloat("Xp",storedXp);n.putInt("Routes",routes);n.putInt("Colors",colors);n.putInt("BrewTime",brewTime);n.putInt("BrewFuel",brewFuel);n.putInt("Vial",vialIndex);
  ListTag names=new ListTag();for(String name:tabNames)names.add(StringTag.valueOf(name));n.put("Names",names);
  ListTag projects=new ListTag();for(int i=0;i<4;i++){CompoundTag p=new CompoundTag();if(projectItems[i]!=null){ResourceLocation id=ForgeRegistries.ITEMS.getKey(projectItems[i]);if(id!=null)p.putString("Id",id.toString());}p.putInt("Goal",projectGoals[i]);projects.add(p);}n.put("Projects",projects);
  ListTag list=new ListTag();for(int i=0;i<18;i++){var b=new CompoundTag();b.put("Item",bulk[i].save(new CompoundTag()));b.putInt("Count",counts[i]);list.add(b);}n.put("Bulk",list);saveWarps(n);owner.getOrCreateTag().put("Pack",n);
 }
 public CompoundTag displayTag(){CompoundTag n=new CompoundTag();int[] sc=new int[252];for(int i=0;i<252;i++)sc[i]=storage(i).getCount();n.putIntArray("StorageCounts",sc);int qc=0;for(int i=0;i<quiver.getSlots();i++)qc+=quiver.getStackInSlot(i).getCount();n.putInt("QuiverCount",qc);int tc=0;for(int i=0;i<torches.getSlots();i++)tc+=torches.getStackInSlot(i).getCount();n.putInt("TorchCount",tc);n.putBoolean("Empty",owner.isEmpty());n.put("FluidTank",tank.writeToNBT(new CompoundTag()));n.put("FluidTank2",tankTwo.writeToNBT(new CompoundTag()));n.putInt("TankIndex",tankIndex);n.putInt("Energy",energy);n.putInt("XpBank",xpBank);n.putInt("Cook",cook);n.putInt("CookTotal",cookTotal);n.putInt("BurnTotal",burnTotal);n.putLongArray("ExtraProtect",extraProtect);n.putInt("Tier",tier);n.putInt("SlotTier",slotTier);n.putInt("Preset",preset);n.putInt("OffIndex",offhandIndex);n.putBoolean("Routing",routing);n.putInt("Routes",routes);n.putInt("Colors",colors);n.putLong("ProtectLo",protectLo);n.putLong("ProtectHi",protectHi);n.putFloat("Xp",storedXp);ListTag names=new ListTag();for(String name:tabNames)names.add(StringTag.valueOf(name));n.put("Names",names);ListTag projects=new ListTag();for(int i=0;i<4;i++){CompoundTag p=new CompoundTag();if(projectItems[i]!=null){ResourceLocation id=ForgeRegistries.ITEMS.getKey(projectItems[i]);if(id!=null)p.putString("Id",id.toString());}p.putInt("Goal",projectGoals[i]);projects.add(p);}n.put("Projects",projects);saveWarps(n);return n;}
 public void readDisplay(CompoundTag n){if(n==null)return;slotTier=Math.max(0,Math.min(5,n.getInt("SlotTier")));int[] sc=n.getIntArray("StorageCounts");System.arraycopy(sc,0,displayStorageCounts,0,Math.min(sc.length,252));displayQuiverCount=n.getInt("QuiverCount");displayTorchCount=n.getInt("TorchCount");tank.readFromNBT(n.getCompound("FluidTank"));if(n.contains("FluidTank2"))tankTwo.readFromNBT(n.getCompound("FluidTank2"));if(n.contains("TankIndex"))tankIndex=n.getInt("TankIndex")==1?1:0;if(n.contains("Energy"))energy=Math.max(0,Math.min(ENERGY_CAPACITY,n.getInt("Energy")));xpBank=n.getInt("XpBank");cook=n.getInt("Cook");cookTotal=Math.max(1,n.getInt("CookTotal"));burnTotal=n.getInt("BurnTotal");long[] ep=n.getLongArray("ExtraProtect");System.arraycopy(ep,0,extraProtect,0,Math.min(3,ep.length));tier=Math.max(0,Math.min(4,n.getInt("Tier")));preset=Math.floorMod(n.getInt("Preset"),4);offhandIndex=Math.floorMod(n.getInt("OffIndex"),4);routing=n.getBoolean("Routing");if(n.contains("Routes"))routes=n.getInt("Routes");if(n.contains("Colors"))colors=n.getInt("Colors");protectLo=n.getLong("ProtectLo");protectHi=n.getLong("ProtectHi");storedXp=n.getFloat("Xp");ListTag names=n.getList("Names",Tag.TAG_STRING);for(int i=0;i<Math.min(TABS,names.size());i++)tabNames[i]=cleanName(names.getString(i),DEFAULT_NAMES[i]);Arrays.fill(projectItems,null);Arrays.fill(projectGoals,0);ListTag projects=n.getList("Projects",Tag.TAG_COMPOUND);for(int i=0;i<Math.min(4,projects.size());i++){CompoundTag p=projects.getCompound(i);projectGoals[i]=p.getInt("Goal");if(p.contains("Id")){ResourceLocation key=ResourceLocation.tryParse(p.getString("Id"));Item item=key==null?null:ForgeRegistries.ITEMS.getValue(key);projectItems[i]=item==null||item==Items.AIR?null:item;}}normalizeTabOrder();loadWarps(n);}
 public void track(Item item){if(item==null||item==Items.AIR)return;for(int i=0;i<4;i++)if(projectItems[i]==item){int next=GOAL_STEPS[0];for(int goal:GOAL_STEPS)if(goal>projectGoals[i]||goal==0){next=goal;break;}if(next==0){projectItems[i]=null;projectGoals[i]=0;}else projectGoals[i]=next;save();return;}for(int i=0;i<4;i++)if(projectItems[i]==null){projectItems[i]=item;projectGoals[i]=64;save();return;}}
 public int collected(Item item,Player player){
  if(item==null)return 0;
  int n=0;
  for(int i=0;i<18;i++)if(!bulk[i].isEmpty()&&bulk[i].is(item))n+=counts[i];
  for(int i=0;i<252;i++)if(storage(i).is(item))n+=storage(i).getCount();
  for(int p=0;p<4;p++){var inv=pouchStorage[p];if(!inv.owner().isEmpty())for(int i=0;i<inv.getSlots();i++){ItemStack s=inv.getStackInSlot(i);if(s.is(item))n+=s.getCount();}}
  for(int i=0;i<furnace.getSlots();i++)if(furnace.getStackInSlot(i).is(item))n+=furnace.getStackInSlot(i).getCount();
  for(int i=0;i<quiver.getSlots();i++)if(quiver.getStackInSlot(i).is(item))n+=quiver.getStackInSlot(i).getCount();
  for(int i=0;i<torches.getSlots();i++)if(torches.getStackInSlot(i).is(item))n+=torches.getStackInSlot(i).getCount();
  for(int i=0;i<offhand.getSlots();i++)if(offhand.getStackInSlot(i).is(item))n+=offhand.getStackInSlot(i).getCount();
  if(player!=null)for(int i=0;i<player.getInventory().getContainerSize();i++){ItemStack s=player.getInventory().getItem(i);if(s.is(item)&&!(s.getItem() instanceof PackItem))n+=s.getCount();}
  return n;
 }
 public void cycleOffhand(Player player){if(player.isUsingItem())return;ItemStack hand=player.getOffhandItem().copy();if(hand.getItem() instanceof PackItem)return;for(int n=1;n<=4;n++){int i=Math.floorMod(offhandIndex+n,4);ItemStack slot=offhand.getStackInSlot(i).copy();if(slot.isEmpty())continue;if(!hand.isEmpty()&&!offhand.isItemValid(i,hand))continue;offhand.setStackInSlot(i,hand);player.setItemInHand(InteractionHand.OFF_HAND,slot);offhandIndex=i;save();return;}if(!hand.isEmpty())for(int i=0;i<4;i++)if(offhand.getStackInSlot(i).isEmpty()&&offhand.isItemValid(i,hand)){offhand.setStackInSlot(i,hand);player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);offhandIndex=i;save();return;}}
 public static boolean upgradeEnabled(ItemStack stack){return !stack.hasTag()||!stack.getTag().getBoolean("PackUpgradeDisabled");}
 public boolean toggleUpgrade(int slot){if(!moduleSlot(slot))return false;ItemStack stack=items.getStackInSlot(slot);if(!(stack.getItem() instanceof AdventurersPack.UpgradeItem))return false;stack.getOrCreateTag().putBoolean("PackUpgradeDisabled",upgradeEnabled(stack));save();return true;}
 public boolean has(Item module){for(int i=0;i<4;i++)if(pouches.getStackInSlot(i).is(module))return true;for(int i=MODULES;i<TOTAL;i++)if(items.getStackInSlot(i).is(module)&&upgradeEnabled(items.getStackInSlot(i)))return true;for(int i=0;i<sections.getSlots();i++)if(sections.getStackInSlot(i).is(module))return true;return false;}
 public int capacity(){int base=PackConfig.BASE_CAPACITY.get();long tiered=Math.min(1000000L,(long)base<<Math.min(tier,4));if(!has(AdventurersPack.CAPACITY.get()))return (int)tiered;long upgraded=PackConfig.UPGRADED_CAPACITY.get();return (int)Math.min(1000000L,tiered+Math.max(0L,upgraded-base));}
 public boolean protectedSlot(int i){return (protectedMask&(1<<i))!=0;}
 public int deposit(int i,ItemStack input){if(i<0||i>=18||!bulkable(input)||(!bulk[i].isEmpty()&&!ItemStack.isSameItemSameTags(bulk[i],input)))return 0;
  int moved=Math.min(input.getCount(),Math.max(0,capacity()-counts[i]));if(moved>0){bulk[i]=input.copyWithCount(1);counts[i]+=moved;input.shrink(moved);save();}return moved;
 }
 public ItemStack withdraw(int i,int amount){if(i<0||i>=18||bulk[i].isEmpty())return ItemStack.EMPTY;int n=Math.min(counts[i],Math.min(amount,bulk[i].getMaxStackSize()));if(n<=0)return ItemStack.EMPTY;ItemStack out=bulk[i].copyWithCount(n);counts[i]-=n;if(counts[i]==0)bulk[i]=ItemStack.EMPTY;save();return out;}
 public ItemStack insert(ItemStack input,boolean automatic){return insert(input,automatic,false);}
 public ItemStack insertMatching(ItemStack input){return insert(input,false,true);}
 private ItemStack insert(ItemStack input,boolean automatic,boolean matchingOnly){ItemStack rest=input.copy();if(!safe(rest))return rest;rest=stashAmmo(rest);if(rest.isEmpty())return rest;rest=stashTorches(rest);if(rest.isEmpty())return rest;
  if(automatic&&!routing)return rest;
  if(bulkable(rest)){
   for(int i=0;i<18;i++)if(!bulk[i].isEmpty()&&ItemStack.isSameItemSameTags(bulk[i],rest)){
    deposit(i,rest);
    if(automatic&&!rest.isEmpty()&&voiding&&has(AdventurersPack.VOID.get())&&!protectedSlot(i)&&counts[i]>=capacity()){rest=ItemStack.EMPTY;}
    if(rest.isEmpty())return rest;
   }
  }
  if(routing||matchingOnly){
   for(int t=0;t<TABS&&!rest.isEmpty();t++)if(tabHasItem(t,rest))rest=insertIntoTab(t,rest);
   if(automatic||matchingOnly||rest.isEmpty())return rest;
   return insertIntoTab(tab,rest);
  }
  rest=insertIntoTab(tab,rest);
  return rest;
 }
 public int findMatching(ItemStack s){for(int i=0;i<18;i++)if(ItemStack.isSameItemSameTags(bulk[i],s))return i;return -1;}
 public ItemStack stashAmmo(ItemStack rest){if(!has(AdventurersPack.QUIVER.get())||!ammo(rest))return rest;tidy(quiver);for(int pass=0;pass<2;pass++)for(int i=0;i<9;i++)if((pass==0)==!quiver.getStackInSlot(i).isEmpty()){rest=quiver.insertItem(i,rest,false);if(rest.isEmpty())return rest;}return rest;}
 public ItemStack stashTorches(ItemStack rest){if(!has(AdventurersPack.TORCH.get())||!torch(rest))return rest;tidy(torches);for(int pass=0;pass<2;pass++)for(int i=0;i<9;i++)if((pass==0)==!torches.getStackInSlot(i).isEmpty()){rest=torches.insertItem(i,rest,false);if(rest.isEmpty())return rest;}return rest;}
 public ItemStack peekTorch(){tidy(torches);for(int i=0;i<9;i++){ItemStack s=torches.getStackInSlot(i);if(!s.isEmpty()&&torch(s))return s.copyWithCount(1);}return ItemStack.EMPTY;}
 public boolean consumeTorch(){for(int i=0;i<9;i++){ItemStack s=torches.getStackInSlot(i);if(s.isEmpty()||!torch(s))continue;s.shrink(1);if(s.isEmpty())torches.setStackInSlot(i,ItemStack.EMPTY);save();return true;}return false;}
 public ItemStack stashBrew(ItemStack rest){if(!has(AdventurersPack.ALEMBIC.get())||rest.isEmpty())return rest;if(rest.is(Items.BLAZE_POWDER))return brew.insertItem(4,rest,false);if(PotionBrewing.isIngredient(rest)&&!(rest.getItem() instanceof PotionItem))return brew.insertItem(3,rest,false);if(rest.getItem() instanceof PotionItem&&PotionUtils.getPotion(rest)==Potions.WATER)for(int i=0;i<3&&!rest.isEmpty();i++)rest=brew.insertItem(i,rest,false);return rest;}
 public ItemStack peekAmmo(Predicate<ItemStack> ok){tidy(quiver);for(int i=0;i<9;i++){ItemStack s=quiver.getStackInSlot(i);if(!s.isEmpty()&&ok.test(s))return s.copy();}return ItemStack.EMPTY;}
 public boolean consumeAmmo(Predicate<ItemStack> ok){for(int i=0;i<9;i++){ItemStack s=quiver.getStackInSlot(i);if(s.isEmpty()||!ok.test(s))continue;s.shrink(1);if(s.isEmpty())quiver.setStackInSlot(i,ItemStack.EMPTY);save();return true;}return false;}
 public ItemStack takeAmmo(Predicate<ItemStack> ok){ItemStack peek=peekAmmo(ok);if(peek.isEmpty()||!consumeAmmo(ok))return ItemStack.EMPTY;return peek.copyWithCount(1);}
 private static void tidy(ItemStackHandler handler){for(int i=0;i<handler.getSlots();i++)if(handler.getStackInSlot(i).isEmpty()&&handler.getStackInSlot(i)!=ItemStack.EMPTY)handler.setStackInSlot(i,ItemStack.EMPTY);}
 public void pourFinished(){if(!has(AdventurersPack.PHILTER.get()))return;for(int b=0;b<3;b++){ItemStack bottle=brew.getStackInSlot(b);if(!finishedPotion(bottle))continue;int vial=emptyVial();if(vial<0)return;vials.setStackInSlot(vial,bottle.copyWithCount(1));brew.setStackInSlot(b,drinkable(bottle)?new ItemStack(Items.GLASS_BOTTLE):ItemStack.EMPTY);}}
 private int emptyVial(){for(int i=0;i<3;i++)if(vials.getStackInSlot(i).isEmpty())return i;return -1;}
 public boolean useVial(Player player){if(!has(AdventurersPack.PHILTER.get())||player.level().isClientSide)return false;for(int n=0;n<3;n++){int i=Math.floorMod(vialIndex+n,3);ItemStack vial=vials.getStackInSlot(i);if(vial.isEmpty())continue;vialIndex=i;if(vial.getItem() instanceof ThrowablePotionItem){ThrownPotion thrown=new ThrownPotion(player.level(),player);thrown.setItem(vial.copy());thrown.shootFromRotation(player,player.getXRot(),player.getYRot(),-20.0F,0.5F,1.0F);player.level().addFreshEntity(thrown);player.level().playSound(null,player.getX(),player.getY(),player.getZ(),vial.is(Items.LINGERING_POTION)?SoundEvents.LINGERING_POTION_THROW:SoundEvents.SPLASH_POTION_THROW,SoundSource.PLAYERS,0.5F,0.4F/(player.level().getRandom().nextFloat()*0.4F+0.8F));}else{for(MobEffectInstance effect:PotionUtils.getMobEffects(vial)){if(effect.getEffect().isInstantenous())effect.getEffect().applyInstantenousEffect(player,player,player,effect.getAmplifier(),1.0);else player.addEffect(new MobEffectInstance(effect));}player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.GENERIC_DRINK,SoundSource.PLAYERS,0.5F,player.level().getRandom().nextFloat()*0.1F+0.9F);}boolean drinkable=!(vial.getItem() instanceof ThrowablePotionItem);vials.setStackInSlot(i,ItemStack.EMPTY);if(drinkable){ItemStack bottle=new ItemStack(Items.GLASS_BOTTLE);if(!player.getInventory().add(bottle))player.drop(bottle,false);}save();return true;}return false;}
 public void grantXp(Player player,int taken,int left){int total=taken+left;if(taken<=0||total<=0||storedXp<=0||!(player instanceof net.minecraft.server.level.ServerPlayer sp))return;float share=storedXp*taken/(float)total;storedXp-=share;int points=net.minecraft.util.Mth.floor(share);if(points>0)sp.giveExperiencePoints(points);if(storedXp<0.001f)storedXp=0;save();}
 public boolean setWarp(Player p,int index){
  if(!warpUnlocked||index<0||index>=WARP_COUNT||p==null)return false;
  warpIndex=index;warpSet[index]=true;
  if(warpNames[index]==null||warpNames[index].isEmpty())warpNames[index]="Warp "+(index+1);
  warpDims[index]=p.level().dimension().location().toString();
  warpX[index]=p.getBlockX();warpY[index]=p.getBlockY();warpZ[index]=p.getBlockZ();
  warpYaw[index]=p.getYRot();warpPitch[index]=p.getXRot();
  save();return true;
 }
 public boolean clearWarp(int index){
  if(!warpUnlocked||index<0||index>=WARP_COUNT||!warpSet[index])return false;
  warpSet[index]=false;warpDims[index]="";warpX[index]=warpY[index]=warpZ[index]=0;warpYaw[index]=warpPitch[index]=0;
  warpNames[index]="Warp "+(index+1);
  save();return true;
 }
 public boolean consumeWarpCost(Player p){
  if(p.isCreative())return true;
  if(xpBank>=WARP_XP_COST){xpBank-=WARP_XP_COST;save();return true;}
  if(PackStorageActions.points(p)>=WARP_XP_COST){p.giveExperiencePoints(-WARP_XP_COST);return true;}
  for(int i=0;i<18;i++)if(!protectedSlot(i)&&bulk[i].is(Items.ENDER_PEARL)&&counts[i]>0){withdraw(i,1);return true;}
  for(int i=0;i<252;i++)if(!protectedStorage(i)&&storage(i).is(Items.ENDER_PEARL)){extractStorage(i,1,false);save();return true;}
  for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(s.is(Items.ENDER_PEARL)){s.shrink(1);if(s.isEmpty())p.getInventory().setItem(i,ItemStack.EMPTY);p.getInventory().setChanged();return true;}}
  return false;
 }
 public boolean teleportToWarp(net.minecraft.server.level.ServerPlayer sp,int index){
  if(!warpUnlocked||index<0||index>=WARP_COUNT||!warpSet[index]||sp==null||sp.server==null)return false;
  ResourceLocation dimLoc=ResourceLocation.tryParse(warpDims[index]==null?"":warpDims[index]);
  if(dimLoc==null)return false;
  var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimLoc);
  var targetLevel=sp.server.getLevel(key);
  if(targetLevel==null){sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Target dimension is unavailable."),true);return false;}
  if(!consumeWarpCost(sp)){sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Warp requires "+WARP_XP_COST+" XP (Player or XP Bank) or 1 Ender Pearl."),true);return false;}
  warpIndex=index;save();
  ToolHand.restore(sp);
  sp.stopRiding();
  if(sp.containerMenu instanceof PackMenu)sp.closeContainer();
  sp.teleportTo(targetLevel,warpX[index]+0.5,warpY[index],warpZ[index]+0.5,warpYaw[index],warpPitch[index]);
  sp.resetFallDistance();
  targetLevel.playSound(null,warpX[index]+0.5,warpY[index]+0.5,warpZ[index]+0.5,SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,1.0F,1.0F);
  sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Warped to "+warpNames[index]+" ("+warpX[index]+", "+warpY[index]+", "+warpZ[index]+")"),true);
  return true;
 }
 public static String formatDim(String id){
  if(id==null||id.isEmpty())return "Unset";
  if("minecraft:overworld".equals(id))return "Overworld";
  if("minecraft:the_nether".equals(id))return "Nether";
  if("minecraft:the_end".equals(id))return "The End";
  int colon=id.indexOf(':');
  String path=colon>=0?id.substring(colon+1):id;
  String[] parts=path.split("_");
  StringBuilder sb=new StringBuilder();
  for(String part:parts)if(!part.isEmpty()){if(sb.length()>0)sb.append(' ');sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}
  return sb.length()==0?id:sb.toString();
 }
}

