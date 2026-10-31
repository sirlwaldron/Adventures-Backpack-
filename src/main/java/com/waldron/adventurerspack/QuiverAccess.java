package com.waldron.adventurerspack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Predicate;

/** Vanilla consumes the returned stack only when a shot actually loads/fires. Queries are read-only. */
public final class QuiverAccess {
 private record Access(ItemStack owner,int slot,ItemStack stack,int count){}
 private static final Map<UUID,List<Access>> PENDING=new HashMap<>();
 private QuiverAccess(){}
 public static ItemStack borrow(Player p,PackData data,Predicate<ItemStack> supported){
  for(int i=0;i<9;i++){ItemStack s=data.quiver.getStackInSlot(i);if(s.isEmpty()||!supported.test(s))continue;
   ItemStack result=s.copy();if(!p.level().isClientSide)PENDING.computeIfAbsent(p.getUUID(),k->new ArrayList<>()).add(new Access(data.owner,i,result,result.getCount()));return result;
  }return ItemStack.EMPTY;
 }
 public static void flush(Player p){
  var pending=PENDING.remove(p.getUUID());if(pending==null)return;
  for(var access:pending){int used=access.count-access.stack.getCount();if(used<=0)continue;
   PackData data=p.containerMenu instanceof PackMenu m&&m.data.owner==access.owner?m.data:new PackData(access.owner);
   data.quiver.extractItem(access.slot,used,false);data.save();
  }
 }
}
