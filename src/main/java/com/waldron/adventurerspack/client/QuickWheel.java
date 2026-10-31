package com.waldron.adventurerspack.client;
import com.waldron.adventurerspack.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Radial screen for tabs and activity presets. Hold the G key, release over a wedge. */
public class QuickWheel extends Screen {
 private int selection = -1;
 private boolean cancelled = false;

 public QuickWheel() {
  super(Component.literal("Quick Wheel"));
 }

 public static boolean isKeyDown(KeyMapping mapping) {
  if (mapping == null) return false;
  long window = Minecraft.getInstance().getWindow().getWindow();
  InputConstants.Key key = mapping.getKey();
  if (key.getType() == InputConstants.Type.KEYSYM) {
   return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
  }
  if (key.getType() == InputConstants.Type.MOUSE) {
   return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
  }
  return false;
 }

 public static void reset() {}

 public static void tick(Minecraft mc) {
  if (mc.player == null) return;
  boolean mod = Screen.hasShiftDown() || Screen.hasControlDown() || Screen.hasAltDown() || mc.player.isShiftKeyDown();
  if (mc.screen == null && !mod && PackItem.find(mc.player) >= 0) {
   if (ClientSetup.WHEEL.isDown() || ClientSetup.WHEEL.consumeClick()) {
    mc.setScreen(new QuickWheel());
   }
  } else {
   while (ClientSetup.WHEEL.consumeClick()) {}
  }
 }

 @Override
 public boolean isPauseScreen() {
  return false;
 }

 @Override
 public void onClose() {
  cancelled = true;
  super.onClose();
 }

 @Override
 public boolean mouseClicked(double x, double y, int b) {
  if (b == 1) { // Right click cancels
   cancelled = true;
   onClose();
   return true;
  }
  return true;
 }

 @Override
 public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
  if (ClientSetup.WHEEL.matches(keyCode, scanCode)) {
   finishAndClose();
   return true;
  }
  return super.keyReleased(keyCode, scanCode, modifiers);
 }

 @Override
 public boolean mouseReleased(double mx, double my, int button) {
  if (ClientSetup.WHEEL.matchesMouse(button)) {
   finishAndClose();
   return true;
  }
  return super.mouseReleased(mx, my, button);
 }

 private void finishAndClose() {
  if (!cancelled && selection >= 0) {
   Network.CHANNEL.sendToServer(new Network.Action(6, selection));
  }
  if (minecraft != null) {
   minecraft.setScreen(null);
  }
 }

 @Override
 public void render(GuiGraphics g, int mx, int my, float pt) {
  if (!isKeyDown(ClientSetup.WHEEL)) {
   finishAndClose();
   return;
  }
  int cx = width / 2, cy = height / 2;
  double dx = mx - cx, dy = my - cy;
  double dist = Math.sqrt(dx * dx + dy * dy);
  if (dist < 34 || dist > 102) {
   selection = -1;
  } else {
   double ang = Math.atan2(dx, -dy);
   if (ang < 0) ang += Math.PI * 2;
   // Wedge 0 is centered on straight up, so aiming at the top is stable.
   selection = Math.floorMod((int) Math.floor((ang + Math.PI / 8.0) / (Math.PI / 4.0)), 8);
  }

  if (minecraft == null || minecraft.player == null) return;
  int slot = PackItem.find(minecraft.player);
  if (slot < 0) return;
  ItemStack stack = PackItem.stack(minecraft.player, slot);
  PackData data = new PackData(stack);
  String[] labels = {data.tabNames[0], data.tabNames[1], data.tabNames[2], data.tabNames[3], "Mining", "Building", "Combat", "Travel"};

  disc(g, cx, cy, 96, 0x66140F0C);
  disc(g, cx, cy, 86, 0x99181412);
  for (int i = 0; i < 8; i++) {
   double ang = i * (Math.PI / 4.0);
   int x = cx + (int) Math.round(Math.sin(ang) * 70);
   int y = cy + (int) Math.round(-Math.cos(ang) * 70);
   boolean preset = i >= 4;
   String shown = fit(labels[i], 40);
   int hw = Math.max(18, font.width(shown) / 2 + 5);
   int color = i == selection ? 0xFFFFF4DE : preset ? 0xFF8FD9CC : 0xFFE7D1A4;
   g.fill(x - hw, y - 6, x + hw, y + 12, i == selection ? 0xEE3A2A1C : 0xCC1A1612);
   g.renderOutline(x - hw, y - 6, hw * 2, 18, i == selection ? 0xFFE7D1A4 : 0x66594C37);
   g.drawCenteredString(font, shown, x, y, color);
  }
  g.drawCenteredString(font, "Compartments", cx, cy - 10, 0xFFE7D1A4);
  g.drawCenteredString(font, "Presets", cx, cy + 2, 0xFF8FD9CC);
  String status = selection < 0 ? "Release to cancel" : (selection < 4 ? "Compartment: " : "Preset: ") + labels[selection];
  int sw = font.width(status);
  int sy = cy + 114;
  g.fill(cx - sw / 2 - 5, sy - 3, cx + sw / 2 + 5, sy + 11, 0xE010141C);
  g.drawCenteredString(font, status, cx, sy, 0xFFFFF4DE);
 }

 private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
  for (int dy = -r; dy <= r; dy++) {
   int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
   g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
  }
 }

 private String fit(String text, int width) {
  if (font.width(text) <= width) return text;
  String cut = font.plainSubstrByWidth(text, Math.max(0, width - font.width("..")));
  return cut + "..";
 }

 public static void draw(GuiGraphics g, int w, int h) {}
}
