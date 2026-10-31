package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=AdventurersPack.ID,value=Dist.CLIENT)
public class ClientEvents {
 private static boolean wasAttacking;
 /** Set only for the vanilla-inventory button, so E keeps opening the pack. */
 private static boolean allowVanilla;
 private static boolean redirectedInventory;
 public static void openVanilla(Minecraft mc){Network.CHANNEL.sendToServer(new Network.Action(10));}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){var mc=Minecraft.getInstance();if(mc.player==null){ClientMirror.toolActive=false;ClientMirror.toolManual=false;ClientMirror.equipment=null;ClientMirror.vanillaReady=false;allowVanilla=false;redirectedInventory=false;ToolWheel.reset();return;}
  if(e.phase==TickEvent.Phase.START){redirectedInventory=false;if(ClientMirror.equipment!=null){PackEquipment.get(mc.player).items.setStackInSlot(0,ClientMirror.equipment);ClientMirror.equipment=null;}if(!PackClientConfig.VANILLA_INVENTORY.get()&&mc.screen==null&&!mc.player.isSpectator())while(mc.options.keyInventory.consumeClick()){redirectedInventory=true;Network.CHANNEL.sendToServer(new Network.Action(0));}return;}
  if(ClientMirror.vanillaReady){ClientMirror.vanillaReady=false;allowVanilla=true;mc.setScreen(new InventoryScreen(mc.player));}
  boolean mod=net.minecraft.client.gui.screens.Screen.hasShiftDown()||net.minecraft.client.gui.screens.Screen.hasControlDown()||net.minecraft.client.gui.screens.Screen.hasAltDown()||mc.player.isShiftKeyDown();
  while(ClientSetup.OPEN.consumeClick())if(mc.screen==null&&!mod)Network.CHANNEL.sendToServer(new Network.Action(0));
  ToolWheel.tick(mc);
  while(ClientSetup.CYCLE.consumeClick())if(mc.screen==null&&!mod)Network.CHANNEL.sendToServer(new Network.Action(2));
  while(ClientSetup.PRESET.consumeClick())if(mc.screen==null&&!mod)Network.CHANNEL.sendToServer(new Network.Action(4));
  while(ClientSetup.OFFHAND.consumeClick())if(mc.screen==null&&!mod)Network.CHANNEL.sendToServer(new Network.Action(3));
  while(ClientSetup.VIAL.consumeClick())if(mc.screen==null&&!mod)Network.CHANNEL.sendToServer(new Network.Action(7));
  boolean attacking=mc.screen==null&&mc.options.keyAttack.isDown();
  if(ClientMirror.toolActive){if(attacking&&mc.player.tickCount%4==0)Network.CHANNEL.sendToServer(new Network.Action(9,1));else if(!attacking&&wasAttacking||mc.screen!=null&&!(mc.screen instanceof RequiemScreen))Network.CHANNEL.sendToServer(new Network.Action(9,0));}
  wasAttacking=attacking;QuickWheel.tick(mc);
 }
 @SubscribeEvent public static void glide(PlayerInteractEvent.RightClickEmpty e){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.screen==null&&mc.player.isFallFlying())Network.CHANNEL.sendToServer(new Network.Action(8));}
 /** Return to the pack and make it the default inventory again. */
 @SubscribeEvent public static void inventoryButton(ScreenEvent.Init.Post e){
  if(!(e.getScreen() instanceof InventoryScreen screen))return;
  int x=Math.max(0,Math.min(screen.width-100,screen.getGuiLeft()+76));
  int y=Math.max(0,screen.getGuiTop()-22);
  var button=net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Pack inventory"),b->{
   PackClientConfig.setVanilla(false);
   Network.CHANNEL.sendToServer(new Network.Action(0));
  }).bounds(x,y,100,20).build();
  button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.literal("Switch back to the pack screen and use it for the inventory key.")));
  e.addListener(button);
 }
 /** Redirect the inventory only when the pack is the selected default. */
 @SubscribeEvent public static void replaceInventory(ScreenEvent.Opening e){
  // Recipe viewers temporarily replace the screen and return to the same menu.
  // Closing here leaves that returning screen with stale slots on both sides.
  if(!(e.getNewScreen() instanceof InventoryScreen))return;
  if(allowVanilla){allowVanilla=false;return;}
  if(PackClientConfig.VANILLA_INVENTORY.get())return;
  e.setCanceled(true);
  var mc=Minecraft.getInstance();
  if(!redirectedInventory&&mc.player!=null&&!mc.player.isSpectator()&&!(mc.screen instanceof PackScreen)){redirectedInventory=true;Network.CHANNEL.sendToServer(new Network.Action(0));}
 }
 /** The pack screen already draws the hotbar. Leave the gameplay HUD off so it is not drawn a second time. */
 @SubscribeEvent public static void screenKey(ScreenEvent.KeyPressed.Pre e){
  var mc=Minecraft.getInstance();
  if(mc.player==null||mc.player.isSpectator())return;
  if(e.getScreen().getFocused() instanceof net.minecraft.client.gui.components.EditBox eb&&eb.canConsumeInput())return;
  for(var child:e.getScreen().children())if(child instanceof net.minecraft.client.gui.components.EditBox eb&&eb.canConsumeInput())return;
  boolean mod=net.minecraft.client.gui.screens.Screen.hasShiftDown()||net.minecraft.client.gui.screens.Screen.hasControlDown()||net.minecraft.client.gui.screens.Screen.hasAltDown()||mc.player.isShiftKeyDown();
  if(!mod&&ClientSetup.OPEN.matches(e.getKeyCode(),e.getScanCode())){
   while(ClientSetup.OPEN.consumeClick()){}
   if(e.getScreen() instanceof PackScreen)e.getScreen().onClose();
   else Network.CHANNEL.sendToServer(new Network.Action(0));
   e.setCanceled(true);
  }
 }
 @SubscribeEvent public static void hideGameplayHud(RenderGuiOverlayEvent.Pre e){if(Minecraft.getInstance().screen instanceof PackScreen)e.setCanceled(true);}
}

