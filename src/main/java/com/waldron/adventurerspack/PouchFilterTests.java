package com.waldron.adventurerspack;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("pouchfilters") @PrefixGameTestTemplate(false)
public class PouchFilterTests {
 @GameTest(template="empty") public static void filtersPersistAndConserveItems(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  p.getInventory().setItem(0,d.owner);d.pouches.setStackInSlot(3,new ItemStack(AdventurersPack.CARTOGRAPHER.get()));
  var menu=new PackMenu(1,p.getInventory(),0,d);menu.clickMenuButton(p,93);
  var pouch=d.pouchStorage[3];var paper=new ItemStack(Items.PAPER,12);var book=new ItemStack(Items.BOOK);
  h.assertTrue(pouch.isItemValid(0,paper),"Default filter accepts the pouch category");
  menu.setCarried(paper);h.assertTrue(menu.clickMenuButton(p,5200),"Filter accepts a sample");
  h.assertTrue(menu.getCarried().getCount()==12&&pouch.filterItem(0).getCount()==1,"Sample is copied without consumption");
  menu.clickMenuButton(p,5220);
  h.assertTrue(pouch.isItemValid(0,paper)&&!pouch.isItemValid(0,book),"Whitelist accepts only listed types");
  h.assertTrue(pouch.insertItem(0,book,false).getCount()==1,"Manual insertion respects filter");
  h.assertTrue(d.stashPouches(book.copy()).getCount()==1,"Automatic routing respects filter");
  pouch.insertItem(0,paper.copy(),false);menu.clickMenuButton(p,5220);
  h.assertTrue(!pouch.isItemValid(1,paper)&&pouch.isItemValid(1,book),"Blacklist rejects listed types");
  h.assertTrue(pouch.extractItem(0,12,false).getCount()==12,"Changing filters never traps existing contents");
  var saved=pouch.owner().copy();d.pouches.setStackInSlot(3,ItemStack.EMPTY);d.pouches.setStackInSlot(3,saved);
  h.assertTrue(pouch.filterMode()==2&&pouch.filterItem(0).is(Items.PAPER),"Filter travels with removed pouch");
  menu.clickMenuButton(p,5210);h.assertTrue(pouch.isItemValid(0,paper),"Clearing a blacklist entry allows the item again");
  pouch.cycleFilter();pouch.cycleFilter();h.assertTrue(!pouch.isItemValid(0,paper),"Empty whitelist rejects incoming items");
  h.succeed();
 }
 @GameTest(template="empty") public static void reusableMealReturnsToSource(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  d.pouches.setStackInSlot(2,new ItemStack(AdventurersPack.FEEDING.get()));var pouch=d.pouchStorage[2];
  // Model the stack returned by a reusable food after finishUsingItem updates its NBT.
  pouch.setStackInSlot(4,new ItemStack(Items.COOKED_BEEF));pouch.extractItem(4,1,false);
  pouch.setFilter(0,new ItemStack(Items.COOKED_BEEF));pouch.cycleFilter();pouch.cycleFilter();
  var returned=new ItemStack(Items.COOKED_BEEF);returned.getOrCreateTag().putInt("MealsEaten",1);
  PackEvents.returnMealRemainder(p,d,Items.COOKED_BEEF,returned,4,true);
  h.assertTrue(pouch.getStackInSlot(4).is(Items.COOKED_BEEF)&&pouch.getStackInSlot(4).getCount()==1,"Reusable meal returns to original pouch slot despite changed filters");
  h.assertTrue(p.getInventory().countItem(Items.COOKED_BEEF)==0,"Reusable food never moves to hotbar");
  var loaded=new PackData(d.owner.copy());h.assertTrue(loaded.pouchStorage[2].getStackInSlot(4).getTag().getInt("MealsEaten")==1,"Returned food and updated tags persist");
  PackEvents.returnMealRemainder(p,d,Items.MUSHROOM_STEW,new ItemStack(Items.BOWL),5,true);
  h.assertTrue(p.getInventory().countItem(Items.BOWL)==1&&pouch.getStackInSlot(5).isEmpty(),"Ordinary bowl remainder goes to player inventory");
  PackEvents.returnMealRemainder(p,d,Items.COOKED_BEEF,ItemStack.EMPTY,6,true);
  h.assertTrue(pouch.getStackInSlot(6).isEmpty(),"Consumed food is not recreated");
  d.tab=0;int source=d.storageId(2,3);PackEvents.returnMealRemainder(p,d,Items.COOKED_BEEF,new ItemStack(Items.COOKED_BEEF),source,false);
  h.assertTrue(d.storage(source).is(Items.COOKED_BEEF)&&d.storage(0).isEmpty(),"Reusable compartment food returns to original tab");h.succeed();
 }
}
