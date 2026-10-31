package com.waldron.adventurerspack;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder(AdventurersPack.ID) @PrefixGameTestTemplate(false)
public class WorkstationTests {
 @GameTest(template="empty") public static void tankTransfersConserveContainers(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));p.getInventory().setItem(0,d.owner);
  d.sections.setStackInSlot(0,new ItemStack(AdventurersPack.TANK.get()));var menu=new PackMenu(1,p.getInventory(),0,d);
  h.assertTrue(menu.clickMenuButton(p,5000),"Tank addon opens");menu.setCarried(new ItemStack(Items.WATER_BUCKET));
  h.assertTrue(menu.clickMenuButton(p,5002)&&d.tank.getFluidAmount()==1000&&menu.getCarried().is(Items.BUCKET),"Bucket fluid enters tank exactly once");
  menu.setCarried(new ItemStack(Items.LAVA_BUCKET));h.assertTrue(!menu.clickMenuButton(p,5002)&&menu.getCarried().is(Items.LAVA_BUCKET)&&d.tank.getFluidAmount()==1000,"Different fluids cannot mix or consume a bucket");
  menu.setCarried(new ItemStack(Items.BUCKET));h.assertTrue(menu.clickMenuButton(p,5003)&&menu.getCarried().is(Items.WATER_BUCKET)&&d.tank.isEmpty(),"Tank returns full bucket without duplication");
  menu.setCarried(new ItemStack(Items.BUCKET,2));h.assertTrue(!menu.clickMenuButton(p,5003)&&menu.getCarried().getCount()==2,"Stacked containers are rejected safely");
  h.assertFalse(d.fluidSlot.isItemValid(0,new ItemStack(Items.COBBLESTONE)),"Non-containers are rejected");
  d.fluidSlot.setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));
  h.assertTrue(menu.clickMenuButton(p,5002)&&d.tank.getFluidAmount()==1000&&d.fluidSlot.getStackInSlot(0).is(Items.BUCKET),"Placed bucket fills the tank and becomes empty");h.succeed();
 }
 @GameTest(template="empty") public static void dedicatedTankBuckets(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  d.sections.setStackInSlot(0,new ItemStack(AdventurersPack.TANK.get()));
  d.fluidSlot.setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));
  d.fluidSlot.setStackInSlot(2,new ItemStack(Items.LAVA_BUCKET));
  PackStorageActions.transferFluidSlots(d,p);
  h.assertTrue(d.tank.getFluidAmount()==1000&&d.tankTwo.getFluidAmount()==1000,"Inputs independently fill both tanks");
  d.fluidSlot.setStackInSlot(1,new ItemStack(Items.BUCKET));d.fluidSlot.setStackInSlot(3,new ItemStack(Items.BUCKET));
  PackStorageActions.transferFluidSlots(d,p);
  h.assertTrue(d.fluidSlot.getStackInSlot(1).is(Items.WATER_BUCKET)&&d.fluidSlot.getStackInSlot(3).is(Items.LAVA_BUCKET)&&d.tank.isEmpty()&&d.tankTwo.isEmpty(),"Outputs withdraw from their own tanks");
  PackStorageActions.transferFluidSlots(d,p);
  var restored=new PackData(d.owner.copy());
  h.assertTrue(restored.fluidSlot.getSlots()==4&&restored.fluidSlot.getStackInSlot(3).is(Items.LAVA_BUCKET)&&d.tankTwo.isEmpty(),"Completed buckets persist without reversing transfer");
  h.succeed();
 }
 @GameTest(template="empty") public static void tankCapacityAndPersistence(GameTestHelper h){
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  int accepted=d.tank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,129000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
  h.assertTrue(accepted==128000,"Tank enforces capacity");var restored=new PackData(d.owner.copy());
  h.assertTrue(restored.tank.getFluidAmount()==128000&&!restored.has(AdventurersPack.TANK.get()),"Fluid persists even when upgrade is absent");h.succeed();
 }
 @GameTest(template="empty") public static void xpBankRoundTrip(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));d.sections.setStackInSlot(0,new ItemStack(AdventurersPack.XP_BANK.get()));p.giveExperiencePoints(850);
  int before=PackStorageActions.points(p);int moved=PackStorageActions.transferXp(d,p,true,100);
  h.assertTrue(moved==100&&PackStorageActions.points(p)==before-100&&d.xpBank==100,"Deposit conserves experience across levels");
  var restored=new PackData(d.owner.copy());h.assertTrue(restored.xpBank==100,"Bank persists");
  PackStorageActions.transferXp(restored,p,false,200000);h.assertTrue(restored.xpBank==0&&PackStorageActions.points(p)==before,"Withdrawal restores exact points");
  p.giveExperiencePoints(250000);int capBefore=PackStorageActions.points(p);int capped=PackStorageActions.transferXp(restored,p,true,250000);
  h.assertTrue(capped==200000&&restored.xpBank==200000&&PackStorageActions.points(p)==capBefore-200000,"Bank holds 200000 points");PackStorageActions.transferXp(restored,p,false,200000);
  restored.sections.setStackInSlot(0,ItemStack.EMPTY);h.assertTrue(PackStorageActions.transferXp(restored,p,true,100)==0,"Missing upgrade cannot transfer XP");h.succeed();
 }
 @GameTest(template="empty") public static void cookingProgressSync(GameTestHelper h){
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));d.cook=78;d.cookTotal=300;d.burnTotal=1600;d.save();
  var restored=new PackData(d.owner.copy());h.assertTrue(restored.cookTotal==300&&restored.burnTotal==1600,"Custom recipe/fuel durations persist");
  var client=new PackData(new ItemStack(AdventurersPack.PACK.get()));client.readDisplay(d.displayTag());h.assertTrue(client.cook==78&&client.cookTotal==300&&client.burnTotal==1600,"Furnace progress reaches screen");h.succeed();
 }
 @GameTest(template="empty") public static void twoTanksStaySeparate(GameTestHelper h){
  var p=h.makeMockPlayer();var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  p.getInventory().setItem(0,d.owner);d.sections.setStackInSlot(0,new ItemStack(AdventurersPack.TANK.get()));var menu=new PackMenu(1,p.getInventory(),0,d);p.containerMenu=menu;
  menu.clickMenuButton(p,5000);menu.setCarried(new ItemStack(Items.WATER_BUCKET));
  h.assertTrue(menu.clickMenuButton(p,5002)&&d.tank.getFluidAmount()==1000,"Tank 1 stores water");
  h.assertTrue(menu.clickMenuButton(p,5011)&&d.tankIndex==1,"Tank 2 can be selected");
  menu.setCarried(new ItemStack(Items.LAVA_BUCKET));
  h.assertTrue(menu.clickMenuButton(p,5002)&&d.tankTwo.getFluidAmount()==1000&&d.tank.getFluid().getFluid()==net.minecraft.world.level.material.Fluids.WATER,"Second tank accepts a different fluid");
  menu.clickMenuButton(p,5010);menu.setCarried(new ItemStack(Items.LAVA_BUCKET));
  h.assertTrue(!menu.clickMenuButton(p,5002)&&d.tank.getFluidAmount()==1000,"The selected tank still refuses a different fluid");
  var restored=new PackData(d.owner.copy());
  h.assertTrue(restored.tank.getFluidAmount()==1000&&restored.tankTwo.getFluidAmount()==1000&&restored.tankIndex==0,"Both fluids and the selection persist");
  var client=new PackData(new ItemStack(AdventurersPack.PACK.get()));client.readDisplay(d.displayTag());
  h.assertTrue(client.tankTwo.getFluidAmount()==1000&&client.tankIndex==0,"Second tank reaches the screen");
  var legacy=new net.minecraftforge.items.ItemStackHandler(5);legacy.setStackInSlot(2,new ItemStack(AdventurersPack.QUIVER.get()));
  restored.sections.deserializeNBT(legacy.serializeNBT());
  h.assertTrue(restored.sections.getSlots()==6&&restored.sections.getStackInSlot(2).is(AdventurersPack.QUIVER.get()),"Older five-socket packs gain the battery socket");h.succeed();
 }
 @GameTest(template="empty") public static void batteryStoresForgeEnergy(GameTestHelper h){
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  d.sections.setStackInSlot(5,new ItemStack(AdventurersPack.BATTERY.get()));
  h.assertTrue(d.has(AdventurersPack.BATTERY.get())&&PackStorageActions.receiveEnergy(d,50000,false)==1000&&d.energy==1000,"The sixth socket charges at 1,000 FE");
  h.assertTrue(PackStorageActions.receiveEnergy(d,1000,true)==1000&&d.energy==1000,"A simulated charge does not move energy");
  var restored=new PackData(d.owner.copy());
  h.assertTrue(restored.energy==1000&&PackStorageActions.extractEnergy(restored,400,false)==400&&restored.energy==600,"Stored energy persists and discharges");
  restored.energy=PackData.ENERGY_CAPACITY;
  h.assertTrue(PackStorageActions.receiveEnergy(restored,1000,false)==0&&restored.energy==PackData.ENERGY_CAPACITY,"The battery stops at 1,000,000 FE");
  var client=new PackData(new ItemStack(AdventurersPack.PACK.get()));client.readDisplay(restored.displayTag());
  h.assertTrue(client.energy==PackData.ENERGY_CAPACITY,"Energy reaches the screen");
  restored.sections.setStackInSlot(5,ItemStack.EMPTY);
  h.assertTrue(PackStorageActions.extractEnergy(restored,1000,false)==0&&restored.energy==PackData.ENERGY_CAPACITY,"Removing the addon keeps the energy and blocks pipes");
  h.assertFalse(d.energySlots.isItemValid(0,new ItemStack(Items.COBBLESTONE)),"Only energy items fit the battery slots");
  var dock=new DockEntity(net.minecraft.core.BlockPos.ZERO,AdventurersPack.DOCK.get().defaultBlockState());
  var pack=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  pack.sections.setStackInSlot(0,new ItemStack(AdventurersPack.TANK.get()));
  pack.sections.setStackInSlot(1,new ItemStack(AdventurersPack.BATTERY.get()));
  pack.tank.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
  dock.setPack(pack.owner);var live=dock.data();
  var fluids=dock.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER).orElseThrow(IllegalStateException::new);
  int moved=fluids.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
  h.assertTrue(moved==1000&&live.tank.getFluidAmount()==1000&&live.tankTwo.getFluidAmount()==1000,"A dock pipe fills the matching or empty tank");
  var energy=dock.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
  h.assertTrue(energy.receiveEnergy(8000,false)==1000&&live.energy==1000,"A dock pipe charges the battery");
  live.sections.setStackInSlot(0,ItemStack.EMPTY);live.sections.setStackInSlot(1,ItemStack.EMPTY);
  h.assertTrue(fluids.fill(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==0&&energy.extractEnergy(1000,false)==0&&live.tankTwo.getFluidAmount()==1000&&live.energy==1000,"Pipes stop when the addons are removed");h.succeed();
 }
 @GameTest(template="empty") public static void caveTorchRoutingAndAutoPlacement(GameTestHelper h){
  var p=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CaveTorchTest"));
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  h.assertTrue(d.items.insertItem(124,new ItemStack(AdventurersPack.TORCH.get()),false).isEmpty(),"Sixth attachment socket accepts Cave Torch");
  h.assertTrue(d.has(AdventurersPack.TORCH.get()),"Cave Torch is active in slot 124");
  h.assertTrue(d.insert(new ItemStack(Items.TORCH,16),true).isEmpty()&&d.torches.getStackInSlot(0).getCount()==16,"Torches route into Cave Torch slots first");
  h.assertFalse(d.torches.isItemValid(0,new ItemStack(Items.REDSTONE_TORCH)),"Redstone torches are rejected");
  net.minecraft.core.BlockPos center=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
  for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
   h.getLevel().setBlock(center.offset(dx,-1,dz),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   h.getLevel().setBlock(center.offset(dx,0,dz),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
   h.getLevel().setBlock(center.offset(dx,1,dz),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
   h.getLevel().setBlock(center.offset(dx,2,dz),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
  }
  p.setPos(center.getX()+0.5,center.getY(),center.getZ()+0.5);
  h.assertTrue(PackEvents.placeTorch(p,d),"Torch is placed automatically in covered dark spot");
  h.assertTrue(h.getLevel().getBlockState(center).is(net.minecraft.world.level.block.Blocks.TORCH)&&d.torches.getStackInSlot(0).getCount()==15,"One torch placed and one consumed");
  h.assertFalse(PackEvents.placeTorch(p,d),"Nearby torch prevents spamming another torch");h.succeed();
 }
 @GameTest(template="empty") public static void warpUnlockAndLocations(GameTestHelper h){
  var p=new net.minecraft.world.entity.player.Player(h.getLevel(),new net.minecraft.core.BlockPos(10,64,20),0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"warp_test")){public boolean isSpectator(){return false;}public boolean isCreative(){return false;}};
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  p.getInventory().setItem(0,d.owner);
  var m=new PackMenu(1,p.getInventory(),0,d);p.containerMenu=m;
  h.assertTrue(!m.clickMenuButton(p,5400)&&!d.warpUnlocked,"Cannot unlock warp without materials");
  p.getInventory().setItem(1,new ItemStack(Items.NETHER_STAR,2));
  p.getInventory().setItem(2,new ItemStack(Items.NETHERITE_INGOT,4));
  p.getInventory().setItem(3,new ItemStack(Items.ENDER_EYE,10));
  h.assertTrue(m.clickMenuButton(p,5400)&&d.warpUnlocked&&p.getInventory().getItem(1).isEmpty()&&p.getInventory().getItem(2).isEmpty()&&p.getInventory().getItem(3).getCount()==2,"Warp unlock consumes exact high-end cost");
  h.assertTrue(!m.clickMenuButton(p,5400),"Cannot purchase warp unlock twice");
  for(int i=0;i<PackData.WARP_COUNT;i++){
   h.assertTrue(m.clickMenuButton(p,5410+i)&&d.warpIndex==i,"Select warp slot "+i);
   p.setPos(100+i*10,70+i,200+i*10);
   h.assertTrue(m.clickMenuButton(p,5420)&&d.warpSet[i]&&d.warpX[i]==100+i*10&&d.warpY[i]==70+i&&d.warpZ[i]==200+i*10,"Set warp slot "+i);
   d.warpNames[i]="Base "+(i+1);
  }
  d.warpDims[9]="minecraft:the_nether";
  d.save();
  var restored=new PackData(d.owner.copy());
  h.assertTrue(restored.warpUnlocked&&restored.warpSet[0]&&restored.warpSet[9]&&"minecraft:the_nether".equals(restored.warpDims[9])&&"Base 10".equals(restored.warpNames[9]),"All 10 warps and dimensions persist");
  var client=new PackData(new ItemStack(AdventurersPack.PACK.get()));client.readDisplay(restored.displayTag());
  h.assertTrue(client.warpUnlocked&&client.warpSet[9]&&client.warpX[9]==190,"Warp display data reaches screen");
  d.xpBank=250;
  h.assertTrue(d.consumeWarpCost(p)&&d.xpBank==150,"Warp cost draws from XP Bank first");
  d.xpBank=0;p.getInventory().setItem(4,new ItemStack(Items.ENDER_PEARL,2));
  h.assertTrue(d.consumeWarpCost(p)&&p.getInventory().getItem(4).getCount()==1,"Warp cost falls back to Ender Pearl");
  h.assertTrue(m.clickMenuButton(p,5422)&&!d.warpSet[9],"Clear warp removes binding");
  h.succeed();
 }
 @GameTest(template="empty") public static void quickActionsAndTrashSlot(GameTestHelper h){
  var p=new net.minecraft.world.entity.player.Player(h.getLevel(),new net.minecraft.core.BlockPos(1,2,1),0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"quick_actions")){public boolean isSpectator(){return false;}public boolean isCreative(){return false;}};
  var d=new PackData(new ItemStack(AdventurersPack.PACK.get()));
  p.getInventory().setItem(0,d.owner);
  var m=new PackMenu(1,p.getInventory(),0,d);p.containerMenu=m;
  d.items.setStackInSlot(3,new ItemStack(Items.COBBLESTONE,20));
  d.items.setStackInSlot(7,new ItemStack(Items.COBBLESTONE,30));
  h.assertTrue(m.clickMenuButton(p,5014)&&d.items.getStackInSlot(0).getCount()==50&&d.items.getStackInSlot(3).isEmpty(),"Sort Compartment consolidates and moves stacks to front");
  p.getInventory().setItem(10,new ItemStack(Items.COBBLESTONE,14));
  p.getInventory().setItem(11,new ItemStack(Items.GOLD_INGOT,5));
  h.assertTrue(m.clickMenuButton(p,5015)&&p.getInventory().getItem(10).isEmpty()&&p.getInventory().getItem(11).getCount()==5,"Quick Deposit moves only items already present in the pack");
  h.assertTrue(m.clickMenuButton(p,5017)&&p.getInventory().getItem(11).isEmpty(),"Deposit Inventory moves remaining main-inventory items");
  p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,10));
  h.assertTrue(m.clickMenuButton(p,5016)&&p.getInventory().getItem(1).getCount()==64,"Restock Hotbar refills hotbar stack from pack");
  m.setCarried(new ItemStack(Items.DIRT,12));
  m.clicked(PackMenu.CAMP_SLOT+22,0,net.minecraft.world.inventory.ClickType.PICKUP,p);
  h.assertTrue(m.getCarried().isEmpty()&&d.trash.getStackInSlot(0).is(Items.DIRT),"Trash slot accepts item");
  h.assertTrue(m.clickMenuButton(p,5018)&&d.trash.getStackInSlot(0).isEmpty(),"Empty Trash destroys held item");
  h.succeed();
 }
}
