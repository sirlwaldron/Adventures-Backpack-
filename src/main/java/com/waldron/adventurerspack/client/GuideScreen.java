package com.waldron.adventurerspack.client;

import com.waldron.adventurerspack.AdventurersPack;
import com.waldron.adventurerspack.GuideBook;
import com.waldron.adventurerspack.PackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.IShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fantasy RPG Codex & Compendium UI for the Adventurer's Field Guide.
 * Features category realm tabs, live search filtering, item-icon roster cards,
 * an animated hero item pedestal, interactive variant selector, RPG attribute bars,
 * live 3x3 crafting blueprint preview, and gilded lore sections.
 */
public final class GuideScreen extends Screen {
 private static final int GUI_W = 600, GUI_H = 388, ROW_H = 30, HERO_H = 74;

 // Fantasy RPG Palette: Obsidian Slate, Antique Brass/Gold, Arcane Teal, and Warm Parchment Ivory
 private static final int INK = 0xFFFFF4DE;
 private static final int MUTED = 0xFFD8CCB8;
 private static final int DIM = 0xFF9A8E7A;
 private static final int TEAL = 0xFF56FAD8;
 private static final int GOLD = 0xFFFFD770;
 private static final int BRASS = 0xFFC8A258;
 private static final int BORDER_OUTER = 0xFF8B7042;
 private static final int BORDER_INNER = 0xFF4A3C24;
 private static final int PANEL_BG = 0xF00B0E14;
 private static final int CARD_BG = 0xCC11161F;
 private static final int PILL_BG = 0xE607090D;

 private float uiScale = 1f;
 private int left, top;
 private int tabY, listX, listY, listW, listH, bodyX, bodyY, bodyW, bodyH, scrollY, scrollH;

 private int activeCategory = 0;
 private int selectedIndex = 0; // Index within filteredEntries
 private int selectedVariant = 0;
 private int listScroll = 0;
 private int bodyScroll = 0;
 private int contentHeight = 0;
 private int drawnEntryKey = -1;

 private EditBox searchBox;
 private final List<Integer> filteredEntries = new ArrayList<>();
 private final List<SectionBlock> layoutBlocks = new ArrayList<>();
 private ItemStack hoveredTooltipStack = ItemStack.EMPTY;
 private String hoveredTabTooltip = null;

 public GuideScreen() {
  super(Component.literal("Adventurer's Field Guide"));
 }

 public static void open() {
  var mc = Minecraft.getInstance();
  if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
   mc.player.closeContainer();
  }
  mc.setScreen(new GuideScreen());
 }

 @Override
 protected void init() {
  int realW = minecraft.getWindow().getGuiScaledWidth();
  int realH = minecraft.getWindow().getGuiScaledHeight();
  uiScale = Math.min(1f, Math.min((realW - 12) / (float) GUI_W, (realH - 12) / (float) GUI_H));
  uiScale = Math.max(0.52f, uiScale);
  width = (int) (realW / uiScale);
  height = (int) (realH / uiScale);
  left = (width - GUI_W) / 2;
  top = (height - GUI_H) / 2;

  tabY = top + 30;
  listX = left + 10;
  listY = top + 58;
  listW = 180;
  listH = GUI_H - 82;

  bodyX = left + 198;
  bodyY = top + 58;
  bodyW = GUI_W - 208;
  bodyH = GUI_H - 82;
  scrollY = bodyY + HERO_H + 4;
  scrollH = bodyH - HERO_H - 4;

  super.init();

  String prevQuery = searchBox != null ? searchBox.getValue() : "";
  searchBox = new EditBox(font, left + GUI_W - 256, top + 8, 112, 12, Component.literal("Search codex"));
  searchBox.setMaxLength(40);
  searchBox.setBordered(false);
  searchBox.setTextColor(INK);
  searchBox.setHint(Component.literal("Search codex..."));
  searchBox.setValue(prevQuery);
  searchBox.setResponder(q -> {
   rebuildFilter();
   selectedIndex = 0;
   selectedVariant = 0;
   listScroll = 0;
   bodyScroll = 0;
   drawnEntryKey = -1;
  });
  addRenderableWidget(searchBox);

  addRenderableWidget(new SoftButton(left + GUI_W - 130, top + 5, 54, 16, Component.literal("Pack"), b -> com.waldron.adventurerspack.Network.CHANNEL.sendToServer(new com.waldron.adventurerspack.Network.Action(0))));
  addRenderableWidget(new SoftButton(left + GUI_W - 72, top + 5, 62, 16, Component.literal("Close"), b -> onClose()));
  addRenderableWidget(new SoftButton(left + 10, top + GUI_H - 20, 52, 15, Component.literal("< Prev"), b -> stepEntry(-1)));
  addRenderableWidget(new SoftButton(left + 66, top + GUI_H - 20, 52, 15, Component.literal("Next >"), b -> stepEntry(1)));

  rebuildFilter();
  revealList();
 }

 private void rebuildFilter() {
  filteredEntries.clear();
  String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
  for (int i = 0; i < GuideBook.ENTRIES.length; i++) {
   GuideBook.Entry e = GuideBook.ENTRIES[i];
   if (activeCategory != 0 && e.category != activeCategory) continue;
   if (!q.isEmpty() && !matchesSearch(e, q)) continue;
   filteredEntries.add(i);
  }
  if (selectedIndex >= filteredEntries.size()) {
   selectedIndex = Math.max(0, filteredEntries.size() - 1);
  }
 }

 private boolean matchesSearch(GuideBook.Entry e, String q) {
  if (e.nav.toLowerCase(Locale.ROOT).contains(q)
   || e.title.toLowerCase(Locale.ROOT).contains(q)
   || e.badge.toLowerCase(Locale.ROOT).contains(q)
   || e.subtitle.toLowerCase(Locale.ROOT).contains(q)
   || e.socketInfo.toLowerCase(Locale.ROOT).contains(q)) {
   return true;
  }
  for (GuideBook.Stat st : e.stats) {
   if (st.label().toLowerCase(Locale.ROOT).contains(q) || st.value().toLowerCase(Locale.ROOT).contains(q)) return true;
  }
  for (GuideBook.Section sec : e.sections) {
   if (sec.heading().toLowerCase(Locale.ROOT).contains(q)) return true;
   for (String b : sec.bullets()) {
    if (b.toLowerCase(Locale.ROOT).contains(q)) return true;
   }
  }
  return false;
 }

 private GuideBook.Entry currentEntry() {
  if (filteredEntries.isEmpty()) return null;
  int idx = filteredEntries.get(Math.max(0, Math.min(filteredEntries.size() - 1, selectedIndex)));
  return GuideBook.ENTRIES[idx];
 }

 private ItemStack activeVariantStack(GuideBook.Entry e) {
  if (e == null) return ItemStack.EMPTY;
  ItemStack[] vars = e.variants != null ? e.variants.get() : null;
  if (vars != null && vars.length > 0) {
   int vi = Math.max(0, Math.min(vars.length - 1, selectedVariant));
   return vars[vi];
  }
  return e.icon != null ? e.icon.get() : ItemStack.EMPTY;
 }

 @Override
 public boolean isPauseScreen() {
  return false;
 }

 @Override
 public void renderBackground(GuiGraphics g) {}

 @Override
 public void render(GuiGraphics g, int mx, int my, float partial) {
  int x = (int) (mx / uiScale);
  int y = (int) (my / uiScale);
  hoveredTooltipStack = ItemStack.EMPTY;
  hoveredTabTooltip = null;

  g.pose().pushPose();
  g.pose().scale(uiScale, uiScale, 1f);

  // 1. Atmospheric RPG Backdrop & Arcane Starlight Motes
  g.fillGradient(0, 0, width, height, 0xCC05070B, 0xE6090B12);
  drawArcaneMotes(g);

  // 2. Main Ornate RPG Window Frame
  drawOrnateFrame(g, left, top, GUI_W, GUI_H, BORDER_OUTER, PANEL_BG, true);
  // Top Royal Banner Header
  g.fillGradient(left + 2, top + 2, left + GUI_W - 2, top + 26, 0xCC241A14, 0x88120E0C);
  g.fill(left + 2, top + 26, left + GUI_W - 2, top + 27, 0x998B7042);

  // Header Crest & Category Subtitle
  pill(g, "❖ ADVENTURER'S CODEX", left + 14, top + 8, GOLD);
  GuideBook.Category cat = GuideBook.CATEGORIES[activeCategory];
  g.drawString(font, "§7·  " + cat.subtitle(), left + 150, top + 9, MUTED, true);

  // Search Box Frame
  int sx = left + GUI_W - 270, sy = top + 5, sw = 136, sh = 16;
  g.fill(sx, sy, sx + sw, sy + sh, 0xEE07090D);
  g.renderOutline(sx, sy, sw, sh, searchBox.isFocused() ? GOLD : 0xFF66563A);
  g.drawString(font, "⌕", sx + 4, sy + 4, TEAL, false);
  if (!searchBox.getValue().isEmpty()) {
   boolean clearHot = x >= sx + sw - 12 && x < sx + sw && y >= sy && y < sy + sh;
   g.drawString(font, "×", sx + sw - 9, sy + 4, clearHot ? 0xFFFF6666 : MUTED, false);
  }

  // 3. Top RPG Category Realm Tabs
  drawCategoryTabs(g, x, y);

  // 4. Left Roster Column & Right Inspector Column
  drawOrnateFrame(g, listX - 2, listY - 2, listW + 4, listH + 4, 0xFF6E5835, CARD_BG, false);
  drawOrnateFrame(g, bodyX - 2, bodyY - 2, bodyW + 4, bodyH + 4, 0xFF6E5835, CARD_BG, false);

  drawRosterList(g, x, y);
  drawInspector(g, x, y);

  // 5. Bottom Status & Navigation Bar
  String countText = filteredEntries.isEmpty() ? "0 / 0" : (selectedIndex + 1) + " / " + filteredEntries.size();
  g.drawString(font, countText, left + 124, top + GUI_H - 16, GOLD, true);
  String helpText = "Click icons to inspect recipes · [↑/↓] Entry · [←/→] Realm · Scroll to read";
  g.drawString(font, helpText, left + GUI_W - 12 - font.width(helpText), top + GUI_H - 16, MUTED, true);

  super.render(g, x, y, partial);

  // 6. Post-render Tooltips
  if (!hoveredTooltipStack.isEmpty()) {
   g.renderTooltip(font, hoveredTooltipStack, x, y);
  } else if (hoveredTabTooltip != null) {
   g.renderTooltip(font, font.split(Component.literal(hoveredTabTooltip), 200), x, y);
  }

  g.pose().popPose();
 }

 private void drawArcaneMotes(GuiGraphics g) {
  long now = System.currentTimeMillis();
  for (int i = 0; i < 24; i++) {
   double phase = now * 0.0008 + i * 1.73;
   int mx = (int) ((Math.sin(phase * 0.7 + i) * 0.5 + 0.5) * width);
   int my = (int) ((Math.cos(phase * 0.5 + i * 2) * 0.5 + 0.5) * height);
   int alpha = (int) (35 + 45 * (Math.sin(phase * 2.1) * 0.5 + 0.5));
   int color = (i % 3 == 0) ? ((alpha << 24) | 0x56FAD8) : ((alpha << 24) | 0xFFD770);
   g.fill(mx, my, mx + 2, my + 2, color);
  }
 }

 private void drawOrnateFrame(GuiGraphics g, int x, int y, int w, int h, int borderColor, int bgColor, boolean corners) {
  g.fill(x, y, x + w, y + h, bgColor);
  g.renderOutline(x, y, w, h, borderColor);
  g.renderOutline(x + 2, y + 2, w - 4, h - 4, 0x663A2E1E);
  g.fill(x + 2, y + 1, x + w - 2, y + 2, 0x55FFE49E);
  if (corners) {
   int c = GOLD;
   // Corner diamond accents
   g.fill(x - 1, y - 1, x + 4, y + 4, c);
   g.fill(x + w - 3, y - 1, x + w + 1, y + 4, c);
   g.fill(x - 1, y + h - 3, x + 4, y + h + 1, c);
   g.fill(x + w - 3, y + h - 3, x + w + 1, y + h + 1, c);
  }
 }

 private void drawCategoryTabs(GuiGraphics g, int mx, int my) {
  int count = GuideBook.CATEGORIES.length;
  int gap = 4;
  int totalW = GUI_W - 20;
  int tabW = (totalW - gap * (count - 1)) / count;
  int tabH = 22;
  String[] shortNames = {"All", "Backpack", "Attach.", "Runes", "Addons", "Arsenal", "Armor"};

  for (int i = 0; i < count; i++) {
   int tx = left + 10 + i * (tabW + gap);
   boolean active = (i == activeCategory);
   boolean hot = mx >= tx && mx < tx + tabW && my >= tabY && my < tabY + tabH;
   GuideBook.Category cat = GuideBook.CATEGORIES[i];

   int bg = active ? 0xEE2B2018 : hot ? 0xCC1E1816 : 0xAA0E1218;
   int border = active ? cat.color() : hot ? BRASS : 0xFF564730;
   g.fill(tx, tabY, tx + tabW, tabY + tabH, bg);
   g.renderOutline(tx, tabY, tabW, tabH, border);
   if (active) {
    g.fill(tx + 1, tabY + tabH - 2, tx + tabW - 1, tabY + tabH - 1, cat.color());
   }

   ItemStack icon = cat.icon().get();
   g.pose().pushPose();
   g.pose().translate(tx + 4, tabY + 3, 0);
   g.pose().scale(0.95f, 0.95f, 1f);
   g.renderItem(icon, 0, 0);
   g.pose().popPose();

   g.drawString(font, shortNames[i], tx + 22, tabY + 7, active ? GOLD : (hot ? INK : MUTED), true);

   if (hot) {
    hoveredTabTooltip = "§6§l" + cat.name() + "\n§7" + cat.subtitle();
   }
  }
 }

 private void drawRosterList(GuiGraphics g, int mx, int my) {
  clip(g, listX, listY, listW, listH);
  if (filteredEntries.isEmpty()) {
   g.drawCenteredString(font, "No matching entries", listX + listW / 2, listY + 36, MUTED);
   g.disableScissor();
   return;
  }

  int visibleRows = Math.max(1, listH / ROW_H);
  for (int i = 0; i <= visibleRows; i++) {
   int fIdx = listScroll + i;
   if (fIdx >= filteredEntries.size()) break;
   GuideBook.Entry entry = GuideBook.ENTRIES[filteredEntries.get(fIdx)];
   int ry = listY + i * ROW_H;
   int rh = ROW_H - 2;
   boolean on = (fIdx == selectedIndex);
   boolean hot = mx >= listX && mx < listX + listW - 5 && my >= ry && my < ry + rh;

   int bg = on ? 0xDD38281E : hot ? 0xBB241C1A : 0x8810141C;
   int border = on ? GOLD : hot ? BRASS : 0x775A4A35;
   g.fill(listX, ry, listX + listW - 5, ry + rh, bg);
   g.renderOutline(listX, ry, listW - 5, rh, border);
   // Left RPG rarity/category bar
   g.fill(listX, ry, listX + 3, ry + rh, on ? GOLD : entry.accentColor);

   // Recessed 20x20 Item Icon Socket
   int ix = listX + 6, iy = ry + 4;
   g.fill(ix, iy, ix + 20, iy + 20, 0xEE080A0F);
   g.renderOutline(ix, iy, 20, 20, on ? entry.accentColor : 0x88685840);
   ItemStack icon = entry.icon.get();
   g.renderItem(icon, ix + 2, iy + 2);

   // Entry Name & Sub-Badge
   String title = font.plainSubstrByWidth(entry.nav, listW - 38);
   g.drawString(font, title, listX + 30, ry + 4, on ? GOLD : INK, true);

   g.pose().pushPose();
   g.pose().translate(listX + 30, ry + 16, 0);
   g.pose().scale(0.72f, 0.72f, 1f);
   g.drawString(font, "◆ " + entry.badge, 0, 0, entry.accentColor, false);
   g.pose().popPose();
  }
  g.disableScissor();

  int max = maxListScroll();
  if (max > 0) {
   int trackX = listX + listW - 3;
   int barH = Math.max(16, listH * listH / (filteredEntries.size() * ROW_H));
   int barY = listY + (listH - barH) * listScroll / max;
   g.fill(trackX, listY, trackX + 3, listY + listH, 0x88090B0E);
   g.fill(trackX, barY, trackX + 3, barY + barH, BRASS);
  }
 }

 private void drawInspector(GuiGraphics g, int mx, int my) {
  GuideBook.Entry entry = currentEntry();
  if (entry == null) return;

  // --- 1. Top Hero Showcase Card ---
  g.fillGradient(bodyX, bodyY, bodyX + bodyW, bodyY + HERO_H, 0xEE141924, 0xEE0C0F17);
  g.renderOutline(bodyX, bodyY, bodyW, HERO_H, 0xFF8A7046);
  g.fill(bodyX + 1, bodyY + 1, bodyX + bodyW - 1, bodyY + 2, entry.accentColor);

  // Animated Floating Item Pedestal (44x44)
  int pedX = bodyX + 8, pedY = bodyY + 8, pedS = 44;
  g.fill(pedX, pedY, pedX + pedS, pedY + pedS, 0xFF07090E);
  g.renderOutline(pedX, pedY, pedS, pedS, entry.accentColor);
  g.renderOutline(pedX + 2, pedY + 2, pedS - 4, pedS - 4, 0x66FFD770);
  // Subtle pulsing inner aura
  g.fill(pedX + 6, pedY + 6, pedX + pedS - 6, pedY + pedS - 6, (entry.accentColor & 0x00FFFFFF) | 0x22000000);

  ItemStack heroStack = activeVariantStack(entry);
  float bob = (float) Math.sin(System.currentTimeMillis() * 0.004) * 1.5f;
  g.pose().pushPose();
  g.pose().translate(pedX + 6, pedY + 6 + bob, 50);
  g.pose().scale(2.0f, 2.0f, 1f);
  g.renderItem(heroStack, 0, 0);
  g.pose().popPose();

  if (mx >= pedX && mx < pedX + pedS && my >= pedY && my < pedY + pedS) {
   hoveredTooltipStack = heroStack;
  }

  g.pose().pushPose();
  g.pose().translate(pedX + 3, pedY + pedS + 4, 0);
  g.pose().scale(0.68f, 0.68f, 1f);
  g.drawString(font, "HOVER ITEM", 0, 0, DIM, false);
  g.pose().popPose();

  // Hero Title, Badge & Subtitle
  int hx = bodyX + 58;
  pill(g, entry.badge, hx, bodyY + 6, entry.accentColor);
  int badgeW = font.width(entry.badge) + 12;
  g.drawString(font, "§l" + entry.title, hx + badgeW, bodyY + 6, GOLD, true);

  String sub = font.plainSubstrByWidth(entry.subtitle, bodyW - 64);
  g.drawString(font, sub, hx, bodyY + 21, INK, false);

  g.pose().pushPose();
  g.pose().translate(hx, bodyY + 34, 0);
  g.pose().scale(0.82f, 0.82f, 1f);
  String sock = font.plainSubstrByWidth(entry.socketInfo, (int) ((bodyW - 64) / 0.82f));
  g.drawString(font, sock, 0, 0, TEAL, true);
  g.pose().popPose();

  // Interactive Variant / Arsenal Showcase Strip
  ItemStack[] variants = entry.variants != null ? entry.variants.get() : new ItemStack[0];
  if (variants.length > 0) {
   g.pose().pushPose();
   g.pose().translate(hx, bodyY + 52, 0);
   g.pose().scale(0.75f, 0.75f, 1f);
   g.drawString(font, "INSPECT:", 0, 4, MUTED, false);
   g.pose().popPose();

   int vxStart = hx + 42;
   for (int v = 0; v < variants.length; v++) {
    int vx = vxStart + v * 20;
    int vy = bodyY + 48;
    if (vx + 18 > bodyX + bodyW - 4) break;
    boolean vSel = (v == selectedVariant);
    boolean vHot = mx >= vx && mx < vx + 18 && my >= vy && my < vy + 18;
    g.fill(vx, vy, vx + 18, vy + 18, vSel ? 0xEE2C2216 : 0xCC090C12);
    g.renderOutline(vx, vy, 18, 18, vSel ? GOLD : (vHot ? TEAL : 0x885C4E38));
    g.renderItem(variants[v], vx + 1, vy + 1);
    if (vHot) {
     hoveredTooltipStack = variants[v];
    }
   }
  }

  // --- 2. Scrollable RPG Inspector Body ---
  rebuildInspectorLayout(entry);
  clip(g, bodyX, scrollY, bodyW - 5, scrollH);

  int curY = scrollY - bodyScroll + 4;
  CraftingRecipe recipe = findRecipeForDisplay(heroStack);

  // Top Section: RPG Stat Bars + Optional 3x3 Forge Recipe Card
  int statsAreaW = (recipe != null) ? (bodyW - 144) : (bodyW - 14);
  int statsBottomY = drawStatBars(g, entry, bodyX + 6, curY, statsAreaW);

  if (recipe != null) {
   int recBottomY = drawRecipeCard(g, recipe, heroStack, bodyX + bodyW - 132, curY, mx, my);
   curY = Math.max(statsBottomY, recBottomY) + 8;
  } else {
   curY = statsBottomY + 6;
  }

  // Lore & Mechanics Sections
  for (SectionBlock block : layoutBlocks) {
   // Section Banner
   if (curY + 16 > scrollY && curY < scrollY + scrollH) {
    g.fillGradient(bodyX + 6, curY, bodyX + bodyW - 10, curY + 14, 0xCC281D16, 0x44120E0C);
    g.fill(bodyX + 6, curY, bodyX + 8, curY + 14, GOLD);
    g.fill(bodyX + 6, curY + 13, bodyX + bodyW - 10, curY + 14, 0x668B7042);
    g.drawString(font, "◆  " + block.heading, bodyX + 13, curY + 3, GOLD, true);
   }
   curY += 18;

   for (List<String> bulletLines : block.wrappedBullets) {
    int cardH = bulletLines.size() * 11 + 6;
    if (curY + cardH > scrollY && curY < scrollY + scrollH) {
     g.fill(bodyX + 8, curY, bodyX + bodyW - 10, curY + cardH - 2, 0x770C1017);
     g.renderOutline(bodyX + 8, curY, bodyW - 18, cardH - 2, 0x44564832);
     g.drawString(font, "❖", bodyX + 13, curY + 3, entry.accentColor, false);
     for (int l = 0; l < bulletLines.size(); l++) {
      g.drawString(font, styleInlineKeys(bulletLines.get(l)), bodyX + 24, curY + 3 + l * 11, INK, true);
     }
    }
    curY += cardH + 2;
   }
   curY += 4;
  }

  g.disableScissor();

  int maxBody = maxBodyScroll();
  if (maxBody > 0) {
   int trackX = bodyX + bodyW - 4;
   int barH = Math.max(18, scrollH * scrollH / Math.max(scrollH + 1, contentHeight));
   int barY = scrollY + (scrollH - barH) * bodyScroll / maxBody;
   g.fill(trackX, scrollY, trackX + 3, scrollY + scrollH, 0x88090B0E);
   g.fill(trackX, barY, trackX + 3, barY + barH, BRASS);
  }
 }

 private int drawStatBars(GuiGraphics g, GuideBook.Entry entry, int x, int y, int w) {
  if (entry.stats == null || entry.stats.length == 0) return y;
  g.fillGradient(x, y, x + w, y + 13, 0xCC1E1814, 0x44100D0B);
  g.fill(x, y, x + 2, y + 13, entry.accentColor);
  g.drawString(font, "◆  ATTRIBUTES & SPECIFICATIONS", x + 7, y + 3, GOLD, true);
  int curY = y + 16;

  boolean twoCols = w > 280;
  int cols = twoCols ? 2 : 1;
  int colW = twoCols ? (w - 6) / 2 : w;
  int rowH = 22;

  for (int i = 0; i < entry.stats.length; i++) {
   GuideBook.Stat st = entry.stats[i];
   int col = i % cols;
   int row = i / cols;
   int sx = x + col * (colW + 6);
   int sy = curY + row * rowH;

   if (sy + rowH > scrollY && sy < scrollY + scrollH) {
    g.fill(sx, sy, sx + colW, sy + rowH - 2, 0xAA0A0D13);
    g.renderOutline(sx, sy, colW, rowH - 2, 0x66564834);

    g.pose().pushPose();
    g.pose().translate(sx + 4, sy + 3, 0);
    g.pose().scale(0.78f, 0.78f, 1f);
    g.drawString(font, st.label(), 0, 0, MUTED, false);
    String val = st.value();
    int vw = font.width(val);
    g.drawString(font, val, (int) ((colW - 8) / 0.78f) - vw, 0, st.color(), true);
    g.pose().popPose();

    // RPG Segmented Meter Bar
    int barX = sx + 4, barY = sy + 13, barW = colW - 8, barH = 4;
    g.fill(barX, barY, barX + barW, barY + barH, 0xFF181C24);
    int fillW = Math.round(barW * Math.max(0f, Math.min(1f, st.fill())));
    if (fillW > 0) {
     g.fill(barX, barY, barX + fillW, barY + barH, st.color());
    }
    for (int seg = 1; seg < 4; seg++) {
     int tickX = barX + (barW * seg) / 4;
     g.fill(tickX, barY, tickX + 1, barY + barH, 0x88080A0E);
    }
   }
  }

  int totalRows = (entry.stats.length + cols - 1) / cols;
  return curY + totalRows * rowH;
 }

 private int drawRecipeCard(GuiGraphics g, CraftingRecipe recipe, ItemStack target, int x, int y, int mx, int my) {
  int w = 122, h = 86;
  if (y + h > scrollY && y < scrollY + scrollH) {
   g.fill(x, y, x + w, y + h, 0xDD0D1119);
   g.renderOutline(x, y, w, h, 0xFF8A7046);
   g.fillGradient(x + 1, y + 1, x + w - 1, y + 13, 0xCC281D16, 0x66120E0C);
   g.pose().pushPose();
   g.pose().translate(x + 5, y + 3, 0);
   g.pose().scale(0.75f, 0.75f, 1f);
   g.drawString(font, "◆ FORGE BLUEPRINT", 0, 0, GOLD, true);
   g.pose().popPose();

   int gx = x + 6, gy = y + 17;
   List<Ingredient> ings = recipe.getIngredients();
   int rw = (recipe instanceof IShapedRecipe<?> sh) ? sh.getRecipeWidth() : 3;
   int rh = (recipe instanceof IShapedRecipe<?> sh) ? sh.getRecipeHeight() : 3;
   long cycle = System.currentTimeMillis() / 1000L;

   for (int row = 0; row < 3; row++) {
    for (int col = 0; col < 3; col++) {
     int cx = gx + col * 18;
     int cy = gy + row * 18;
     g.fill(cx, cy, cx + 17, cy + 17, 0xEE07090D);
     g.renderOutline(cx, cy, 17, 17, 0x88625238);

     if (col < rw && row < rh) {
      int idx = col + row * rw;
      if (idx < ings.size()) {
       Ingredient ing = ings.get(idx);
       ItemStack[] stacks = ing.getItems();
       if (stacks.length > 0) {
        ItemStack disp = stacks[(int) (cycle % stacks.length)];
        // Show previous backpack tier in center of tier-upgrade recipe
        if (target.is(AdventurersPack.PACK.get()) && PackData.tierOf(target) > 0 && disp.is(AdventurersPack.PACK.get())) {
         disp = GuideBook.tierPack(PackData.tierOf(target) - 1);
        }
        g.renderItem(disp, cx, cy);
        if (mx >= cx && mx < cx + 17 && my >= Math.max(scrollY, cy) && my < Math.min(scrollY + scrollH, cy + 17)) {
         hoveredTooltipStack = disp;
        }
       }
      }
     }
    }
   }

   // Arrow & Output Slot
   g.drawString(font, "⇒", gx + 58, gy + 23, GOLD, true);
   int ox = gx + 72, oy = gy + 17;
   g.fill(ox - 2, oy - 2, ox + 18, oy + 18, 0xEE181410);
   g.renderOutline(ox - 2, oy - 2, 20, 20, GOLD);
   ItemStack res = target;
   if (minecraft != null && minecraft.level != null) {
    ItemStack out = recipe.getResultItem(minecraft.level.registryAccess());
    if (!out.isEmpty()) res = out;
   }
   g.renderItem(res, ox, oy);
   if (mx >= ox - 2 && mx < ox + 18 && my >= Math.max(scrollY, oy - 2) && my < Math.min(scrollY + scrollH, oy + 18)) {
    hoveredTooltipStack = res;
   }

   g.pose().pushPose();
   g.pose().translate(x + 5, y + 74, 0);
   g.pose().scale(0.68f, 0.68f, 1f);
   String rName = font.plainSubstrByWidth(res.getHoverName().getString(), (int) ((w - 10) / 0.68f));
   g.drawString(font, rName, 0, 0, MUTED, false);
   g.pose().popPose();
  }
  return y + h;
 }

 private CraftingRecipe findRecipeForDisplay(ItemStack target) {
  if (target.isEmpty() || minecraft == null || minecraft.level == null) return null;
  var reg = minecraft.level.registryAccess();
  var all = minecraft.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
  for (CraftingRecipe r : all) {
   ItemStack out = r.getResultItem(reg);
   if (!out.isEmpty() && out.is(target.getItem())) {
    if (target.is(AdventurersPack.PACK.get()) && PackData.tierOf(out) != PackData.tierOf(target)) continue;
    return r;
   }
  }
  // If inspecting one of Endgame's morphed forms, show the base Endgame / Requiem recipe
  if (com.waldron.adventurerspack.Requiem.form(target) >= 0) {
   for (CraftingRecipe r : all) {
    ItemStack out = r.getResultItem(reg);
    if (out.is(AdventurersPack.ENDGAME.get()) || out.is(AdventurersPack.REQUIEM.get())) return r;
   }
  }
  return null;
 }

 private String styleInlineKeys(String line) {
  return line.replace("[", "§e[").replace("]", "]§r");
 }

 private void rebuildInspectorLayout(GuideBook.Entry entry) {
  int entryIdx = filteredEntries.isEmpty() ? -1 : filteredEntries.get(selectedIndex);
  int key = (entryIdx << 8) | (selectedVariant & 0xFF);
  if (drawnEntryKey == key) return;
  drawnEntryKey = key;
  layoutBlocks.clear();

  ItemStack heroStack = activeVariantStack(entry);
  CraftingRecipe recipe = findRecipeForDisplay(heroStack);
  int statsAreaW = (recipe != null) ? (bodyW - 144) : (bodyW - 14);
  boolean twoCols = statsAreaW > 280;
  int cols = twoCols ? 2 : 1;
  int statRows = entry.stats != null ? (entry.stats.length + cols - 1) / cols : 0;
  int statsH = entry.stats != null && entry.stats.length > 0 ? (16 + statRows * 22) : 0;
  int topH = (recipe != null) ? Math.max(statsH, 86) + 8 : statsH + 6;

  contentHeight = topH + 8;
  int wrapW = bodyW - 38;

  for (GuideBook.Section sec : entry.sections) {
   List<List<String>> wrappedBullets = new ArrayList<>();
   contentHeight += 18;
   for (String bullet : sec.bullets()) {
    List<String> lines = new ArrayList<>();
    wrapText(bullet, wrapW, lines);
    wrappedBullets.add(lines);
    contentHeight += lines.size() * 11 + 8;
   }
   contentHeight += 4;
   layoutBlocks.add(new SectionBlock(sec.heading(), wrappedBullets));
  }
  bodyScroll = Math.min(bodyScroll, maxBodyScroll());
 }

 private void wrapText(String text, int maxW, List<String> out) {
  String line = "";
  for (String word : text.split(" ")) {
   if (font.width(word) > maxW) {
    if (!line.isEmpty()) {
     out.add(line);
     line = "";
    }
    String rest = word;
    while (!rest.isEmpty()) {
     String part = font.plainSubstrByWidth(rest, maxW);
     if (part.isEmpty()) part = rest.substring(0, 1);
     out.add(part);
     rest = rest.substring(part.length());
    }
    continue;
   }
   String next = line.isEmpty() ? word : line + " " + word;
   if (font.width(next) > maxW && !line.isEmpty()) {
    out.add(line);
    line = word;
   } else {
    line = next;
   }
  }
  if (!line.isEmpty()) out.add(line);
 }

 private void pill(GuiGraphics g, String text, int x, int y, int color) {
  int w = font.width(text);
  g.fill(x - 3, y - 2, x + w + 3, y + 10, PILL_BG);
  g.renderOutline(x - 3, y - 2, w + 6, 12, 0x886E5835);
  g.drawString(font, text, x, y, color, true);
 }

 private void clip(GuiGraphics g, int x, int y, int w, int h) {
  g.enableScissor((int) (x * uiScale), (int) (y * uiScale), (int) ((x + w) * uiScale), (int) ((y + h) * uiScale));
 }

 private void selectEntry(int fIndex) {
  if (filteredEntries.isEmpty()) return;
  int next = Math.max(0, Math.min(filteredEntries.size() - 1, fIndex));
  if (next != selectedIndex) {
   selectedIndex = next;
   selectedVariant = 0;
   bodyScroll = 0;
   drawnEntryKey = -1;
  }
  revealList();
 }

 private void stepEntry(int delta) {
  selectEntry(selectedIndex + delta);
 }

 private void selectCategory(int cat) {
  int next = Math.floorMod(cat, GuideBook.CATEGORIES.length);
  if (next != activeCategory) {
   activeCategory = next;
   rebuildFilter();
   selectedIndex = 0;
   selectedVariant = 0;
   listScroll = 0;
   bodyScroll = 0;
   drawnEntryKey = -1;
  }
 }

 private void revealList() {
  int rows = Math.max(1, listH / ROW_H);
  if (selectedIndex < listScroll) listScroll = selectedIndex;
  if (selectedIndex >= listScroll + rows) listScroll = selectedIndex - rows + 1;
  listScroll = Math.max(0, Math.min(listScroll, maxListScroll()));
 }

 private int maxListScroll() {
  return Math.max(0, filteredEntries.size() - Math.max(1, listH / ROW_H));
 }

 private int maxBodyScroll() {
  return Math.max(0, contentHeight - scrollH);
 }

 @Override
 public boolean mouseClicked(double rawX, double rawY, int button) {
  double x = rawX / uiScale;
  double y = rawY / uiScale;

  // Clear search button
  int sx = left + GUI_W - 270, sy = top + 5, sw = 136, sh = 16;
  if (!searchBox.getValue().isEmpty() && x >= sx + sw - 12 && x < sx + sw && y >= sy && y < sy + sh) {
   searchBox.setValue("");
   return true;
  }

  // Category Tabs
  int count = GuideBook.CATEGORIES.length;
  int gap = 4;
  int tabW = (GUI_W - 20 - gap * (count - 1)) / count;
  if (y >= tabY && y < tabY + 22) {
   for (int i = 0; i < count; i++) {
    int tx = left + 10 + i * (tabW + gap);
    if (x >= tx && x < tx + tabW) {
     selectCategory(i);
     return true;
    }
   }
  }

  // Roster List Click
  if (x >= listX && x < listX + listW && y >= listY && y < listY + listH) {
   int clicked = listScroll + (int) ((y - listY) / ROW_H);
   if (clicked >= 0 && clicked < filteredEntries.size()) {
    selectEntry(clicked);
    return true;
   }
  }

  // Variant Selector Strip Click
  GuideBook.Entry entry = currentEntry();
  if (entry != null && entry.variants != null) {
   ItemStack[] vars = entry.variants.get();
   int vxStart = bodyX + 100;
   int vy = bodyY + 48;
   if (y >= vy && y < vy + 18) {
    for (int v = 0; v < vars.length; v++) {
     int vx = vxStart + v * 20;
     if (x >= vx && x < vx + 18) {
      selectedVariant = v;
      drawnEntryKey = -1;
      return true;
     }
    }
   }
  }

  return super.mouseClicked(x, y, button);
 }

 @Override
 public boolean mouseReleased(double x, double y, int b) {
  return super.mouseReleased(x / uiScale, y / uiScale, b);
 }

 @Override
 public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
  return super.mouseDragged(x / uiScale, y / uiScale, b, dx / uiScale, dy / uiScale);
 }

 @Override
 public boolean mouseScrolled(double rawX, double rawY, double delta) {
  double x = rawX / uiScale;
  int dir = delta > 0 ? 1 : -1;
  if (x >= listX && x < listX + listW + 6) {
   listScroll = Math.max(0, Math.min(maxListScroll(), listScroll - dir));
  } else {
   bodyScroll = Math.max(0, Math.min(maxBodyScroll(), bodyScroll - dir * 22));
  }
  return true;
 }

 @Override
 public boolean keyPressed(int key, int scan, int mods) {
  if (searchBox != null && searchBox.isFocused()) {
   if (key == 256) { // Escape unfocuses search first if focused
    searchBox.setFocused(false);
    return true;
   }
   return searchBox.keyPressed(key, scan, mods) || searchBox.canConsumeInput();
  }
  if (key == 265) { // Up
   stepEntry(-1);
   return true;
  }
  if (key == 264) { // Down
   stepEntry(1);
   return true;
  }
  if (key == 263) { // Left
   selectCategory(activeCategory - 1);
   return true;
  }
  if (key == 262) { // Right
   selectCategory(activeCategory + 1);
   return true;
  }
  if (key == 266) { // Page Up
   bodyScroll = Math.max(0, bodyScroll - 64);
   return true;
  }
  if (key == 267) { // Page Down
   bodyScroll = Math.min(maxBodyScroll(), bodyScroll + 64);
   return true;
  }
  if (ClientSetup.OPEN.matches(key, scan)) {
   com.waldron.adventurerspack.Network.CHANNEL.sendToServer(new com.waldron.adventurerspack.Network.Action(0));
   return true;
  }
  if (minecraft != null && minecraft.options.keyInventory.matches(key, scan)) {
   if (com.waldron.adventurerspack.PackClientConfig.VANILLA_INVENTORY.get()) ClientEvents.openVanilla(minecraft);
   else com.waldron.adventurerspack.Network.CHANNEL.sendToServer(new com.waldron.adventurerspack.Network.Action(0));
   return true;
  }
  return super.keyPressed(key, scan, mods);
 }

 private record SectionBlock(String heading, List<List<String>> wrappedBullets) {}
}
