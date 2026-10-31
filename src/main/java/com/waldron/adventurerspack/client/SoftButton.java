package com.waldron.adventurerspack.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Pack button drawn as a translucent plate instead of the opaque vanilla widget. */
public class SoftButton extends Button {
 public boolean lit; public String packTooltip="";
 public SoftButton(int x,int y,int w,int h,Component text,OnPress press){super(x,y,w,h,text,press,DEFAULT_NARRATION);}
 @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){if(!visible)return;
  int bg=!active?0x44141012:isHoveredOrFocused()?0x99503830:0x66181014;
  g.fill(getX(),getY(),getX()+width,getY()+height,bg);
  g.renderOutline(getX(),getY(),width,height,lit?0xCCE7D1A4:isHoveredOrFocused()?0xCCB8A078:0x998A7050);
  int color=active?0xFFE7D1A4:0xFF7A6E5C;
  g.drawCenteredString(Minecraft.getInstance().font,getMessage(),getX()+width/2,getY()+(height-8)/2,color);
 }
}
