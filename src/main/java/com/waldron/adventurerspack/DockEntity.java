package com.waldron.adventurerspack;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
public class DockEntity extends BlockEntity {
 public ItemStack pack=ItemStack.EMPTY;private PackData data;
 private LazyOptional<IItemHandler> capability=LazyOptional.of(()->new DockHandler());
 private LazyOptional<IFluidHandler> fluids=LazyOptional.of(()->new DockFluids());
 private LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->new DockEnergy());
 public DockEntity(BlockPos pos,BlockState state){super(AdventurersPack.DOCK_ENTITY.get(),pos,state);}
 public PackData data(){if(data==null||data.owner!=pack)data=new PackData(pack);return data;}
 public void setPack(ItemStack s){pack=s;data=null;setChanged();}
 private CompoundTag lastVisual;
 @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide){CompoundTag vis=PackEquipment.visualStack(pack).save(new CompoundTag());if(!vis.equals(lastVisual)){lastVisual=vis;level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),net.minecraft.world.level.block.Block.UPDATE_ALL);}}}
 @Override public CompoundTag getUpdateTag(){CompoundTag n=new CompoundTag();n.put("Backpack",PackEquipment.visualStack(pack).save(new CompoundTag()));return n;}
 @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 @Override public void onDataPacket(net.minecraft.network.Connection net,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt){if(pkt.getTag()!=null)load(pkt.getTag());}
 public boolean inUse(){return level!=null&&level.players().stream().anyMatch(p->p.containerMenu instanceof PackMenu m&&m.dock==this);}
 public static void serverTick(Level level,BlockPos pos,BlockState state,DockEntity dock){if(dock.pack.isEmpty())return;PackData d=dock.data();boolean changed=false;if(d.has(AdventurersPack.FURNACE.get())&&PackEvents.smelt(level,d))changed=true;if(d.has(AdventurersPack.ALEMBIC.get())&&PackEvents.brew(level,d,null,pos))changed=true;int before=d.energy;PackStorageActions.tickEnergy(d);if(d.energy!=before)changed=true;if(changed)dock.setChanged();}
 @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.put("Backpack",pack.save(new CompoundTag()));}
 @Override public void load(CompoundTag n){super.load(n);pack=ItemStack.of(n.getCompound("Backpack"));data=null;}
 @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(cap==ForgeCapabilities.ITEM_HANDLER)return capability.cast();if(cap==ForgeCapabilities.FLUID_HANDLER)return fluids.cast();if(cap==ForgeCapabilities.ENERGY)return energy.cast();return super.getCapability(cap,side);}
 @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();fluids.invalidate();energy.invalidate();}
 @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->new DockHandler());fluids=LazyOptional.of(()->new DockFluids());energy=LazyOptional.of(()->new DockEnergy());}
 private class DockHandler implements IItemHandler {
  public int getSlots(){return 270;}
  public ItemStack getStackInSlot(int slot){if(pack.isEmpty()||slot<0||slot>=270)return ItemStack.EMPTY;var d=data();return slot<252?d.storage(slot).copy():d.bulk[slot-252].copyWithCount(Math.min(64,d.counts[slot-252]));}
  public int getSlotLimit(int slot){return !pack.isEmpty()&&slot>=0&&slot<252?data().slotCapacity():64;}
  public boolean isItemValid(int slot,ItemStack s){return slot>=0&&slot<270&&(slot<252?PackData.safe(s):PackData.bulkable(s));}
  public ItemStack insertItem(int slot,ItemStack input,boolean simulate){if(pack.isEmpty()||!isItemValid(slot,input))return input;var d=data();if(slot<252){var out=d.insertStorage(slot,input,simulate);if(!simulate)setChanged();return out;}int i=slot-252;
   if(!d.bulk[i].isEmpty()&&!ItemStack.isSameItemSameTags(d.bulk[i],input))return input;int moved=Math.min(input.getCount(),Math.max(0,d.capacity()-d.counts[i]));ItemStack out=input.copy();if(!simulate){d.deposit(i,out);setChanged();}else out.shrink(moved);return out;
  }
  public ItemStack extractItem(int slot,int amount,boolean simulate){if(pack.isEmpty()||slot<0||slot>=270||amount<=0)return ItemStack.EMPTY;var d=data();if(slot<252){if(d.protectedStorage(slot))return ItemStack.EMPTY;var out=d.extractStorage(slot,amount,simulate);if(!simulate)setChanged();return out;}int i=slot-252;if(d.protectedSlot(i))return ItemStack.EMPTY;ItemStack out=simulate?d.bulk[i].copyWithCount(Math.min(Math.min(amount,64),d.counts[i])):d.withdraw(i,amount);if(!simulate)setChanged();return out;}
 }
 private class DockFluids implements IFluidHandler {
  public int getTanks(){return 2;}
  public FluidStack getFluidInTank(int tank){return pack.isEmpty()?FluidStack.EMPTY:data().tankAt(tank).getFluid();}
  public int getTankCapacity(int tank){return PackData.FLUID_CAPACITY;}
  public boolean isFluidValid(int tank,FluidStack stack){if(pack.isEmpty()||!data().has(AdventurersPack.TANK.get())||stack.isEmpty())return false;var stored=data().tankAt(tank);return stored.isEmpty()||stored.getFluid().isFluidEqual(stack);}
  public int fill(FluidStack resource,FluidAction action){if(pack.isEmpty()||!data().has(AdventurersPack.TANK.get())||resource.isEmpty())return 0;FluidStack left=resource.copy();int moved=0;for(int i=0;i<2&&!left.isEmpty();i++){int n=data().tankAt(i).fill(left,action);moved+=n;left.shrink(n);}if(moved>0&&action.execute())setChanged();return moved;}
  public FluidStack drain(FluidStack resource,FluidAction action){if(pack.isEmpty()||!data().has(AdventurersPack.TANK.get())||resource.isEmpty())return FluidStack.EMPTY;for(int i=0;i<2;i++){if(!data().tankAt(i).getFluid().isFluidEqual(resource))continue;FluidStack out=data().tankAt(i).drain(resource,action);if(!out.isEmpty()&&action.execute())setChanged();return out;}return FluidStack.EMPTY;}
  public FluidStack drain(int max,FluidAction action){if(pack.isEmpty()||!data().has(AdventurersPack.TANK.get())||max<=0)return FluidStack.EMPTY;for(int i=0;i<2;i++){if(data().tankAt(i).isEmpty())continue;FluidStack out=data().tankAt(i).drain(max,action);if(!out.isEmpty()&&action.execute())setChanged();return out;}return FluidStack.EMPTY;}
 }
 private class DockEnergy implements IEnergyStorage {
  public int receiveEnergy(int max,boolean simulate){if(pack.isEmpty())return 0;int moved=PackStorageActions.receiveEnergy(data(),max,simulate);if(moved>0&&!simulate)setChanged();return moved;}
  public int extractEnergy(int max,boolean simulate){if(pack.isEmpty())return 0;int moved=PackStorageActions.extractEnergy(data(),max,simulate);if(moved>0&&!simulate)setChanged();return moved;}
  public int getEnergyStored(){return pack.isEmpty()?0:data().energy;}
  public int getMaxEnergyStored(){return PackData.ENERGY_CAPACITY;}
  public boolean canExtract(){return !pack.isEmpty()&&data().has(AdventurersPack.BATTERY.get());}
  public boolean canReceive(){return canExtract();}
 }
}
