package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=AdventurersPack.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public class PackWornRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
 public PackWornRenderer(PlayerRenderer renderer){super(renderer);}
 @SubscribeEvent public static void addLayers(EntityRenderersEvent.AddLayers event){for(String skin:event.getSkins()){PlayerRenderer renderer=event.getSkin(skin);if(renderer!=null)renderer.addLayer(new PackWornRenderer(renderer));}}
 private static final ResourceLocation TEXTURE=new ResourceLocation(AdventurersPack.ID,"textures/entity/worn_pack.png");
 private static final float[][] TINT={{1,1,1},{.83f,1,.72f},{.72f,.87f,1},{.84f,.67f,1},{.53f,.57f,.62f}};
 private static ModelPart root;
 @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,float swing,float amount,float partial,float age,float yaw,float pitch){
  if(player.isInvisible()||player.isSpectator())return;
  ItemStack stack=PackEquipment.stack(player);if(stack.isEmpty())stack=CuriosPacks.visualStack(player);if(stack.isEmpty())return;
  PackData data=new PackData(stack);
  pose.pushPose();getParentModel().body.translateAndRotate(pose);
  pose.translate(0.0F,0.06F,player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty()?0.17F:0.23F);
  float scale=.78F+data.tier*.015F;pose.scale(scale,scale,scale);
  renderPack(data,pose,buffers,light);
  pose.popPose();
 }
 public static void renderDock(DockEntity dock,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  if(dock==null||dock.pack.isEmpty())return;
  PackData data=dock.data();
  float scale=.78F+data.tier*.015F;
  int packedLight=light;
  if(dock.getLevel()!=null){
   int above=net.minecraft.client.renderer.LevelRenderer.getLightColor(dock.getLevel(),dock.getBlockPos().above());
   int sky=Math.max((light>>20)&15,(above>>20)&15);
   int blk=Math.max((light>>4)&15,(above>>4)&15);
   packedLight=(sky<<20)|(blk<<4);
  }
  pose.pushPose();
  pose.translate(0.5F,(13.0F+13.42F*scale)/16.0F,11.4F/16.0F);
  pose.mulPose(Axis.XP.rotationDegrees(180.0F));
  pose.scale(scale,scale,scale);
  renderPack(data,pose,buffers,packedLight);
  pose.popPose();
 }
 public static void renderPack(PackData data,PoseStack pose,MultiBufferSource buffers,int light){
  if(root==null)root=ExpeditionPackModel.bake();
  VertexConsumer buffer=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
  for(String name:ExpeditionPackModel.PARTS){
   String group=name.substring(0,name.indexOf('_'));
   boolean show=switch(group){
    case "bedroll"->!data.camp.getStackInSlot(0).isEmpty();
    case "strap"->hasTool(data);
    case "vent"->data.has(AdventurersPack.FURNACE.get());
    case "pouch"->data.has(AdventurersPack.FEEDING.get());
    case "rune"->data.tier>=3;
    case "belt"->data.has(AdventurersPack.BUILDER.get());
    case "quiver"->data.has(AdventurersPack.QUIVER.get());
    case "flask"->data.has(AdventurersPack.PHILTER.get())||data.has(AdventurersPack.ALEMBIC.get());
    default->true;
   };
   if(!show)continue;
   float[] tint=name.contains("_canvas_")?TINT[data.tier]:new float[]{1,1,1};
   root.getChild(name).render(pose,buffer,light,OverlayTexture.NO_OVERLAY,tint[0],tint[1],tint[2],1);
  }
 }
 private static boolean hasTool(PackData data){for(int i=108;i<114;i++)if(!data.items.getStackInSlot(i).isEmpty())return true;return false;}
}
