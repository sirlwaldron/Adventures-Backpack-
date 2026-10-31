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

public class RequiemScreen extends Screen {
    private final int slot, expected;
    private final long openTime;
    private int hover = -1;
    private boolean cancelled = false;

    private static final String[] LABELS = new String[]{"Lance", "Pickaxe", "Axe", "Shovel", "Hoe", "Sword", "Double spear", "Shortbow", "Recurve", "Greatbow"};
    private static final String[] HINTS = new String[]{"Throw & recall", "Mine & harvest", "Cleave & chop", "Excavate & flatten", "Till & cultivate", "Sweeping strikes", "Extended reach", "3-arrow spread", "Piercing shot", "Heavy knockback"};
    private static final String[] CATEGORIES = new String[]{"[ MELEE ]", "[ HARVEST ]", "[ HARVEST ]", "[ HARVEST ]", "[ HARVEST ]", "[ MELEE ]", "[ MELEE ]", "[ RANGED ]", "[ RANGED ]", "[ RANGED ]"};
    private static final int[] CAT_COLORS = new int[]{0xFFFF5555, 0xFFFFD38A, 0xFF55FF55, 0xFFFFAA44, 0xFF88FF55, 0xFFFF6060, 0xFFFF7799, 0xFF55DDFF, 0xFF70B8FF, 0xFFAA88FF};

    public RequiemScreen(int slot, int expected) {
        super(Component.literal("Endgame forms"));
        this.slot = slot;
        this.expected = expected;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null || Requiem.form(minecraft.player.getInventory().getItem(slot)) != expected || (slot != 40 && minecraft.player.getInventory().selected != slot)) {
            onClose();
        }
    }

    private double radius() {
        return Math.min(68.0, Math.min(width * 0.28, height * 0.28));
    }

    private int pick(double x, double y) {
        double dx = x - width / 2.0;
        double dy = y - height / 2.0;
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < 22.0 || r > radius() + 28.0) return -1;
        double step = Math.PI * 2.0 / Requiem.WHEEL_ORDER.length;
        return Math.floorMod((int) Math.floor((Math.atan2(dy, dx) + Math.PI / 2.0 + step / 2.0) / step), Requiem.WHEEL_ORDER.length);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        if (System.currentTimeMillis() - openTime > 250 && !isKeyDown(ClientSetup.FORMS)) {
            finishAndClose();
            return;
        }

        int cx = width / 2;
        int cy = height / 2;
        double radius = radius();
        hover = pick(mx, my);

        // --- Cool Backdrop: Frosted void disc ---
        int rBack = (int) Math.round(radius + 24);
        g.fill(cx - rBack, cy - rBack, cx + rBack, cy + rBack, 0x550A0B12);
        g.fill(cx - rBack + 6, cy - rBack + 6, cx + rBack - 6, cy + rBack - 6, 0x880E1018);
        g.fill(cx - rBack + 12, cy - rBack + 12, cx + rBack - 12, cy + rBack - 12, 0xBB11131C);

        // Orbital ring with sector ticks
        for (int step = 0; step < 40; step++) {
            double a = step * Math.PI / 20.0;
            int px = cx + (int) Math.round(Math.cos(a) * radius);
            int py = cy + (int) Math.round(Math.sin(a) * radius);
            g.fill(px, py, px + 1, py + 1, (step % 4 == 0) ? 0xFFFFD38A : 0x408A6699);
        }

        // --- 10 Form Nodes ---
        for (int i = 0; i < Requiem.WHEEL_ORDER.length; ++i) {
            int form = Requiem.WHEEL_ORDER[i];
            double a = (double) i * Math.PI * 2.0 / Requiem.WHEEL_ORDER.length - Math.PI / 2.0;
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int y = cy + (int) Math.round(Math.sin(a) * radius);
            boolean isHovered = (i == hover);
            boolean isCurrent = (form == expected);

            // Node plate (24x24)
            if (isHovered) {
                g.fill(x - 13, y - 13, x + 13, y + 13, 0xD0781228);
                g.renderOutline(x - 14, y - 14, 28, 28, 0x80FF7088);
                g.renderOutline(x - 13, y - 13, 26, 26, 0xFFFFDF70);
            } else {
                g.fill(x - 12, y - 12, x + 12, y + 12, 0xCC11141C);
                g.renderOutline(x - 12, y - 12, 24, 24, isCurrent ? 0xFFFFD38A : 0x60526072);
            }

            // Item render with hover scale pop
            ItemStack itemStack = new ItemStack(Requiem.item(form));
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0F);
            float scale = isHovered ? 1.35F : 1.1F;
            g.pose().scale(scale, scale, 1.0F);
            g.renderItem(itemStack, -8, -8);
            g.pose().popPose();

            int num = (i == 9 ? 0 : i + 1);
            g.drawString(font, String.valueOf(num), x + 5, y - 12, isHovered ? 0xFFFFFFFF : 0x70A0B0C0, false);
        }

        // --- Center Core: Translucent Gem HUD ---
        g.fill(cx - 28, cy - 22, cx + 28, cy + 20, 0xEE0E1016);
        g.renderOutline(cx - 28, cy - 22, 56, 42, hover >= 0 ? 0xFFFFD38A : 0x60505A68);

        if (hover >= 0) {
            g.drawCenteredString(font, CATEGORIES[hover], cx, cy - 18, CAT_COLORS[hover]);
            g.drawCenteredString(font, "§6§l" + LABELS[hover], cx, cy - 5, 0xFFFFFFFF);
            g.drawCenteredString(font, "§d" + HINTS[hover], cx, cy + 7, 0xFFE0A0FF);
        } else {
            g.drawCenteredString(font, "[ ENDGAME ]", cx, cy - 18, 0xFFE088FF);
            g.drawCenteredString(font, "Choose Form", cx, cy - 5, 0xFFE4E8ED);
            g.drawCenteredString(font, "§810 Bound Forms", cx, cy + 7, 0xFF888888);
        }

        g.drawCenteredString(font, "Release " + ClientSetup.FORMS.getTranslatedKeyMessage().getString() + " to transform · Click or 1–9, 0", cx, cy + (int)radius + 36, 0xBECAD5);
    }

    private void choose(int index) {
        if (index >= 0 && index < Requiem.WHEEL_ORDER.length) {
            Network.CHANNEL.sendToServer(new Network.WeaponForm(slot, expected, Requiem.WHEEL_ORDER[index]));
        }
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void finishAndClose() {
        if (!cancelled && hover >= 0) {
            choose(hover);
            return;
        }
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            int target = pick(x, y);
            if (target >= 0) {
                choose(target);
            }
            return true;
        } else if (button == 1) {
            cancelled = true;
            onClose();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ClientSetup.FORMS.matches(keyCode, scanCode)) {
            finishAndClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (ClientSetup.FORMS.matchesMouse(button)) {
            finishAndClose();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            choose(key - GLFW.GLFW_KEY_1);
            return true;
        } else if (key == GLFW.GLFW_KEY_0) {
            choose(9);
            return true;
        } else if (ClientSetup.FORMS.matches(key, scan)) {
            if (System.currentTimeMillis() - openTime > 250) {
                finishAndClose();
            } else {
                onClose();
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
