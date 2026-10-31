package com.waldron.adventurerspack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Integer counts survive NBT; withdrawals always produce legal vanilla stacks. */
public class StackStorageHandler extends ItemStackHandler {
 private final PackData data;
 private final int storageSlots;
 public StackStorageHandler(PackData data,int size,int storageSlots){super(size);this.data=data;this.storageSlots=storageSlots;}
 @Override public int getSlotLimit(int slot){return slot<storageSlots?data.slotCapacity():1;}
 @Override protected int getStackLimit(int slot,ItemStack stack){return data.slotTier>0&&slot<storageSlots&&stack.getMaxStackSize()>1?getSlotLimit(slot):Math.min(getSlotLimit(slot),stack.getMaxStackSize());}
 @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return super.insertItem(slot,stack.copy(),simulate);}
 @Override public ItemStack extractItem(int slot,int amount,boolean simulate){ItemStack stack=getStackInSlot(slot);return super.extractItem(slot,Math.min(amount,stack.getMaxStackSize()),simulate);}
 @Override public CompoundTag serializeNBT(){
  CompoundTag tag=super.serializeNBT();
  for(Tag entry:tag.getList("Items",Tag.TAG_COMPOUND)){CompoundTag item=(CompoundTag)entry;int slot=item.getInt("Slot");item.putInt("StorageCount",getStackInSlot(slot).getCount());item.putByte("Count",(byte)1);}
  return tag;
 }
 @Override public void deserializeNBT(CompoundTag tag){
  super.deserializeNBT(tag);
  for(Tag entry:tag.getList("Items",Tag.TAG_COMPOUND)){CompoundTag item=(CompoundTag)entry;int slot=item.getInt("Slot");if(slot>=0&&slot<getSlots()&&item.contains("StorageCount"))getStackInSlot(slot).setCount(Math.max(0,item.getInt("StorageCount")));}
 }
}
