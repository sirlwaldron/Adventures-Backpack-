package com.waldron.adventurerspack;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
/** Reads the live installed item on each access: replacing a pouch never leaves stale contents. */
public final class PouchInventory implements IItemHandlerModifiable {
 private final PackData data;private final int kind;
 public PouchInventory(PackData data,int kind){this.data=data;this.kind=kind;}
 public ItemStack owner(){ItemStack s=data.pouches.getStackInSlot(kind);if(kind==2&&s.isEmpty())for(int i:PackData.GEAR_SLOTS)if(data.items.getStackInSlot(i).is(AdventurersPack.FEEDING.get()))return data.items.getStackInSlot(i);return s;}
 private ItemStackHandler load(){var h=new ItemStackHandler(18);ItemStack s=owner();if(s.hasTag()&&s.getTag().contains("Pouch"))h.deserializeNBT(s.getTag().getCompound("Pouch"));return h;}
 private void save(ItemStackHandler h){if(owner().isEmpty())return;owner().getOrCreateTag().put("Pouch",h.serializeNBT());data.save();}
 public int getSlots(){return 18;}
 public ItemStack getStackInSlot(int i){return owner().isEmpty()?ItemStack.EMPTY:load().getStackInSlot(i);}
 public boolean isItemValid(int i,ItemStack s){return !owner().isEmpty()&&PouchItem.accepts(kind,s)&&passesFilter(s);}
 /** Filters match item types, and stay on the pouch when it is removed. */
 public int filterMode(){return owner().hasTag()?Math.floorMod(owner().getTag().getInt("PouchFilterMode"),3):0;}
 private ItemStackHandler filters(){var h=new ItemStackHandler(9);if(owner().hasTag()&&owner().getTag().contains("PouchFilters"))h.deserializeNBT(owner().getTag().getCompound("PouchFilters"));return h;}
 public ItemStack filterItem(int i){var h=filters();return i<h.getSlots()?h.getStackInSlot(i):ItemStack.EMPTY;}
 public boolean passesFilter(ItemStack s){
  int mode=filterMode();if(mode==0)return true;
  boolean listed=false;for(int i=0;i<9;i++)if(!filterItem(i).isEmpty()&&filterItem(i).is(s.getItem())){listed=true;break;}
  return mode==1?listed:!listed;
 }
 public void cycleFilter(){if(owner().isEmpty())return;owner().getOrCreateTag().putInt("PouchFilterMode",(filterMode()+1)%3);data.save();}
 public void setFilter(int i,ItemStack sample){
  if(owner().isEmpty()||i<0||i>=9)return;
  var h=new ItemStackHandler(9);for(int j=0;j<9;j++)h.setStackInSlot(j,filterItem(j).copy());
  h.setStackInSlot(i,sample.isEmpty()?ItemStack.EMPTY:new ItemStack(sample.getItem()));
  owner().getOrCreateTag().put("PouchFilters",h.serializeNBT());data.save();
 }
 public int getSlotLimit(int i){return 64;}
 public ItemStack insertItem(int i,ItemStack s,boolean simulate){if(!isItemValid(i,s))return s;var h=load();ItemStack rest=h.insertItem(i,s,simulate);if(!simulate)save(h);return rest;}
 public ItemStack extractItem(int i,int n,boolean simulate){if(owner().isEmpty())return ItemStack.EMPTY;var h=load();ItemStack out=h.extractItem(i,n,simulate);if(!simulate)save(h);return out;}
 public void setStackInSlot(int i,ItemStack s){if(owner().isEmpty())return;var h=load();h.setStackInSlot(i,s);save(h);}
}
