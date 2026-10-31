package com.waldron.adventurerspack;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("routingtests") @PrefixGameTestTemplate(false)
public class RoutingTests {
 @GameTest(template="empty") public static void toggleAndRepeat(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  p.getInventory().setItem(0,d.owner);var m=new PackMenu(1,p.getInventory(),0,d);p.containerMenu=m;
  int target=d.storageId(2,0);d.storageHandler(target).setStackInSlot(d.localSlot(target),new ItemStack(Items.STONE,1));
  d.tab=0;p.getInventory().setItem(9,new ItemStack(Items.STONE,20));p.getInventory().setItem(10,new ItemStack(Items.DIRT,7));p.getInventory().setItem(1,new ItemStack(Items.STONE,8));
  PackStorageActions.autoRoute(d,p,0);h.assertTrue(p.getInventory().getItem(9).getCount()==20,"Disabled routing leaves inventory alone");
  h.assertTrue(m.clickMenuButton(p,13)&&d.routing&&p.getInventory().getItem(9).isEmpty(),"Enabling Route deposits immediately without a Pickup Rune");
  h.assertTrue(d.storage(target).getCount()==21&&d.storage(0).isEmpty(),"Items route to matching tab, not selected tab");
  h.assertTrue(p.getInventory().getItem(10).getCount()==7&&p.getInventory().getItem(1).getCount()==8,"Unmatched items and hotbar stay put");
  p.getInventory().setItem(9,new ItemStack(Items.STONE,5));PackStorageActions.autoRoute(d,p,0);
  h.assertTrue(d.storage(target).getCount()==26&&p.getInventory().getItem(9).isEmpty(),"Subsequent automation deposits new inventory items");
  h.assertTrue(new PackData(d.owner.copy()).routing,"Enabled routing persists");
  m.clickMenuButton(p,13);p.getInventory().setItem(9,new ItemStack(Items.STONE,3));PackStorageActions.autoRoute(d,p,0);
  h.assertTrue(!d.routing&&p.getInventory().getItem(9).getCount()==3,"Disabling stops automatic deposit");h.succeed();
 }
 @GameTest(template="empty") public static void fullTabAndManualDeposit(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));p.getInventory().setItem(0,d.owner);
  var m=new PackMenu(1,p.getInventory(),0,d);int target=d.storageId(1,0);
  for(int n=0;n<d.pageSize();n++){int id=d.storageId(1,n);d.storageHandler(id).setStackInSlot(d.localSlot(id),new ItemStack(n==0?Items.STONE:Items.DIRT,64));}
  d.tab=0;d.routing=true;p.getInventory().setItem(9,new ItemStack(Items.STONE,12));PackStorageActions.autoRoute(d,p,0);
  h.assertTrue(p.getInventory().getItem(9).getCount()==12&&d.storage(0).isEmpty(),"Full matching tab keeps overflow in inventory");
  d.storageHandler(target).setStackInSlot(d.localSlot(target),new ItemStack(Items.STONE,60));PackStorageActions.autoRoute(d,p,0);
  h.assertTrue(d.storage(target).getCount()==64&&p.getInventory().getItem(9).getCount()==8,"Partial transfer conserves items");
  d.routing=false;d.storageHandler(target).setStackInSlot(d.localSlot(target),new ItemStack(Items.STONE,50));PackStorageActions.quickStack(m,p);
  h.assertTrue(d.storage(target).getCount()==58&&p.getInventory().getItem(9).isEmpty(),"Manual Quick Deposit targets matching tabs even with Route off");h.succeed();
 }
}
