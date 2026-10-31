package com.waldron.adventurerspack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Supplier;
public class Network {
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(AdventurersPack.ID,"main"),()->"14","14"::equals,"14"::equals);
 public record Action(int kind,int arg){
  public Action(int kind){this(kind,0);}
  public static void encode(Action p,FriendlyByteBuf b){b.writeVarInt(p.kind);b.writeVarInt(p.arg);}
  public static Action decode(FriendlyByteBuf b){return new Action(b.readVarInt(),b.readVarInt());}
  public static void handle(Action msg,Supplier<NetworkEvent.Context> supplier){var ctx=supplier.get();ctx.enqueueWork(()->{var p=ctx.getSender();if(p==null||!p.isAlive()||p.isSpectator())return;
   if(msg.kind==10){p.closeContainer();CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new VanillaReady());return;}
   if(msg.kind==9){if(ToolHand.manual(p))return;if(msg.arg==0)ToolHand.restore(p);else if(msg.arg==1&&p.containerMenu==p.inventoryMenu&&ToolHand.active(p)){PackEvents.selectTool(p,false);ToolHand.touch(p);}return;}
   if(msg.kind==0){PackItem.open(p,PackItem.wornSlot(p));return;}
   if(p.containerMenu!=p.inventoryMenu)return;
   if(msg.kind==0){PackItem.open(p,PackItem.wornSlot(p));return;}
   if(msg.kind==11){
    int packSlot=PackItem.find(p);
    PackData d=packSlot<0?null:PackItem.data(p,PackItem.stack(p,packSlot));
    ToolHand.draw(p,d,msg.arg);
    return;
   }
   if(msg.kind==1){
    if(p.isShiftKeyDown())return;
    if(ToolHand.active(p)){ToolHand.restore(p);return;}
    int packSlot=PackItem.find(p);if(packSlot<0)return;
    PackData drawn=PackItem.data(p,PackItem.stack(p,packSlot));
    for(int i=0;i<6;i++)if(ToolHand.usable(drawn.items.getStackInSlot(108+i))){ToolHand.equip(p,drawn,108+i,false);break;}
    return;
   }
   int slot=PackItem.find(p);if(slot<0)return;var stack=PackItem.stack(p,slot);var d=PackItem.data(p,stack);
   if(msg.kind==0)PackItem.open(p,slot);
   else if(msg.kind==2){for(int n=1;n<=18;n++){int next=(d.selected+n)%18;if(d.counts[next]>0&&d.bulk[next].getItem() instanceof net.minecraft.world.item.BlockItem){d.selected=next;break;}}d.save();p.displayClientMessage(net.minecraft.network.chat.Component.literal("Building: ").append(d.bulk[d.selected].getHoverName()),true);}
   else if(msg.kind==3)d.cycleOffhand(p);
   else if(msg.kind==4){d.applyPreset(d.preset+1);p.displayClientMessage(net.minecraft.network.chat.Component.literal("Preset: "+PackData.PRESET_NAMES[d.preset]),true);}
   else if(msg.kind==6){if(msg.arg>=0&&msg.arg<4){d.tab=msg.arg;d.save();p.displayClientMessage(net.minecraft.network.chat.Component.literal("Tab: "+d.tabNames[d.tab]),true);}else if(msg.arg>=4&&msg.arg<8){int[] map={1,2,3,0};d.applyPreset(map[msg.arg-4]);p.displayClientMessage(net.minecraft.network.chat.Component.literal("Preset: "+PackData.PRESET_NAMES[d.preset]),true);}}
   else if(msg.kind==7)d.useVial(p);
   else if(msg.kind==8)PackEvents.launchRocket(p,d);
  });ctx.setPacketHandled(true);}
 }
 public record Command(int kind,int arg,String text){
  public static void encode(Command p,FriendlyByteBuf b){b.writeVarInt(p.kind);b.writeVarInt(p.arg);b.writeUtf(p.text==null?"":p.text,256);}
  public static Command decode(FriendlyByteBuf b){return new Command(b.readVarInt(),b.readVarInt(),b.readUtf(256));}
  public static void handle(Command msg,Supplier<NetworkEvent.Context> supplier){var ctx=supplier.get();ctx.enqueueWork(()->{ServerPlayer p=ctx.getSender();if(p==null||!(p.containerMenu instanceof PackMenu menu)||!menu.stillValid(p))return;
   if(msg.kind==1&&msg.arg>=0&&msg.arg<4){menu.data.tabNames[msg.arg]=PackData.cleanName(msg.text,PackData.DEFAULT_NAMES[msg.arg]);menu.data.save();}
   else if(msg.kind==4&&menu.data.warpUnlocked&&msg.arg>=0&&msg.arg<PackData.WARP_COUNT){menu.data.warpNames[msg.arg]=PackData.cleanName(msg.text,"Warp "+(msg.arg+1));menu.data.save();}
   else if(msg.kind==2){ResourceLocation id=ResourceLocation.tryParse(msg.text==null?"":msg.text);var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);if(item!=null&&item!=net.minecraft.world.item.Items.AIR)menu.data.track(item);}
   else if(msg.kind==3&&Integer.toString(menu.containerId).equals(msg.text)){menu.clickMenuButton(p,msg.arg);}
   else return;menu.broadcastChanges();
  });ctx.setPacketHandled(true);}
 }
 public record Display(int containerId,CompoundTag tag){
  public static void encode(Display p,FriendlyByteBuf b){b.writeVarInt(p.containerId);b.writeNbt(p.tag);}
  public static Display decode(FriendlyByteBuf b){return new Display(b.readVarInt(),b.readNbt());}
  public static void handle(Display msg,Supplier<NetworkEvent.Context> supplier){var ctx=supplier.get();ctx.enqueueWork(()->{if(msg.tag!=null){ClientMirror.containerId=msg.containerId;ClientMirror.tag=msg.tag;}});ctx.setPacketHandled(true);}
 }
 public record HandState(boolean active,int source,int slot,net.minecraft.world.item.ItemStack original,net.minecraft.world.item.ItemStack equipped,boolean manual){
  static void encode(HandState p,FriendlyByteBuf b){b.writeBoolean(p.active);b.writeInt(p.source);b.writeInt(p.slot);b.writeItem(p.original);b.writeItem(p.equipped);b.writeBoolean(p.manual);}
  static HandState decode(FriendlyByteBuf b){return new HandState(b.readBoolean(),b.readInt(),b.readInt(),b.readItem(),b.readItem(),b.readBoolean());}
  static void handle(HandState p,Supplier<NetworkEvent.Context> supplier){var ctx=supplier.get();ctx.enqueueWork(()->{ClientMirror.toolActive=p.active;ClientMirror.toolSource=p.source;ClientMirror.toolSlot=p.slot;ClientMirror.originalHand=p.original;ClientMirror.toolManual=p.active&&p.manual;
   com.waldron.adventurerspack.client.ClientToolEquipment.update(p.active,p.slot,p.equipped);});ctx.setPacketHandled(true);}
 }
 public static void sendToolHand(ServerPlayer p,boolean active,int source,int slot,net.minecraft.world.item.ItemStack original,net.minecraft.world.item.ItemStack equipped,boolean manual){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new HandState(active,source,slot,original,equipped,manual));}
 public static void sendDisplay(ServerPlayer player,int containerId,CompoundTag tag){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Display(containerId,tag));}
 public record VanillaReady(){
  static void encode(VanillaReady p,FriendlyByteBuf b){}
  static VanillaReady decode(FriendlyByteBuf b){return new VanillaReady();}
  static void handle(VanillaReady p,Supplier<NetworkEvent.Context> s){s.get().enqueueWork(()->ClientMirror.vanillaReady=true);s.get().setPacketHandled(true);}
 }
 public record Equipment(int playerId,net.minecraft.world.item.ItemStack stack){
  static void encode(Equipment p,FriendlyByteBuf b){b.writeVarInt(p.playerId);b.writeItem(p.stack);}
  static Equipment decode(FriendlyByteBuf b){return new Equipment(b.readVarInt(),b.readItem());}
  static void handle(Equipment p,Supplier<NetworkEvent.Context> s){
   s.get().enqueueWork(()->{
    net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getInstance();
    if(mc.level!=null){
     net.minecraft.world.entity.Entity e=mc.level.getEntity(p.playerId);
     if(e instanceof net.minecraft.world.entity.player.Player player){
      PackEquipment.get(player).items.setStackInSlot(0,p.stack);
     }
    }
    if(mc.player!=null&&mc.player.getId()==p.playerId){
     ClientMirror.equipment=p.stack;
    }
   });
   s.get().setPacketHandled(true);
  }
 }
 public record WeaponForm(int slot,int expected,int target){
  static void encode(WeaponForm p,FriendlyByteBuf b){b.writeVarInt(p.slot);b.writeVarInt(p.expected);b.writeVarInt(p.target);}
  static WeaponForm decode(FriendlyByteBuf b){return new WeaponForm(b.readVarInt(),b.readVarInt(),b.readVarInt());}
  static void handle(WeaponForm p,Supplier<NetworkEvent.Context> s){var ctx=s.get();ctx.enqueueWork(()->{var player=ctx.getSender();if(player!=null)Requiem.transform(player,p.slot,p.expected,p.target);});ctx.setPacketHandled(true);}
 }
 public static void init(){
  CHANNEL.messageBuilder(WeaponForm.class,6,NetworkDirection.PLAY_TO_SERVER).encoder(WeaponForm::encode).decoder(WeaponForm::decode).consumerMainThread(WeaponForm::handle).add();
  CHANNEL.messageBuilder(VanillaReady.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder(VanillaReady::encode).decoder(VanillaReady::decode).consumerMainThread(VanillaReady::handle).add();
  CHANNEL.messageBuilder(Equipment.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder(Equipment::encode).decoder(Equipment::decode).consumerMainThread(Equipment::handle).add();
  CHANNEL.messageBuilder(HandState.class,3,NetworkDirection.PLAY_TO_CLIENT).encoder(HandState::encode).decoder(HandState::decode).consumerMainThread(HandState::handle).add();
  CHANNEL.messageBuilder(Action.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Action::encode).decoder(Action::decode).consumerMainThread(Action::handle).add();
  CHANNEL.messageBuilder(Command.class,1,NetworkDirection.PLAY_TO_SERVER).encoder(Command::encode).decoder(Command::decode).consumerMainThread(Command::handle).add();
  CHANNEL.messageBuilder(Display.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(Display::encode).decoder(Display::decode).consumerMainThread(Display::handle).add();
 }
}


