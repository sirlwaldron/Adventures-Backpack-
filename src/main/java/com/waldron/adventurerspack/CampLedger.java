package com.waldron.adventurerspack;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
public class CampLedger extends SavedData {
 public CompoundTag records=new CompoundTag();
 public static CampLedger get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(CampLedger::load,CampLedger::new,"adventurerspack_camps");}
 public static CampLedger load(CompoundTag n){var d=new CampLedger();d.records=n.getCompound("Camps");return d;}
 public CompoundTag save(CompoundTag n){n.put("Camps",records);return n;}
}
