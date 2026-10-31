package com.waldron.adventurerspack;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.fml.ModList;

@GameTestHolder(AdventurersPack.ID)
@PrefixGameTestTemplate(false)
public class BackpackIntegrationTests {
 @GameTest(template="empty") public static void upgradeMenuAndWideActionIds(GameTestHelper h) {
  var player=h.makeMockPlayer();
  var data=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  player.getInventory().setItem(0,data.owner);
  data.items.setStackInSlot(117,new ItemStack(AdventurersPack.PICKUP.get()));
  var menu=new PackMenu(7,player.getInventory(),0,data);
  h.assertTrue(menu.clickMenuButton(player,4117)&&!data.has(AdventurersPack.PICKUP.get()),"Menu toggle reaches installed rune");
  h.assertTrue(!menu.clickMenuButton(player,4000),"Storage slot cannot be toggled");
  var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
  try {
   Network.Command.encode(new Network.Command(3,4117,"7"),buffer);
   var decoded=Network.Command.decode(buffer);
   h.assertTrue(decoded.arg()==4117&&decoded.text().equals("7"),"Packet retains full action and container IDs");
  } finally {buffer.release();}
  player.getInventory().setItem(0,ItemStack.EMPTY);
  h.assertTrue(!menu.clickMenuButton(player,4117),"Stale menu cannot modify removed backpack");
  h.succeed();
 }
 @GameTest(template="empty") public static void upgradeTogglePersistsAndPreservesContents(GameTestHelper h) {
  var data=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  data.items.setStackInSlot(117,new ItemStack(AdventurersPack.CAPACITY.get()));
  for(int i=0;i<80;i++)data.deposit(0,new ItemStack(Items.STONE,64));
  h.assertTrue(data.toggleUpgrade(117)&&!data.has(AdventurersPack.CAPACITY.get()),"Installed upgrade can be disabled");
  var restored=new PackData(data.owner.copy());
  h.assertTrue(restored.counts[0]==5120&&!restored.has(AdventurersPack.CAPACITY.get()),"Contents and disabled state survive saving");
  h.assertTrue(restored.deposit(0,new ItemStack(Items.STONE))==0,"Over-capacity bag rejects new items");
  h.assertTrue(restored.toggleUpgrade(117)&&restored.has(AdventurersPack.CAPACITY.get()),"Upgrade re-enables without removal");
  h.assertTrue(!restored.toggleUpgrade(0)&&!restored.toggleUpgrade(118),"Storage and empty sockets cannot toggle");
  h.succeed();
 }
 @GameTest(template="empty") public static void curiosEquipmentAndVisibility(GameTestHelper h) {
  var player=h.makeMockPlayer();
  if(!ModList.get().isLoaded("curios")) {
   h.assertTrue(CuriosPacks.stack(player).isEmpty(),"Optional bridge works without Curios");h.succeed();return;
  }
  checkCurios(h,player);
 }
 private static void checkCurios(GameTestHelper h,net.minecraft.world.entity.player.Player player) {
  var inventory=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).orElseThrow(()->new IllegalStateException("Curios inventory missing"));
  var back=inventory.getStacksHandler("back").orElseThrow(()->new IllegalStateException("Back slot missing"));
  ItemStack pack=new ItemStack(AdventurersPack.PACK.get());
  h.assertTrue(pack.getCapability(top.theillusivec4.curios.api.CuriosCapability.ITEM).isPresent(),"Backpack exposes Curios capability");
  back.getStacks().setStackInSlot(0,pack);
  h.assertTrue(CuriosPacks.stack(player)==pack&&PackItem.find(player)==PackItem.CURIOS,"Equipped pack is usable through standard pack access");
  back.getRenders().set(0,false);
  h.assertTrue(CuriosPacks.visualStack(player).isEmpty()&&CuriosPacks.stack(player)==pack,"Hiding the model does not disable storage");
  back.getRenders().set(0,true);
  h.assertTrue(CuriosPacks.visualStack(player)==pack,"Visible pack renders");
  h.succeed();
 }
}
