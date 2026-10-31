package com.waldron.adventurerspack;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import java.util.UUID;
/** Owns exactly one physical weapon until it returns. No inventory template is recreated. */
public class EndgameProjectile extends ThrowableItemProjectile {
 public boolean returning,creativeCopy,delivered;public int age,returnSlot=-1,homeSlot=-1;public UUID homePack;
 public EndgameProjectile(EntityType<? extends EndgameProjectile> type,Level level){super(type,level);}
 public EndgameProjectile(Level level,Player owner,ItemStack stack){this(AdventurersPack.ENDGAME_PROJECTILE.get(),level);setOwner(owner);setPos(owner.getX(),owner.getEyeY()-0.15,owner.getZ());setItem(stack.copyWithCount(1));}
 protected Item getDefaultItem(){return AdventurersPack.ENDGAME.get();}
 protected float getGravity(){return returning?0:0.025f;}
 protected boolean canHitEntity(Entity e){return !returning&&e!=getOwner()&&super.canHitEntity(e);}
 protected void onHitEntity(EntityHitResult hit){if(level().isClientSide||returning)return;Entity target=hit.getEntity(),owner=getOwner();if(target==owner)return;if(target instanceof Player victim&&owner instanceof Player attacker&&!attacker.canHarmPlayer(victim)){returning=true;return;}float bonus=target instanceof LivingEntity living?EnchantmentHelper.getDamageBonus(getItem(),living.getMobType()):0;boolean damaged=target.hurt(damageSources().trident(this,owner==null?this:owner),54f+bonus);if(damaged&&target instanceof LivingEntity living&&owner instanceof LivingEntity attacker){EnchantmentHelper.doPostHurtEffects(living,attacker);EnchantmentHelper.doPostDamageEffects(attacker,living);}returning=true;}
 protected void onHitBlock(BlockHitResult hit){if(!level().isClientSide)returning=true;}
 public void tick(){if(delivered){discard();return;}super.tick();if(level().isClientSide)return;age++;if(age>=60||getY()<level().getMinBuildHeight()+8)returning=true;
  if(!returning)return;setNoGravity(true);noPhysics=true;Entity owner=getOwner();
  // Keep the actual weapon serialized while an owner is offline; never discard it on a timer.
  if(owner==null){setDeltaMovement(Vec3.ZERO);return;}
  if(!owner.isAlive()||owner.isRemoved()){if(!creativeCopy)spawnAtLocation(getItem().copy());discard();return;}
  if(!(owner instanceof Player p)){if(!creativeCopy)spawnAtLocation(getItem().copy());discard();return;}
  Vec3 delta=p.getEyePosition().subtract(position());if(age>2&&delta.lengthSqr()<2.25){if(returnWeapon(p))discard();else setDeltaMovement(Vec3.ZERO);return;}
  setDeltaMovement(delta.normalize().scale(Math.min(2.2,0.7+age*0.025)));
 }
 public boolean returnWeapon(Player p){if(delivered)return true;if(creativeCopy){delivered=true;return true;}ItemStack actual=getItem().copy();
  if(homePack!=null)for(int i=0;i<=PackItem.CURIOS;i++){ItemStack pack=PackItem.stack(p,i);if(pack.getItem() instanceof PackItem&&pack.hasTag()&&pack.getTag().hasUUID("AdventurersPackId")&&homePack.equals(pack.getTag().getUUID("AdventurersPackId"))){var d=PackItem.data(p,pack);if(homeSlot>=108&&homeSlot<114)actual=d.items.insertItem(homeSlot,actual,false);if(actual.isEmpty()){d.save();delivered=true;return true;}break;}}
  if(returnSlot>=0&&returnSlot<=40&&p.getInventory().getItem(returnSlot).isEmpty()){p.getInventory().setItem(returnSlot,actual);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();delivered=true;return true;}
  for(int i=0;i<36;i++)if(p.getInventory().getItem(i).isEmpty()){p.getInventory().setItem(i,actual);p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();delivered=true;return true;}return false;
 }
 public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putBoolean("Delivered",delivered);n.putBoolean("Returning",returning);n.putBoolean("CreativeCopy",creativeCopy);n.putInt("Age",age);n.putInt("ReturnSlot",returnSlot);n.putInt("HomeSlot",homeSlot);if(homePack!=null)n.putUUID("HomePack",homePack);}
 public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);delivered=n.getBoolean("Delivered");returning=n.getBoolean("Returning");creativeCopy=n.getBoolean("CreativeCopy");age=n.getInt("Age");returnSlot=n.contains("ReturnSlot")?n.getInt("ReturnSlot"):-1;homeSlot=n.getInt("HomeSlot");if(n.hasUUID("HomePack"))homePack=n.getUUID("HomePack");}
 public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
