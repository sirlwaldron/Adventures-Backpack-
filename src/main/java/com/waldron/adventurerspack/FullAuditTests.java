package com.waldron.adventurerspack;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder(AdventurersPack.ID)
@PrefixGameTestTemplate(false)
public class FullAuditTests {
 private static ServerPlayer player(GameTestHelper h){return FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"FullAudit"));}
 @GameTest(template="empty") public static void upgradeCategoriesAllowAnyPosition(GameTestHelper h){
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  h.assertTrue(d.items.insertItem(119,new ItemStack(AdventurersPack.PICKUP.get()),false).isEmpty(),"Rune can use last category position at base tier");
  h.assertTrue(d.items.isItemValid(117,new ItemStack(AdventurersPack.VOID.get())),"Every rune socket accepts another rune");
  h.assertFalse(d.items.isItemValid(114,new ItemStack(AdventurersPack.VOID.get())),"Category boundaries remain");
  h.assertTrue(d.items.insertItem(116,new ItemStack(AdventurersPack.CRAFTING.get()),false).isEmpty(),"Attachment can use last position");
  h.assertTrue(d.sectionOpen(4),"Last storage-addon position available");
  var saved=new PackData(d.owner);
  h.assertTrue(saved.has(AdventurersPack.PICKUP.get())&&saved.has(AdventurersPack.CRAFTING.get()),"Non-first-position upgrades survive reload and remain active");
  d.items.extractItem(119,1,false);
  h.assertTrue(d.socketOpen(117)&&d.socketOpen(118)&&d.socketOpen(119),"Removing rune frees every position");h.succeed();
 }
 @GameTest(template="empty") public static void armorWingsFollowFlightState(GameTestHelper h){
  var p=player(h);
  h.assertFalse(WingFlightState.shouldFlap(p),"Resting wings stay curled");
  p.setOnGround(false);
  h.assertFalse(WingFlightState.shouldFlap(p),"Jumping or falling does not flap");
  p.getAbilities().mayfly=true;
  h.assertFalse(WingFlightState.shouldFlap(p),"Flight permission alone does not flap");
  p.getAbilities().flying=true;
  h.assertTrue(WingFlightState.shouldFlap(p),"Creative flight flaps");
  p.getAbilities().flying=false;p.startFallFlying();
  h.assertTrue(WingFlightState.shouldFlap(p),"Elytra gliding flaps");
  p.stopFallFlying();
  h.assertFalse(WingFlightState.shouldFlap(p),"Landing returns curled pose");h.succeed();
 }
 @GameTest(template="empty") public static void craftedFlagCannotCreateOrOverwrite(GameTestHelper h){
  var p=player(h);p.getPersistentData().putBoolean("EndgameCrafted",true);
  p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
  ToolHand.draw(p,null,1);
  h.assertTrue(p.getMainHandItem().is(Items.DIAMOND)&&p.getMainHandItem().getCount()==7,"History must not create a weapon or overwrite held diamonds");
  h.assertFalse(ToolHand.hasEndgame(p,null),"Ownership requires a physical weapon");h.succeed();
 }
 @GameTest(template="empty") public static void changingFromBorrowedToolPreservesBoth(GameTestHelper h){
  var p=player(h);var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  p.getInventory().setItem(9,d.owner);p.getInventory().setItem(0,new ItemStack(Items.TORCH,17));
  d.items.setStackInSlot(108,new ItemStack(Items.DIAMOND_PICKAXE));
  d.items.setStackInSlot(109,new ItemStack(AdventurersPack.ENDGAME.get()));
  h.assertTrue(ToolHand.equip(p,d,108),"Borrow ordinary pickaxe");
  ToolHand.draw(p,PackItem.data(p,d.owner),5);
  h.assertTrue(p.getMainHandItem().is(AdventurersPack.REQUIEM.get()),"Sword selected");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).is(Items.DIAMOND_PICKAXE),"Ordinary pickaxe returned, not erased");
  ToolHand.restore(p);
  h.assertTrue(p.getMainHandItem().is(Items.TORCH)&&p.getMainHandItem().getCount()==17,"Original hand preserved");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(109).is(AdventurersPack.REQUIEM.get()),"Physical Endgame returned to its rack");h.succeed();
 }
 @GameTest(template="empty") public static void expandedStorageWeaponUsesFreeRack(GameTestHelper h){
  var p=player(h);var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));d.tier=4;
  p.getInventory().setItem(9,d.owner);p.getInventory().setItem(0,new ItemStack(Items.DIRT,13));
  d.items.setStackInSlot(108,new ItemStack(Items.IRON_AXE));
  d.extra.setStackInSlot(0,new ItemStack(AdventurersPack.ENDGAME.get()));
  ToolHand.draw(p,d,1);
  h.assertTrue(p.getMainHandItem().is(AdventurersPack.ENDGAME_PICKAXE.get()),"Extra-tier storage reaches selector");
  var live=new PackData(d.owner);h.assertTrue(live.extra.getStackInSlot(0).isEmpty(),"Weapon moved rather than copied");
  h.assertTrue(live.items.getStackInSlot(108).is(Items.IRON_AXE),"Occupied rack slot preserved");
  ToolHand.draw(p,live,5);
  h.assertTrue(p.getMainHandItem().is(AdventurersPack.ENDGAME_PICKAXE.get()),"Selector respects shared cooldown");
  ToolHand.restore(p);h.assertTrue(p.getMainHandItem().getCount()==13,"Original hand restored");h.succeed();
 }
 @GameTest(template="empty") public static void craftedEventDoesNotCopyResult(GameTestHelper h){
  var p=player(h);var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));p.getInventory().setItem(9,d.owner);
  var result=new ItemStack(AdventurersPack.ENDGAME.get());p.getInventory().setItem(0,result.copy());
  new PackEvents().onCraft(new net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent(p,result,new SimpleContainer(9)));
  h.assertTrue(result.getCount()==1,"Craft event never mutates result during extraction");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).isEmpty(),"No second copy in rack");h.succeed();
 }
 @GameTest(template="empty") public static void progressionRecipesAreLoaded(GameTestHelper h){
  for(String name:new String[]{"endgame_ingot","crimson_ingot","backpack_ingot","endgame","requiem","crimson_helmet","endgame_chestplate","explorer_pack","relic_pack"})
   h.assertTrue(h.getLevel().getRecipeManager().byKey(new ResourceLocation(AdventurersPack.ID,name)).isPresent(),"Recipe loaded: "+name);
  h.succeed();
 }
 @GameTest(template="empty") public static void basePackMatchesUpgradeIngredient(GameTestHelper h){
  var recipe=h.getLevel().getRecipeManager().byKey(new ResourceLocation(AdventurersPack.ID,"explorer_pack")).orElseThrow();
  h.assertTrue(recipe.getIngredients().get(1).test(new ItemStack(AdventurersPack.PACK.get())),"Fresh untagged backpack is accepted by recipe ingredient");
  h.assertTrue(GuideBook.CATEGORIES.length==7&&GuideBook.ENTRIES.length>=25,"RPG codex realms and entries present");
  for(var cat:GuideBook.CATEGORIES)h.assertFalse(cat.icon().get().isEmpty(),"Category icon resolves: "+cat.name());
  for(var entry:GuideBook.ENTRIES){h.assertFalse(entry.icon.get().isEmpty(),"Entry icon resolves: "+entry.title);ItemStack[] vars=entry.variants.get();h.assertTrue(vars!=null&&vars.length>0&&entry.stats.length>0&&entry.sections.length>0,"Entry complete: "+entry.title);}
  var pos=new net.minecraft.core.BlockPos(1,1,1);h.setBlock(pos,AdventurersPack.DOCK.get());
  var dock=(DockEntity)h.getBlockEntity(pos);
  var custom=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  custom.tier=3;custom.camp.setStackInSlot(0,new ItemStack(AdventurersPack.CAMP_KIT.get()));
  custom.sections.setStackInSlot(0,new ItemStack(AdventurersPack.QUIVER.get()));
  custom.items.setStackInSlot(108,new ItemStack(Items.DIAMOND_PICKAXE));
  custom.items.setStackInSlot(114,new ItemStack(AdventurersPack.FURNACE.get()));
  custom.save();
  dock.setPack(custom.owner);
  var clientDock=new DockEntity(pos,AdventurersPack.DOCK.get().defaultBlockState());
  clientDock.load(dock.getUpdateTag());
  var synced=clientDock.data();
  h.assertTrue(!clientDock.pack.isEmpty()&&synced.tier==3&&!synced.camp.getStackInSlot(0).isEmpty()&&synced.has(AdventurersPack.QUIVER.get())&&synced.has(AdventurersPack.FURNACE.get())&&!synced.items.getStackInSlot(108).isEmpty(),"Dock syncs placed backpack tier and visual attachments to client");
  h.succeed();
 }
}
