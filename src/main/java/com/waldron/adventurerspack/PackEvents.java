package com.waldron.adventurerspack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.ForgeHooks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class PackEvents {
 @SubscribeEvent public void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){ToolHand.restore(e.getEntity());ROCKETS.remove(e.getEntity().getUUID());QuiverAccess.flush(e.getEntity());}
 @SubscribeEvent public void tracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking e){if(net.minecraftforge.fml.ModList.get().isLoaded("azurelib")&&e.getTarget() instanceof ServerPlayer tracked)AzureLibDispatcher.replay(tracked);}
 @SubscribeEvent public void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){ToolHand.restore(e.getEntity());}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public void death(net.minecraftforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof Player p&&!p.level().isClientSide)ToolHand.restore(p);}
 @SubscribeEvent public void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){ToolHand.restore(e.getEntity());}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public void toss(net.minecraftforge.event.entity.item.ItemTossEvent e){if(ToolHand.isLeased(e.getPlayer(),e.getEntity().getItem())){e.setCanceled(true);ToolHand.restore(e.getPlayer(),e.getEntity().getItem());}}
 @SubscribeEvent public void swapHands(net.minecraftforge.event.entity.living.LivingSwapItemsEvent.Hands e){
  if(e.getEntity() instanceof Player p&&ToolEquipment.active(p)){e.setCanceled(true);ToolHand.restore(p);}
 }
 private static final Map<UUID,Integer> ROCKETS=new HashMap<>();
 @SubscribeEvent public void mine(net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock e){if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!(e.getEntity() instanceof ServerPlayer p)||p.containerMenu!=p.inventoryMenu||p.isShiftKeyDown())return;selectTool(p,e.getPos());}
 @SubscribeEvent public void till(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){if(e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!(e.getEntity() instanceof ServerPlayer p)||p.containerMenu!=p.inventoryMenu||ToolHand.manual(p)||p.isShiftKeyDown()||hasRightClickUse(p,p.getMainHandItem()))return;int pack=PackItem.find(p);if(pack<0)return;PackData d=PackItem.data(p,PackItem.stack(p,pack));if(!autoTools(d))return;int best=ToolHand.bestTill(p,d,p.level(),e.getPos());if(best>=0&&ToolHand.equip(p,d,best)){e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.CONSUME);}}
 private static boolean hasRightClickUse(Player p,ItemStack held){
  if(held.isEmpty())return false;
  if(ToolHand.isForeignWeaponOrTool(p,held)||held.isEdible()||held.getUseDuration()>0)return true;
  Item item=held.getItem();
  return item instanceof BlockItem||item instanceof BoneMealItem||item instanceof BucketItem||item instanceof FlintAndSteelItem||item instanceof FireChargeItem||item instanceof SpawnEggItem||item instanceof EnderpearlItem||item instanceof PotionItem;
 }
 @SubscribeEvent public void attack(AttackEntityEvent e){if(!(e.getEntity() instanceof ServerPlayer p)||p.containerMenu!=p.inventoryMenu||p.isShiftKeyDown())return;int pack=PackItem.find(p);if(pack<0)return;PackData d=PackItem.data(p,PackItem.stack(p,pack));boolean combat=d.preset==PackData.COMBAT&&PackConfig.COMBAT_WEAPONS.get();if(combat||autoTools(d))selectWeapon(p,d);}

 @SubscribeEvent public void pickup(EntityItemPickupEvent e){var p=e.getEntity();if(p.level().isClientSide||p.isSpectator()||p.containerMenu!=p.inventoryMenu)return;int i=PackItem.find(p);if(i<0)return;PackData d=PackItem.data(p,PackItem.stack(p,i));if(!d.pickup||!d.has(AdventurersPack.PICKUP.get()))return;
  ItemStack original=e.getItem().getItem();ItemStack rest=d.insert(original,true);int moved=original.getCount()-rest.getCount();if(moved<=0)return;
  p.take(e.getItem(),moved);e.setCanceled(true);if(rest.isEmpty())e.getItem().discard();else e.getItem().setItem(rest);d.save();p.getInventory().setChanged();
 }
 @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!p.isAlive()||p.isSpectator())return;
  ToolHand.tick(p);
  if(p.tickCount%10==0)PackEquipment.sync(p);
  if(p.containerMenu!=p.inventoryMenu){if(p.containerMenu instanceof PackMenu routeMenu&&p.tickCount%20==0)PackStorageActions.autoRoute(routeMenu.data,p,routeMenu.boundSlot);if(p.containerMenu instanceof PackMenu menu&&menu.dock==null){if(p.tickCount%5==0){for(int n=0;n<p.getInventory().getContainerSize();n++)syncMap(p,p.getInventory().getItem(n));for(int n=0;n<18;n++)syncMap(p,menu.data.pouchStorage[3].getStackInSlot(n));}if(menu.data.has(AdventurersPack.ALEMBIC.get()))brew(p.level(),menu.data,p,p.blockPosition());if(menu.data.has(AdventurersPack.FURNACE.get()))smelt(p.level(),menu.data);PackStorageActions.tickEnergy(menu.data);}return;}
  int i=PackItem.find(p);if(i<0)return;PackData d=PackItem.data(p,PackItem.stack(p,i));
  if(p.tickCount%10==0 && d.pickup&&d.has(AdventurersPack.MAGNET.get())&&d.has(AdventurersPack.PICKUP.get())){
   for(ItemEntity entity:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(PackConfig.MAGNET_RANGE.get()),it->it.isAlive()&&!it.hasPickUpDelay()))entity.playerTouch(p);
   // Pickup callbacks mutate item NBT; refresh before other automation to avoid stale overwrites.
   d=PackItem.data(p,PackItem.stack(p,i));
  }
  if(p.tickCount%20==0)PackStorageActions.autoRoute(d,p,i);
  if(p.tickCount%10==0&&d.has(AdventurersPack.TORCH.get()))placeTorch(p,d);
  if(p.tickCount%20==0){if(d.has(AdventurersPack.FEEDING.get())&&p.getFoodData().needsFood())feed(p,d);if(d.has(AdventurersPack.RESTOCK.get()))restock(p,d);}
  if(d.has(AdventurersPack.FURNACE.get()))smelt(p.level(),d);
  if(d.has(AdventurersPack.ALEMBIC.get()))brew(p.level(),d,p,p.blockPosition());
  PackStorageActions.tickEnergy(d);
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public void projectile(LivingGetProjectileEvent e){
  if(!(e.getEntity() instanceof Player p)||!(e.getProjectileWeaponItemStack().getItem() instanceof ProjectileWeaponItem gun))return;
  int slot=PackItem.find(p);if(slot<0)return;PackData d=PackItem.data(p,PackItem.stack(p,slot));if(!d.has(AdventurersPack.QUIVER.get()))return;
  ItemStack ammo=QuiverAccess.borrow(p,d,gun.getAllSupportedProjectiles());if(!ammo.isEmpty())e.setProjectileItemStack(ammo);
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public void flushAmmo(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.START&&!e.player.level().isClientSide)QuiverAccess.flush(e.player);}
 private static void syncMap(ServerPlayer p,ItemStack s){if(!(s.getItem() instanceof MapItem map))return;var data=MapItem.getSavedData(s,p.level());if(data!=null)data.tickCarriedBy(p,s);var packet=map.getUpdatePacket(s,p.level(),p);if(packet!=null)p.connection.send(packet);}
 public static void launchRocket(ServerPlayer player,PackData data){if(!data.has(AdventurersPack.QUIVER.get())||!player.isFallFlying())return;int now=player.tickCount;Integer last=ROCKETS.get(player.getUUID());if(last!=null&&now-last<10)return;Predicate<ItemStack> rockets=stack->stack.is(Items.FIREWORK_ROCKET);if(data.peekAmmo(rockets).isEmpty())return;ROCKETS.put(player.getUUID(),now);ItemStack rocket=player.getAbilities().instabuild?data.peekAmmo(rockets).copyWithCount(1):data.takeAmmo(rockets);if(rocket.isEmpty())return;player.level().addFreshEntity(new FireworkRocketEntity(player.level(),rocket,player));}
 public static boolean placeTorch(Player p,PackData d){
  if(p==null||d==null||!d.has(AdventurersPack.TORCH.get())||p.level().isClientSide||p.isSpectator())return false;
  ItemStack sample=d.peekTorch();if(sample.isEmpty())return false;
  Level level=p.level();net.minecraft.core.BlockPos origin=p.blockPosition();
  for(int r=0;r<=2;r++)for(int dy:new int[]{0,-1,1})for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
   net.minecraft.core.BlockPos pos=origin.offset(dx,dy,dz);
   if(!level.getBlockState(pos).isAir()||!level.getFluidState(pos).isEmpty())continue;
   if(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos)>6)continue;
   if(level.canSeeSky(pos)&&level.getMaxLocalRawBrightness(pos)>7)continue;
   if(hasNearbyTorch(level,pos,5))continue;
   var state=torchState(level,pos,sample);if(state==null)continue;
   if(!p.getAbilities().instabuild&&!d.consumeTorch())return false;
   level.setBlock(pos,state,3);
   level.playSound(null,pos,net.minecraft.sounds.SoundEvents.WOOD_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,0.8F,1.0F);
   return true;
  }
  return false;
 }
 private static boolean hasNearbyTorch(Level level,net.minecraft.core.BlockPos center,int r){
  for(int dx=-r;dx<=r;dx++)for(int dy=-2;dy<=2;dy++)for(int dz=-r;dz<=r;dz++)
   if(level.getBlockState(center.offset(dx,dy,dz)).getBlock() instanceof net.minecraft.world.level.block.TorchBlock)return true;
  return false;
 }
 private static net.minecraft.world.level.block.state.BlockState torchState(Level level,net.minecraft.core.BlockPos pos,ItemStack sample){
  var block=sample.is(Items.SOUL_TORCH)?net.minecraft.world.level.block.Blocks.SOUL_TORCH:sample.getItem() instanceof BlockItem bi&&bi.getBlock() instanceof net.minecraft.world.level.block.TorchBlock?bi.getBlock():net.minecraft.world.level.block.Blocks.TORCH;
  var floor=block.defaultBlockState();if(floor.canSurvive(level,pos))return floor;
  var wallBlock=sample.is(Items.SOUL_TORCH)?net.minecraft.world.level.block.Blocks.SOUL_WALL_TORCH:sample.is(Items.TORCH)?net.minecraft.world.level.block.Blocks.WALL_TORCH:null;
  if(wallBlock!=null)for(var dir:net.minecraft.core.Direction.Plane.HORIZONTAL){var wall=wallBlock.defaultBlockState().setValue(net.minecraft.world.level.block.WallTorchBlock.FACING,dir);if(wall.canSurvive(level,pos))return wall;}
  return null;
 }
 public static boolean brew(Level level,PackData d){return brew(level,d,null,null);}
 public static boolean brew(Level level,PackData d,ServerPlayer player,net.minecraft.core.BlockPos at){if(!d.has(AdventurersPack.ALEMBIC.get()))return false;boolean changed=false;ItemStack fuel=d.brew.getStackInSlot(4);if(d.brewFuel<=0&&fuel.is(Items.BLAZE_POWDER)){d.brew.extractItem(4,1,false);d.brewFuel=20;changed=true;}boolean can=canBrew(d);if(d.brewTime>0){if(can){if(--d.brewTime==0)finishBrew(level,d,player,at);changed=true;}else{d.brewTime=0;changed=true;}}else if(can&&d.brewFuel>0){d.brewFuel--;d.brewTime=400;changed=true;}if(changed)d.save();return changed;}
 private static boolean canBrew(PackData d){ItemStack ingredient=d.brew.getStackInSlot(3);if(ingredient.isEmpty()||!PotionBrewing.isIngredient(ingredient))return false;for(int i=0;i<3;i++){ItemStack bottle=d.brew.getStackInSlot(i);if(!bottle.isEmpty()&&PotionBrewing.hasMix(bottle,ingredient))return true;}return false;}
 private static void finishBrew(Level level,PackData d,ServerPlayer player,net.minecraft.core.BlockPos at){ItemStack ingredient=d.brew.getStackInSlot(3);if(ingredient.isEmpty())return;for(int i=0;i<3;i++){ItemStack bottle=d.brew.getStackInSlot(i);if(!bottle.isEmpty()&&PotionBrewing.hasMix(bottle,ingredient))d.brew.setStackInSlot(i,PotionBrewing.mix(ingredient,bottle));}ItemStack left=d.brew.getStackInSlot(3);if(left.isEmpty())return;ItemStack remainder=left.getCraftingRemainingItem();if(left.getCount()<=1)d.brew.setStackInSlot(3,remainder.isEmpty()?ItemStack.EMPTY:remainder.copy());else{left.shrink(1);if(!remainder.isEmpty()){ItemStack rest=d.insert(remainder.copy(),false);if(!rest.isEmpty()&&player!=null)player.getInventory().add(rest);if(!rest.isEmpty()&&level!=null&&at!=null)net.minecraft.world.Containers.dropItemStack(level,at.getX()+.5,at.getY()+.5,at.getZ()+.5,rest);}}d.pourFinished();}
 private void feed(ServerPlayer p,PackData d){
  for(int i=0;i<18;i++){
   ItemStack s=d.pouchStorage[2].getStackInSlot(i);var food=s.getFoodProperties(p);
   if(food==null||!food.getEffects().isEmpty()||food.getNutrition()>20-p.getFoodData().getFoodLevel())continue;
   Item consumed=s.getItem();ItemStack meal=d.pouchStorage[2].extractItem(i,1,false);
   returnMealRemainder(p,d,consumed,meal.finishUsingItem(p.level(),p),i,true);return;
  }
  for(int i=0;i<252;i++){
   if(d.protectedStorage(i))continue;ItemStack s=d.storage(i);var food=s.getFoodProperties(p);
   if(food==null||!food.getEffects().isEmpty()||food.getNutrition()>20-p.getFoodData().getFoodLevel())continue;
   Item consumed=s.getItem();ItemStack meal=d.extractStorage(i,1,false);
   returnMealRemainder(p,d,consumed,meal.finishUsingItem(p.level(),p),i,false);return;
  }
 }
 /** Reusable food belongs back in its source slot, even if eating updates its tags. */
 static void returnMealRemainder(Player p,PackData d,Item consumed,ItemStack rest,int slot,boolean pouch){
  if(!rest.isEmpty()&&rest.is(consumed)){
   var handler=pouch?d.pouchStorage[2]:d.storageHandler(slot);
   int local=pouch?slot:d.localSlot(slot);
   // Returning an existing item is not a new insertion: a changed pouch filter must not eject it.
   if(handler.getStackInSlot(local).isEmpty()&&(!pouch||!d.pouchStorage[2].owner().isEmpty())){
    handler.setStackInSlot(local,rest);rest=ItemStack.EMPTY;
   }else rest=handler.insertItem(local,rest,false);
  }
  if(!rest.isEmpty()&&!pouch)rest=d.insert(rest,false);
  if(!rest.isEmpty()&&!p.getInventory().add(rest))p.drop(rest,false);
  d.save();
 }
 private void restock(ServerPlayer p,PackData d){for(int hotbar=0;hotbar<9;hotbar++){ItemStack held=p.getInventory().getItem(hotbar);if(held.isEmpty()||!held.isStackable()||held.getCount()>=held.getMaxStackSize())continue;
  int need=held.getMaxStackSize()-held.getCount(),b=d.findMatching(held);if(b>=0&&!d.protectedSlot(b)){var out=d.withdraw(b,need);held.grow(out.getCount());need-=out.getCount();}
  for(int s=0;s<252&&need>0;s++)if(!d.protectedStorage(s)&&ItemStack.isSameItemSameTags(d.storage(s),held)){var out=d.extractStorage(s,need,false);held.grow(out.getCount());need-=out.getCount();}
 }p.getInventory().setChanged();d.save();}
 public static boolean smelt(Level level,PackData d){
  var input=d.furnace.getStackInSlot(0);var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SimpleContainer(input),level);boolean changed=false;
  if(recipe.isEmpty()){if(d.burn>0){d.burn--;changed=true;}if(d.cook!=0){d.cook=0;changed=true;}if(changed)d.save();return changed;}
  d.cookTotal=Math.max(1,recipe.get().getCookingTime());ItemStack result=recipe.get().getResultItem(level.registryAccess()).copy();ItemStack output=d.furnace.getStackInSlot(2);
  boolean room=output.isEmpty()||ItemStack.isSameItemSameTags(output,result)&&output.getCount()+result.getCount()<=output.getMaxStackSize();
  if(d.burn<=0&&room){ItemStack fuel=d.furnace.getStackInSlot(1);int ticks=ForgeHooks.getBurnTime(fuel,RecipeType.SMELTING);if(ticks>0){ItemStack container=fuel.getCraftingRemainingItem();d.furnace.extractItem(1,1,false);if(d.furnace.getStackInSlot(1).isEmpty())d.furnace.setStackInSlot(1,container);d.burn=ticks;d.burnTotal=ticks;changed=true;}}
  if(d.burn>0){d.burn--;changed=true;if(room&&++d.cook>=recipe.get().getCookingTime()){d.furnace.extractItem(0,1,false);d.storedXp+=recipe.get().getExperience();d.furnace.setStackInSlot(2,output.isEmpty()?result:output.copyWithCount(output.getCount()+result.getCount()));d.cook=0;}}
  else if(d.cook!=0){d.cook=0;changed=true;}
  if(changed)d.save();return changed;
 }
 public static void selectTool(ServerPlayer p){selectTool(p,false);}
 public static void selectTool(ServerPlayer p,boolean manual){selectTool(p,manual,null);}
 public static void selectTool(ServerPlayer p,net.minecraft.core.BlockPos clicked){selectTool(p,false,clicked);}
 private static void selectTool(ServerPlayer p,boolean manual,net.minecraft.core.BlockPos clicked){int pack=PackItem.find(p);if(pack<0)return;PackData d=PackItem.data(p,PackItem.stack(p,pack));
  if(!ToolHand.canSwap(p)||ToolHand.manual(p))return;
  if(!manual&&!autoTools(d))return;
  net.minecraft.core.BlockPos at=clicked;
  if(at==null){HitResult hit=p.pick(p.getBlockReach(),0,false);if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return;at=block.getBlockPos();}
  int best=ToolHand.bestTool(p,d,p.getMainHandItem(),p.level().getBlockState(at));
  if(best>=0)ToolHand.equip(p,d,best);else if(ToolHand.active(p))ToolHand.touch(p);
 }
 private static boolean autoTools(PackData d){return PackConfig.AUTO_TOOLS.get()||d.preset==PackData.MINING;}
 public static void selectWeapon(ServerPlayer p,PackData d){
  if(!ToolHand.canSwap(p)||ToolHand.manual(p))return;
  ItemStack held=p.getMainHandItem();
  if(ToolHand.isModdedItem(held))return;
  // A held sword, spear, lance, bow, or axe is already the weapon. Leave that form alone.
  if(ToolHand.usable(held)&&ToolHand.weaponForm(Requiem.form(held))){ToolHand.touch(p);return;}
  int best=weaponScore(held),selected=-1;
  if(d!=null){
   for(int i=108;i<114;i++){
    ItemStack s=d.items.getStackInSlot(i);
    if(Requiem.form(s)>=0)continue;
    int score=weaponScore(s);
    if(score>best){best=score;selected=i;}
   }
  }
  if(!ToolHand.isForeignWeaponOrTool(p,held)){
   ItemStack eg=ToolHand.findEndgameForAuto(p,d);
   if(!eg.isEmpty()){
    int form=Requiem.form(eg);
    int use=ToolHand.weaponForm(form)?form:ToolHand.bestWeaponForm(eg);
    if(use>=0){
     ItemStack candidate=use==form?eg:Requiem.transformed(eg.copyWithCount(1),use);
     if(weaponScore(candidate)>best){
      int placed=ToolHand.applyEndgameForm(p,d,use);
      if(placed==-1){ToolHand.touch(p);return;}
      if(placed>=108)selected=placed;
     }
    }
   }
  }
  if(selected>=0)ToolHand.equip(p,d,selected);else ToolHand.touch(p);
 }
 @SubscribeEvent public void onCraft(net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent e){
  if(e.getEntity() instanceof ServerPlayer p){
   ItemStack crafted=e.getCrafting();
   if(Requiem.form(crafted)>=0){
    p.getPersistentData().putBoolean("EndgameCrafted",true);
    // The crafting result may already have moved to the cursor or inventory.
    // Copying it into the rack here duplicates shift-crafted results.
    p.displayClientMessage(net.minecraft.network.chat.Component.literal("Endgame crafted: all ten forms are available while you carry it. Place it in the tool rack for automatic tool selection."),false);
   }
  }
 }
 private static boolean usable(ItemStack tool){return !tool.isEmpty()&&(!tool.isDamageableItem()||tool.getDamageValue()<tool.getMaxDamage()-1);}
 public static int weaponScore(ItemStack stack){
  if(!usable(stack))return 0;
  double dmg=1.0,speed=4.0,reach=0.0;
  var map=stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
  for(var mod:map.get(Attributes.ATTACK_DAMAGE)){
   if(mod.getOperation()==AttributeModifier.Operation.ADDITION)dmg+=mod.getAmount();
   else if(mod.getOperation()==AttributeModifier.Operation.MULTIPLY_BASE)dmg+=1.0*mod.getAmount();
   else if(mod.getOperation()==AttributeModifier.Operation.MULTIPLY_TOTAL)dmg*=(1.0+mod.getAmount());
  }
  for(var mod:map.get(Attributes.ATTACK_SPEED)){
   if(mod.getOperation()==AttributeModifier.Operation.ADDITION)speed+=mod.getAmount();
  }
  for(var mod:map.get(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get())){
   if(mod.getOperation()==AttributeModifier.Operation.ADDITION)reach+=mod.getAmount();
  }
  dmg+=EnchantmentHelper.getDamageBonus(stack,net.minecraft.world.entity.MobType.UNDEFINED);
  if(dmg<=1.0&&!(stack.getItem() instanceof SwordItem||stack.getItem() instanceof AxeItem||stack.getItem() instanceof TridentItem||Requiem.form(stack)>=0)){
   return 0;
  }
  return (int)Math.round(dmg*1000.0+reach*100.0+speed*10.0);
 }
}
