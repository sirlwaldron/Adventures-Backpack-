package com.waldron.adventurerspack;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

@GameTestHolder(AdventurersPack.ID)
@PrefixGameTestTemplate(false)
public class PackGameTests {
 private static PackData pack(){return new PackData(new ItemStack(AdventurersPack.PACK.get()));}
 @GameTest(template="empty") public static void bulkRoundTrip(GameTestHelper h){var d=pack();for(int n=0;n<64;n++){var input=new ItemStack(Items.COBBLESTONE,64);h.assertTrue(d.deposit(0,input)==64&&input.isEmpty(),"Each deposit consumes exactly its input");}h.assertTrue(d.counts[0]==4096,"4096 stored");var full=new ItemStack(Items.COBBLESTONE,64);h.assertTrue(d.deposit(0,full)==0&&full.getCount()==64,"Full slot preserves remainder");var restored=new PackData(d.owner.copy());h.assertTrue(restored.counts[0]==4096,"NBT keeps full bulk count");h.assertTrue(restored.withdraw(0,1000).getCount()==64&&restored.counts[0]==4032,"Withdrawal is capped at a vanilla stack");h.succeed();}
 @GameTest(template="empty") public static void rejectNestedAndTagged(GameTestHelper h){var d=pack();h.assertTrue(!d.items.isItemValid(0,d.owner),"Nested packs rejected");h.assertTrue(!d.items.isItemValid(0,new ItemStack(Items.SHULKER_BOX)),"Shulker nesting rejected");var s=new ItemStack(Items.STONE,64);s.getOrCreateTag().putString("Custom","value");h.assertTrue(d.deposit(0,s)==0&&s.getCount()==64,"Tagged items do not enter bulk");h.succeed();}
 @GameTest(template="empty") public static void tabsAndRemainders(GameTestHelper h){var d=pack();d.tab=2;ItemStack remain=d.insert(new ItemStack(Items.DIRT,64),false);h.assertTrue(remain.isEmpty()&&d.items.getStackInSlot(54).getCount()==64&&d.items.getStackInSlot(0).isEmpty(),"Routing respects selected tab");for(int i=54;i<81;i++)d.items.setStackInSlot(i,new ItemStack(Items.DIRT,64));remain=d.insert(new ItemStack(Items.DIAMOND,12),false);h.assertTrue(remain.getCount()==12,"Full compartment preserves input");h.succeed();}
 @GameTest(template="empty") public static void capacityRemovalPreservesItems(GameTestHelper h){var d=pack();d.items.setStackInSlot(114,new ItemStack(AdventurersPack.CAPACITY.get()));for(int n=0;n<80;n++)d.deposit(0,new ItemStack(Items.STONE,64));h.assertTrue(d.counts[0]==5120,"Upgrade allows larger capacity");d.items.extractItem(114,1,false);h.assertTrue(d.counts[0]==5120,"Removing capacity upgrade never truncates contents");var s=new ItemStack(Items.STONE,1);h.assertTrue(d.deposit(0,s)==0&&s.getCount()==1,"Over-capacity slot refuses new items");h.succeed();}
 @GameTest(template="empty") public static void protectedOverflow(GameTestHelper h){var d=pack();d.items.setStackInSlot(114,new ItemStack(AdventurersPack.VOID.get()));d.voiding=true;for(int n=0;n<64;n++)d.deposit(0,new ItemStack(Items.STONE,64));for(int n=0;n<27;n++)d.items.setStackInSlot(n,new ItemStack(Items.DIRT,64));d.protectedMask=1;h.assertTrue(d.insert(new ItemStack(Items.STONE,64),true).getCount()==64,"Protected slot never voids overflow");d.protectedMask=0;h.assertTrue(d.insert(new ItemStack(Items.STONE,64),true).isEmpty(),"Enabled overflow consumes only matching excess");h.assertTrue(d.insert(new ItemStack(Items.DIAMOND,7),true).getCount()==7,"Unmatched valuables never voided");h.succeed();}
 @GameTest(template="empty") public static void dockSimulationConserves(GameTestHelper h){h.setBlock(new BlockPos(1,1,1),AdventurersPack.DOCK.get());var dock=(DockEntity)h.getBlockEntity(new BlockPos(1,1,1));dock.setPack(new ItemStack(AdventurersPack.PACK.get()));var handler=dock.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing dock inventory"));var stone=new ItemStack(Items.STONE,64);h.assertTrue(handler.insertItem(252,stone,true).isEmpty()&&dock.data().counts[0]==0,"Simulated insertion does not mutate");handler.insertItem(252,stone,false);h.assertTrue(stone.getCount()==64&&dock.data().counts[0]==64,"Insertion does not mutate caller's stack");h.assertTrue(handler.extractItem(252,32,true).getCount()==32&&dock.data().counts[0]==64,"Simulated extraction does not mutate");h.assertTrue(handler.extractItem(252,32,false).getCount()==32&&dock.data().counts[0]==32,"Extraction conserves count");dock.data().protectedMask=1;h.assertTrue(handler.extractItem(252,32,false).isEmpty(),"Dock respects protection");h.succeed();}

 @GameTest(template="empty") public static void menuTransferAndPackLock(GameTestHelper h){var player=h.makeMockPlayer();var d=pack();player.getInventory().setItem(0,d.owner);var menu=new PackMenu(1,player.getInventory(),0,d);player.containerMenu=menu;
  menu.setCarried(new ItemStack(Items.STONE,64));menu.clicked(PackMenu.BULK,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(menu.getCarried().isEmpty()&&d.counts[0]==64,"Cursor deposit conserves items");
  menu.clicked(PackMenu.BULK,1,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(menu.getCarried().getCount()==1&&d.counts[0]==63,"Right click withdraws exactly one");
  menu.setCarried(ItemStack.EMPTY);menu.quickMoveStack(player,PackMenu.PLAYER+27);h.assertTrue(player.getInventory().getItem(0)==d.owner,"Open pack cannot be shift-clicked into itself");
  d.items.setStackInSlot(27,new ItemStack(Items.DIAMOND,3));menu.clicked(27,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(menu.getCarried().isEmpty()&&d.items.getStackInSlot(27).getCount()==3,"Hidden tab slots reject clicks");h.assertTrue(menu.slots.size()==PackMenu.CAMP_SLOT+23&&menu.slots.get(PackMenu.CAMP_SLOT+1).getSlotIndex()==120&&menu.slots.get(PackMenu.CAMP_SLOT+4).getSlotIndex()==123&&PackMenu.CRAFT==PackMenu.CAROUSEL+4,"Offhand kit, crafting, addons, and the ender chest keep their slot order");
  menu.setCarried(new ItemStack(Items.DIAMOND));menu.clicked(PackMenu.ENDER,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(menu.getCarried().is(Items.DIAMOND),"A closed ender chest rejects clicks");d.items.setStackInSlot(114,new ItemStack(AdventurersPack.ENDER.get()));d.addon=6;menu.clicked(PackMenu.ENDER,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(player.getEnderChestInventory().getItem(0).is(Items.DIAMOND)&&menu.getCarried().isEmpty(),"The open drawer stores into the player ender chest");h.succeed();}
 @GameTest(template="empty") public static void tiersSocketsAndRecipe(GameTestHelper h){var d=pack();h.assertTrue(d.openGear()==6&&d.openRunes()==5&&d.capacity()==4096,"Every pack opens six attachment sockets and five rune sockets");h.assertFalse(d.items.isItemValid(114,new ItemStack(AdventurersPack.PICKUP.get())),"Runes stay out of gear sockets");h.assertTrue(d.items.insertItem(117,new ItemStack(AdventurersPack.PICKUP.get()),false).isEmpty(),"The open rune socket accepts a pickup rune");h.assertTrue(d.items.insertItem(118,new ItemStack(AdventurersPack.VOID.get()),false).isEmpty(),"A second rune uses its own socket");d.tier=2;for(int n=0;n<80;n++)d.deposit(0,new ItemStack(Items.STONE,64));h.assertTrue(d.counts[0]==5120,"Ranger capacity holds more than the base pouch");d.tier=0;d.save();var extra=new ItemStack(Items.STONE,1);h.assertTrue(d.deposit(0,extra)==0&&d.counts[0]==5120,"Lowering the tier never deletes stored items");
  var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,1){public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return net.minecraft.world.item.ItemStack.EMPTY;}public void slotsChanged(net.minecraft.world.Container c){}};var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);var found=h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(AdventurersPack.ID,"explorer_pack"));h.assertTrue(found.isPresent()&&found.get() instanceof PackTierRecipe,"Explorer recipe is loaded");var recipe=(PackTierRecipe)found.get();for(int i:new int[]{0,2,6,8})grid.setItem(i,new ItemStack(Items.LEATHER));grid.setItem(3,new ItemStack(Items.IRON_INGOT));grid.setItem(5,new ItemStack(Items.IRON_INGOT));grid.setItem(4,new ItemStack(Items.GOLD_INGOT));grid.setItem(7,new ItemStack(Items.IRON_INGOT));grid.setItem(1,d.owner);h.assertTrue(recipe.matches(grid,h.getLevel()),"Filled grid matches the explorer recipe");var crafted=recipe.assemble(grid,h.getLevel().registryAccess());h.assertTrue(PackData.tierOf(crafted)==1&&new PackData(crafted).counts[0]==5120,"Crafting the next tier keeps the pack contents");h.succeed();}
 @GameTest(template="empty") public static void routingProjectsAndPresets(GameTestHelper h){var d=pack();d.routing=true;d.tab=2;d.items.setStackInSlot(27,new ItemStack(Items.OAK_PLANKS,1));d.items.setStackInSlot(0,new ItemStack(Items.COBBLESTONE,1));d.items.setStackInSlot(54,new ItemStack(Items.APPLE,1));h.assertTrue(d.insert(new ItemStack(Items.OAK_PLANKS,9),true).isEmpty()&&d.items.getStackInSlot(27).getCount()==10,"Blocks route to Build");h.assertTrue(d.insert(new ItemStack(Items.COBBLESTONE,7),true).isEmpty()&&d.items.getStackInSlot(0).getCount()==8,"Cobblestone routes to Mine");h.assertTrue(d.insert(new ItemStack(Items.APPLE,2),true).isEmpty()&&d.items.getStackInSlot(54).getCount()==3,"Food routes to Loot");d.track(Items.COBBLESTONE);h.assertTrue(d.projectGoals[0]==64,"A new supply goal starts at 64");d.track(Items.COBBLESTONE);h.assertTrue(d.projectGoals[0]==256&&d.collected(Items.COBBLESTONE,null)==8,"Tracking the same item raises the goal");d.applyPreset(PackData.BUILDING);h.assertTrue(d.building&&d.preset==PackData.BUILDING,"Building preset enables placement");d.applyPreset(PackData.MINING);h.assertTrue(!d.building&&d.pickup&&d.preset==PackData.MINING,"Mining preset collects without placing");d.tabNames[1]=PackData.cleanName("  Quarry\n","Build");d.save();h.assertTrue(new PackData(d.owner.copy()).tabNames[1].equals("Quarry"),"Tab names persist");h.succeed();}
 @GameTest(template="empty") public static void smartRoutingMatchingAndOffToInventory(GameTestHelper h){
  var d=pack();d.routing=false;d.tab=0;
  var unrouted=d.insert(new ItemStack(Items.DIRT,16),true);
  h.assertTrue(unrouted.getCount()==16&&d.items.getStackInSlot(0).isEmpty(),"Routing OFF leaves pickups for player inventory");
  d.routing=true;d.tab=0;
  d.items.setStackInSlot(54,new ItemStack(Items.DIRT,10));
  d.items.setStackInSlot(27,new ItemStack(Items.COBBLESTONE,5));
  h.assertTrue(d.insert(new ItemStack(Items.DIRT,6),true).isEmpty(),"Dirt routed into existing matching tab");
  h.assertTrue(d.items.getStackInSlot(54).getCount()==16,"Dirt sorted into Tab 2 where Dirt already was");
  h.assertTrue(d.items.getStackInSlot(0).isEmpty(),"Tab 0 remains untouched despite being active");
  h.assertTrue(d.insert(new ItemStack(Items.COBBLESTONE,10),true).isEmpty(),"Cobblestone routed into Tab 1");
  h.assertTrue(d.items.getStackInSlot(27).getCount()==15,"Cobblestone sorted into Tab 1 where Cobblestone already was");
  h.succeed();
 }
 @GameTest(template="empty") public static void protectionOffhandAndSmelting(GameTestHelper h){h.setBlock(new net.minecraft.core.BlockPos(1,2,1),AdventurersPack.DOCK.get());var dock=(DockEntity)h.getBlockEntity(new net.minecraft.core.BlockPos(1,2,1));var d=pack();d.items.setStackInSlot(4,new ItemStack(Items.DIAMOND,5));d.toggleStorage(4);dock.setPack(d.owner);var handler=dock.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing dock inventory"));h.assertTrue(handler.extractItem(4,5,false).isEmpty()&&dock.data().items.getStackInSlot(4).getCount()==5,"Dock automation leaves protected items");dock.data().toggleStorage(4);h.assertTrue(handler.extractItem(4,5,false).getCount()==5,"Clearing protection allows extraction");
  var kit=pack();kit.offhand.setStackInSlot(0,new ItemStack(Items.SHIELD));var player=h.makeMockPlayer();player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));kit.cycleOffhand(player);h.assertTrue(player.getOffhandItem().is(Items.SHIELD)&&kit.offhand.getStackInSlot(0).is(Items.TORCH),"Offhand kit swaps with the carried item");player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,kit.owner);kit.cycleOffhand(player);h.assertTrue(player.getOffhandItem()==kit.owner,"The pack itself is never moved into the kit");
  var ember=pack();ember.items.setStackInSlot(116,new ItemStack(AdventurersPack.FURNACE.get()));ember.furnace.setStackInSlot(0,new ItemStack(Items.RAW_IRON));ember.furnace.setStackInSlot(1,new ItemStack(Items.COAL,8));for(int i=0;i<400&&ember.furnace.getStackInSlot(2).isEmpty();i++)PackEvents.smelt(h.getLevel(),ember);h.assertTrue(ember.furnace.getStackInSlot(2).is(Items.IRON_INGOT)&&ember.storedXp>0,"Ember Core smelts and stores experience");h.succeed();}
 @GameTest(template="empty") public static void sectionsQuiverPhilterAndBrewing(GameTestHelper h){var d=pack();h.assertTrue(d.openSections()==6,"Every pack opens a socket for each storage addon");h.assertTrue(d.sections.isItemValid(1,new ItemStack(AdventurersPack.QUIVER.get())),"Available category capacity permits any addon position");h.assertFalse(d.items.isItemValid(114,new ItemStack(AdventurersPack.QUIVER.get())),"Section addons stay out of gear sockets");h.assertTrue(d.sections.insertItem(0,new ItemStack(AdventurersPack.QUIVER.get()),false).isEmpty(),"The open section socket accepts the quiver");h.assertTrue(d.sections.isItemValid(4,new ItemStack(AdventurersPack.PHILTER.get())),"Each addon has its own socket");h.assertFalse(d.sections.insertItem(0,new ItemStack(AdventurersPack.PHILTER.get()),false).isEmpty(),"A filled socket does not swap addons");h.assertTrue(d.sections.getStackInSlot(0).is(AdventurersPack.QUIVER.get()),"The quiver stays installed");
  h.assertTrue(d.insert(new ItemStack(Items.ARROW,10),true).isEmpty()&&d.quiver.getStackInSlot(0).getCount()==10,"Arrows fill the quiver before the compartments");d.sections.setStackInSlot(0,ItemStack.EMPTY);h.assertTrue(!d.has(AdventurersPack.QUIVER.get())&&d.quiver.getStackInSlot(0).getCount()==10,"Removing the quiver keeps the arrows");d.routing=true;d.items.setStackInSlot(0,new ItemStack(Items.ARROW,1));h.assertTrue(d.insert(new ItemStack(Items.ARROW,3),true).isEmpty()&&d.items.getStackInSlot(0).getCount()==4,"Without the quiver, arrows use a compartment");
  d.sections.setStackInSlot(0,new ItemStack(AdventurersPack.PHILTER.get()));d.addon=3;var player=h.makeMockPlayer();player.getInventory().setItem(0,d.owner);var menu=new PackMenu(1,player.getInventory(),0,d);player.containerMenu=menu;menu.setCarried(PotionUtils.setPotion(new ItemStack(Items.POTION,2),Potions.HEALING));menu.clicked(PackMenu.PHILTER,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(menu.getCarried().is(Items.POTION)&&menu.getCarried().getCount()==1&&PotionUtils.getPotion(d.vials.getStackInSlot(0))==Potions.HEALING,"Pouring stores one potion");boolean bottle=false;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Items.GLASS_BOTTLE))bottle=true;h.assertTrue(bottle,"A drinkable potion returns its bottle");for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Items.GLASS_BOTTLE))player.getInventory().setItem(i,ItemStack.EMPTY);menu.setCarried(ItemStack.EMPTY);menu.clicked(PackMenu.PHILTER,1,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(PotionUtils.getPotion(d.vials.getStackInSlot(0))==Potions.HEALING,"Taking a drinkable vial needs a glass bottle");player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));menu.clicked(PackMenu.PHILTER,1,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(menu.getCarried().is(Items.POTION)&&d.vials.getStackInSlot(0).isEmpty(),"A glass bottle takes the potion back");
  d.vials.setStackInSlot(0,PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.HEALING));h.assertTrue(d.useVial(player)&&d.vials.getStackInSlot(0).isEmpty(),"The philter drinks the selected vial");
  d.sections.setStackInSlot(1,new ItemStack(AdventurersPack.ALEMBIC.get()));var water=PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.WATER);d.brew.setStackInSlot(0,water);d.brew.setStackInSlot(3,new ItemStack(Items.NETHER_WART));d.brew.setStackInSlot(4,new ItemStack(Items.BLAZE_POWDER));h.assertTrue(net.minecraft.world.item.alchemy.PotionBrewing.hasMix(d.brew.getStackInSlot(0),d.brew.getStackInSlot(3)),"Nether wart mixes with a water bottle");for(int i=0;i<500&&PotionUtils.getPotion(d.brew.getStackInSlot(0))!=Potions.AWKWARD;i++)PackEvents.brew(h.getLevel(),d);h.assertTrue(PotionUtils.getPotion(d.brew.getStackInSlot(0))==Potions.AWKWARD,"The alembic brews an awkward potion");d.brew.setStackInSlot(3,new ItemStack(Items.GLISTERING_MELON_SLICE));for(int i=0;i<500&&d.vials.getStackInSlot(0).isEmpty();i++)PackEvents.brew(h.getLevel(),d);h.assertTrue(PotionUtils.getPotion(d.vials.getStackInSlot(0))==Potions.HEALING&&d.brew.getStackInSlot(0).is(Items.GLASS_BOTTLE),"A finished potion pours into the philter and leaves the bottle");
  d.addon=0;menu.setCarried(new ItemStack(Items.ARROW,8));menu.clicked(PackMenu.QUIVER,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(menu.getCarried().getCount()==8,"A closed quiver rejects clicks");h.succeed();}

 @GameTest(template="empty") public static void toolHandReturnsSameTool(GameTestHelper h){
  var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(9,d.owner);p.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,37));
  var pick=new ItemStack(Items.IRON_PICKAXE);pick.setHoverName(net.minecraft.network.chat.Component.literal("Old reliable"));pick.setDamageValue(11);d.items.setStackInSlot(110,pick);
  h.assertTrue(ToolHand.equip(p,d,110),"Kit tool equips while holding building blocks");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(110).isEmpty()&&p.getMainHandItem().is(Items.IRON_PICKAXE),"Exactly one tool moves into the active hand");
  p.getMainHandItem().setDamageValue(15);ToolHand.restore(p);var returned=new PackData(d.owner).items.getStackInSlot(110);
  h.assertTrue(returned.is(Items.IRON_PICKAXE)&&returned.getDamageValue()==15&&returned.getHoverName().getString().equals("Old reliable"),"Actual tool returns with damage and custom name");
  h.assertTrue(p.getMainHandItem().is(Items.COBBLESTONE)&&p.getMainHandItem().getCount()==37&&!ToolHand.active(p),"Original held stack is restored exactly");
  ToolHand.restore(p);h.assertTrue(new PackData(d.owner).items.getStackInSlot(110).getCount()==1,"Repeated restore never duplicates the tool");h.succeed();
 }
 @GameTest(template="empty") public static void toolHandSwitchAndHotbarChange(GameTestHelper h){
  var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(9,d.owner);p.getInventory().setItem(0,new ItemStack(Items.TORCH,16));d.items.setStackInSlot(108,new ItemStack(Items.IRON_PICKAXE));d.items.setStackInSlot(113,new ItemStack(Items.IRON_AXE));
  ToolHand.equip(p,d,108);ToolHand.equip(p,new PackData(d.owner),113);
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).is(Items.IRON_PICKAXE)&&p.getMainHandItem().is(Items.IRON_AXE),"Switching tools returns the previous tool to its own slot");
  p.getInventory().setItem(1,new ItemStack(Items.APPLE,3));p.getInventory().selected=1;ToolHand.tick(p);
  h.assertTrue(p.getMainHandItem().is(Items.APPLE)&&p.getInventory().getItem(0).is(Items.TORCH)&&new PackData(d.owner).items.getStackInSlot(113).is(Items.IRON_AXE),"Scrolling hotbar restores both home slots without changing the new selection");h.succeed();
 }
 @GameTest(template="empty") public static void toolHandBreakAndOffhandMove(GameTestHelper h){
  var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(9,d.owner);p.getInventory().setItem(0,new ItemStack(Items.DIRT,12));d.items.setStackInSlot(108,new ItemStack(Items.STONE_PICKAXE));ToolHand.equip(p,d,108);
  p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,p.getMainHandItem());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);ToolHand.tick(p);
  h.assertTrue(p.getOffhandItem().isEmpty()&&new PackData(d.owner).items.getStackInSlot(108).is(Items.STONE_PICKAXE)&&p.getMainHandItem().is(Items.DIRT),"Swapping borrowed tool to offhand returns it without duplication");
  ToolHand.equip(p,new PackData(d.owner),108);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);ToolHand.tick(p);
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).isEmpty()&&p.getMainHandItem().getCount()==12,"Broken tools stay broken and original stack still returns");h.succeed();
 }
 @GameTest(template="empty") public static void toolHandFullInventoryAndTimeout(GameTestHelper h){
  var p=h.makeMockPlayer();for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));var d=pack();p.getInventory().setItem(9,d.owner);d.items.setStackInSlot(109,new ItemStack(Items.IRON_SHOVEL));ToolHand.equip(p,d,109);p.tickCount+=11;ToolHand.tick(p);
  h.assertTrue(p.getMainHandItem().getCount()==64&&new PackData(d.owner).items.getStackInSlot(109).is(Items.IRON_SHOVEL)&&!ToolHand.active(p),"Timeout restores into reserved homes even when every inventory slot is occupied");h.succeed();
 }
 @GameTest(template="empty") public static void toolHandSavedSessionAndToss(GameTestHelper h){
  var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(9,d.owner);p.getInventory().setItem(0,new ItemStack(Items.OAK_PLANKS,21));d.items.setStackInSlot(112,new ItemStack(Items.DIAMOND_PICKAXE));ToolHand.equip(p,d,112);
  var other=h.makeMockPlayer();other.getPersistentData().merge(p.getPersistentData().copy());for(int i=0;i<36;i++)other.getInventory().setItem(i,p.getInventory().getItem(i).copy());ToolHand.restore(other);
  h.assertTrue(other.getMainHandItem().getCount()==21&&new PackData(other.getInventory().getItem(9)).items.getStackInSlot(112).is(Items.DIAMOND_PICKAXE),"Saved player data can recover an interrupted tool session");
  ItemStack tossed=p.getMainHandItem();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);ToolHand.restore(p,tossed);
  h.assertTrue(p.getMainHandItem().getCount()==21&&new PackData(d.owner).items.getStackInSlot(112).is(Items.DIAMOND_PICKAXE),"A tossed borrowed tool returns to its kit home");h.succeed();
 }
 @GameTest(template="empty") public static void toolSelectionHarvestAndWornOut(GameTestHelper h){
  var d=pack();d.items.setStackInSlot(108,new ItemStack(Items.GOLDEN_PICKAXE));d.items.setStackInSlot(109,new ItemStack(Items.IRON_PICKAXE));var ore=Blocks.DIAMOND_ORE.defaultBlockState();
  h.assertTrue(ToolHand.bestTool(d,ItemStack.EMPTY,ore)==109,"Harvest capability beats an unsuitable faster pick");var worn=new ItemStack(Items.DIAMOND_PICKAXE);worn.setDamageValue(worn.getMaxDamage()-1);d.items.setStackInSlot(110,worn);
  h.assertTrue(ToolHand.bestTool(d,ItemStack.EMPTY,ore)==109,"Nearly broken tools are not selected automatically");h.succeed();
 }
 @GameTest(template="empty") public static void correctToolSwapsAndReturnsHand(GameTestHelper h){
  var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ToolSwap"));
  var d=pack();p.getInventory().setItem(9,d.owner);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,37));
  d.items.setStackInSlot(108,new ItemStack(Items.IRON_SWORD));d.items.setStackInSlot(109,new ItemStack(Items.IRON_PICKAXE));d.items.setStackInSlot(110,new ItemStack(Items.IRON_AXE));
  d.items.setStackInSlot(111,new ItemStack(Items.IRON_SHOVEL));d.items.setStackInSlot(112,new ItemStack(Items.IRON_HOE));d.items.setStackInSlot(113,new ItemStack(Items.SHEARS));
  ItemStack hand=p.getMainHandItem();
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.STONE.defaultBlockState())==109,"Stone draws the pickaxe");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.DIAMOND_ORE.defaultBlockState())==109,"Ore draws the pickaxe");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.OBSIDIAN.defaultBlockState())<0,"No kit tool can harvest obsidian");
  h.assertTrue(p.getMainHandItem()==hand&&hand.getCount()==37,"An unsuitable block leaves the held stack alone");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.OAK_LOG.defaultBlockState())==110,"Logs draw the axe");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.PUMPKIN.defaultBlockState())==110,"Axe plants draw the axe");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.DIRT.defaultBlockState())==111,"Dirt draws the shovel");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.SAND.defaultBlockState())==111,"Sand draws the shovel");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.GRAVEL.defaultBlockState())==111,"Gravel draws the shovel");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.GRASS_BLOCK.defaultBlockState())==111,"Grass blocks are dug with the shovel");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.OAK_LEAVES.defaultBlockState())==113,"Leaves draw the shears");
  h.assertTrue(ToolHand.bestTool(p,d,new ItemStack(Items.IRON_SWORD),Blocks.OAK_LEAVES.defaultBlockState())==113,"A held sword is not the leaf tool");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.WHITE_WOOL.defaultBlockState())==113,"Wool draws the shears");
  h.assertTrue(ToolHand.bestTool(p,d,hand,Blocks.COBWEB.defaultBlockState())==113,"Cobweb draws the shears");
  h.assertTrue(ToolHand.bestTool(p,d,new ItemStack(Items.WOODEN_PICKAXE),Blocks.STONE.defaultBlockState())<0,"A suitable pickaxe stays in hand");
  h.assertTrue(ToolHand.bestTool(p,d,new ItemStack(Items.WOODEN_PICKAXE),Blocks.DIAMOND_ORE.defaultBlockState())==109,"A pickaxe of the wrong tier swaps for one that can harvest");
  h.assertTrue(ToolHand.bestTool(p,d,new ItemStack(Items.IRON_HOE),Blocks.OAK_LEAVES.defaultBlockState())<0,"A held hoe already harvests leaves");
  var faster=pack();faster.items.setStackInSlot(108,new ItemStack(Items.DIAMOND_SHOVEL));
  h.assertTrue(ToolHand.bestTool(p,faster,new ItemStack(Items.IRON_SHOVEL),Blocks.DIRT.defaultBlockState())<0,"A suitable shovel is not replaced by a faster one");
  h.assertTrue(faster.items.getStackInSlot(108).is(Items.DIAMOND_SHOVEL),"The unused shovel stays in the kit");
  var shearsOnly=pack();shearsOnly.items.setStackInSlot(108,new ItemStack(Items.SHEARS));
  h.assertTrue(ToolHand.bestTool(p,shearsOnly,hand,Blocks.STONE.defaultBlockState())<0,"Shears are not drawn for stone");
  BlockPos grass=new BlockPos(1,1,1);h.setBlock(grass,Blocks.GRASS_BLOCK.defaultBlockState());h.setBlock(grass.above(),Blocks.AIR.defaultBlockState());
  BlockPos at=h.absolutePos(grass);
  h.assertTrue(ToolHand.bestTill(p,d,h.getLevel(),at)==112,"Tillable ground draws the hoe");
  BlockPos dirt=new BlockPos(1,1,2);h.setBlock(dirt,Blocks.DIRT.defaultBlockState());h.setBlock(dirt.above(),Blocks.AIR.defaultBlockState());
  h.assertTrue(ToolHand.bestTill(p,d,h.getLevel(),h.absolutePos(dirt))==112,"Dirt draws the hoe when it can be tilled");
  h.setBlock(new BlockPos(1,1,3),Blocks.STONE.defaultBlockState());
  h.assertTrue(ToolHand.bestTill(p,d,h.getLevel(),h.absolutePos(new BlockPos(1,1,3)))<0,"Stone is not tilled");
  var hoes=pack();hoes.items.setStackInSlot(108,new ItemStack(Items.IRON_HOE));hoes.items.setStackInSlot(109,new ItemStack(Items.DIAMOND_HOE));
  h.assertTrue(ToolHand.bestTill(p,hoes,h.getLevel(),at)==109,"The faster hoe is chosen");
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_HOE));
  h.assertTrue(ToolHand.bestTill(p,hoes,h.getLevel(),at)<0,"A held hoe stays for tilling");
  h.assertTrue(hoes.items.getStackInSlot(109).is(Items.DIAMOND_HOE),"The kit hoe is not taken while one is already held");
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.NETHERITE_AXE));
  PackEvents.selectWeapon(p,d);
  h.assertTrue(p.getMainHandItem().is(Items.NETHERITE_AXE)&&new PackData(d.owner).items.getStackInSlot(110).is(Items.IRON_AXE),"A stronger held weapon stays, and the kit weapon stays put");
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,37));
  h.assertTrue(ToolHand.equip(p,d,109),"The chosen pickaxe is drawn through the normal tool hand");
  h.assertTrue(p.getMainHandItem().is(Items.IRON_PICKAXE)&&copies(p,d.owner,Items.COBBLESTONE)==37,"The cobblestone stays in the inventory while the pick is out");
  h.assertTrue(copies(p,d.owner,Items.IRON_PICKAXE)==1,"Drawing the pickaxe does not duplicate it");
  ToolHand.restore(p);ToolHand.restore(p);
  h.assertTrue(p.getMainHandItem().is(Items.COBBLESTONE)&&p.getMainHandItem().getCount()==37,"The cobblestone stack returns");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(109).is(Items.IRON_PICKAXE)&&copies(p,d.owner,Items.IRON_PICKAXE)==1,"The pickaxe is home and was not duplicated");
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,12));
  PackEvents.selectWeapon(p,new PackData(d.owner));
  h.assertTrue(p.getMainHandItem().is(Items.IRON_AXE),"Attacking draws the strongest kit weapon");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).is(Items.IRON_SWORD),"The sword stays in its own slot");
  h.assertTrue(copies(p,d.owner,Items.DIRT)==12&&copies(p,d.owner,Items.IRON_AXE)==1,"The dirt is stored and the axe exists once");
  ToolHand.restore(p);ToolHand.restore(p);
  h.assertTrue(p.getMainHandItem().is(Items.DIRT)&&p.getMainHandItem().getCount()==12,"The dirt stack returns after the attack");
  PackData back=new PackData(d.owner);
  h.assertTrue(back.items.getStackInSlot(110).is(Items.IRON_AXE)&&back.items.getStackInSlot(108).is(Items.IRON_SWORD)&&copies(p,d.owner,Items.IRON_AXE)==1,"The weapon returns home without a copy");
  var eg=pack();eg.items.setStackInSlot(108,new ItemStack(AdventurersPack.ENDGAME.get()));
  h.assertTrue(ToolHand.bestTool(eg,ItemStack.EMPTY,Blocks.STONE.defaultBlockState())==108,"A rack endgame tool is eligible");
  h.assertTrue(Requiem.form(eg.items.getStackInSlot(108))==6&&eg.items.getStackInSlot(108).getCount()==1,"Endgame takes its pickaxe form once");
  ToolHand.bestTool(eg,ItemStack.EMPTY,Blocks.STONE.defaultBlockState());
  h.assertTrue(eg.items.getStackInSlot(108).getCount()==1&&Requiem.form(eg.items.getStackInSlot(108))==6,"Choosing the endgame pickaxe again does not copy it");
  var egHoe=pack();egHoe.items.setStackInSlot(108,new ItemStack(AdventurersPack.ENDGAME.get()));
  h.assertTrue(ToolHand.bestTill(p,egHoe,h.getLevel(),at)==108,"Endgame can till from the rack");
  h.assertTrue(Requiem.form(egHoe.items.getStackInSlot(108))==9&&egHoe.items.getStackInSlot(108).getCount()==1,"Endgame takes its hoe form once");
  h.succeed();
 }
 private static int copies(net.minecraft.world.entity.player.Player p,ItemStack owner,Item item){int n=0;if(ToolEquipment.active(p)&&ToolEquipment.stack(p).is(item))n+=ToolEquipment.stack(p).getCount();for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();var kit=new PackData(owner);for(int i=108;i<114;i++)if(kit.items.getStackInSlot(i).is(item))n+=kit.items.getStackInSlot(i).getCount();return n;}
 @GameTest(template="empty") public static void everyEndgameFormAutoSwaps(GameTestHelper h){
  h.assertTrue(Requiem.NAMES.length==10,"Endgame has ten forms");
  var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EndgameSwap"));
  BlockPos grass=new BlockPos(1,1,1);h.setBlock(grass,Blocks.GRASS_BLOCK.defaultBlockState());h.setBlock(grass.above(),Blocks.AIR.defaultBlockState());
  BlockPos dirt=new BlockPos(1,1,2);h.setBlock(dirt,Blocks.DIRT.defaultBlockState());h.setBlock(dirt.above(),Blocks.AIR.defaultBlockState());
  h.setBlock(new BlockPos(1,1,3),Blocks.STONE.defaultBlockState());
  BlockPos tillGrass=h.absolutePos(grass),tillDirt=h.absolutePos(dirt),tillStone=h.absolutePos(new BlockPos(1,1,3));
  int pick=uniqueForm(item->item instanceof PickaxeItem),axe=uniqueForm(item->item instanceof AxeItem),shovel=uniqueForm(item->item instanceof ShovelItem),hoe=uniqueForm(item->item instanceof HoeItem);
  h.assertTrue(pick>=0&&axe>=0&&shovel>=0&&hoe>=0,"Pickaxe, axe, shovel, and hoe forms all exist");
  int firstSword=-1;for(int i=0;i<Requiem.NAMES.length;i++)if(Requiem.item(i) instanceof SwordItem){firstSword=i;break;}
  h.assertTrue(firstSword>=0,"A sword form exists");
  int strongest=strongestAttackForm();
  h.assertTrue(strongest>=0,"A weapon form exists");
  boolean[] started=new boolean[Requiem.NAMES.length];
  for(int start=0;start<Requiem.NAMES.length;start++){
   started[start]=true;
   String name=Requiem.NAMES[start];
   for(var state:new net.minecraft.world.level.block.state.BlockState[]{Blocks.STONE.defaultBlockState(),Blocks.DIAMOND_ORE.defaultBlockState(),Blocks.ANCIENT_DEBRIS.defaultBlockState()})
    assertRackForm(h,p,start,state,pick,name+" mines pickaxe blocks as the pickaxe");
   for(var state:new net.minecraft.world.level.block.state.BlockState[]{Blocks.OAK_LOG.defaultBlockState(),Blocks.PUMPKIN.defaultBlockState()})
    assertRackForm(h,p,start,state,axe,name+" chops axe blocks as the axe");
   for(var state:new net.minecraft.world.level.block.state.BlockState[]{Blocks.DIRT.defaultBlockState(),Blocks.SAND.defaultBlockState(),Blocks.GRAVEL.defaultBlockState(),Blocks.GRASS_BLOCK.defaultBlockState()})
    assertRackForm(h,p,start,state,shovel,name+" digs shovel blocks as the shovel");
   assertRackForm(h,p,start,Blocks.COBWEB.defaultBlockState(),Requiem.item(start) instanceof SwordItem?start:firstSword,name+" cuts cobweb with a sword form");
   assertRackUntouched(h,p,start,Blocks.WHITE_WOOL.defaultBlockState(),name+" has no shears form for wool");
   assertTill(h,p,start,tillGrass,hoe,name+" tills grass as the hoe");
   assertTill(h,p,start,tillDirt,hoe,name+" tills dirt as the hoe");
   assertTillUntouched(h,p,start,tillStone,name+" does not till stone");
   if(start!=pick)assertHeldSwitches(h,p,start,Blocks.STONE.defaultBlockState(),pick,name+" in hand becomes the pickaxe");
   if(start!=axe)assertHeldSwitches(h,p,start,Blocks.OAK_LOG.defaultBlockState(),axe,name+" in hand becomes the axe");
   if(start!=shovel)assertHeldSwitches(h,p,start,Blocks.DIRT.defaultBlockState(),shovel,name+" in hand becomes the shovel");
   if(start!=hoe)assertHeldTills(h,p,start,tillGrass,hoe,name+" in hand becomes the hoe");
   int attack=attackForm(start)?start:strongest;
   assertAttack(h,p,start,attack,name+" attacks as "+Requiem.NAMES[attack]);
  }
  for(int i=0;i<started.length;i++)h.assertTrue(started[i],"Form "+i+" was skipped");
  assertHeldStays(h,p,pick,Blocks.STONE.defaultBlockState(),"Held pickaxe stays for stone");
  assertHeldStays(h,p,axe,Blocks.OAK_LOG.defaultBlockState(),"Held axe stays for logs");
  assertHeldStays(h,p,shovel,Blocks.DIRT.defaultBlockState(),"Held shovel stays for dirt");
  p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,endgameForm(hoe));
  var heldHoe=pack();p.getInventory().setItem(9,heldHoe.owner);ItemStack hoeStack=p.getMainHandItem();
  h.assertTrue(ToolHand.bestTill(p,heldHoe,h.getLevel(),tillGrass)<0&&p.getMainHandItem()==hoeStack,"Held hoe stays for tilling");
  for(int form=0;form<Requiem.NAMES.length;form++)if(attackForm(form)){
   var d=pack();p.getInventory().setItem(9,d.owner);d.items.setStackInSlot(109,new ItemStack(Items.DIAMOND_SWORD));
   p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,endgameForm(form));ItemStack hand=p.getMainHandItem();
   PackEvents.selectWeapon(p,d);
   h.assertTrue(p.getMainHandItem()==hand&&Requiem.form(hand)==form,"Held "+Requiem.NAMES[form]+" is not swapped on attack");
   h.assertTrue(d.items.getStackInSlot(109).is(Items.DIAMOND_SWORD),"The rack sword stays while an Endgame weapon is already held");
   h.assertTrue(endgameCopies(p,d.owner)==1,"Held weapon was not copied");
  }
  var named=endgameForm(2);named.setHoverName(net.minecraft.network.chat.Component.literal("My Endgame"));
  var lease=pack();p.getInventory().setItem(9,lease.owner);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,37));
  lease.items.setStackInSlot(108,named);
  h.assertTrue(ToolHand.bestTool(p,lease,p.getMainHandItem(),Blocks.STONE.defaultBlockState())==108,"Shortbow in the rack is drawn as a pickaxe");
  h.assertTrue(Requiem.form(lease.items.getStackInSlot(108))==pick&&lease.items.getStackInSlot(108).getHoverName().getString().equals("My Endgame"),"The same named item takes the pickaxe form");
  h.assertTrue(ToolHand.equip(p,lease,108),"Pickaxe form is leased");
  h.assertTrue(p.getMainHandItem().getItem() instanceof PickaxeItem&&copies(p,lease.owner,Items.COBBLESTONE)==37&&endgameCopies(p,lease.owner)==1,"Cobblestone leaves and the Endgame item exists once");
  ToolHand.restore(p);ToolHand.restore(p);
  h.assertTrue(p.getMainHandItem().is(Items.COBBLESTONE)&&p.getMainHandItem().getCount()==37,"Cobblestone returns when the button is released");
  var back=new PackData(lease.owner);
  h.assertTrue(Requiem.form(back.items.getStackInSlot(108))==pick&&back.items.getStackInSlot(108).getCount()==1&&back.items.getStackInSlot(108).getHoverName().getString().equals("My Endgame")&&endgameCopies(p,lease.owner)==1,"The named Endgame tool returns home once");
  var fight=pack();p.getInventory().setItem(9,fight.owner);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,12));
  fight.items.setStackInSlot(108,endgameForm(shovel));
  PackEvents.selectWeapon(p,fight);
  h.assertTrue(Requiem.form(p.getMainHandItem())==strongest&&endgameCopies(p,fight.owner)==1&&copies(p,fight.owner,Items.DIRT)==12,"A shovel form attacks as the strongest weapon and the dirt is stored");
  ToolHand.restore(p);ToolHand.restore(p);
  h.assertTrue(p.getMainHandItem().is(Items.DIRT)&&p.getMainHandItem().getCount()==12,"Dirt returns after the attack");
  h.assertTrue(Requiem.form(new PackData(fight.owner).items.getStackInSlot(108))==strongest&&endgameCopies(p,fight.owner)==1,"The weapon form returns home without a copy");
  h.succeed();
 }
 private static ItemStack endgameForm(int form){return Requiem.transformed(new ItemStack(AdventurersPack.ENDGAME.get()),form);}
 private static int uniqueForm(java.util.function.Predicate<Item> test){int found=-1;for(int i=0;i<Requiem.NAMES.length;i++)if(test.test(Requiem.item(i))){if(found>=0)return -2;found=i;}return found;}
 private static boolean attackForm(int form){Item item=Requiem.item(form);return item instanceof SwordItem||item instanceof BowItem||item instanceof AxeItem||item instanceof TridentItem;}
 private static int strongestAttackForm(){int best=-1,score=0;for(int f=0;f<Requiem.NAMES.length;f++){if(!attackForm(f))continue;int s=PackEvents.weaponScore(endgameForm(f));if(s>score){score=s;best=f;}}return best;}
 private static PackData rack(net.minecraft.world.entity.player.Player p,int form){var d=pack();p.getInventory().setItem(9,d.owner);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COBBLESTONE,37));d.items.setStackInSlot(108,endgameForm(form));return d;}
 private static void assertRackForm(GameTestHelper h,net.minecraft.world.entity.player.Player p,int start,net.minecraft.world.level.block.state.BlockState state,int expect,String message){var d=rack(p,start);int slot=ToolHand.bestTool(p,d,p.getMainHandItem(),state);ItemStack got=d.items.getStackInSlot(108);h.assertTrue(slot==108&&Requiem.form(got)==expect&&got.getCount()==1&&p.getMainHandItem().is(Items.COBBLESTONE)&&p.getMainHandItem().getCount()==37,message+" got "+Requiem.form(got));}
 private static void assertRackUntouched(GameTestHelper h,net.minecraft.world.entity.player.Player p,int start,net.minecraft.world.level.block.state.BlockState state,String message){var d=rack(p,start);h.assertTrue(ToolHand.bestTool(p,d,p.getMainHandItem(),state)<0&&Requiem.form(d.items.getStackInSlot(108))==start,message);}
 private static void assertTill(GameTestHelper h,net.minecraft.world.entity.player.Player p,int start,BlockPos pos,int expect,String message){var d=rack(p,start);int slot=ToolHand.bestTill(p,d,p.level(),pos);h.assertTrue(slot==108&&Requiem.form(d.items.getStackInSlot(108))==expect&&d.items.getStackInSlot(108).getCount()==1,message);}
 private static void assertTillUntouched(GameTestHelper h,net.minecraft.world.entity.player.Player p,int start,BlockPos pos,String message){var d=rack(p,start);h.assertTrue(ToolHand.bestTill(p,d,p.level(),pos)<0&&Requiem.form(d.items.getStackInSlot(108))==start,message);}
 private static void assertHeldSwitches(GameTestHelper h,net.minecraft.world.entity.player.Player p,int start,net.minecraft.world.level.block.state.BlockState state,int expect,String message){var d=pack();p.getInventory().setItem(9,d.owner);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,endgameForm(start));h.assertTrue(ToolHand.bestTool(p,d,p.getMainHandItem(),state)<0&&Requiem.form(p.getMainHandItem())==expect&&p.getMainHandItem().getCount()==1&&endgameCopies(p,d.owner)==1,message);}
 private static void assertHeldTills(GameTestHelper h,net.minecraft.world.entity.player.Player p,int start,BlockPos pos,int expect,String message){var d=pack();p.getInventory().setItem(9,d.owner);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,endgameForm(start));h.assertTrue(ToolHand.bestTill(p,d,p.level(),pos)<0&&Requiem.form(p.getMainHandItem())==expect&&endgameCopies(p,d.owner)==1,message);}
 private static void assertHeldStays(GameTestHelper h,net.minecraft.world.entity.player.Player p,int form,net.minecraft.world.level.block.state.BlockState state,String message){var d=pack();p.getInventory().setItem(9,d.owner);d.items.setStackInSlot(110,new ItemStack(Items.DIAMOND_PICKAXE));p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,endgameForm(form));ItemStack hand=p.getMainHandItem();h.assertTrue(ToolHand.bestTool(p,d,hand,state)<0&&p.getMainHandItem()==hand&&d.items.getStackInSlot(110).is(Items.DIAMOND_PICKAXE),message);}
 private static void assertAttack(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,int start,int expect,String message){var d=pack();p.getInventory().setItem(9,d.owner);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT,12));d.items.setStackInSlot(108,endgameForm(start));PackEvents.selectWeapon(p,d);h.assertTrue(Requiem.form(p.getMainHandItem())==expect&&p.getMainHandItem().getCount()==1&&endgameCopies(p,d.owner)==1&&copies(p,d.owner,Items.DIRT)==12,message+" got "+Requiem.form(p.getMainHandItem()));ToolHand.restore(p);ToolHand.restore(p);h.assertTrue(p.getMainHandItem().is(Items.DIRT)&&p.getMainHandItem().getCount()==12&&Requiem.form(new PackData(d.owner).items.getStackInSlot(108))==expect&&endgameCopies(p,d.owner)==1,message+" returns the dirt and the weapon");}
 private static int endgameCopies(net.minecraft.world.entity.player.Player p,ItemStack owner){int n=0;if(ToolEquipment.active(p)&&Requiem.form(ToolEquipment.stack(p))>=0)n+=ToolEquipment.stack(p).getCount();for(int i=0;i<p.getInventory().getContainerSize();i++)if(Requiem.form(p.getInventory().getItem(i))>=0)n+=p.getInventory().getItem(i).getCount();if(owner!=null&&!owner.isEmpty()){var kit=new PackData(owner);for(int i=108;i<114;i++)if(Requiem.form(kit.items.getStackInSlot(i))>=0)n+=kit.items.getStackInSlot(i).getCount();for(int i=0;i<252;i++)if(Requiem.form(kit.storage(i))>=0)n+=kit.storage(i).getCount();}return n;}
 @GameTest(template="empty") public static void quiverQueriesConserveAmmo(GameTestHelper h){
  var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(9,d.owner);d.quiver.setStackInSlot(0,new ItemStack(Items.ARROW,7));
  for(int i=0;i<5;i++){QuiverAccess.borrow(p,PackItem.data(p,d.owner),s->s.is(Items.ARROW));QuiverAccess.flush(p);}
  h.assertTrue(new PackData(d.owner).quiver.getStackInSlot(0).getCount()==7,"Checking ammo repeatedly consumes nothing");
  ItemStack ammo=QuiverAccess.borrow(p,PackItem.data(p,d.owner),s->s.is(Items.ARROW));ammo.shrink(1);QuiverAccess.flush(p);QuiverAccess.flush(p);
  h.assertTrue(new PackData(d.owner).quiver.getStackInSlot(0).getCount()==6,"Actual vanilla consumption is committed exactly once");h.succeed();
 }
 @GameTest(template="empty") public static void healingVialActuallyHeals(GameTestHelper h){
  var p=h.makeMockPlayer();p.setHealth(8);var d=pack();d.sections.setStackInSlot(0,new ItemStack(AdventurersPack.PHILTER.get()));d.vials.setStackInSlot(0,PotionUtils.setPotion(new ItemStack(Items.POTION),Potions.HEALING));
  h.assertTrue(d.useVial(p)&&p.getHealth()>8&&d.vials.getStackInSlot(0).isEmpty(),"Instant healing changes health and consumes exactly one vial");h.succeed();
 }
 @GameTest(template="empty") public static void workstationVisibilityAndCraftReturn(GameTestHelper h){
  var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(0,d.owner);d.items.setStackInSlot(114,new ItemStack(AdventurersPack.CRAFTING.get()));var menu=new PackMenu(1,p.getInventory(),0,d);p.containerMenu=menu;
  menu.clickMenuButton(p,40);h.assertTrue(menu.slots.get(PackMenu.CRAFT).isActive()&&!menu.slots.get(114).isActive(),"Craft grid replaces sockets in the workstation panel");
  menu.slots.get(PackMenu.CRAFT).set(new ItemStack(Items.DIAMOND,5));menu.clickMenuButton(p,81);
  h.assertTrue(!menu.slots.get(PackMenu.CRAFT).isActive()&&menu.slots.get(PackMenu.CRAFT).getItem().isEmpty(),"Switching to controls closes and clears the craft grid");
  int diamonds=0;for(var item:p.getInventory().items)if(item.is(Items.DIAMOND))diamonds+=item.getCount();h.assertTrue(diamonds==5,"All crafting ingredients return to player inventory");h.succeed();
 }

 @GameTest(template="empty") public static void expandedStorageAndLegacySlots(GameTestHelper h){var d=pack();d.items.setStackInSlot(110,new ItemStack(Items.DIAMOND_PICKAXE));for(int tier=0;tier<5;tier++){d.tier=tier;h.assertTrue(d.pageSize()==27+tier*9,"Each tier adds a row");for(int page=0;page<4;page++)for(int cell=0;cell<d.pageSize();cell++)d.insertStorage(d.storageId(page,cell),new ItemStack(Items.STONE),false);}d.toggleStorage(251);d.save();var loaded=new PackData(d.owner.copy());h.assertTrue(loaded.extra.getStackInSlot(143).is(Items.STONE)&&loaded.protectedStorage(251),"Last extra slot and protection persist");h.assertTrue(loaded.items.getStackInSlot(110).is(Items.DIAMOND_PICKAXE),"New storage never overlaps old tools");h.succeed();}
 @GameTest(template="empty") public static void secondBulkRowAndMenuProtection(GameTestHelper h){var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(9,d.owner);var m=new PackMenu(1,p.getInventory(),9,d);p.containerMenu=m;m.setCarried(new ItemStack(Items.COBBLESTONE,32));m.clicked(m.bulkSlot(17),0,net.minecraft.world.inventory.ClickType.PICKUP,p);h.assertTrue(d.counts[17]==32&&m.getCarried().isEmpty(),"Last bulk slot accepts deposit");m.clickMenuButton(p,2117);m.clickMenuButton(p,2017);var loaded=new PackData(d.owner.copy());h.assertTrue(loaded.protectedSlot(17)&&loaded.selected==17,"High protection bits and selected bulk persist");m.clicked(m.bulkSlot(17),1,net.minecraft.world.inventory.ClickType.PICKUP,p);h.assertTrue(m.getCarried().getCount()==1&&d.counts[17]==31,"Second row withdraws exactly one");h.succeed();}
 @GameTest(template="empty") public static void separatePacksSharedPlayer(GameTestHelper h){var p=h.makeMockPlayer();var equipped=pack();var held=pack();PackEquipment.get(p).items.setStackInSlot(0,equipped.owner);p.getInventory().setItem(0,held.owner);p.getInventory().setItem(10,new ItemStack(Items.APPLE,7));equipped.items.setStackInSlot(0,new ItemStack(Items.DIAMOND,3));held.items.setStackInSlot(0,new ItemStack(Items.EMERALD,5));h.assertTrue(PackItem.find(p)==41,"Equipped pack takes priority");var a=new PackMenu(1,p.getInventory(),41,equipped);var b=new PackMenu(2,p.getInventory(),0,held);h.assertTrue(a.slots.get(0).getItem().is(Items.DIAMOND)&&b.slots.get(0).getItem().is(Items.EMERALD),"Each bag keeps its own contents");h.assertTrue(a.slots.get(PackMenu.PLAYER+1).getItem()==b.slots.get(PackMenu.PLAYER+1).getItem(),"Personal inventory is shared");h.succeed();}
 @GameTest(template="empty") public static void emptyEquipmentPageCannotEatItems(GameTestHelper h){var p=h.makeMockPlayer();var m=new PackMenu(1,p.getInventory(),41,new PackData(ItemStack.EMPTY));p.containerMenu=m;m.setCarried(new ItemStack(Items.DIAMOND,4));m.clicked(PackMenu.BULK,0,net.minecraft.world.inventory.ClickType.PICKUP,p);m.clicked(0,0,net.minecraft.world.inventory.ClickType.PICKUP,p);h.assertTrue(m.getCarried().getCount()==4,"No phantom storage exists without a backpack");h.succeed();}
 @GameTest(template="empty") public static void manualToolsStayAndReturnToEquippedPack(GameTestHelper h){var p=h.makeMockPlayer();var d=pack();PackEquipment.get(p).items.setStackInSlot(0,d.owner);p.getInventory().setItem(0,new ItemStack(Items.TORCH,23));d.items.setStackInSlot(111,new ItemStack(Items.IRON_AXE));ToolHand.draw(p,d,3);p.tickCount+=100;ToolHand.tick(p);h.assertTrue(ToolHand.manual(p)&&p.getMainHandItem().is(Items.IRON_AXE),"Manual draw survives idle timeout");p.getMainHandItem().setDamageValue(9);ToolHand.restore(p);h.assertTrue(new PackData(d.owner).items.getStackInSlot(111).getDamageValue()==9&&p.getMainHandItem().getCount()==23,"Equipped pack receives original tool with durability");h.succeed();}
 @GameTest(template="empty") public static void pouchFiltersRemovalAndRouting(GameTestHelper h){var d=pack();d.pouches.setStackInSlot(0,new ItemStack(AdventurersPack.FORAGER.get()));d.pouches.setStackInSlot(1,new ItemStack(AdventurersPack.GEM.get()));h.assertTrue(d.insert(new ItemStack(Items.WHEAT_SEEDS,17),true).isEmpty(),"Seeds routed");h.assertTrue(d.pouchStorage[0].getStackInSlot(0).getCount()==17,"Seeds enter forager");h.assertTrue(!d.pouchStorage[0].insertItem(1,new ItemStack(Items.DIAMOND),false).isEmpty(),"Wrong category rejected");h.assertTrue(d.insert(new ItemStack(Items.DIAMOND,8),true).isEmpty()&&d.pouchStorage[1].getStackInSlot(0).getCount()==8,"Gems enter gem pouch");ItemStack removed=d.pouches.extractItem(0,1,false);h.assertTrue(d.pouchStorage[0].getStackInSlot(0).isEmpty(),"Removed pouch contents are not left behind");var second=pack();second.pouches.setStackInSlot(0,removed);h.assertTrue(second.pouchStorage[0].getStackInSlot(0).getCount()==17,"Contents travel with pouch to another backpack");h.assertTrue(!second.pouchStorage[0].isItemValid(0,second.owner),"Recursive storage rejected");h.succeed();}
 @GameTest(template="empty") public static void provisionPouchLegacyAttachment(GameTestHelper h){var d=pack();d.items.setStackInSlot(114,new ItemStack(AdventurersPack.FEEDING.get()));d.insert(new ItemStack(Items.APPLE,12),false);h.assertTrue(d.pouchStorage[2].getStackInSlot(0).getCount()==12,"Existing provision attachment gains food storage");ItemStack old=d.items.extractItem(114,1,false);d.pouches.setStackInSlot(2,old);h.assertTrue(d.pouchStorage[2].getStackInSlot(0).getCount()==12,"Moving provision to its socket preserves food");h.succeed();}
 @GameTest(template="empty") public static void campDeployFoldAndRedeploy(GameTestHelper h){
  var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CampTest"));var origin=h.absolutePos(new BlockPos(0,2,0));p.setPos(origin.getX(),origin.getY(),origin.getZ());p.setYRot(0);BlockPos base=p.blockPosition().relative(p.getDirection(),3);
  for(int x=0;x<3;x++)for(int z=0;z<2;z++){h.getLevel().setBlock(base.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);for(int y=0;y<3;y++)h.getLevel().setBlock(base.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);}
  for(var item:h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(base).inflate(4)))item.discard();var d=pack();d.camp.setStackInSlot(0,new ItemStack(AdventurersPack.CAMP_KIT.get()));CampKit.action(p,d,0);h.assertTrue(h.getLevel().getBlockState(base).is(net.minecraft.world.level.block.Blocks.RED_BED),"Camp deploys on clear terrain");var barrel=(net.minecraft.world.Container)h.getLevel().getBlockEntity(base.offset(2,0,0));var furnace=(net.minecraft.world.Container)h.getLevel().getBlockEntity(base.offset(2,0,1));barrel.setItem(0,new ItemStack(Items.DIAMOND,13));furnace.setItem(1,new ItemStack(Items.COAL,7));CampKit.action(p,d,1);h.assertTrue(h.getLevel().isEmptyBlock(base)&&d.camp.getStackInSlot(0).getTag().contains("Packed"),"Camp folds into the kit");h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(base).inflate(4)).isEmpty(),"Folding creates no furniture or inventory drops");var loaded=new PackData(d.owner.copy());CampKit.action(p,loaded,0);h.assertTrue(h.getLevel().getBlockEntity(base.offset(2,0,0))!=null,"Camp redeploys after fold; kit="+loaded.camp.getStackInSlot(0));h.assertTrue(((net.minecraft.world.Container)h.getLevel().getBlockEntity(base.offset(2,0,0))).getItem(0).getCount()==13,"Packed supplies survive save/load and redeploy");h.assertTrue(((net.minecraft.world.Container)h.getLevel().getBlockEntity(base.offset(2,0,1))).getItem(1).getCount()==7,"Fuel survives fold");CampKit.action(p,loaded,1);h.succeed();
 }
 @GameTest(template="empty") public static void shiftClickEquipsLikeInventory(GameTestHelper h){
  var p=h.makeMockPlayer();var d=pack();p.getInventory().setItem(8,d.owner);var m=new PackMenu(1,p.getInventory(),8,d);p.containerMenu=m;
  p.getInventory().setItem(9,new ItemStack(Items.DIAMOND_HELMET));m.quickMoveStack(p,PackMenu.PLAYER);
  h.assertTrue(p.getInventory().getItem(39).is(Items.DIAMOND_HELMET)&&p.getInventory().getItem(9).isEmpty(),"Shift-click wears a helmet");
  p.getInventory().setItem(10,new ItemStack(Items.DIAMOND_LEGGINGS));m.quickMoveStack(p,PackMenu.PLAYER+1);
  h.assertTrue(p.getInventory().getItem(37).is(Items.DIAMOND_LEGGINGS),"Shift-click wears leggings");
  p.getInventory().setItem(11,new ItemStack(Items.DIAMOND_BOOTS));m.quickMoveStack(p,PackMenu.PLAYER+2);
  h.assertTrue(p.getInventory().getItem(36).is(Items.DIAMOND_BOOTS),"Shift-click wears boots");
  p.getInventory().setItem(0,new ItemStack(Items.SHIELD));m.quickMoveStack(p,PackMenu.PLAYER+27);
  h.assertTrue(p.getOffhandItem().is(Items.SHIELD)&&p.getInventory().getItem(0).isEmpty(),"Shift-click puts a shield in the offhand");
  d.items.setStackInSlot(0,new ItemStack(Items.DIAMOND_CHESTPLATE));m.quickMoveStack(p,0);
  h.assertTrue(p.getInventory().getItem(38).is(Items.DIAMOND_CHESTPLATE)&&d.items.getStackInSlot(0).isEmpty(),"Armor in the pack equips when the slot is free");
  p.getInventory().setItem(12,new ItemStack(Items.ELYTRA));m.quickMoveStack(p,PackMenu.PLAYER+3);
  h.assertTrue(p.getInventory().getItem(38).is(Items.DIAMOND_CHESTPLATE)&&d.items.getStackInSlot(0).is(Items.ELYTRA),"A filled chest slot keeps its armor and stores the extra piece");
  m.quickMoveStack(p,171);
  h.assertTrue(p.getInventory().getItem(39).isEmpty()&&p.getInventory().getItem(9).is(Items.DIAMOND_HELMET),"Shift-click takes armor off into the inventory");
  d.items.setStackInSlot(2,new ItemStack(Items.DIAMOND,11));m.quickMoveStack(p,2);
  h.assertTrue(p.getInventory().getItem(7).getCount()==11&&p.getInventory().getItem(9).is(Items.DIAMOND_HELMET),"Items leaving the pack fill the hotbar before the inventory");
  p.getInventory().setItem(13,new ItemStack(Items.COBBLESTONE,32));m.quickMoveStack(p,PackMenu.PLAYER+4);
  h.assertTrue(p.getInventory().getItem(13).isEmpty()&&d.items.getStackInSlot(1).getCount()==32,"Other items still shift-click into the pack");
  h.succeed();
 }
 @GameTest(template="empty") public static void blockedCampLeavesKitUntouched(GameTestHelper h){var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"BlockedCamp"));var at=h.absolutePos(new BlockPos(0,2,0));p.setPos(at.getX(),at.getY(),at.getZ());p.setYRot(0);var base=p.blockPosition().relative(p.getDirection(),3);h.getLevel().setBlock(base,net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(),2);var d=pack();d.camp.setStackInSlot(0,new ItemStack(AdventurersPack.CAMP_KIT.get()));CampKit.action(p,d,0);h.assertTrue(h.getLevel().getBlockState(base).is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK)&&!d.camp.getStackInSlot(0).getTag().getBoolean("Deployed"),"Blocked camp never overwrites terrain");h.succeed();}
}

