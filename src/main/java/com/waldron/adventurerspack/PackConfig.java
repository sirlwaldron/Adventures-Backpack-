package com.waldron.adventurerspack;
import net.minecraftforge.common.ForgeConfigSpec;
public class PackConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.BooleanValue AUTO_TOOLS,COMBAT_WEAPONS;
 public static final ForgeConfigSpec.IntValue BASE_CAPACITY,UPGRADED_CAPACITY,MAGNET_RANGE;
 static {var b=new ForgeConfigSpec.Builder();
 AUTO_TOOLS=b.define("automaticToolSelection",true);
 COMBAT_WEAPONS=b.comment("While the Combat preset is active, attacking swaps in the strongest rack weapon.").define("combatPresetSelectsWeapon",true);
 BASE_CAPACITY=b.comment("Items per bulk slot without Deep Pockets.").defineInRange("baseBulkCapacity",4096,64,1000000);
 UPGRADED_CAPACITY=b.defineInRange("upgradedBulkCapacity",65536,64,1000000);
 MAGNET_RANGE=b.defineInRange("magnetRange",5,1,12); SPEC=b.build();}
}
