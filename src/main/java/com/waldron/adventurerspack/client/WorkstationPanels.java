package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
final class WorkstationPanels {
 private static void bar(GuiGraphics g,int x,int y,int w,int h,float value,int color){g.fill(x,y,x+w,y+h,0xff373737);g.fill(x+1,y+1,x+1+Math.round((w-2)*Math.max(0,Math.min(1,value))),y+h-1,color);}
 private static void vertical(GuiGraphics g,int x,int y,int w,int h,float value,int color){g.fill(x,y,x+w,y+h,0xff373737);int filled=Math.round((h-2)*Math.max(0,Math.min(1,value)));g.fill(x+1,y+h-1-filled,x+w-1,y+h-1,color);}
 static void draw(GuiGraphics g,Font font,PackMenu menu,int station){
  PackData d=menu.data;
  if(station==2||station==4||station==5){
   g.fill(386,172,540,257,0xffc6c6c6);g.renderOutline(385,171,156,87,0xfff0f0f0);
   for(var slot:menu.slots)if(slot.isActive()&&slot.x>=380){g.fill(slot.x-1,slot.y-1,slot.x+17,slot.y+17,0xff373737);g.fill(slot.x,slot.y,slot.x+17,slot.y+17,0xffffffff);g.fill(slot.x,slot.y,slot.x+16,slot.y+16,0xff8b8b8b);}
  }
  if(station==2){
   float progress=(float)d.cook/Math.max(1,d.cookTotal);
   bar(g,459,190,30,12,progress,0xfff3efcf);g.drawString(font,">",484,191,0xffe8e8e8,false);
   vertical(g,424,210,12,18,(float)d.burn/Math.max(1,d.burnTotal),0xffffa535);
   g.drawString(font,Math.round(progress*100)+"%",460,211,0xff373737,false);
  }else if(station==5){
   g.fill(461,198,464,230,0xff656565);g.fill(431,227,495,230,0xff656565);
   for(int x:new int[]{431,461,491})g.fill(x,227,x+3,237,0xff656565);
   bar(g,395,207,36,6,d.brewFuel/20f,0xffffaa25);
   float progress=d.brewTime>0?(400-d.brewTime)/400f:0;
   vertical(g,507,183,10,50,progress,0xffae66d4);

   for(int i=0;i<3;i++)if(d.brew.getStackInSlot(i).isEmpty())g.renderFakeItem(new ItemStack(Items.GLASS_BOTTLE),424+i*30,238);
  }else if(station==4){
   for(int i=0;i<3;i++){
    int x=410+i*44;g.renderOutline(x-4,200,24,24,i==d.vialIndex?0xffa45ce0:0xff777777);
    if(d.vials.getStackInSlot(i).isEmpty())g.renderFakeItem(new ItemStack(Items.GLASS_BOTTLE),x,204);
   }
   ItemStack selected=d.vials.getStackInSlot(d.vialIndex);
   String name=selected.isEmpty()?"Empty vial":selected.getHoverName().getString();
   g.drawString(font,font.plainSubstrByWidth(name,150),388,234,0xff373737,false);
   var effects=net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(selected);
   String detail=effects.isEmpty()?"Store a potion to begin":effects.get(0).getEffect().getDisplayName().getString()+" "+(effects.get(0).getAmplifier()+1);
   g.drawString(font,font.plainSubstrByWidth(detail,150),388,246,0xff494949,false);
  }else if(station==11){
   gauge(g,392,176,d.tank,false);
   g.drawString(font,Math.round(100f*d.tank.getFluidAmount()/PackData.FLUID_CAPACITY)+"%",424,198,0xFFFFF4DE,false);
   gauge(g,476,176,d.tankTwo,false);
   g.drawString(font,Math.round(100f*d.tankTwo.getFluidAmount()/PackData.FLUID_CAPACITY)+"%",508,198,0xFFFFF4DE,false);
   for(int i=0;i<2;i++){
    int x=384+i*84;
    g.drawString(font,"Tank "+(i+1),x+10,232,0xFFFFF4DE,false);
    g.drawString(font,"Input",x,252,0xFFFFF4DE,false);
    g.drawString(font,"Output",x+38,252,0xFFFFF4DE,false);
    for(int direction=0;direction<2;direction++){
     int sx=x+6+direction*40;
     g.fill(sx-1,265,sx+17,283,0xff373737);
     g.fill(sx,266,sx+16,282,0xff8b8b8b);
     if(d.fluidSlot.getStackInSlot(i*2+direction).isEmpty())g.renderFakeItem(new ItemStack(Items.BUCKET),sx,266);
    }
   }
  }else if(station==12)bar(g,386,221,154,7,d.xpBank/(float)PackData.XP_CAPACITY,0xff7fe146);
  else if(station==13){
   bar(g,392,188,144,18,d.energy/(float)PackData.ENERGY_CAPACITY,0xff3ddc97);
   g.renderOutline(391,187,146,20,0xffc3bda8);
   g.drawString(font,Math.round(100f*d.energy/PackData.ENERGY_CAPACITY)+"%",454,211,0xFFFFF4DE,false);
   g.drawString(font,"Drain",408,227,0xFFFFF4DE,false);
   g.drawString(font,"Charge",486,227,0xFFFFF4DE,false);
   g.drawString(font,">  >",449,245,0xFFFFD770,false);
  }
 }
 private static void gauge(GuiGraphics g,int x,int y,net.minecraftforge.fluids.capability.templates.FluidTank tank,boolean selected){
  int h=52;
  g.fill(x,y,x+28,y+h,0xff202b30);
  int fill=Math.round((h-4)*tank.getFluidAmount()/(float)PackData.FLUID_CAPACITY);
  if(!tank.isEmpty()&&fill>0)stillFluid(g,tank.getFluid(),x+2,y+h-2,24,fill);
  g.renderOutline(x,y,28,h,selected?0xffffd770:0xffc3bda8);
  for(int mark=y+8;mark<y+h-4;mark+=10)g.fill(x+2,mark,x+8,mark+1,0xffdde5e7);
 }
 private static void stillFluid(GuiGraphics g,net.minecraftforge.fluids.FluidStack stack,int x,int bottom,int w,int h){
  var ext=net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(stack.getFluid());
  var still=ext.getStillTexture(stack);if(still==null)still=ext.getStillTexture();if(still==null)return;
  var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(still);
  int tint=ext.getTintColor(stack);if(tint==0)tint=0xFFFFFFFF;else if((tint>>>24)==0)tint|=0xFF000000;
  com.mojang.blaze3d.systems.RenderSystem.enableBlend();
  com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((tint>>16)&255)/255f,((tint>>8)&255)/255f,(tint&255)/255f,((tint>>>24)&255)/255f);
  int top=bottom-h;
  for(int yy=0;yy<h;){int dh=Math.min(16,h-yy);for(int xx=0;xx<w;){int dw=Math.min(16,w-xx);g.blit(x+xx,top+yy,0,dw,dh,sprite);xx+=dw;}yy+=dh;}
  com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
 }
}

