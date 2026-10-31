package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.EndgameProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.util.Mth;
public class EndgameRenderer extends EntityRenderer<EndgameProjectile> {
 private final net.minecraft.client.renderer.entity.ItemRenderer items;
 public EndgameRenderer(EntityRendererProvider.Context c){super(c);items=c.getItemRenderer();}
 public void render(EndgameProjectile e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,e.yRotO,e.getYRot())-90));pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,e.xRotO,e.getXRot())-90));pose.scale(.85f,.85f,.85f);items.renderStatic(e.getItem(),ItemDisplayContext.NONE,light,OverlayTexture.NO_OVERLAY,pose,buffers,e.level(),e.getId());pose.popPose();super.render(e,yaw,partial,pose,buffers,light);}
 public ResourceLocation getTextureLocation(EndgameProjectile e){return TextureAtlas.LOCATION_BLOCKS;}
}
