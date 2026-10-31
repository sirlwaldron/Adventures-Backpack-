package com.waldron.adventurerspack.client;

import com.waldron.adventurerspack.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Centered Equipment layout. Storage on the left, Equipment in the center, Workstations on the right. Enlarged player inventory beneath the three utility columns. */
public class PackScreen extends AbstractContainerScreen<PackMenu> {
 private EditBox search,rename,warpRename;
 private final SoftButton[] slotUpgrades=new SoftButton[5];
 private final SoftButton[] warpSlots=new SoftButton[PackData.WARP_COUNT];
 private final SoftButton[] tabs=new SoftButton[4],stations=new SoftButton[8];
 private final List<AbstractWidget> controls=new ArrayList<>();
 private SoftButton camp,nav,pouches,tankButton,xpButton,batteryButton,torchButton;
 private SoftButton warpUnlock,warpSetBtn,warpGoBtn,warpClearBtn,warpSaveNameBtn;
 private SoftButton sortBtn,quickStackBtn,depositAllBtn,restockBtn,trashClearBtn;
 private SoftButton quickPickup,quickBuild,quickRoute,quickVoid,quickPreset;
 private final List<AbstractWidget> tankControls=new ArrayList<>(),xpControls=new ArrayList<>();
 private final List<AbstractWidget> campControls=new ArrayList<>(),pouchControls=new ArrayList<>();
 private boolean navigation;
 private SoftButton inventoryMode,pouchFilter;
 private SoftButton preset,pickup,voiding,building,routing,color,rule,use,next,pour,scaleBtn;
 private float uiScale=1; private int renamedTab=-1,renamedWarp=-1;

 private static final String[] STATIONS={"Pack","Craft","Smelt","Quiver","Potions","Brew","Ender","Controls"};
 private static final int[] ACTIONS={72,40,79,73,74,75,80,81};

 // High contrast color palette with crisp readability over transparent glass
 private static final int INK=0xFFFFF4DE;       // Luminous warm ivory text
 private static final int MUTED=0xFFDFD4C2;     // Clean, crisp secondary label text
 private static final int TEAL=0xFF56FAD8;      // Bright accent teal
 private static final int GOLD=0xFFFFD770;      // Amber gold highlight
 private static final int CARD_BG=0x880E1418;   // High-grade tinted smoke glass
 private static final int CARD_BORDER=0xFF7D6846;// Antique brass border
 private static final int CARD_ACCENT=0x55EAC476;// Subtle top inner reflection
 private static final int PILL_BG=0xDD090D10;   // Deep contrast backing plate for text

 public PackScreen(PackMenu m,Inventory inv,Component title){
  super(m,inv,title);
  imageWidth=560;
  imageHeight=m.panelHeight();
  inventoryLabelY=-100;
  titleLabelY=-100;
 }

 private void action(int id){
  if(id>=0&&id<4)menu.data.tab=id;
  if(minecraft!=null&&minecraft.gameMode!=null)
   Network.CHANNEL.sendToServer(new Network.Command(3,id,Integer.toString(menu.containerId)));
 }

 private void pull(){
  if(ClientMirror.tag!=null&&ClientMirror.containerId==menu.containerId)
   menu.data.readDisplay(ClientMirror.tag);
 }

 private SoftButton button(String name,int x,int y,int w,Button.OnPress press,String tip){
  var b=addRenderableWidget(new SoftButton(leftPos+x,topPos+y,w,18,Component.literal(name),press));
  b.packTooltip=tip;
  return b;
 }

 private SoftButton control(String name,int y,Button.OnPress press,String tip){
  var b=button(name,384,y,158,press,tip);
  controls.add(b);
  return b;
 }

 @Override protected void init(){
  int realW=minecraft.getWindow().getGuiScaledWidth(),realH=minecraft.getWindow().getGuiScaledHeight();
  imageHeight=menu.panelHeight();
  float fitW=(realW-16f)/560f;
  float fitH=(realH-16f)/(float)imageHeight;
  float maxFit=Math.min(fitW,fitH);
  double boost=com.waldron.adventurerspack.PackClientConfig.getScaleBoost();
  float scaleFactor=boost<=0.85?0.80f:(boost<=0.95?0.90f:1.00f);
  uiScale=Math.max(0.1f, maxFit * scaleFactor);
  width=(int)(realW/uiScale);
  height=(int)(realH/uiScale);
  super.init();
  topPos=Math.max(6,(height-imageHeight)/2);
  leftPos=Math.max(6,(width-560)/2);
  // External overlays and tooltip positioning need the actual GUI dimensions.
  width=realW;
  height=realH;
  pull();
  controls.clear();
  campControls.clear();
  pouchControls.clear();

  button("Codex",326,5,54,b->GuideScreen.open(),"Open the Adventurer's RPG Codex & Field Guide.");
  scaleBtn=button("Scale: "+Math.round(scaleFactor*100)+"%",384,5,72,b->{
   double cur=com.waldron.adventurerspack.PackClientConfig.getScaleBoost();
   double next=cur<0.85?0.90:(cur<0.95?1.00:0.80);
   com.waldron.adventurerspack.PackClientConfig.setScaleBoost(next);
   if(minecraft!=null)init(minecraft,minecraft.getWindow().getGuiScaledWidth(),minecraft.getWindow().getGuiScaledHeight());
  },"Cycle backpack UI scale (100% full fit, 90% comfortable, 80% compact). Saved across worlds.");
  button("Vanilla",460,5,92,b->ClientEvents.openVanilla(minecraft),"Open Minecraft's regular inventory and other mods' inventory integrations.");

  // Left column: Search and Storage Tabs
  search=new EditBox(font,leftPos+16,topPos+34,160,16,Component.literal("Search storage"));
  search.setMaxLength(50);
  search.setHint(Component.literal("Search storage..."));
  search.setBordered(false);
  search.setTextColor(0xFFFFFFFF);
  addRenderableWidget(search);

  for(int i=0;i<4;i++){
   final int t=i;
   tabs[i]=button(menu.data.tabNames[i],14+i*41,54,40,b->action(t),"Open this compartment and sort by stack size (biggest first).");
  }

  if(net.minecraftforge.fml.ModList.get().isLoaded("curios"))
   button("C",238,52,18,b->CuriosScreenBridge.open(),"Open the standard Curios equipment slots.");

  for(int i=0;i<5;i++){
   final int t=i+1;
   slotUpgrades[i]=button("Tier "+t+"  |  "+PackData.slotCapacityForTier(t)+" / slot",18,slotUpgradeY()+16+i*19,158,b->action(5300+t),"Permanent upgrade for all normal storage slots. Requires Tier "+(t-1)+" and "+PackMenu.SLOT_COST_COUNTS[i]+" "+new net.minecraft.world.item.ItemStack(PackMenu.SLOT_COST_ITEMS[i]).getHoverName().getString()+" in your inventory. Non-stackable items remain limited to one. Click to install.");
  }

  // Right column: Workstations
  for(int i=0;i<8;i++){
   final int n=i;int x,y,w;
   if(i==0||i==7){x=380+(i==7?112:0);y=50;w=54;}
   else{x=380+(i-1)%3*56;y=72+(i-1)/3*22;w=54;}
   stations[i]=button(i==0?"Upgrades":STATIONS[i],x,y,w,b->{navigation=false;action(ACTIONS[n]);},stationTip(i));
  }
  torchButton=button("Torches",436,50,54,b->{navigation=false;action(82);},"Requires Cave Torch attachment; stores torches and auto-places them in dark places nearby.");
  pouches=button("Pouches",380,116,54,b->{navigation=false;action(90);},"Four dedicated filtered pouches. Contents stay in each pouch.");
  camp=button("Camp",436,116,54,b->{navigation=false;action(94);},"Set up or fold your Camp Kit; supplies stay packed.");
  nav=button("Compass",492,116,54,b->{navigation=true;action(95);},"Built-in compass. No compass item required.");

  String[] pouchTips={
   "Forager pouch: seeds, plants and crops. Contents stay in the pouch.",
   "Gem pouch: gems, ingots, nuggets and raw minerals.",
   "Provision pouch: food. The first slot is preferred for automatic meals.",
   "Cartographer pouch: maps, compasses, paper and books."
  };
  for(int i=0;i<4;i++){
   final int k=i;
   pouchControls.add(button(new String[]{"Seeds","Gem","Food","Maps"}[i],382+i*40,164,38,b->action(90+k),pouchTips[i]));
  }

  pouchFilter=button("Filter: OFF",384,264,158,b->action(5220),"Cycle Off, Whitelist and Blacklist. Filters narrow the pouch's accepted item types.");
  pouchControls.add(pouchFilter);

  campControls.add(button("Set Up",384,206,76,b->action(3000),"Unfold on level ground ahead. Needs a Camp Kit."));
  campControls.add(button("Pack Up",466,206,76,b->action(3001),"Fold nearby camp, preserving stored items, fuel and cooking progress."));
  campControls.add(button("Supplies",384,230,158,b->action(3002),"Open your deployed supply barrel."));
  campControls.add(button("Cooking",384,254,158,b->action(3003),"Open your deployed camp furnace."));

  warpUnlock=button("Unlock Warping",384,234,158,b->action(5400),"Permanent pack upgrade: unlocks 10 cross-dimensional warp anchors. Requires 2 Nether Stars, 4 Netherite Ingots, and 8 Eyes of Ender in your inventory.");
  for(int i=0;i<PackData.WARP_COUNT;i++){
   final int w=i;
   var wb=addRenderableWidget(new SoftButton(leftPos+384+(i%2)*80,topPos+174+(i/2)*12,78,11,Component.literal((i+1)+". Warp "+(i+1)),b->action(5410+w)));
   wb.packTooltip="Select warp slot "+(i+1)+".";
   warpSlots[i]=wb;
  }
  warpRename=new EditBox(font,leftPos+384,topPos+250,104,14,Component.literal("Warp name"));
  warpRename.setMaxLength(12);
  warpRename.setTextColor(INK);
  addRenderableWidget(warpRename);
  warpSaveNameBtn=button("Save",492,248,50,b->saveWarpName(),"Save this warp anchor's name.");
  warpSetBtn=button("Set Here",384,268,50,b->action(5420),"Bind this warp slot to your current position and dimension.");
  warpGoBtn=button("Warp",438,268,50,b->action(5421),"Teleport to this warp anchor. Costs "+PackData.WARP_XP_COST+" XP (from XP Bank or Player) or 1 Ender Pearl.");
  warpClearBtn=button("Clear",492,268,50,b->action(5422),"Clear this warp anchor.");



  preset=control("Preset",152,b->action(14),"Cycle Travel, Mining, Building and Combat presets.");
  pickup=control("Auto pickup",174,b->action(10),"Requires Pickup Rune. Matching bulk slots fill first, then a compartment.");
  voiding=control("Void overflow",196,b->action(11),"Requires Void Sigil. Deletes only overflow for full, matching, unprotected bulk slots.");
  building=control("Build mode",218,b->action(12),"Requires Builder's Belt. Hold the pack to place the selected bulk block. Ctrl-click bulk to select.");
  routing=control("Smart routing",240,b->action(13),"When ON: Quick Deposit runs immediately and every second, moving matching main-inventory items into their existing backpack tabs or bulk storage. Hotbar and unmatched items stay in your inventory.");

  inventoryMode=control("Vanilla inventory: OFF",308,b->{
   boolean vanilla=!PackClientConfig.VANILLA_INVENTORY.get();
   PackClientConfig.setVanilla(vanilla);
   if(vanilla)ClientEvents.openVanilla(minecraft);
  },"Choose which inventory opens with your inventory key. Vanilla inventory has a Pack inventory button to switch back.");

  rename=new EditBox(font,leftPos+384,topPos+264,104,14,Component.literal("Compartment name"));
  rename.setMaxLength(12);
  rename.setTextColor(INK);
  addRenderableWidget(rename);
  controls.add(rename);
  controls.add(button("Save",492,262,50,b->saveName(),"Save this compartment's name."));
  color=button("Color",384,284,76,b->action(60+menu.data.tab),"Cycle this compartment's accent color.");
  controls.add(color);
  rule=button("Rule",466,284,76,b->action(64+menu.data.tab),"Cycle this compartment's routing rule.");
  controls.add(rule);

  use=button("Use potion",384,264,100,b->action(76),"Drink or throw the selected vial. U also works with the pack closed.");
  next=button("Next",488,264,54,b->action(77),"Select the next vial.");
  pour=button("Pour into vials",384,260,158,b->action(78),"Transfer finished potions into installed potion storage.");

  batteryButton=button("Battery",384,30,70,b->{navigation=false;action(5012);},"Requires Forge Battery. Stores 1,000,000 FE. The In slot drains an item into the battery. The Out slot charges an item. Pipes can move energy while the pack is docked.");
  tankButton=button("Tank",458,30,42,b->{navigation=false;action(5000);},"Requires Fluid Tank. Two tanks, 128 buckets each. A different fluid goes in the other tank.");
  xpButton=button("XP",504,30,40,b->{navigation=false;action(5001);},"Requires Experience Bank storage addon. Holds 200,000 points.");
  xpControls.add(button("Store 100",384,230,76,b->action(5004),"Deposit up to 100 experience points."));
  xpControls.add(button("Take 100",466,230,76,b->action(5005),"Withdraw up to 100 experience points."));
  xpControls.add(button("Store all",384,254,76,b->action(5006),"Deposit experience, up to the bank limit."));
  xpControls.add(button("Take all",466,254,76,b->action(5007),"Withdraw all stored experience."));
  // Top header button

  int iy=menu.inventoryY();
  sortBtn=button("Sort Compartment",16,iy+1,130,b->action(5014),"Consolidate and sort all unlocked slots in the current compartment.");
  quickStackBtn=button("Quick Deposit",16,iy+21,130,b->action(5015),"Deposit matching items from your main inventory into the backpack. Only item types already in the pack are moved; your hotbar is excluded.");
  depositAllBtn=button("Deposit Inventory",16,iy+41,130,b->action(5017),"Move all storeable items from your main inventory (excluding hotbar) into the backpack.");
  restockBtn=button("Restock Hotbar",16,iy+61,130,b->action(5016),"Top up existing stackable items on your hotbar from unprotected bulk and compartment storage.");
  trashClearBtn=button("Empty Trash",40,iy+84,106,b->action(5018),"Permanently destroy the item in the Trash slot. Dropping a new item into the Trash slot also overwrites the previous one.");
  quickPickup=button("Pickup",384,338,78,b->action(10),"Toggle Auto Pickup (requires Pickup Rune).");
  quickBuild=button("Build",466,338,78,b->action(12),"Toggle Build Mode (requires Builder's Belt).");
  quickRoute=button("Route",384,358,78,b->action(13),"Toggle Smart Routing. When ON: Quick Deposit runs immediately and every second, moving matching main-inventory items into their existing backpack tabs or bulk storage. Hotbar and unmatched items stay in your inventory.");
  quickVoid=button("Void",466,358,78,b->action(11),"Toggle Void Overflow (requires Void Sigil).");
  quickPreset=button("Preset",384,378,160,b->action(14),"Cycle Travel, Mining, Building and Combat presets.");

  sync();
 }

 private String stationTip(int n){
  return switch(n){
   case 0->"Six attachment sockets, five rune sockets, and six addon sockets. Every pack opens all of them.";
   case 1->"Requires Artisan's Crafting Kit.";
   case 2->"Requires Ember Core; normal furnace recipes and fuel.";
   case 3->"Requires Quiver; stores arrows and fireworks.";
   case 4->"Requires Philter; pour potions in, or right-click with an empty cursor to retrieve using a bottle.";
   case 5->"Requires Alembic; bottles, ingredient and blaze powder.";
   case 6->"Requires Ender Chest attachment; opens your personal ender storage.";
   default->"Pickup, routing, building, protection and compartment customization.";
  };
 }

 private int currentStation(){
  if(navigation)return 10;
  if(menu.data.addon==14)return 11;if(menu.data.addon==15)return 12;if(menu.data.addon==16)return 13;if(menu.data.addon==17)return 14;
  if(menu.data.addon>=8&&menu.data.addon<=11)return 8;
  if(menu.data.addon==12)return 9;
  if(menu.craftingOpen)return 1;
  return switch(menu.data.addon){
   case 2->3;case 3->4;case 4->5;case 5->2;case 6->6;case 7->7;default->0;
  };
 }

 private void sync(){
  int current=currentStation();
  boolean packed=!menu.data.owner.isEmpty();
  for(var w:tankControls)w.visible=packed&&current==11;
  for(var w:xpControls)w.visible=packed&&current==12;
  tankButton.active=packed&&menu.data.has(AdventurersPack.TANK.get());xpButton.active=packed&&menu.data.has(AdventurersPack.XP_BANK.get());batteryButton.active=packed&&menu.data.has(AdventurersPack.BATTERY.get());torchButton.active=packed&&menu.data.has(AdventurersPack.TORCH.get());
  tankButton.lit=current==11;xpButton.lit=current==12;batteryButton.lit=current==13;torchButton.lit=current==14;
  for(var w:campControls)w.visible=packed&&current==9;
  for(var w:pouchControls)w.visible=packed&&current==8;
  if(current==8){var pouch=menu.data.pouchStorage[menu.data.addon-8];pouchFilter.active=!pouch.owner().isEmpty();pouchFilter.setMessage(Component.literal("Filter: "+new String[]{"OFF","WHITELIST","BLACKLIST"}[pouch.filterMode()]));}
  nav.visible=true;
  nav.active=pouches.active=camp.active=packed;
  camp.lit=packed&&current==9;
  pouches.lit=packed&&current==8;
  nav.lit=packed&&current==10;
  for(int i=0;i<4;i++)tabs[i].active=packed;
  for(int i=0;i<8;i++){
   stations[i].lit=packed&&i==current;
   stations[i].active=packed&&(switch(i){
    case 1->menu.data.has(AdventurersPack.CRAFTING.get());
    case 2->menu.data.has(AdventurersPack.FURNACE.get());
    case 3->menu.data.has(AdventurersPack.QUIVER.get());
    case 4->menu.data.has(AdventurersPack.PHILTER.get());
    case 5->menu.data.has(AdventurersPack.ALEMBIC.get());
    case 6->menu.data.has(AdventurersPack.ENDER.get());
    default->true;
   });
  }
  for(int i=0;i<4;i++){
   tabs[i].setMessage(Component.literal(font.plainSubstrByWidth(menu.data.tabNames[i],34)));
   tabs[i].lit=packed&&menu.data.tab==i;
  }
  for(var w:controls)w.visible=packed&&current==7;
  if(current!=7)rename.setFocused(false);
  if(renamedTab!=menu.data.tab){
   renamedTab=menu.data.tab;
   rename.setValue(menu.data.tabNames[renamedTab]);
  }
  preset.setMessage(Component.literal("Preset: "+PackData.PRESET_NAMES[menu.data.preset]));
  state(inventoryMode,"Vanilla inventory",PackClientConfig.VANILLA_INVENTORY.get());
  state(pickup,"Auto pickup",menu.data.pickup);
  state(voiding,"Void overflow",menu.data.voiding);
  state(building,"Build mode",menu.data.building);
  state(routing,"Smart routing",menu.data.routing);
  pickup.active=menu.data.has(AdventurersPack.PICKUP.get());
  voiding.active=menu.data.has(AdventurersPack.VOID.get());
  building.active=menu.data.has(AdventurersPack.BUILDER.get());
  rule.setMessage(Component.literal(font.plainSubstrByWidth("Rule: "+PackData.routeName(menu.data.routeOf(menu.data.tab)),72)));
  use.visible=next.visible=packed&&current==4;
  pour.visible=packed&&current==5&&menu.data.has(AdventurersPack.PHILTER.get());
  boolean warpTab=packed&&current==10;
  boolean unlocked=warpTab&&menu.data.warpUnlocked;
  if(warpUnlock!=null){warpUnlock.visible=warpTab&&!menu.data.warpUnlocked;warpUnlock.active=warpTab&&!menu.data.warpUnlocked;}
  for(int i=0;i<PackData.WARP_COUNT;i++)if(warpSlots[i]!=null){
   warpSlots[i].visible=unlocked;
   warpSlots[i].active=unlocked;
   warpSlots[i].lit=unlocked&&menu.data.warpIndex==i;
   String wName=menu.data.warpNames[i]==null||menu.data.warpNames[i].isEmpty()?"Warp "+(i+1):menu.data.warpNames[i];
   String label=(i+1)+(menu.data.warpSet[i]?"* ":". ")+wName;
   warpSlots[i].setMessage(Component.literal(font.plainSubstrByWidth(label,72)));
   warpSlots[i].packTooltip=menu.data.warpSet[i]?wName+" ("+PackData.formatDim(menu.data.warpDims[i])+": "+menu.data.warpX[i]+", "+menu.data.warpY[i]+", "+menu.data.warpZ[i]+")":"Slot "+(i+1)+" (Unbound) - Select and click Set Here to bind.";
  }
  if(warpRename!=null){
   warpRename.visible=unlocked;
   if(!unlocked)warpRename.setFocused(false);
   int idx=Math.max(0,Math.min(PackData.WARP_COUNT-1,menu.data.warpIndex));
   String curName=menu.data.warpNames[idx]==null||menu.data.warpNames[idx].isEmpty()?"Warp "+(idx+1):menu.data.warpNames[idx];
   if(renamedWarp!=idx){renamedWarp=idx;warpRename.setValue(curName);}
   else if(!warpRename.isFocused()&&!warpRename.getValue().equals(curName))warpRename.setValue(curName);
  }
  if(warpSaveNameBtn!=null)warpSaveNameBtn.visible=warpSaveNameBtn.active=unlocked;
  if(warpSetBtn!=null)warpSetBtn.visible=warpSetBtn.active=unlocked;
  if(warpGoBtn!=null){warpGoBtn.visible=unlocked;warpGoBtn.active=unlocked&&menu.data.warpSet[Math.max(0,Math.min(PackData.WARP_COUNT-1,menu.data.warpIndex))];}
  if(warpClearBtn!=null){warpClearBtn.visible=unlocked;warpClearBtn.active=unlocked&&menu.data.warpSet[Math.max(0,Math.min(PackData.WARP_COUNT-1,menu.data.warpIndex))];}
  if(sortBtn!=null)sortBtn.active=quickStackBtn.active=depositAllBtn.active=restockBtn.active=packed;
  if(trashClearBtn!=null)trashClearBtn.active=packed&&!menu.data.trash.getStackInSlot(0).isEmpty();
  if(quickPickup!=null){state(quickPickup,"Pickup",menu.data.pickup);quickPickup.active=packed&&menu.data.has(AdventurersPack.PICKUP.get());}
  if(quickBuild!=null){state(quickBuild,"Build",menu.data.building);quickBuild.active=packed&&menu.data.has(AdventurersPack.BUILDER.get());}
  if(quickRoute!=null){state(quickRoute,"Route",menu.data.routing);quickRoute.active=packed;}
  if(quickVoid!=null){state(quickVoid,"Void",menu.data.voiding);quickVoid.active=packed&&menu.data.has(AdventurersPack.VOID.get());}
  if(quickPreset!=null){quickPreset.setMessage(Component.literal("Preset: "+PackData.PRESET_NAMES[menu.data.preset]));quickPreset.active=packed;}
 }

 private void state(SoftButton b,String text,boolean on){
  b.lit=on;
  b.setMessage(Component.literal(text+": "+(on?"ON":"OFF")));
 }

 private void saveName(){
  if(renamedTab>=0)Network.CHANNEL.sendToServer(new Network.Command(1,renamedTab,rename.getValue()));
 }

 private void saveWarpName(){
  if(renamedWarp>=0&&menu.data.warpUnlocked)Network.CHANNEL.sendToServer(new Network.Command(4,renamedWarp,warpRename.getValue()));
 }

 @Override protected void containerTick(){
  super.containerTick();
  for(int i=0;i<5;i++)if(slotUpgrades[i]!=null){slotUpgrades[i].visible=!menu.data.owner.isEmpty();slotUpgrades[i].active=menu.data.slotTier==i;slotUpgrades[i].setMessage(Component.literal((menu.data.slotTier>=i+1?"Installed ":"Tier ")+(i+1)+" | "+PackData.slotCapacityForTier(i+1)+" / slot"));}
  pull();
  sync();
 }

 private void card(GuiGraphics g,int x,int y,int w,int h){
  g.fill(x,y,x+w,y+h,CARD_BG);
  g.renderOutline(x,y,w,h,CARD_BORDER);
  g.fill(x+1,y+1,x+w-1,y+2,CARD_ACCENT);
 }

 @Override public void renderBackground(GuiGraphics g){}

 @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
  // Main container window
  card(g,leftPos,topPos,560,imageHeight);
  g.fill(leftPos+1,topPos+1,leftPos+559,topPos+27,0x44201A14);

  // Left panel: Storage Compartment & Bulk
  card(g,leftPos+10,topPos+30,174,menu.inventoryY()-54);
  g.fill(leftPos+14,topPos+32,leftPos+178,topPos+50,0xDD090D10);
  g.renderOutline(leftPos+14,topPos+32,164,18,0xFF665A45);

  // Center panel: Equipment, Character Pedestal & Player Inventory
  card(g,leftPos+190,topPos+30,180,menu.inventoryY()-54);
  card(g,leftPos+196,topPos+260,168,menu.inventoryY()-288);
  card(g,leftPos+10,topPos+menu.inventoryY()-20,142,128);
  card(g,leftPos+158,topPos+menu.inventoryY()-20,244,128);
  card(g,leftPos+408,topPos+menu.inventoryY()-20,142,128);
  // Pedestal fills the equipment stage; the player stands on the brass floor
  int pedX=leftPos+236,pedY=topPos+50,pedW=88,pedH=156;
  g.fill(pedX,pedY,pedX+pedW,pedY+pedH,0x55080C0E);
  g.renderOutline(pedX,pedY,pedW,pedH,0x668F764E);
  g.fill(pedX+1,pedY+pedH-12,pedX+pedW-1,pedY+pedH-1,0x33201A14);
  g.fill(pedX+8,pedY+pedH-3,pedX+pedW-8,pedY+pedH-2,0x667D6846);

  // Right panel: Workstations & Control Cards
  card(g,leftPos+376,topPos+30,174,menu.inventoryY()-54);
  card(g,leftPos+380,topPos+142,166,176);
  card(g,leftPos+380,topPos+322,166,menu.inventoryY()-346);

  g.pose().pushPose();g.pose().translate(leftPos,topPos,0);WorkstationPanels.draw(g,font,menu,currentStation());g.pose().popPose();
  // Active slot inset outlines for maximum item visibility
  for(Slot s:menu.slots)if(s.isActive()){
   int x=leftPos+s.x,y=topPos+s.y;
   int size=isPlayerInventory(s)?22:16;
   g.fill(x-1,y-1,x+size+1,y+size+1,0x55080C0E);
   g.renderOutline(x-1,y-1,size+2,size+2,0xFF536264);
  }

  // Selected bulk slot glow outline
  if(!menu.data.owner.isEmpty()){
   Slot bulk=menu.slots.get(menu.bulkSlot(Math.floorMod(menu.data.selected,18)));
   if(bulk.isActive())g.renderOutline(leftPos+bulk.x-2,topPos+bulk.y-2,20,20,TEAL);
  }

  // Locked socket overlays
  if(currentStation()==0){
   for(Slot s:menu.slots)if(s instanceof net.minecraftforge.items.SlotItemHandler h&&h.getItemHandler()==menu.data.items&&PackData.moduleSlot(h.getSlotIndex())&&s.isActive()&&!menu.data.socketOpen(h.getSlotIndex())){
    g.fill(leftPos+s.x,topPos+s.y,leftPos+s.x+16,topPos+s.y+16,0x6616191A);
    g.drawString(font,"x",leftPos+s.x+5,topPos+s.y+4,MUTED,true);
   }
   for(Slot s:menu.slots)if(s instanceof net.minecraftforge.items.SlotItemHandler section&&section.getItemHandler()==menu.data.sections&&s.isActive()&&!menu.data.sectionOpen(section.getSlotIndex())){
    g.fill(leftPos+s.x,topPos+s.y,leftPos+s.x+16,topPos+s.y+16,0x6616191A);
    g.drawString(font,"x",leftPos+s.x+5,topPos+s.y+4,MUTED,true);
   }
  }

  // Active potion vial outline
  if(currentStation()==4){
   Slot vial=menu.slots.get(PackMenu.PHILTER+Math.floorMod(menu.data.vialIndex,3));
   g.renderOutline(leftPos+vial.x-2,topPos+vial.y-2,20,20,TEAL);
  }

  // Center 3D animated player character model
  if(minecraft.player!=null)
   InventoryScreen.renderEntityInInventoryFollowsMouse(g,leftPos+280,topPos+198,70,leftPos+280-mx,topPos+120-my,minecraft.player);
 }

 private void label(GuiGraphics g,String text,int x,int y,int color){
  int tw=font.width(text);
  g.fill(x-3,y-2,x+tw+3,y+10,PILL_BG);
  g.renderOutline(x-3,y-2,tw+6,12,0x66594C37);
  g.drawString(font,text,x,y,color,true);
 }

 private static String fluidLine(net.minecraftforge.fluids.capability.templates.FluidTank tank){return tank.isEmpty()?"Empty":tank.getFluid().getDisplayName().getString()+" "+tank.getFluidAmount();}
 private void tiny(GuiGraphics g,String text,int x,int y,int maxWidth){
  g.pose().pushPose();
  g.pose().translate(x,y,0);
  g.pose().scale(.8f,.8f,1);
  int maxW=(int)(maxWidth/.8f);
  String substr=font.plainSubstrByWidth(text,maxW);
  int tw=font.width(substr);
  g.fill(-2,-1,tw+2,9,0x99090C0E);
  g.drawString(font,substr,0,0,MUTED,true);
  g.pose().popPose();
 }

 @Override protected void renderLabels(GuiGraphics g,int mx,int my){
  // Draw our own alpha-blended highlight; PackSlotHighlightMixin opts these slots out of vanilla's overlay.
  if(hoveredSlot!=null&&hoveredSlot.isActive()){
   int size=isPlayerInventory(hoveredSlot)?22:16;
   g.fill(net.minecraft.client.renderer.RenderType.gui(),hoveredSlot.x,hoveredSlot.y,hoveredSlot.x+size,hoveredSlot.y+size,300,0x40FFFFFF);
  }

  for(Slot slot:menu.slots)if(slot.isActive()&&slot.hasItem()&&slot.getItem().getItem() instanceof AdventurersPack.UpgradeItem&&!PackData.upgradeEnabled(slot.getItem())){g.fill(slot.x,slot.y,slot.x+16,slot.y+16,0x88000000);g.drawString(font,"OFF",slot.x,slot.y+9,0xFFFF7777,true);}
  // Top Header
  label(g,"ADVENTURE'S BACKPACK",18,10,INK);
  label(g,tierName()+"  /  "+PackData.PRESET_NAMES[menu.data.preset],175,10,TEAL);

  // Left Column Labels (Storage)
  boolean packed=!menu.data.owner.isEmpty();
  if(packed){
   for(int t=0;t<4;t++)
    g.fill(14+t*41,73,54+t*41,75,0xFF000000|menu.data.colorRgb(t));
   label(g,"SLOT UPGRADES",18,slotUpgradeY(),MUTED);
   label(g,"BULK STORAGE",18,138+PackMenu.EQUIP_DROP+menu.data.tier*18,MUTED);
  }else{
   label(g,"NO BACKPACK",18,84,INK);
   tiny(g,"Equip one in the Pack socket.",18,104,156);
   tiny(g,"Compartments and bulk storage open with it.",18,118,156);
  }

  if(packed)for(Slot slot:menu.slots){int id=menu.storageIndex(slot.index);if(id>=0&&usesModCount(slot)){String count=Integer.toString(storageDisplayCount(id,slot));g.pose().pushPose();g.pose().translate(slot.x+17,slot.y+9,300);g.pose().scale(.78f,.78f,1);g.drawString(font,count,-font.width(count),0,0xFFFFFFFF,true);g.pose().popPose();}}

  // Center Column Labels (Equipment, Tool Kit & Inventory)
  label(g,"EQUIPMENT",208,36,MUTED);
  tiny(g,"Pack",338,66,28);
  tiny(g,"Off",338,98,24);
  if(packed){
   tiny(g,"Kit",338,130,24);
   label(g,"TOOL KIT",208,PackMenu.TOOL_Y-14,INK);
   tiny(g,"Tap "+ClientSetup.TOOL.getTranslatedKeyMessage().getString()+": select / stow",208,PackMenu.TOOL_Y+20,88);
   tiny(g,"Hold "+ClientSetup.TOOL.getTranslatedKeyMessage().getString()+": wheel",300,PackMenu.TOOL_Y+20,64);
  }

  label(g,"YOUR INVENTORY",PackMenu.PLAYER_X,menu.inventoryY()-15,MUTED);


  // Right Column Labels (Workstations)

  int station=currentStation();
  if(!packed){
   label(g,"NEEDS A PACK",384,148,INK);
   tiny(g,"Workstations, pouches, camp and the compass need a backpack.",384,168,158);
  }else{
  g.pose().pushPose();
  g.pose().translate(0,24,0);
  if(station==0){
   label(g,"PACK UPGRADES",384,124,INK);
   tiny(g,"Shift-click upgrades to install",384,262,158);
   label(g,"Attachments",384,142,MUTED);
   label(g,"Runes",384,180,MUTED);
   label(g,"Storage addons",384,222,MUTED);
   tiny(g,"6 attachments  ·  5 runes  ·  6 addons",384,278,158);
  } else if(station==1){
   label(g,"CRAFTING",384,124,INK);
   label(g,">",466,178,TEAL);
   tiny(g,"Ingredients return when closed.",384,228,158);
  } else if(station==2){
   label(g,"PORTABLE FURNACE",384,124,INK);
   tiny(g,"Input",418,146,40);tiny(g,"Output",492,146,45);
   tiny(g,"Fuel",418,234,35);
   tiny(g,"Smelting XP: "+(int)menu.data.storedXp,384,266,158);
  } else if(station==3){
   label(g,"QUIVER",384,124,INK);
   tiny(g,"Arrows and fireworks",384,140,158);
   tiny(g,"Used automatically by your bow",384,232,158);
  } else if(station==4){
   label(g,"POTION STORAGE",384,124,INK);
   tiny(g,"Select a vial, then drink or throw",384,142,158);
   tiny(g,"1",416,201,20);tiny(g,"2",460,201,20);tiny(g,"3",504,201,20);
   tiny(g,"Right-click a vial with an empty cursor",384,264,158);
   tiny(g,"and a bottle in inventory to retrieve.",384,276,158);
  } else if(station==5){
   label(g,"BREWING STAND",384,124,INK);
   tiny(g,"Fuel",390,140,36);tiny(g,"Ingredient",446,140,75);
   tiny(g,"Blaze charges: "+menu.data.brewFuel,384,266,158);
  } else if(station==11){
   label(g,"FLUID TANKS",384,124,INK);
   tiny(g,menu.data.tank.isEmpty()?"Empty":menu.data.tank.getFluid().getDisplayName().getString(),384,140,74);
   tiny(g,menu.data.tankTwo.isEmpty()?"Empty":menu.data.tankTwo.getFluid().getDisplayName().getString(),468,140,74);
  } else if(station==13){
   label(g,"FORGE BATTERY",384,124,INK);
   tiny(g,String.format(Locale.ROOT,"%,d / %,d FE",menu.data.energy,PackData.ENERGY_CAPACITY),392,146,146);
  } else if(station==12){
   label(g,"EXPERIENCE BANK",384,124,INK);
   tiny(g,menu.data.xpBank+" / "+PackData.XP_CAPACITY+" XP",384,162,158);
   tiny(g,"Stored as points, not levels",384,186,158);
  } else if(station==6){
   label(g,"ENDER STORAGE",384,124,INK);
   tiny(g,"Your personal ender chest",384,140,158);
  } else if(station==14){
   label(g,"CAVE TORCHES",384,124,INK);
   tiny(g,"Torches and soul torches",384,140,158);
   tiny(g,"Auto-places in dark spots nearby",384,232,158);
  }
  g.pose().popPose();

  if(station==8){
   label(g,"SPECIALIZED POUCHES",384,148,INK);
   tiny(g,"Pouch contents",384,214,158);
   var pouch=menu.data.pouchStorage[menu.data.addon-8];
   if(!pouch.owner().isEmpty())for(int i=0;i<9;i++){
    int fx=382+i*18;g.fill(fx-1,287,fx+17,305,0xFF10191D);g.renderOutline(fx-1,287,18,18,0xFF7D6846);
    var sample=pouch.filterItem(i);if(!sample.isEmpty())g.renderItem(sample,fx,288);
   }
  }
  if(station==9){
   label(g,"FOLD-OUT CAMP",384,148,INK);
   tiny(g,"Install kit",448,168,80);
   tiny(g,"Bedroll / lantern / stash / furnace",384,296,158);
  }
  if(station==10)
   Navigation.draw(g,font,menu.inventory.player,menu.data,384,150);
  }

  // Bulk storage item counts rendered with high readability
  for(int i=0;i<18;i++){
   Slot s=menu.slots.get(menu.bulkSlot(i));
   int count=menu.data.counts[i];
   if(count>0){
    // Dedicated label band; reserve 0% for full and 100% for empty slots.
    int remaining=(int)(100L*Math.max(0,menu.data.capacity()-count)/Math.max(1,menu.data.capacity()));
    if(count<menu.data.capacity())remaining=Math.max(1,remaining);
    String free=remaining+"%";
    g.pose().pushPose();
    g.pose().translate(s.x+8,s.y-9,200);
    g.pose().scale(.55f,.55f,1);
    g.drawString(font,free,-font.width(free)/2,0,TEAL,true);
    g.pose().popPose();
    String text=count>=1000?String.format(Locale.ROOT,"%.1fk",count/1000f):""+count;
    g.pose().pushPose();
    g.pose().translate(s.x+17,s.y+9,300);
    g.pose().scale(.78f,.78f,1);
    int tw=font.width(text);

    g.drawString(font,text,-tw,0,0xFFFFFFFF,true);
    g.pose().popPose();
   }
   if(menu.data.protectedSlot(i))g.drawString(font,"*",s.x+11,s.y+1,0xFFFFCD66,false);
  }

  for(int i=0;i<108;i++){
   Slot s=menu.slots.get(i);
   if(s.isActive()&&menu.data.protectedStorage(i))g.drawString(font,"*",s.x+10,s.y-1,0xFFFFCD66,false);
  }
  for(int i=108;i<252;i++){
   Slot slot=menu.slots.get(PackMenu.EXTRA+i-108);
   if(slot.isActive()&&menu.data.protectedStorage(i))g.drawString(font,"*",slot.x+10,slot.y-1,0xFFFFCD66,true);
  }

  // Search filtering
  String query=search.getValue().trim().toLowerCase(Locale.ROOT);
  if(!query.isEmpty()){
   for(Slot s:menu.slots)
    if(s.isActive()&&s.hasItem()&&!matches(s,query))
     g.fill(s.x,s.y,s.x+(isPlayerInventory(s)?22:16),s.y+(isPlayerInventory(s)?22:16),0xBD111617);
   for(int t=0;t<4;t++){
    int found=0;
    for(int cell=0;cell<menu.data.pageSize();cell++){
     int i=menu.data.storageId(t,cell);
     if(matches(menu.slots.get(i<108?i:PackMenu.EXTRA+i-108),query))found++;
    }
    if(packed&&found>0)g.fill(14+t*41,74,54+t*41,76,TEAL);
   }
  }

  if(packed)drawSupplies(g);
  drawLowerDashboards(g,packed);
 }

 private void miniBar(GuiGraphics g,int x,int y,int w,int h,float ratio,int color){
  g.fill(x,y,x+w,y+h,0xDD090D10);
  g.renderOutline(x,y,w,h,0xFF536264);
  int fill=Math.round((w-2)*Math.max(0f,Math.min(1f,ratio)));
  if(fill>0)g.fill(x+1,y+1,x+1+fill,y+h-1,color);
 }

 private void drawLowerDashboards(GuiGraphics g,boolean packed){
  int iy=menu.inventoryY();
  label(g,"INVENTORY ACTIONS",16,iy-15,MUTED);
  label(g,"PACK GAUGES",414,iy-15,MUTED);
  label(g,"PACK OVERVIEW",204,266,INK);
  label(g,"QUICK TOGGLES",386,326,INK);
  if(packed&&menu.data.trash.getStackInSlot(0).isEmpty())
   g.renderFakeItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BARRIER),18,iy+85);
  if(!packed){
   tiny(g,"Equip a backpack to use actions.",16,iy+106,130);
   tiny(g,"Equip a backpack to view gauges.",414,iy+8,130);
   tiny(g,"Equip a backpack to view stats.",204,286,152);
   return;
  }
  PackData d=menu.data;
  // Bottom-Right: Live Pack Status & Gauges
  var t1=d.tank;var t2=d.tankTwo;
  String t1Name=t1.isEmpty()?"Tank 1: Empty":"T1: "+t1.getFluid().getDisplayName().getString()+" ("+(t1.getFluidAmount()/1000)+"B)";
  tiny(g,t1Name,414,iy+2,130);
  miniBar(g,414,iy+12,130,6,t1.getFluidAmount()/(float)PackData.FLUID_CAPACITY,0xFF3FA9F5);
  String t2Name=t2.isEmpty()?"Tank 2: Empty":"T2: "+t2.getFluid().getDisplayName().getString()+" ("+(t2.getFluidAmount()/1000)+"B)";
  tiny(g,t2Name,414,iy+22,130);
  miniBar(g,414,iy+32,130,6,t2.getFluidAmount()/(float)PackData.FLUID_CAPACITY,0xFFFF8833);
  tiny(g,String.format(Locale.ROOT,"Battery: %,d / %,dk FE",d.energy/1000,PackData.ENERGY_CAPACITY/1000),414,iy+42,130);
  miniBar(g,414,iy+52,130,6,d.energy/(float)PackData.ENERGY_CAPACITY,0xFF3DDC97);
  tiny(g,String.format(Locale.ROOT,"XP Bank: %,d / %,dk XP",d.xpBank,PackData.XP_CAPACITY/1000),414,iy+62,130);
  miniBar(g,414,iy+72,130,6,d.xpBank/(float)PackData.XP_CAPACITY,0xFF7FE146);
  g.renderFakeItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TORCH),414,iy+84);
  tiny(g,""+d.displayTorchCount,432,iy+88,36);
  g.renderFakeItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARROW),464,iy+84);
  tiny(g,""+d.displayQuiverCount,482,iy+88,36);
  String act=d.cook>0?"Smelt "+Math.round(100f*d.cook/Math.max(1,d.cookTotal))+"%":d.brewTime>0?"Brew "+Math.round(100f*(400-d.brewTime)/400f)+"%":"Idle";
  tiny(g,act,508,iy+88,38);

  // Center Lower Card: Pack Overview
  int compUsed=0,compMax=d.pageSize();
  for(int j=0;j<compMax;j++){int id=d.storageId(d.tab,j);if(d.displayStorageCounts[id]>0||!d.storage(id).isEmpty())compUsed++;}
  tiny(g,d.tabNames[d.tab]+": "+compUsed+" / "+compMax+" slots",204,284,152);
  miniBar(g,204,294,152,6,compUsed/(float)Math.max(1,compMax),0xFF000000|d.colorRgb(d.tab));
  int bulkUsed=0;long bulkTotal=0;
  for(int i=0;i<18;i++)if(d.counts[i]>0){bulkUsed++;bulkTotal+=d.counts[i];}
  tiny(g,String.format(Locale.ROOT,"Bulk: %d/18 slots (%,d items)",bulkUsed,bulkTotal),204,306,152);
  miniBar(g,204,316,152,6,bulkUsed/18f,TEAL);
  tiny(g,"Slot Cap: Tier "+d.slotTier+" ("+d.slotCapacity()+" / slot)",204,328,152);
  int gUsed=0,rUsed=0,sUsed=0;
  for(int s:PackData.GEAR_SLOTS)if(!d.items.getStackInSlot(s).isEmpty())gUsed++;
  for(int s:PackData.RUNE_SLOTS)if(!d.items.getStackInSlot(s).isEmpty())rUsed++;
  for(int i=0;i<d.sections.getSlots();i++)if(!d.sections.getStackInSlot(i).isEmpty())sUsed++;
  tiny(g,"Gear "+gUsed+"/6 · Runes "+rUsed+"/5 · Addon "+sUsed+"/6",204,342,152);
  String smeltLine=d.cook>0?"Smelting: "+Math.round(100f*d.cook/Math.max(1,d.cookTotal))+"%  (XP: "+(int)d.storedXp+")":"Furnace: Idle  (XP: "+(int)d.storedXp+")";
  tiny(g,smeltLine,204,356,152);
  String brewLine=d.brewTime>0?"Brewing: "+Math.round(100f*(400-d.brewTime)/400f)+"%  (Fuel: "+d.brewFuel+")":"Brewing: Idle  (Fuel: "+d.brewFuel+")";
  tiny(g,brewLine,204,370,152);
  int warpsBound=0;for(boolean b:d.warpSet)if(b)warpsBound++;
  tiny(g,"Warps: "+(d.warpUnlocked?warpsBound+" / "+PackData.WARP_COUNT+" bound":"Locked"),204,384,152);

  // Right Lower Card: Active Compartment Route & Color summary
  g.fill(386,400,396,410,0xFF000000|d.colorRgb(d.tab));
  g.renderOutline(386,400,10,10,0xFF7D6846);
  tiny(g,d.tabNames[d.tab]+" Rule: "+PackData.routeName(d.routeOf(d.tab)),400,401,142);
 }

 private int slotUpgradeY(){return 220+PackMenu.EQUIP_DROP+menu.data.tier*18;}
 private int supplyOrigin(){return slotUpgradeY()+126;}

 private void drawSupplies(GuiGraphics g){
  int y=supplyOrigin(),row=0;
  for(int i=0;i<4;i++){
   var item=menu.data.projectItems[i];
   if(item==null||menu.data.projectGoals[i]<=0)continue;
   if(row==0)label(g,"SUPPLIES",18,y,MUTED);
   int ry=y+16+row*18;
   g.renderItem(new net.minecraft.world.item.ItemStack(item),18,ry);
   int have=menu.data.collected(item,minecraft==null?null:minecraft.player);
   int goal=menu.data.projectGoals[i];
   g.drawString(font,have+" / "+goal,38,ry+4,have>=goal?TEAL:INK,true);
   row++;
  }
 }

 private int supplyAt(double x,double y){
  if(menu.data.owner.isEmpty())return -1;
  int row=0,top=topPos+supplyOrigin()+16;
  for(int i=0;i<4;i++){
   if(menu.data.projectItems[i]==null||menu.data.projectGoals[i]<=0)continue;
   int ry=top+row*18;
   if(x>=leftPos+14&&x<leftPos+168&&y>=ry&&y<ry+16)return i;
   row++;
  }
  return -1;
 }

 private int pouchFilterAt(double x,double y){
  if(currentStation()!=8||menu.data.pouchStorage[menu.data.addon-8].owner().isEmpty())return -1;
  double px=x-leftPos-381,py=y-topPos;
  return px>=0&&px<162&&py>=287&&py<305?(int)(px/18):-1;
 }

 private boolean matches(Slot s,String q){
  return s.hasItem()&&s.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q);
 }

 private String tierName(){
  return switch(menu.data.tier){
   case 1->"Explorer";case 2->"Ranger";case 3->"Runic";case 4->"Relic";default->"Traveler";
  };
 }

 @Override public boolean mouseClicked(double x,double y,int button){
  x/=uiScale;y/=uiScale;
  int filter=pouchFilterAt(x,y);
  if(filter>=0&&(button==0||button==1)){action((button==1?5210:5200)+filter);return true;}
  if(button==0){
   int project=supplyAt(x,y);
   if(project>=0){
    var key=ForgeRegistries.ITEMS.getKey(menu.data.projectItems[project]);
    if(key!=null)Network.CHANNEL.sendToServer(new Network.Command(2,0,key.toString()));
    return true;
   }
  }
  if(hoveredSlot!=null&&hoveredSlot.isActive()){
   int id=hoveredSlot.index;
   if(button==1&&hasAltDown()&&hoveredSlot.getItem().getItem() instanceof AdventurersPack.UpgradeItem){action(4000+id);return true;}
   if(hoveredSlot.hasItem()&&hasAltDown()&&hasControlDown()){
    var key=ForgeRegistries.ITEMS.getKey(hoveredSlot.getItem().getItem());
    if(key!=null)Network.CHANNEL.sendToServer(new Network.Command(2,0,key.toString()));
    return true;
   }
   if(id>=PackMenu.EXTRA&&id<PackMenu.BULK_EXTRA&&hasAltDown()){action(1000+id-PackMenu.EXTRA);return true;}
   if(id<108&&hasAltDown()){action(100+id);return true;}
   if(menu.bulkIndex(id)>=0){
    if(hasAltDown()){action(2100+menu.bulkIndex(id));return true;}
    if(hasControlDown()){action(2000+menu.bulkIndex(id));return true;}
   }
  }
  return super.mouseClicked(x,y,button);
 }

 @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/uiScale,y/uiScale,b);}
 @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){return super.mouseDragged(x/uiScale,y/uiScale,b,dx/uiScale,dy/uiScale);}
 @Override public boolean mouseScrolled(double x,double y,double d){return super.mouseScrolled(x/uiScale,y/uiScale,d);}

 @Override public boolean keyPressed(int key,int scan,int mods){
  if(rename.isFocused()&&key!=256){
   if(key==257||key==335){saveName();rename.setFocused(false);return true;}
   return rename.keyPressed(key,scan,mods)||rename.canConsumeInput();
  }
  if(warpRename!=null&&warpRename.isFocused()&&key!=256){
   if(key==257||key==335){saveWarpName();warpRename.setFocused(false);return true;}
   return warpRename.keyPressed(key,scan,mods)||warpRename.canConsumeInput();
  }
  if(search.isFocused()&&key!=256)return search.keyPressed(key,scan,mods)||search.canConsumeInput();
  return super.keyPressed(key,scan,mods);
 }

 @Override public void render(GuiGraphics g,int mx,int my,float partial){
  int realW=minecraft.getWindow().getGuiScaledWidth(),realH=minecraft.getWindow().getGuiScaledHeight();
  // Cover the world and the gameplay hotbar so only this inventory is on screen.
  g.fillGradient(0,0,realW,realH,0xC0101010,0xE0101010);
  int x=(int)(mx/uiScale),y=(int)(my/uiScale);
  g.pose().pushPose();
  g.pose().scale(uiScale,uiScale,1);
  super.render(g,x,y,partial);
  g.pose().popPose();
  // Hit testing stays in pack coordinates; tooltips use screen coordinates.
  int filter=pouchFilterAt(x,y);
  int project=supplyAt(x,y);
  int tankHover=-1;
  if(currentStation()==11&&!menu.data.owner.isEmpty()){
   for(int i=0;i<2;i++)if(x>=leftPos+392+i*84&&x<leftPos+420+i*84&&y>=topPos+176&&y<topPos+228)tankHover=i;
  }
  if(tankHover>=0){
   var tank=menu.data.tankAt(tankHover);
   g.renderTooltip(font,List.of(Component.literal("Tank "+(tankHover+1)),tank.isEmpty()?Component.literal("Empty"):tank.getFluid().getDisplayName(),Component.literal(String.format(Locale.ROOT,"%,d / %,d mB",tank.getFluidAmount(),tank.getCapacity()))),Optional.empty(),mx,my);
  }else if(filter>=0){
   var sample=menu.data.pouchStorage[menu.data.addon-8].filterItem(filter);
   g.renderTooltip(font,List.of(sample.isEmpty()?Component.literal("Item filter"):sample.getHoverName(),Component.literal("Click with an item to copy its type."),Component.literal("Right-click to clear. Items are not consumed.")),Optional.empty(),mx,my);
  }else if(project>=0){
   var item=menu.data.projectItems[project];
   int have=menu.data.collected(item,minecraft.player);
   int goal=menu.data.projectGoals[project];
   g.renderTooltip(font,List.of(new net.minecraft.world.item.ItemStack(item).getHoverName(),Component.literal(have+" / "+goal+" collected"),Component.literal("Click to raise the goal. The last step clears it.")),Optional.empty(),mx,my);
  }else if(hoveredSlot!=null&&menu.bulkIndex(hoveredSlot.index)>=0&&hoveredSlot.hasItem()){
   int i=menu.bulkIndex(hoveredSlot.index);
   g.renderTooltip(font,List.of(
    hoveredSlot.getItem().getHoverName(),
    Component.literal(String.format(Locale.ROOT,"%,d / %,d items",menu.data.counts[i],menu.data.capacity())),
    Component.literal(String.format(Locale.ROOT,"%,d more items fit (%.1f%% free)",Math.max(0,menu.data.capacity()-menu.data.counts[i]),100.0*Math.max(0,menu.data.capacity()-menu.data.counts[i])/Math.max(1,menu.data.capacity()))),
    Component.literal("Click: take stack | Right-click: take one"),
    Component.literal("Alt: protect | Ctrl: select building block")),
    Optional.empty(),mx,my);
  }else if(hoveredSlot!=null&&menu.storageIndex(hoveredSlot.index)>=0&&hoveredSlot.hasItem()&&menu.data.slotTier>0){
   int id=menu.storageIndex(hoveredSlot.index);var stack=hoveredSlot.getItem();
   g.renderTooltip(font,List.of(stack.getHoverName(),Component.literal(menu.data.displayStorageCounts[id]+" / "+(stack.getMaxStackSize()==1?1:menu.data.slotCapacity())+" items"),Component.literal("Slot upgrade: Tier "+menu.data.slotTier),Component.literal("Click: take stack | Right-click: take one")),Optional.empty(),mx,my);
  }else if(hoveredSlot!=null&&hoveredSlot.index==PackMenu.CAMP_SLOT+22&&!hoveredSlot.hasItem()){
   g.renderTooltip(font,List.of(Component.literal("Trash / Void Slot"),Component.literal("Place unwanted items here to discard them."),Component.literal("Placing another item or clicking Empty Trash"),Component.literal("permanently destroys the held item.")),Optional.empty(),mx,my);
  } else renderTooltip(g,mx,my);
  for(var child:children())if(child instanceof SoftButton b&&b.visible&&b.isMouseOver(x,y)&&!b.packTooltip.isEmpty()){g.renderTooltip(font,font.split(Component.literal(b.packTooltip),180),mx,my);break;}
 }

 public static boolean isPlayerInventory(Slot slot){return slot.index>=PackMenu.PLAYER&&slot.index<PackMenu.PLAYER+36;}
 public int storageDisplayCount(int id,Slot slot){return menu.data.displayStorageCounts[id]>0?menu.data.displayStorageCounts[id]:slot.getItem().getCount();}
 public boolean usesModCount(Slot slot){
  if(slot==null||!slot.isActive()||!slot.hasItem())return false;
  if(menu.bulkIndex(slot.index)>=0)return true;
  int id=menu.storageIndex(slot.index);
  if(id>=0){
   int count=storageDisplayCount(id,slot);
   return (menu.data.slotTier>0&&count>1)||count>slot.getItem().getMaxStackSize();
  }
  return false;
 }
 public void renderCustomCountSlot(GuiGraphics g,Slot slot){
  var stack=slot.getItem();
  if(stack.isEmpty())return;
  var single=stack.copyWithCount(1);
  g.renderItem(single,slot.x,slot.y,slot.x+slot.y*imageWidth);
  g.renderItemDecorations(font,single,slot.x,slot.y,null);
 }

 public float getUiScale(){return uiScale;}
 public int getActualGuiLeft(){return (int)(leftPos*uiScale);}
 public int getActualGuiTop(){return (int)(topPos*uiScale);}
 public int getActualGuiWidth(){return (int)(560*uiScale);}
 public int getActualGuiHeight(){return (int)(imageHeight*uiScale);}

 public Slot getSlotAtScreen(double mx,double my){
  if(uiScale<=0)return null;
  double cx=mx/uiScale,cy=my/uiScale;
  for(Slot s:menu.slots)if(s.isActive()){
   double sx=leftPos+s.x,sy=topPos+s.y;
   int size=isPlayerInventory(s)?22:16;
   if(cx>=sx&&cx<sx+size&&cy>=sy&&cy<sy+size)return s;
  }
  return null;
 }

 public net.minecraft.world.item.ItemStack getItemAtScreen(double mx,double my){
  Slot s=getSlotAtScreen(mx,my);
  if(s==null)return net.minecraft.world.item.ItemStack.EMPTY;
  int bulk=menu.bulkIndex(s.index);
  if(bulk>=0&&bulk<menu.data.bulk.length){
   net.minecraft.world.item.ItemStack st=menu.data.bulk[bulk];
   if(st!=null&&!st.isEmpty())return st;
   return s.getItem();
  }
  return s.getItem();
 }
}



