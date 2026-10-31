package com.waldron.adventurerspack;

import net.minecraftforge.common.ForgeConfigSpec;

/** Local inventory preference, saved across worlds and restarts. */
public final class PackClientConfig {
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.BooleanValue VANILLA_INVENTORY;
 public static final ForgeConfigSpec.DoubleValue UI_SCALE_BOOST;

 static {
  var builder=new ForgeConfigSpec.Builder();
  VANILLA_INVENTORY=builder.comment("Use the vanilla inventory for the inventory key instead of the pack screen.")
    .define("vanillaInventory",false);
  UI_SCALE_BOOST=builder.comment("Backpack UI scale multiplier (default 1.20 = 20% wider). Range: 0.80 to 2.00.")
    .defineInRange("uiScaleBoost",1.00,0.50,1.50);
  SPEC=builder.build();
 }

 private PackClientConfig() {}

 public static void setVanilla(boolean enabled){
  VANILLA_INVENTORY.set(enabled);
  SPEC.save();
 }

 public static double getScaleBoost(){
  try{
   return UI_SCALE_BOOST!=null?UI_SCALE_BOOST.get():1.00;
  }catch(Throwable t){
   return 1.00;
  }
 }

 public static void setScaleBoost(double boost){
  try{
   if(UI_SCALE_BOOST!=null){
    UI_SCALE_BOOST.set(boost);
    SPEC.save();
   }
  }catch(Throwable ignored){}
 }
}
