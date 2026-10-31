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

public class ToolWheel extends Screen {
 private final long openTime;
 private int selected = -1;
 private boolean cancelled = false;

 private static final String[] LABELS_10 = new String[]{"Lance", "Pickaxe", "Axe", "Shovel", "Hoe", "Sword", "Double spear", "Shortbow", "Recurve", "Greatbow"};
 private static final String[] HINTS_10 = new String[]{"Throw & recall", "Mine & harvest", "Cleave & chop", "Excavate & flatten", "Till & cultivate", "Sweeping strikes", "Extended reach", "3-arrow spread", "Piercing shot", "Heavy knockback"};
 private static final String[] CATEGORIES_10 = new String[]{"[ MELEE ]", "[ HARVEST ]", "[ HARVEST ]", "[ HARVEST ]", "[ HARVEST ]", "[ MELEE ]", "[ MELEE ]", "[ RANGED ]", "[ RANGED ]", "[ RANGED ]"};
 private static final int[] CAT_COLORS_10 = new int[]{0xFFFF5555, 0xFFFFD38A, 0xFF55FF55, 0xFFFFAA44, 0xFF88FF55, 0xFFFF6060, 0xFFFF7799, 0xFF55DDFF, 0xFF70B8FF, 0xFFAA88FF};

 private static final String[] BADGES_6 = new String[]{"[ MELEE ]", "[ MINING ]", "[ CHOPPING ]", "[ DIGGING ]", "[ FARMING ]", "[ RANGED ]"};
 private static final int[] BADGE_COLORS_6 = new int[]{0xFFFF5555, 0xFFFFD38A, 0xFF55FF55, 0xFFFFAA44, 0xFF88FF55, 0xFF55DDFF};

 public ToolWheel() {
  super(Component.literal("Tool kit"));
  this.openTime = System.currentTimeMillis();
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

 public static void tick(Minecraft m) {
  if (m.player == null) return;
  boolean tool = ClientSetup.TOOL.isDown() || ClientSetup.TOOL.consumeClick();
  boolean forms = ClientSetup.FORMS.isDown() || ClientSetup.FORMS.consumeClick();
  if (m.screen != null || (!tool && !forms) || m.player.isUsingItem() || m.player.isSpectator()) return;
  if (Screen.hasShiftDown() || Screen.hasControlDown() || Screen.hasAltDown() || m.player.isShiftKeyDown()) return;
  if (forms && !tool && ToolHand.isForeignWeaponOrTool(m.player, m.player.getMainHandItem())) return;
  if (PackItem.find(m.player) < 0 && !ClientMirror.toolActive) return;
  m.setScreen(new ToolWheel());
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
  if (b == 1) {
   cancelled = true;
   onClose();
   return true;
  }
  if (b == 0 && selected >= 0) {
   finishAndClose();
   return true;
  }
  return true;
 }

 @Override
 public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
  if (ClientSetup.TOOL.matches(keyCode, scanCode) || ClientSetup.FORMS.matches(keyCode, scanCode)) {
   finishAndClose();
   return true;
  }
  return super.keyReleased(keyCode, scanCode, modifiers);
 }

 @Override
 public boolean mouseReleased(double mx, double my, int button) {
  if (ClientSetup.TOOL.matchesMouse(button) || ClientSetup.FORMS.matchesMouse(button)) {
   finishAndClose();
   return true;
  }
  return super.mouseReleased(mx, my, button);
 }

 @Override
 public boolean keyPressed(int key, int scan, int mods) {
  if (minecraft == null || minecraft.player == null) return super.keyPressed(key, scan, mods);
  int pack = PackItem.find(minecraft.player);
  PackData d = pack < 0 ? null : new PackData(PackItem.stack(minecraft.player, pack));
  boolean isEg = ToolHand.hasEndgame(minecraft.player, d);

  if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
   int slot = key - GLFW.GLFW_KEY_1;
   if (isEg || slot < 6) {
    selected = slot;
    finishAndClose();
    return true;
   }
  } else if (key == GLFW.GLFW_KEY_0 && isEg) {
   selected = 9;
   finishAndClose();
   return true;
  }
  return super.keyPressed(key, scan, mods);
 }

 private void finishAndClose() {
  if (!cancelled && minecraft != null && minecraft.player != null) {
   if (selected >= 0) {
    Network.CHANNEL.sendToServer(new Network.Action(11, selected));
   } else if (System.currentTimeMillis() - openTime < 250) {
    Network.CHANNEL.sendToServer(new Network.Action(1));
   }
  }
  if (minecraft != null) {
   minecraft.setScreen(null);
  }
 }

 @Override
 public void render(GuiGraphics g, int mx, int my, float p) {
  if (System.currentTimeMillis() - openTime > 250 && !isKeyDown(ClientSetup.TOOL) && !isKeyDown(ClientSetup.FORMS)) {
   finishAndClose();
   return;
  }
  if (minecraft == null || minecraft.player == null) return;
  int pack = PackItem.find(minecraft.player);
  PackData d = pack < 0 ? null : new PackData(PackItem.stack(minecraft.player, pack));

  boolean isEg = ToolHand.hasEndgame(minecraft.player, d);
  int count = isEg ? 10 : 6;
  double radius = isEg ? 74.0 : 68.0;
  // The center plate is inside this radius, so reading a name does not select a wedge.
  double deadzone = isEg ? 46.0 : 42.0;

  int cx = width / 2, cy = height / 2;
  double dx = mx - cx, dy = my - cy;
  double dist = Math.sqrt(dx * dx + dy * dy);

  if (dist < deadzone || dist > radius + 28.0) {
   selected = -1;
  } else {
   double step = Math.PI * 2.0 / count;
   selected = Math.floorMod((int) Math.floor((Math.atan2(dy, dx) + Math.PI / 2.0 + step / 2.0) / step), count);
  }

  int rBack = (int) Math.round(radius + 22);
  disc(g, cx, cy, rBack, 0x660A0B12);
  disc(g, cx, cy, rBack - 8, 0x9910141C);
  disc(g, cx, cy, rBack - 16, 0xCC12151E);

  // Orbital ring with sector ticks
  int steps = count * 4;
  for (int step = 0; step < steps; step++) {
   double a = step * Math.PI * 2.0 / steps;
   int px = cx + (int) Math.round(Math.cos(a) * radius);
   int py = cy + (int) Math.round(Math.sin(a) * radius);
   g.fill(px, py, px + 1, py + 1, (step % 4 == 0) ? (isEg ? 0xFFFFD38A : 0xFF70F5E8) : 0x407A8A9A);
  }

  // --- Render Nodes ---
  for (int i = 0; i < count; i++) {
   double a = (double) i * Math.PI * 2.0 / count - Math.PI / 2.0;
   int x = cx + (int) Math.round(Math.cos(a) * radius);
   int y = cy + (int) Math.round(Math.sin(a) * radius);
   boolean isHovered = (i == selected);

   ItemStack stack = ToolHand.getToolSlot(minecraft.player, d, i);
   boolean hasStack = !stack.isEmpty();

   // Node background plate (24x24 for 10-slot, 26x26 for 6-slot)
   int half = isEg ? 12 : 13;
   if (isHovered) {
    int flareCol = isEg ? 0xD0781228 : 0xD0163248;
    g.fill(x - half - 1, y - half - 1, x + half + 1, y + half + 1, flareCol);
    g.renderOutline(x - half - 2, y - half - 2, (half + 2) * 2, (half + 2) * 2, isEg ? 0x80FF7088 : 0x8070F5E8);
    g.renderOutline(x - half - 1, y - half - 1, (half + 1) * 2, (half + 1) * 2, isEg ? 0xFFFFDF70 : 0xFF5DF2D6);
   } else {
    g.fill(x - half, y - half, x + half, y + half, 0xCC11141C);
    g.renderOutline(x - half, y - half, half * 2, half * 2, hasStack ? (isEg ? 0x90A068B8 : 0x7052687A) : 0x4034404C);
   }

   // Item rendering with hover scale pop
   if (hasStack) {
    g.pose().pushPose();
    g.pose().translate(x, y, 0.0F);
    float scale = isHovered ? 1.35F : (isEg ? 1.1F : 1.0F);
    g.pose().scale(scale, scale, 1.0F);
    g.renderItem(stack, -8, -8);
    g.pose().popPose();
   } else {
    g.fill(x - 2, y - 2, x + 2, y + 2, 0x408A9BB0);
   }

   int num = (i == 9 ? 0 : i + 1);
   String badge = String.valueOf(num);
   int bx = x + (int) Math.round(Math.cos(a) * (half + 7)) - font.width(badge) / 2;
   int by = y + (int) Math.round(Math.sin(a) * (half + 7)) - 4;
   g.drawString(font, badge, bx, by, isHovered ? 0xFFFFFFFF : 0xD0C8D4DE, true);
  }

  int plate = (int) deadzone - 4;
  String title;
  String detail;
  int titleColor;
  String hint = null;
  int hintColor = 0xFFE7D1A4;
  if (selected >= 0) {
   ItemStack sel = ToolHand.getToolSlot(minecraft.player, d, selected);
   if (isEg) {
    title = CATEGORIES_10[selected];
    titleColor = CAT_COLORS_10[selected];
    detail = LABELS_10[selected];
    hint = HINTS_10[selected];
    hintColor = 0xFFFFD0E8;
   } else {
    title = BADGES_6[selected];
    titleColor = BADGE_COLORS_6[selected];
    if (!sel.isEmpty()) {
     detail = sel.getHoverName().getString();
     hint = sel.isDamageableItem() ? ((sel.getMaxDamage() - sel.getDamageValue()) + " / " + sel.getMaxDamage()) : "Ready";
     hintColor = 0xFFB7D4C8;
    } else {
     detail = "Empty slot";
     hint = "No tool in this rack";
     hintColor = 0xFF9AA4AE;
    }
   }
  } else {
   title = isEg ? "[ ENDGAME ]" : "[ TOOL KIT ]";
   titleColor = isEg ? 0xFFE088FF : 0xFF70D9C0;
   detail = isEg ? "Select a form" : "Aim at a tool";
   hint = isEg ? "10 forms, one weapon" : "A quick tap draws or stows";
   hintColor = 0xFFB7C4CE;
  }
  int textWidth = plate * 2 - 10;
  g.fill(cx - plate, cy - 16, cx + plate, cy + 14, 0xF00E1218);
  g.renderOutline(cx - plate, cy - 16, plate * 2, 30, selected >= 0 ? 0xFFFFD38A : 0x80608090);
  g.drawCenteredString(font, fit(title, textWidth), cx, cy - 12, titleColor);
  g.drawCenteredString(font, fit(detail, textWidth), cx, cy - 1, 0xFFFFF4DE);
  if (hint != null) {
   String shown = fit(hint, 180);
   int hw = font.width(shown);
   int hy = cy + (int) radius + 34;
   g.fill(cx - hw / 2 - 5, hy - 3, cx + hw / 2 + 5, hy + 11, 0xE010141C);
   g.drawCenteredString(font, shown, cx, hy, hintColor);
  }

  boolean formsKey = isKeyDown(ClientSetup.FORMS) && !isKeyDown(ClientSetup.TOOL);
  String keyName = (formsKey ? ClientSetup.FORMS : ClientSetup.TOOL).getTranslatedKeyMessage().getString();
  String numbers = isEg ? "1-9 or 0" : "1-6";
  g.drawCenteredString(font, "Release " + keyName + "  ·  " + numbers + "  ·  right-click cancels", cx, cy + (int) radius + 50, 0xFFB0BCC8);
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

 public static void hand(GuiGraphics g, int w, int h) {
  var m = Minecraft.getInstance();
  if (m.player == null || m.options.hideGui || (!ClientMirror.toolActive && PackItem.find(m.player)<0)) return;
  int x = w / 2 + (m.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 100 : -122);
  int y = h - 22;
  g.fill(x, y, x + 22, y + 22, 0x40202020);
  if(ClientMirror.toolCursor(m.player)) g.blit(new net.minecraft.resources.ResourceLocation("textures/gui/widgets.png"), x - 1, y - 1, 0, 22, 24, 22);
  else g.renderOutline(x, y, 22, 22, 0xFF70D9C0);
  if(ToolEquipment.active(m.player)){
   ItemStack equipped=ToolEquipment.stack(m.player);
   g.renderItem(equipped, x + 3, y + 3);
   g.renderItemDecorations(m.font,equipped,x+3,y+3);
  }
 }
}
