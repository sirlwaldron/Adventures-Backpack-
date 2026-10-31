package com.waldron.adventurerspack;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("adventurerspack_tools")
@PrefixGameTestTemplate(false)
public class ToolEquipmentTests {
 private static ServerPlayer player(GameTestHelper h){
  var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"ToolEquipment"));
  p.getInventory().setItem(0,new ItemStack(Items.IRON_AXE));
  return p;
 }
 private static PackData kit(ServerPlayer p){
  var pack=new ItemStack(AdventurersPack.PACK.get());p.getInventory().setItem(9,pack);
  var d=PackItem.data(p,pack);d.items.setStackInSlot(108,new ItemStack(Items.DIAMOND_PICKAXE));d.save();return d;
 }
 @GameTest(template="empty") public static void equipmentDoesNotOccupyHotbar(GameTestHelper h){
  var p=player(h);var d=kit(p);var original=p.getInventory().getItem(0);
  for(int i=1;i<36;i++)if(i!=9)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
  h.assertTrue(ToolHand.equip(p,d,108),"Tool equips with a full inventory");
  h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_PICKAXE),"Equipped tool is the usable main hand");
  h.assertTrue(p.getInventory().getItem(0)==original,"Original hotbar stack stays in its slot");
  h.assertTrue(ToolEquipment.stack(p)==p.getMainHandItem(),"Main hand uses separate equipment");
  p.getMainHandItem().setDamageValue(12);ToolHand.restore(p);
  h.assertTrue(p.getMainHandItem()==original,"Stowing reveals the untouched original item");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).getDamageValue()==12,"Tool damage survives stowing");
  h.assertTrue(ToolEquipment.stack(p).isEmpty(),"Separate equipment clears once");h.succeed();
 }
 @GameTest(template="empty") public static void switchingAndDroppingPreserveItems(GameTestHelper h){
  var p=player(h);var d=kit(p);ToolHand.equip(p,d,108);
  h.assertTrue(p.getInventory().removeFromSelected(false).isEmpty(),"Drop stows the kit tool");
  h.assertTrue(p.getMainHandItem().is(Items.IRON_AXE)&&!ToolHand.active(p),"Drop never removes the hotbar item");
  ToolHand.equip(p,PackItem.data(p,d.owner),108);p.getInventory().selected=1;ToolHand.tick(p);
  h.assertFalse(ToolHand.active(p),"Changing hotbar selection stows tool");
  h.assertTrue(p.getInventory().getItem(0).is(Items.IRON_AXE),"Original item is still in slot zero");h.succeed();
 }
 @GameTest(template="empty") public static void formChangesStayInEquipment(GameTestHelper h){
  var p=player(h);var d=kit(p);d.items.setStackInSlot(108,new ItemStack(AdventurersPack.ENDGAME.get()));d.save();
  ToolHand.equip(p,d,108);
  h.assertTrue(Requiem.transform(p,0,5,0),"Equipped Endgame transforms");
  h.assertTrue(ToolEquipment.stack(p).is(AdventurersPack.REQUIEM.get()),"Form remains in equipment");
  h.assertTrue(p.getInventory().getItem(0).is(Items.IRON_AXE),"Transformation cannot overwrite hotbar");
  ToolHand.restore(p);h.succeed();
 }
 @GameTest(template="empty") public static void brokenToolDoesNotReappear(GameTestHelper h){
  var p=player(h);var d=kit(p);ToolHand.equip(p,d,108);
  p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);ToolHand.tick(p);
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).isEmpty(),"Broken tool is not recreated");
  h.assertTrue(p.getMainHandItem().is(Items.IRON_AXE),"Broken tool leaves hotbar untouched");h.succeed();
 }
 @GameTest(template="empty") public static void drawSelectsKitSlotWithoutHeldTool(GameTestHelper h){
  var p=player(h);p.getInventory().setItem(0,new ItemStack(Items.DIRT,12));var d=kit(p);
  ToolHand.draw(p,d,0);
  h.assertTrue(ToolHand.manual(p)&&p.getMainHandItem().is(Items.DIAMOND_PICKAXE),"The tool kit key draws without a held tool");
  h.assertTrue(p.getInventory().getItem(0).getCount()==12,"The hotbar stack stays in its slot");
  h.assertTrue(p.getInventory().selected==0,"The numbered slot stays put while the kit cursor is selected");
  ToolHand.restore(p);
  h.assertTrue(!ToolHand.active(p)&&p.getMainHandItem().is(Items.DIRT)&&p.getMainHandItem().getCount()==12,"Stowing returns the hotbar item");
  h.succeed();
 }
 @GameTest(template="empty") public static void blocksCannotActivateToolSwap(GameTestHelper h){
  var p=player(h);var d=kit(p);p.getInventory().setItem(0,new ItemStack(Items.DIRT,12));
  h.assertTrue(ToolHand.equip(p,d,108),"Tool equips while holding blocks");
  h.assertTrue(p.getInventory().getItem(0).is(Items.DIRT)&&p.getInventory().getItem(0).getCount()==12,"Hotbar blocks remain untouched");
  ToolHand.restore(p);
  h.assertTrue(p.getMainHandItem().is(Items.DIRT)&&p.getMainHandItem().getCount()==12,"Blocks remain untouched after restore");
  h.succeed();
 }
 @GameTest(template="empty") public static void savedEquipmentRestoresWithoutDuplication(GameTestHelper h){
  var p=player(h);var d=kit(p);ToolHand.equip(p,d,108);p.getMainHandItem().setDamageValue(37);
  var saved=new net.minecraft.nbt.CompoundTag();p.saveWithoutId(saved);
  var loaded=player(h);loaded.load(saved);
  h.assertTrue(ToolEquipment.stack(loaded).is(Items.DIAMOND_PICKAXE),"Equipped tool survives entity serialization");
  h.assertTrue(loaded.getInventory().getItem(0).is(Items.IRON_AXE),"Hotbar item survives independently");
  ToolHand.restore(loaded);
  var pack=loaded.getInventory().getItem(9);
  h.assertTrue(new PackData(pack).items.getStackInSlot(108).getDamageValue()==37,"Recovery returns the actual worn tool");
  h.assertTrue(ToolEquipment.stack(loaded).isEmpty()&&!ToolHand.active(loaded),"Recovery clears equipment and session");
  ToolHand.restore(p);h.succeed();
 }
 @GameTest(template="empty") public static void offhandSwapStowsWithoutMovingItems(GameTestHelper h){
  var p=player(h);var d=kit(p);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
  ToolHand.equip(p,d,108);
  var event=new net.minecraftforge.event.entity.living.LivingSwapItemsEvent.Hands(p);
  new PackEvents().swapHands(event);
  h.assertTrue(event.isCanceled()&&!ToolHand.active(p),"Swapping hands stows kit tool");
  h.assertTrue(p.getMainHandItem().is(Items.IRON_AXE)&&p.getOffhandItem().is(Items.SHIELD),"Both original hands remain intact");h.succeed();
 }
 @GameTest(template="empty") public static void shiftAndHeldWeaponsNeverSummonEndgame(GameTestHelper h){
  var p=player(h);var d=kit(p);
  d.items.setStackInSlot(108,new ItemStack(AdventurersPack.ENDGAME.get()));d.save();
  p.getInventory().setItem(0,new ItemStack(Items.DIAMOND_SWORD));
  p.getInventory().selected=0;
  PackEvents.selectWeapon(p,new PackData(d.owner));
  h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD)&&!ToolHand.active(p),"Held weapon is not replaced by Endgame on attack");
  h.assertTrue(ToolHand.bestTool(p,new PackData(d.owner),p.getMainHandItem(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())<0,"Held weapon does not summon Endgame when hitting blocks");
  var grass=new net.minecraft.core.BlockPos(1,1,1);
  h.setBlock(grass,net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
  h.setBlock(grass.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
  h.assertTrue(ToolHand.bestTill(p,new PackData(d.owner),h.getLevel(),h.absolutePos(grass))<0,"Held weapon does not summon Endgame hoe on right-click");
  p.getInventory().setItem(0,new ItemStack(Items.DIRT,8));
  p.setShiftKeyDown(true);
  PackEvents.selectWeapon(p,new PackData(d.owner));
  h.assertTrue(p.getMainHandItem().is(Items.DIRT)&&!ToolHand.active(p),"Crouching prevents weapon auto-summon");
  h.assertTrue(ToolHand.bestTool(p,new PackData(d.owner),p.getMainHandItem(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())<0,"Crouching prevents mining auto-summon");
  h.assertTrue(ToolHand.bestTill(p,new PackData(d.owner),h.getLevel(),h.absolutePos(grass))<0,"Crouching prevents tilling auto-summon");
  ToolHand.draw(p,new PackData(d.owner),0);
  h.assertTrue(p.getMainHandItem().is(Items.DIRT)&&!ToolHand.active(p),"Crouching prevents drawing Endgame over held item");
  p.setShiftKeyDown(false);
  h.succeed();
 }
}
