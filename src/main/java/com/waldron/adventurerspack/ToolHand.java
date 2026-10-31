package com.waldron.adventurerspack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.common.ToolActions;
import java.util.UUID;

/** A temporary main-hand lease. Original items and home slots are saved on the player. */
public final class ToolHand {
 private static final String SESSION="AdventurersPackToolHand",MARKER="AdventurersPackLease",PACK_ID="AdventurersPackId";
 private ToolHand(){}
 /** Swapping is opt-in through the main-hand tool, never through a stored tool alone. */
 public static boolean canSwap(ItemStack held){return true;}
 public static boolean canSwap(Player p){return p==null||!p.isShiftKeyDown();}
 public static boolean manual(Player p){return active(p)&&session(p).getBoolean("Manual");}

 public static boolean isModdedItem(ItemStack stack){
  if(stack.isEmpty()||Requiem.form(stack)>=0)return false;
  var key=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
  return key!=null&&!"minecraft".equals(key.getNamespace())&&!AdventurersPack.ID.equals(key.getNamespace());
 }

 public static boolean isForeignWeaponOrTool(Player p,ItemStack held){
  if(held.isEmpty()||Requiem.form(held)>=0)return false;
  if(p!=null&&active(p)&&isLeased(p,held))return false;
  if(isModdedItem(held))return true;
  Item item=held.getItem();
  if(item instanceof TieredItem||item instanceof DiggerItem||item instanceof SwordItem
   ||item instanceof TridentItem||item instanceof net.minecraft.world.item.ProjectileWeaponItem
   ||item instanceof ShearsItem||item instanceof net.minecraft.world.item.ShieldItem)return true;
  if(!held.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).isEmpty())return true;
  return held.getMaxStackSize()==1&&held.isDamageableItem();
 }

 public static ItemStack findEndgameForAuto(Player p,PackData d){
  if(p!=null){
   ItemStack main=p.getMainHandItem();
   if(main.getCount()==1&&usable(main)&&Requiem.form(main)>=0)return main;
  }
  if(d!=null){
   for(int i=108;i<114;i++){
    ItemStack s=d.items.getStackInSlot(i);
    if(s.getCount()==1&&usable(s)&&Requiem.form(s)>=0)return s;
   }
  }
  return ItemStack.EMPTY;
 }

 public static ItemStack findEndgameItem(Player p,PackData d){
  if(p!=null){
   ItemStack main=p.getMainHandItem();
   if(main.getCount()==1&&usable(main)&&Requiem.form(main)>=0)return main;
   ItemStack off=p.getOffhandItem();
   if(off.getCount()==1&&usable(off)&&Requiem.form(off)>=0)return off;
  }
  if(d!=null){
   for(int i=108;i<114;i++){
    ItemStack s=d.items.getStackInSlot(i);
    if(s.getCount()==1&&usable(s)&&Requiem.form(s)>=0)return s;
   }
  }
  if(p!=null){
   for(int i=0;i<p.getInventory().getContainerSize();i++){
    ItemStack s=p.getInventory().getItem(i);
    if(s.getCount()==1&&usable(s)&&Requiem.form(s)>=0)return s;
   }
  }
  if(d!=null){
   for(int i=0;i<252;i++){
    ItemStack s=d.storage(i);
    if(s.getCount()==1&&usable(s)&&Requiem.form(s)>=0)return s;
   }
  }
  return ItemStack.EMPTY;
 }

 public static boolean hasEndgame(Player p,PackData d){
  return !findEndgameItem(p,d).isEmpty();
 }

 public static int findEndgameSlot(PackData d,Player p){
  if(d!=null){
   for(int i=108;i<114;i++){ItemStack s=d.items.getStackInSlot(i);if(s.getCount()==1&&usable(s)&&Requiem.form(s)>=0)return i;}
   for(int i=0;i<108;i++){ItemStack s=d.items.getStackInSlot(i);if(s.getCount()==1&&usable(s)&&Requiem.form(s)>=0)return i;}
  }
  return -1;
 }

 public static ItemStack getEndgameForm(Player p,PackData d,int index){
  if(index<0||index>=Requiem.WHEEL_ORDER.length)return ItemStack.EMPTY;
  int formId=Requiem.WHEEL_ORDER[index];
  ItemStack eg=findEndgameItem(p,d);
  if(!eg.isEmpty()){
   ItemStack tr=Requiem.transformed(eg.copyWithCount(1),formId);
   if(!tr.isEmpty())return tr;
   ItemStack fb=new ItemStack(Requiem.item(formId));
   if(eg.hasTag())fb.setTag(eg.getTag().copy());
   return fb;
  }
  return ItemStack.EMPTY;
 }

 public static ItemStack getToolSlot(Player p,PackData d,int slotIndex){
  if(hasEndgame(p,d)){
   if(slotIndex>=0&&slotIndex<Requiem.WHEEL_ORDER.length){
    return getEndgameForm(p,d,slotIndex);
   }
  }
  if(d!=null&&slotIndex>=0&&slotIndex<6){
   ItemStack existing=d.items.getStackInSlot(108+slotIndex);
   if(!existing.isEmpty())return existing;
  }
  if(p!=null&&active(p)&&source(p)==108+slotIndex){
   return p.getMainHandItem();
  }
  return ItemStack.EMPTY;
 }

 public static void draw(Player p,PackData d,int slot){
  if(p.level().isClientSide||!p.isAlive()||p.isSpectator()||p.containerMenu!=p.inventoryMenu||p.isUsingItem())return;
  if(p.isShiftKeyDown()&&Requiem.form(p.getMainHandItem())<0)return;
  // The tool-kit key selects the kit slot itself. The held hotbar item does not have to be a tool.
  if(slot<0||slot>=Requiem.WHEEL_ORDER.length)return;
  if(hasEndgame(p,d)){
   for(int i=0;i<Requiem.NAMES.length;i++)if(p.getCooldowns().isOnCooldown(Requiem.item(i)))return;
   // Return a different borrowed tool before touching the Endgame source.
   if(active(p)&&Requiem.form(p.getMainHandItem())<0){
    ItemStack owner=d==null?ItemStack.EMPTY:d.owner;restore(p);
    if(!owner.isEmpty())d=PackItem.data(p,owner);
   }
  }
  if(hasEndgame(p,d)&&slot>=0&&slot<Requiem.WHEEL_ORDER.length){
   int targetForm=Requiem.WHEEL_ORDER[slot];
   ItemStack held=p.getMainHandItem();
   // 1. If holding Endgame in hand
   if(Requiem.form(held)>=0){
    Requiem.transform(p,p.getInventory().selected,Requiem.form(held),targetForm);
    return;
   }
   // 3. If Endgame in backpack rack
   if(d!=null){
    int rackSlot=-1;
    for(int i=108;i<114;i++)if(d.items.getStackInSlot(i).getCount()==1&&Requiem.form(d.items.getStackInSlot(i))>=0){rackSlot=i;break;}
    int freeRack=-1;
    for(int i=108;i<114;i++)if(d.items.getStackInSlot(i).isEmpty()){freeRack=i;break;}
    if(rackSlot<0&&freeRack>=0){
     for(int i=0;i<252;i++)if(d.storage(i).getCount()==1&&Requiem.form(d.storage(i))>=0){
      ItemStack st=d.extractStorage(i,1,false);
      d.items.setStackInSlot(freeRack,st);
      rackSlot=freeRack;
      break;
     }
    }
     if(rackSlot>=108&&rackSlot<114){
     ItemStack master=d.items.getStackInSlot(rackSlot);
     ItemStack target=Requiem.transformed(master.copyWithCount(1),targetForm);
     d.items.setStackInSlot(rackSlot,target);
     d.save();
     if(equip(p,d,rackSlot,false))formCooldown(p);
     p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,0.7F,0.85F+(float)targetForm*0.08F);
     return;
    }
   }
   // 4. If Endgame in player inventory
   for(int i=0;i<p.getInventory().getContainerSize();i++){
    ItemStack in=p.getInventory().getItem(i);
    if(in.getCount()==1&&Requiem.form(in)>=0){
     ItemStack transformed=Requiem.transformed(in.copyWithCount(1),targetForm);
     if(d!=null){int freeRack=-1;for(int r=108;r<114;r++)if(d.items.getStackInSlot(r).isEmpty()){freeRack=r;break;}if(freeRack>=108){p.getInventory().setItem(i,ItemStack.EMPTY);p.getInventory().setChanged();d.items.setStackInSlot(freeRack,transformed);d.save();if(equip(p,d,freeRack,false))formCooldown(p);p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,0.7F,0.85F+(float)targetForm*0.08F);return;}}
     ItemStack current=p.getMainHandItem();
     if(isForeignWeaponOrTool(p,current))return;
     p.getInventory().setItem(i,current);
     p.setItemInHand(InteractionHand.MAIN_HAND,transformed);
     formCooldown(p);
     p.getInventory().setChanged();
     p.level().playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.PLAYERS,0.7F,0.85F+(float)targetForm*0.08F);
     return;
    }
   }
   // No owned weapon was found: never create a template or replace the hand.
   return;
  }
  if(slot<0||slot>=6||d==null)return;
  ItemStack existing=d.items.getStackInSlot(108+slot);
  if(usable(existing))equip(p,d,108+slot,false);
 }

 private static void formCooldown(Player p){for(int i=0;i<Requiem.NAMES.length;i++)p.getCooldowns().addCooldown(Requiem.item(i),8);p.inventoryMenu.broadcastChanges();}

 public static boolean active(Player p){return p.getPersistentData().contains(SESSION);}
 private static CompoundTag session(Player p){return p.getPersistentData().getCompound(SESSION);}
 public static int source(Player p){return active(p)?session(p).getInt("Home"):-1;}
 public static ItemStack original(Player p){return active(p)?ItemStack.of(session(p).getCompound("Original")):p.getMainHandItem();}
 public static boolean usable(ItemStack s){return !s.isEmpty()&&(!s.isDamageableItem()||s.getDamageValue()<s.getMaxDamage()-1);}
 /** Sword, spear, lance, bows, and the axe mine or fight as that tool. Pickaxe, shovel, and hoe stay harvest forms. */
 public static boolean weaponForm(int form){
  if(form<0||form>=Requiem.NAMES.length)return false;
  Item item=Requiem.item(form);
  return item instanceof SwordItem||item instanceof BowItem||item instanceof AxeItem||item instanceof TridentItem;
 }
 /** Strongest attack form, scored the same way as a normal rack weapon. */
 public static int bestWeaponForm(ItemStack sample){
  int best=-1,score=0;
  ItemStack base=sample.getCount()==1&&Requiem.form(sample)>=0?sample.copyWithCount(1):ItemStack.EMPTY;
  for(int f=0;f<Requiem.NAMES.length;f++){
   if(!weaponForm(f))continue;
   ItemStack candidate=base.isEmpty()?new ItemStack(Requiem.item(f)):Requiem.transformed(base,f);
   int s=PackEvents.weaponScore(candidate);
   if(s>score){score=s;best=f;}
  }
  return best;
 }
 /**
  * Switches the one Endgame item into {@code targetForm}, using the same order as the tool wheel:
  * main hand, tool rack, pack storage, then the rest of the inventory.
  * @return rack slot to lease, -1 when the item is already in the main hand, or -2 when no item was changed
  */
 public static int applyEndgameForm(Player p,PackData d,int targetForm){
  if(!canSwap(p)||targetForm<0||targetForm>=Requiem.NAMES.length)return -2;
  if(p!=null){
   ItemStack held=p.getMainHandItem();
   if(isForeignWeaponOrTool(p,held))return -2;
   if(held.getCount()==1&&Requiem.form(held)>=0){
    if(Requiem.form(held)!=targetForm){
     ItemStack next=Requiem.transformed(held,targetForm);
     if(next.isEmpty())return -2;
     p.setItemInHand(InteractionHand.MAIN_HAND,next);
     p.getInventory().setChanged();
     if(p.containerMenu!=null)p.inventoryMenu.broadcastChanges();
    }
    return -1;
   }
  }
  if(d==null)return -2;
  for(int i=108;i<114;i++){
   ItemStack s=d.items.getStackInSlot(i);
   if(s.getCount()==1&&Requiem.form(s)>=0){
    if(Requiem.form(s)!=targetForm){
     ItemStack next=Requiem.transformed(s,targetForm);
     if(next.isEmpty())return -2;
     d.items.setStackInSlot(i,next);d.save();
    }
    return i;
   }
  }
  return -2;
 }
 public static int bestTool(PackData d,ItemStack held,BlockState state){return bestTool(null,d,held,state);}
 /** Suitable hand tools stay put. A kit tool is chosen only when it can harvest, fastest first. */
 public static int bestTool(Player p,PackData d,ItemStack held,BlockState state){
  if(p!=null&&p.isShiftKeyDown())return -1;
  if(!canSwap(held)||isModdedItem(held)||correctFor(held,state))return -1;
  float speed=-1;int best=-1;ItemStack winner=ItemStack.EMPTY;
  if(d!=null){
   for(int i=108;i<114;i++){ItemStack s=d.items.getStackInSlot(i);if(Requiem.form(s)>=0&&isForeignWeaponOrTool(p,held))continue;if(!correctFor(s,state))continue;float v=s.getDestroySpeed(state);
    if(better(s,v,winner,speed)){speed=v;best=i;winner=s;}
   }
  }
  if(!isForeignWeaponOrTool(p,held)){
   ItemStack eg=findEndgameForAuto(p,d);
   if(!eg.isEmpty()){
    int bestForm=-1;
    for(int f=0;f<Requiem.NAMES.length;f++){
     ItemStack candidate=Requiem.transformed(eg.copyWithCount(1),f);
     if(!correctFor(candidate,state))continue;
     float v=candidate.getDestroySpeed(state);
     if(better(candidate,v,winner,speed)){speed=v;bestForm=f;winner=candidate;}
    }
    if(bestForm>=0){
     int placed=applyEndgameForm(p,d,bestForm);
     if(placed>=108||placed==-1)return placed;
    }
   }
  }
  return best;
 }
 public static int bestTill(Player p,PackData d,Level level,BlockPos pos){
  if(!canSwap(p)||level==null||pos==null)return -1;
  ItemStack held=p==null?ItemStack.EMPTY:p.getMainHandItem();
  if(isForeignWeaponOrTool(p,held)||canTill(level,pos,held))return -1;
  float speed=-1;int best=-1;
  if(d!=null){
   for(int i=108;i<114;i++){ItemStack s=d.items.getStackInSlot(i);if(!canTill(level,pos,s))continue;float v=tillRank(s);if(v>speed){speed=v;best=i;}}
  }
  ItemStack eg=findEndgameForAuto(p,d);
  if(!eg.isEmpty()){
   int bestForm=-1;
   for(int f=0;f<Requiem.NAMES.length;f++){
    ItemStack candidate=Requiem.transformed(eg.copyWithCount(1),f);
    if(!canTill(level,pos,candidate))continue;
    float v=tillRank(candidate);
    if(v>speed){speed=v;bestForm=f;}
   }
   if(bestForm>=0){
    int placed=applyEndgameForm(p,d,bestForm);
    if(placed>=108||placed==-1)return placed;
   }
  }
  return best;
 }
 public static boolean canTill(Level level,BlockPos pos,ItemStack stack){
  if(!usable(stack)||!stack.canPerformAction(ToolActions.HOE_TILL))return false;
  BlockHitResult hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
  UseOnContext ctx=new UseOnContext(level,null,InteractionHand.MAIN_HAND,stack,hit);
  return level.getBlockState(pos).getToolModifiedState(ctx,ToolActions.HOE_TILL,true)!=null;
 }
 private static float tillRank(ItemStack stack){return stack.getItem() instanceof TieredItem tiered?tiered.getTier().getSpeed():1.0F;}
 private static boolean correctFor(ItemStack stack,BlockState state){
  if(!usable(stack))return false;
  if(state.requiresCorrectToolForDrops())return stack.isCorrectToolForDrops(state);
  var item=stack.getItem();
  if(!(item instanceof DiggerItem||item instanceof ShearsItem||item instanceof SwordItem))return false;
  // SwordItem reports 1.5 on plants. Harvest tools report their real mining speed.
  return stack.getDestroySpeed(state)>1.5F;
 }
 private static boolean better(ItemStack candidate,float speed,ItemStack current,float best){
  if(current.isEmpty()||speed>best)return true;
  return speed==best&&candidate.getItem() instanceof ShearsItem&&!(current.getItem() instanceof ShearsItem);
 }
 public static boolean equip(Player p,PackData d,int home){return equip(p,d,home,true);}
 /** Manual draws skip the held-tool check and move the hotbar cursor onto the kit slot. */
 public static boolean equip(Player p,PackData d,int home,boolean requireHeldTool){
  if((requireHeldTool&&!canSwap(p))||home<108||home>=114)return false;
  if(active(p)&&source(p)==home){if(!requireHeldTool)session(p).putBoolean("Manual",true);touch(p);if(!requireHeldTool)sync(p);return true;}
  // Resolve the live owner again after a return: it may have changed the kit inventory.
  ItemStack owner=d.owner;restore(p);d=PackItem.data(p,owner);
  ItemStack tool=d.items.getStackInSlot(home);if(!usable(tool))return false;
  var tag=owner.getOrCreateTag();if(!tag.hasUUID(PACK_ID))tag.putUUID(PACK_ID,UUID.randomUUID());UUID lease=UUID.randomUUID();
  CompoundTag n=new CompoundTag();n.putUUID("Id",lease);n.putUUID("Pack",tag.getUUID(PACK_ID));n.putInt("Home",home);n.putInt("HandSlot",p.getInventory().selected);n.putInt("Last",p.tickCount);n.put("Original",p.getMainHandItem().save(new CompoundTag()));n.put("Tool",tool.save(new CompoundTag()));if(!requireHeldTool)n.putBoolean("Manual",true);
  // Ownership moves kit -> separate equipment; the original hotbar item never moves.
  n.putBoolean("Virtual",true);
  p.getPersistentData().put(SESSION,n);d.items.setStackInSlot(home,ItemStack.EMPTY);
  tool.getOrCreateTag().putUUID(MARKER,lease);ToolEquipment.set(p,tool);d.save();sync(p);return true;
 }
 public static void touch(Player p){if(active(p))session(p).putInt("Last",p.tickCount);}
 public static boolean isLeased(Player p,ItemStack stack){if(!active(p)||!stack.hasTag()||!stack.getTag().hasUUID(MARKER))return false;return stack.getTag().getUUID(MARKER).equals(session(p).getUUID("Id"));}
 private static void clean(ItemStack tool){if(tool.hasTag()){tool.getTag().remove(MARKER);if(tool.getTag().isEmpty())tool.setTag(null);}}
 public static void restore(Player p){restore(p,ItemStack.EMPTY);}
 public static void restore(Player p,ItemStack tossed){
  if(!active(p))return;CompoundTag n=session(p).copy();int hand=n.getInt("HandSlot");ItemStack original=ItemStack.of(n.getCompound("Original"));ItemStack tool=ItemStack.EMPTY;
  if(n.getBoolean("Virtual")){tool=ToolEquipment.stack(p);ToolEquipment.set(p,ItemStack.EMPTY);}
  for(int i=0;tool.isEmpty()&&i<p.getInventory().getContainerSize();i++)if(isLeased(p,p.getInventory().getItem(i))){tool=p.getInventory().getItem(i);p.getInventory().setItem(i,ItemStack.EMPTY);break;}
  if(tool.isEmpty()&&isLeased(p,tossed))tool=tossed;if(tool.isEmpty()&&n.contains("Tool"))tool=ItemStack.of(n.getCompound("Tool"));clean(tool);
  PackData d=null;for(int i=0;i<=PackItem.CURIOS;i++){ItemStack s=PackItem.stack(p,i);if(s.getItem() instanceof PackItem&&s.hasTag()&&s.getTag().hasUUID(PACK_ID)&&s.getTag().getUUID(PACK_ID).equals(n.getUUID("Pack"))){d=PackItem.data(p,s);break;}}
  p.getPersistentData().remove(SESSION);
  if(!tool.isEmpty()&&d!=null){int home=n.getInt("Home");if(home>=108&&home<114)tool=d.items.insertItem(home,tool,false);if(!tool.isEmpty())for(int i=108;i<114&&!tool.isEmpty();i++)tool=d.items.insertItem(i,tool,false);d.save();}
  // A broken tool is simply absent. Never recreate it from a saved template.
  if(!n.getBoolean("Virtual")&&!original.isEmpty()){if(hand>=0&&hand<9&&p.getInventory().getItem(hand).isEmpty())p.getInventory().setItem(hand,original);else give(p,original);}
  if(!tool.isEmpty())give(p,tool);sync(p);
 }
 private static void give(Player p,ItemStack stack){if(!p.getInventory().add(stack)&&!stack.isEmpty())p.drop(stack,false);}
 public static void tick(Player p){if(!active(p))return;CompoundTag n=session(p);
  boolean changed=p.getInventory().selected!=n.getInt("HandSlot")||!isLeased(p,p.getMainHandItem());
  if(changed||p.containerMenu!=p.inventoryMenu||(!manual(p)&&p.tickCount-n.getInt("Last")>10)||p.isSpectator()||!p.isAlive())restore(p);
  else if(n.getBoolean("Virtual")){
   var tool=ToolEquipment.stack(p);
   tool.inventoryTick(p.level(),p,-1,true);
   ItemStack previous=LAST_TOOL.get(p);
   if(previous==null||!ItemStack.matches(previous,tool))sync(p);
  }
 }
 private static final java.util.Map<Player,ItemStack> LAST_TOOL=new java.util.WeakHashMap<>();
 private static void sync(Player p){if(active(p)&&session(p).getBoolean("Virtual")){ItemStack t=ToolEquipment.stack(p);if(t.isEmpty())session(p).remove("Tool");else session(p).put("Tool",t.save(new CompoundTag()));}LAST_TOOL.put(p,ToolEquipment.stack(p).copy());p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();if(p instanceof ServerPlayer sp)Network.sendToolHand(sp,active(p),active(p)?source(p):-1,active(p)?session(p).getInt("HandSlot"):-1,active(p)?original(p):ItemStack.EMPTY,active(p)&&session(p).getBoolean("Virtual")?ToolEquipment.stack(p):ItemStack.EMPTY,active(p)&&manual(p));}
}
