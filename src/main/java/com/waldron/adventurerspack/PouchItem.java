package com.waldron.adventurerspack;
import net.minecraft.world.item.*;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.Tags;
import net.minecraft.network.chat.Component;
public class PouchItem extends AdventurersPack.UpgradeItem {
 public final int kind;
 public PouchItem(String name,int kind){super(name,false,true);this.kind=kind;}
 public static int kind(ItemStack s){return s.is(AdventurersPack.FEEDING.get())?2:s.getItem() instanceof PouchItem p?p.kind:-1;}
 public static boolean accepts(int kind,ItemStack s){if(!PackData.safe(s)||s.getItem() instanceof AdventurersPack.UpgradeItem||s.is(AdventurersPack.CAMP_KIT.get()))return false;return switch(kind){
  case 0 -> s.is(Tags.Items.SEEDS)||s.is(ItemTags.FLOWERS)||s.is(ItemTags.SAPLINGS)||s.is(Items.WHEAT)||s.is(Items.SUGAR_CANE)||s.is(Items.BAMBOO)||s.is(Items.KELP)||s.is(Items.RED_MUSHROOM)||s.is(Items.BROWN_MUSHROOM)||s.is(Items.SWEET_BERRIES)||s.is(Items.GLOW_BERRIES);
  case 1 -> s.is(Tags.Items.GEMS)||s.is(Tags.Items.RAW_MATERIALS)||s.is(Tags.Items.NUGGETS)||s.is(Tags.Items.INGOTS);
  case 2 -> s.isEdible();
  case 3 -> s.getItem() instanceof MapItem||s.is(Items.MAP)||s.getItem() instanceof CompassItem||s.is(Items.RECOVERY_COMPASS)||s.is(Items.PAPER)||s.is(Items.BOOK)||s.is(Items.WRITABLE_BOOK)||s.is(Items.WRITTEN_BOOK);
  default -> false;
 };}
 public void appendHoverText(ItemStack s,net.minecraft.world.level.Level l,java.util.List<Component> lines,TooltipFlag f){lines.add(Component.translatable("upgrade.adventurerspack.kind.section"));lines.add(Component.literal("18 filtered slots. Install in the Pouches panel."));lines.add(Component.literal("Contents stay inside the pouch when removed."));}
}
