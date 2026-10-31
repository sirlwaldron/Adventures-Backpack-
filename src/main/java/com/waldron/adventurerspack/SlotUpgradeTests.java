package com.waldron.adventurerspack;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.gametest.*;
@GameTestHolder("slotupgrades")
@PrefixGameTestTemplate(false)
public class SlotUpgradeTests {
 @GameTest(template="empty") public static void countsAndPersistence(GameTestHelper h){
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));d.tier=4;
  for(int t=1;t<=5;t++){d.slotTier=t;int cap=new int[]{64,384,768,1536,3072,6144}[t];
   for(int id:new int[]{0,251}){d.storageHandler(id).setStackInSlot(d.localSlot(id),ItemStack.EMPTY);
    ItemStack sample=new ItemStack(Items.STONE,64);sample.getOrCreateTag().putString("Test","preserved");
    for(int i=0;i<cap/64;i++)h.assertTrue(d.insertStorage(id,sample,false).isEmpty(),"Every full stack fits");
    h.assertTrue(d.insertStorage(id,sample,true).getCount()==64&&d.storage(id).getCount()==cap,"Full simulation conserves input");
   }
   d.save();var loaded=new PackData(d.owner.copy());
   h.assertTrue(loaded.slotTier==t&&loaded.storage(0).getCount()==cap&&loaded.storage(251).getCount()==cap,"Both storage regions persist integer counts");
   h.assertTrue(loaded.extractStorage(251,9999,true).getCount()==64&&loaded.storage(251).getCount()==cap,"Simulation caps withdrawal without mutation");
   h.assertTrue(loaded.extractStorage(0,9999,false).getCount()==64&&loaded.storage(0).getCount()==cap-64,"Withdrawal produces a legal stack");
  }
  h.assertTrue(d.insertStorage(1,new ItemStack(Items.DIAMOND_SWORD),false).isEmpty()&&!d.insertStorage(1,new ItemStack(Items.DIAMOND_SWORD),false).isEmpty(),"Tools remain single items");h.succeed();
 }
 @GameTest(template="empty") public static void dockAndLegacy(GameTestHelper h){
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));var old=new net.minecraftforge.items.ItemStackHandler(PackData.TOTAL);old.setStackInSlot(0,new ItemStack(Items.STONE,64));d.owner.getOrCreateTagElement("Pack").put("Items",old.serializeNBT());var restored=new PackData(d.owner.copy());h.assertTrue(restored.storage(0).getCount()==64&&restored.slotTier==0&&restored.slotCapacity()==64,"Legacy stacks load unchanged");
  d.slotTier=5;d.save();var pos=new net.minecraft.core.BlockPos(1,1,1);h.setBlock(pos,AdventurersPack.DOCK.get());var dock=(DockEntity)h.getBlockEntity(pos);dock.setPack(d.owner);var handler=dock.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("No handler"));
  for(int i=0;i<96;i++)handler.insertItem(0,new ItemStack(Items.STONE,64),false);
  h.assertTrue(handler.getSlotLimit(0)==6144&&handler.getStackInSlot(0).getCount()==6144,"Dock fills upgraded slots");h.assertTrue(handler.extractItem(0,9999,true).getCount()==64&&handler.getStackInSlot(0).getCount()==6144,"Dock simulation conserves count");dock.data().toggleStorage(0);h.assertTrue(handler.extractItem(0,64,false).isEmpty(),"Protection still blocks automation");h.succeed();
 }
 @GameTest(template="empty") public static void menuAndUpgradeCosts(GameTestHelper h){
  var p=new net.minecraft.world.entity.player.Player(h.getLevel(),net.minecraft.core.BlockPos.ZERO,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"slot_test")){public boolean isSpectator(){return false;}public boolean isCreative(){return false;}};var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));p.getInventory().setItem(0,d.owner);var m=new PackMenu(1,p.getInventory(),0,d);p.containerMenu=m;
  h.assertTrue(!m.clickMenuButton(p,5302)&&!m.clickMenuButton(p,5301),"Cannot skip tiers or buy without materials");
  p.getInventory().setItem(1,new ItemStack(Items.IRON_INGOT,20));h.assertTrue(m.clickMenuButton(p,5301)&&d.slotTier==1&&p.getInventory().getItem(1).getCount()==4,"Upgrade consumes exact materials");
  h.assertTrue(!m.clickMenuButton(p,5301),"Cannot purchase the same tier twice");
  for(int i=0;i<2;i++){m.setCarried(new ItemStack(Items.STONE,64));m.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(m.getCarried().isEmpty(),"Cursor deposit consumed");}
  h.assertTrue(d.storage(0).getCount()==128&&m.slots.get(0).getItem().getCount()==64,"Menu displays legal stack while retaining full count");
  m.clicked(0,1,ClickType.PICKUP,p);h.assertTrue(m.getCarried().getCount()==1&&d.storage(0).getCount()==127,"Right click takes one");m.setCarried(ItemStack.EMPTY);
  m.clicked(0,0,ClickType.PICKUP,p);h.assertTrue(m.getCarried().getCount()==64&&d.storage(0).getCount()==63,"Left click takes a normal stack");m.setCarried(ItemStack.EMPTY);
  m.clicked(0,0,ClickType.QUICK_MOVE,p);h.assertTrue(d.storage(0).isEmpty()&&p.getInventory().countItem(Items.STONE)==63,"Shift click transfers safely");h.succeed();
 }
}
