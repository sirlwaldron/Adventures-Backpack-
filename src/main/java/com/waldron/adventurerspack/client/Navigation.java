package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.PackData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Locale;

/** Built-in vanilla compass and cross-dimensional warp anchor hub. */
public final class Navigation {
 private static final ItemStack COMPASS = new ItemStack(Items.COMPASS);
 public static void draw(GuiGraphics g,Font font,Player player,PackData data,int x,int y){
  g.drawString(font,"COMPASS & WARP",x,y,0xffe8ddc8,true);
  g.pose().pushPose();g.pose().translate(x+132,y-2,1);g.pose().scale(1.35f,1.35f,1);
  g.renderItem(COMPASS,0,0);g.pose().popPose();
  g.drawString(font,"Facing: "+player.getDirection().getName().toUpperCase(Locale.ROOT),x,y+12,0xff70d9c0,true);
  if(!data.warpUnlocked){
   g.drawString(font,"Dimensional Wayfinder",x,y+30,0xffffd770,true);
   g.drawString(font,"10 cross-dimension warps",x,y+42,0xffdfd4c2,true);
   g.drawString(font,"Cost: 2 Nether Stars,",x,y+56,0xffdfd4c2,true);
   g.drawString(font,"4 Netherite, 8 Ender Eyes",x,y+68,0xffdfd4c2,true);
   return;
  }
  int idx=Math.max(0,Math.min(PackData.WARP_COUNT-1,data.warpIndex));
  if(data.warpSet[idx]){
   String dim=PackData.formatDim(data.warpDims[idx]);
   String coords=data.warpX[idx]+", "+data.warpY[idx]+", "+data.warpZ[idx];
   g.drawString(font,font.plainSubstrByWidth(dim+": "+coords,156),x,y+86,0xff56fad8,true);
  }else{
   g.drawString(font,"Slot "+(idx+1)+": Unbound",x,y+86,0xffdfd4c2,true);
  }
 }
}
