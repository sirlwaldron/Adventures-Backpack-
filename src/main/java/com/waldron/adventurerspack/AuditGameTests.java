package com.waldron.adventurerspack;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(AdventurersPack.ID)
@PrefixGameTestTemplate(false)
public class AuditGameTests {
 private static PackData pack() {
  return new PackData(new ItemStack(AdventurersPack.PACK.get()));
 }

 private static ServerPlayer makePlayer(GameTestHelper h) {
  return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "audit_player"));
 }

 private static ServerPlayer campPlayer(GameTestHelper h) {
  ServerPlayer p=makePlayer(h);BlockPos origin=h.absolutePos(new BlockPos(0,2,0));
  p.setPos(origin.getX(),origin.getY(),origin.getZ());p.setYRot(0);
  BlockPos base=p.blockPosition().relative(p.getDirection(),3);
  for(int x=0;x<3;x++)for(int z=0;z<2;z++){
   h.getLevel().setBlock(base.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
   for(int y=0;y<3;y++)h.getLevel().setBlock(base.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
  }
  return p;
 }

 @GameTest(template = "empty")
 public static void auditCombatUsesToolRack(GameTestHelper h) {
  ServerPlayer p = makePlayer(h);
  var d = pack();
  d.preset = PackData.COMBAT;
  p.getInventory().setItem(9, d.owner);
  p.getInventory().setItem(0, new ItemStack(Items.WOODEN_SWORD));
  d.items.setStackInSlot(108, new ItemStack(Items.DIAMOND_SWORD));
  PackEvents.selectWeapon(p, d);
  h.assertTrue(p.getMainHandItem().is(Items.DIAMOND_SWORD), "Diamond sword equipped from tool rack");
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).isEmpty(), "Tool rack slot emptied during equip");
  ToolHand.restore(p);
  h.assertTrue(new PackData(d.owner).items.getStackInSlot(108).is(Items.DIAMOND_SWORD), "Diamond sword restored to tool rack");
  h.assertTrue(p.getMainHandItem().is(Items.WOODEN_SWORD), "Original wooden sword restored to hand");
  h.succeed();
 }

 @GameTest(template = "empty")
 public static void auditTransformedBowReturnsToRack(GameTestHelper h) {
  ServerPlayer p = makePlayer(h);
  var d = pack();
  p.getInventory().setItem(9, d.owner);
  p.getInventory().setItem(0, new ItemStack(Items.DIRT));
  d.items.setStackInSlot(108, new ItemStack(AdventurersPack.ENDGAME.get()));
  h.assertTrue(ToolHand.equip(p, d, 108), "Endgame equips from tool rack");
  h.assertTrue(p.getMainHandItem().is(AdventurersPack.ENDGAME.get()), "Player holds Endgame");

  // Transform into a bow form (shortbow = 2)
  ItemStack bowStack = Requiem.transformed(p.getMainHandItem(), 2);
  h.assertTrue(!bowStack.isEmpty() && Requiem.form(bowStack) == 2, "Transformed to shortbow");
  p.setItemInHand(InteractionHand.MAIN_HAND, bowStack);

  // Fill player inventory completely to test stow when inventory cannot accept items
  for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
   if (i != 0 && i != 9) {
    p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
   }
  }

  ToolHand.restore(p);
  PackData refreshed = new PackData(d.owner);
  ItemStack rackItem = refreshed.items.getStackInSlot(108);
  h.assertTrue(!rackItem.isEmpty() && Requiem.form(rackItem) == 2, "Transformed bow returned home to tool rack");
  h.assertTrue(p.getMainHandItem().is(Items.DIRT), "Original hand item restored");
  h.succeed();
 }

 @GameTest(template = "empty")
 public static void auditTransformKeepsCapabilities(GameTestHelper h) {
  ItemStack source = new ItemStack(AdventurersPack.ENDGAME.get());
  CompoundTag fullTag = source.serializeNBT();
  CompoundTag caps = new CompoundTag();
  CompoundTag energy = new CompoundTag();
  energy.putInt("Energy", 321);
  caps.put("test_mod:energy", energy);
  fullTag.put("ForgeCaps", caps);

  ItemStack withCap = ItemStack.of(fullTag);
  ItemStack transformed = Requiem.transformed(withCap, 0); // Transform to sword (form 0)

  h.assertTrue(!transformed.isEmpty() && Requiem.form(transformed) == 0, "Transformed to sword");
  CompoundTag outTag = transformed.serializeNBT();
  h.assertTrue(outTag.contains("ForgeCaps", 10), "ForgeCaps tag preserved");
  h.assertTrue(outTag.getCompound("ForgeCaps").contains("test_mod:energy", 10), "Capability preserved");
  h.assertTrue(outTag.getCompound("ForgeCaps").getCompound("test_mod:energy").getInt("Energy") == 321, "Energy value 321 preserved");
  h.succeed();
 }

 @GameTest(template = "empty")
 public static void auditLostCampKitCanRecover(GameTestHelper h) {
  ServerPlayer p = campPlayer(h);
  var d = pack();
  d.camp.setStackInSlot(0, new ItemStack(AdventurersPack.CAMP_KIT.get()));
  CampKit.action(p, d, 0); // Deploy
  h.assertTrue(d.camp.getStackInSlot(0).getTag().getBoolean("Deployed"), "Camp deployed");

  // Simulate lost kit by replacing with a fresh camp kit
  d.camp.setStackInSlot(0, new ItemStack(AdventurersPack.CAMP_KIT.get()));
  CampKit.action(p, d, 1); // Pack Up with fresh kit
  h.assertTrue(d.camp.getStackInSlot(0).getTag().contains("Packed"), "Camp recovered into fresh kit");
  h.assertTrue(CampLedger.get(p.server).records.getAllKeys().stream().noneMatch(key -> CampLedger.get(p.server).records.getCompound(key).getUUID("Owner").equals(p.getUUID())), "Camp record removed from ledger");
  h.succeed();
 }

 @GameTest(template = "empty")
 public static void auditCampPreventsNestedBackpacks(GameTestHelper h) {
  ServerPlayer p = campPlayer(h);
  var d = pack();
  d.camp.setStackInSlot(0, new ItemStack(AdventurersPack.CAMP_KIT.get()));
  CampKit.action(p, d, 0); // Deploy

  // Put a backpack inside the camp barrel at base+pos(2)
  var record = CampLedger.get(p.server).records.getAllKeys().stream().filter(key -> CampLedger.get(p.server).records.getCompound(key).getUUID("Owner").equals(p.getUUID())).findFirst();
  h.assertTrue(record.isPresent(), "Camp record exists");
  BlockPos base = BlockPos.of(CampLedger.get(p.server).records.getCompound(record.get()).getLong("Pos"));
  BlockPos barrelPos = base.offset(2, 0, 0);
  var be = p.level().getBlockEntity(barrelPos);
  h.assertTrue(be instanceof net.minecraft.world.Container, "Barrel container found");
  ((net.minecraft.world.Container) be).setItem(0, new ItemStack(AdventurersPack.PACK.get()));

  // Attempt to fold camp with backpack inside
  CampKit.action(p, d, 1);
  h.assertTrue(!d.camp.getStackInSlot(0).getTag().contains("Packed"), "Folding refused while backpack is in barrel");
  h.assertTrue(!CampLedger.get(p.server).records.getAllKeys().stream().noneMatch(key -> CampLedger.get(p.server).records.getCompound(key).getUUID("Owner").equals(p.getUUID())), "Camp remains deployed and intact");

  // Also verify PackData.safe rejects a pre-packed kit containing a backpack
  ItemStack packedKit = new ItemStack(AdventurersPack.CAMP_KIT.get());
  CompoundTag packed = new CompoundTag();
  CompoundTag part2 = new CompoundTag();
  ListTag items = new ListTag();
  CompoundTag packTag = new ItemStack(AdventurersPack.PACK.get()).save(new CompoundTag());
  items.add(packTag);
  part2.put("Items", items);
  packed.put("Part2", part2);
  packedKit.getOrCreateTag().put("Packed", packed);
  h.assertTrue(!PackData.safe(packedKit), "Packed camp kit with nested pack is rejected by safe check");

  // Clean up: remove pack from barrel and fold camp
  ((net.minecraft.world.Container) be).setItem(0, ItemStack.EMPTY);
  CampKit.action(p, d, 1);
  h.succeed();
 }

 @GameTest(template = "empty")
 public static void auditWeaponScoresUseDamage(GameTestHelper h) {
  ItemStack woodSword = new ItemStack(Items.WOODEN_SWORD);
  ItemStack netheriteAxe = new ItemStack(Items.NETHERITE_AXE);
  ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
  ItemStack sharpSword = new ItemStack(Items.DIAMOND_SWORD);
  sharpSword.enchant(Enchantments.SHARPNESS, 5);

  ItemStack spear = new ItemStack(AdventurersPack.REQUIEM_SPEAR.get());
  ItemStack endgameSword = new ItemStack(AdventurersPack.REQUIEM.get());

  int woodScore = PackEvents.weaponScore(woodSword);
  int axeScore = PackEvents.weaponScore(netheriteAxe);
  int diamondScore = PackEvents.weaponScore(diamondSword);
  int sharpScore = PackEvents.weaponScore(sharpSword);
  int spearScore = PackEvents.weaponScore(spear);
  int endgameSwordScore = PackEvents.weaponScore(endgameSword);

  h.assertTrue(axeScore > woodScore, "Netherite axe scores higher than wooden sword");
  h.assertTrue(sharpScore > diamondScore, "Sharpness V increases weapon score");
  h.assertTrue(spearScore != endgameSwordScore, "Spear and sword Endgame forms have distinct weapon scores");
  h.succeed();
 }

 @GameTest(template = "empty")
 public static void auditProjectsCountPouches(GameTestHelper h) {
  var d = pack();
  d.pouches.setStackInSlot(1, new ItemStack(AdventurersPack.GEM.get()));
  d.pouchStorage[1].insertItem(0, new ItemStack(Items.DIAMOND, 12), false);

  int collected = d.collected(Items.DIAMOND, null);
  h.assertTrue(collected == 12, "Supply tracking counts 12 diamonds in gem pouch");
  h.succeed();
 }
 @GameTest(template = "empty")
 public static void auditCampDoesNotEraseReplacementChest(GameTestHelper h) {
  ServerPlayer p=campPlayer(h);var d=pack();
  d.camp.setStackInSlot(0,new ItemStack(AdventurersPack.CAMP_KIT.get()));
  CampKit.action(p,d,0);
  h.assertTrue(d.camp.getStackInSlot(0).getTag().getBoolean("Deployed"),"Camp deployed");
  BlockPos base=p.blockPosition().relative(p.getDirection(),3);BlockPos at=base.offset(2,0,0);
  p.level().setBlock(at,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),2);
  var chest=(net.minecraft.world.Container)p.level().getBlockEntity(at);
  chest.setItem(0,new ItemStack(Items.DIAMOND,9));
  CampKit.action(p,d,1);
  h.assertTrue(p.level().getBlockEntity(at) instanceof net.minecraft.world.Container,"Replacement chest still exists");
  h.assertTrue(((net.minecraft.world.Container)p.level().getBlockEntity(at)).getItem(0).getCount()==9,"Replacement chest contents preserved");
  h.assertFalse(d.camp.getStackInSlot(0).getTag().getCompound("Packed").contains("Part2"),"Foreign chest was not copied into kit");
  h.succeed();
 }
}
