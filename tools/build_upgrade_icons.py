"""Generates standalone 32x32 pixel-art icons for all 19 backpack upgrades and pouches.

Replaces the old square card frames with distinct, shaded 32x32 item/rune sprites
matching the mod's ingots and armor icons.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ITEM_TEX = ROOT / "src/main/resources/assets/adventurerspack/textures/item"
UPGRADE_TEX = ITEM_TEX / "upgrades"
PREVIEW_PATH = ROOT / "art/backpack-1.7.16/upgrade-icons.png"


def new_canvas():
    return Image.new("RGBA", (32, 32), (0, 0, 0, 0))


def px(img, x, y, color):
    if 0 <= x < 32 and 0 <= y < 32:
        if len(color) == 3:
            color = (*color, 255)
        img.putpixel((int(x), int(y)), color)


def fill_rect(img, x0, y0, x1, y1, color):
    for y in range(int(y0), int(y1) + 1):
        for x in range(int(x0), int(x1) + 1):
            px(img, x, y, color)


def fill_Waiting_circle(img, cx, cy, r, color):
    for y in range(32):
        for x in range(32):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px(img, x, y, color)


def fill_ellipse(img, cx, cy, rx, ry, color):
    for y in range(32):
        for x in range(32):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                px(img, x, y, color)


def add_outline(img, outline_color=(16, 14, 20, 255)):
    src = img.copy()
    for y in range(32):
        for x in range(32):
            if src.getpixel((x, y))[3] == 0:
                for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 32 and 0 <= ny < 32 and src.getpixel((nx, ny))[3] > 180:
                        px(img, x, y, outline_color)
                        break


def shade_stone_tablet(img, points, base=(38, 42, 54), rim=(198, 156, 78), rim_hi=(246, 214, 132), rim_sh=(116, 82, 36)):
    d = ImageDraw.Draw(img)
    d.polygon(points, fill=(*rim, 255))
    # Inner stone polygon shrunk by 2px toward center (15.5, 15.5)
    inner = []
    for x, y in points:
        ix = round(15.5 + (x - 15.5) * 0.80)
        iy = round(15.5 + (y - 15.5) * 0.80)
        inner.append((ix, iy))
    d.polygon(inner, fill=(*base, 255))
    # Shading on stone & gold rim
    for y in range(32):
        for x in range(32):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            bias = (15.5 - x) * 1.4 + (15.5 - y) * 1.6
            if (r, g, b) == rim:
                if bias > 10:
                    px(img, x, y, rim_hi)
                elif bias < -10:
                    px(img, x, y, rim_sh)
            elif (r, g, b) == base:
                delta = int(max(-16, min(20, bias * 0.7)))
                px(img, x, y, (max(0, min(255, r + delta)), max(0, min(255, g + delta)), max(0, min(255, b + delta))))
    add_outline(img, (15, 13, 20, 255))


def draw_glow_points(img, coords, core_col, glow_col):
    for x, y in coords:
        for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nx, ny = x + dx, y + dy
            if 0 <= nx < 32 and 0 <= ny < 32:
                cr, cg, cb, ca = img.getpixel((nx, ny))
                if ca > 0 and (cr, cg, cb) != core_col:
                    px(img, nx, ny, (
                        (cr + glow_col[0]) // 2,
                        (cg + glow_col[1]) // 2,
                        (cb + glow_col[2]) // 2,
                    ))
    for x, y in coords:
        px(img, x, y, core_col)


def make_pickup_rune():
    img = new_canvas()
    pts = [(16, 3), (26, 8), (26, 23), (16, 28), (6, 23), (6, 8)]
    shade_stone_tablet(img, pts, base=(34, 42, 56), rim=(206, 164, 82), rim_hi=(248, 220, 140), rim_sh=(122, 86, 38))
    # Corner gold rivets
    for rx, ry in [(16, 5), (24, 9), (24, 22), (16, 26), (8, 22), (8, 9)]:
        px(img, rx, ry, (255, 236, 175))
    # Glowing cyan/violet gravity pull arrow + tray glyph
    glyph = []
    for y in range(8, 20):
        glyph += [(15, y), (16, y)]
    for i in range(1, 5):
        glyph += [(15 - i, 19 - i), (15 - i, 18 - i), (16 + i, 19 - i), (16 + i, 18 - i)]
    for x in range(11, 21):
        glyph.append((x, 22))
    glyph += [(11, 20), (11, 21), (20, 20), (20, 21)]
    draw_glow_points(img, glyph, (130, 246, 255), (56, 168, 220))
    px(img, 15, 9, (235, 255, 255))
    px(img, 16, 9, (235, 255, 255))
    return img


def make_gathering_rune():
    img = new_canvas()
    # Horseshoe lodestone magnet angled/upright with gold bands and glowing magnetic arcs
    for y in range(7, 27):
        for x in range(6, 26):
            # U-shape arch at bottom (y >= 17), two arms at top (y < 17)
            in_arm_l = 7 <= x <= 12 and 8 <= y <= 18
            in_arm_r = 19 <= x <= 24 and 8 <= y <= 18
            dist = ((x - 15.5) ** 2 + (y - 17.5) ** 2) ** 0.5
            in_arch = y >= 16 and 4.2 <= dist <= 9.6
            if in_arm_l or in_arm_r or in_arch:
                if y <= 11:
                    # Polarized tips: left crimson-red, right cyan-blue
                    col = (224, 54, 72) if x < 16 else (62, 196, 240)
                    if (x in (7, 8, 19, 20)) and y <= 10:
                        col = (255, 118, 132) if x < 16 else (150, 238, 255)
                elif y in (12, 13):
                    col = (232, 186, 92) if x % 2 == 0 else (184, 134, 52)
                else:
                    shade = int((15.5 - x) * 1.5 + (17 - y) * 1.2)
                    col = (max(24, min(115, 64 + shade)), max(28, min(122, 70 + shade)), max(38, min(138, 86 + shade)))
                px(img, x, y, col)
    add_outline(img, (16, 14, 22, 255))
    # Glowing magnetic lightning/wave arcs at top between poles
    arcs = [(14, 6), (15, 5), (16, 5), (17, 6), (13, 9), (14, 8), (17, 8), (18, 9), (15, 11), (16, 11)]
    draw_glow_points(img, arcs, (232, 140, 255), (146, 68, 210))
    px(img, 15, 5, (255, 230, 255))
    px(img, 16, 5, (255, 230, 255))
    return img


def make_deep_pockets_rune():
    img = new_canvas()
    pts = [(16, 2), (29, 15), (16, 29), (3, 15)]
    shade_stone_tablet(img, pts, base=(40, 30, 56), rim=(218, 174, 84), rim_hi=(255, 230, 148), rim_sh=(128, 90, 38))
    # Nested dimensional portal squares
    outer_portal = []
    for i in range(10, 22):
        outer_portal += [(i, 10), (i, 21), (10, i), (21, i)]
    inner_portal = []
    for i in range(13, 19):
        inner_portal += [(i, 13), (i, 18), (13, i), (18, i)]
    draw_glow_points(img, outer_portal, (194, 122, 255), (112, 54, 175))
    draw_glow_points(img, inner_portal, (255, 216, 112), (205, 138, 52))
    fill_rect(img, 15, 15, 16, 16, (255, 250, 210))
    return img


def make_restock_rune():
    img = new_canvas()
    pts = [(11, 3), (20, 3), (28, 11), (28, 20), (20, 28), (11, 28), (3, 20), (3, 11)]
    shade_stone_tablet(img, pts, base=(28, 46, 42), rim=(204, 162, 78), rim_hi=(248, 218, 136), rim_sh=(118, 84, 36))
    # Two cycling emerald/gold arrows around center
    top_arrow = [(11, 12), (12, 10), (13, 9), (14, 9), (15, 9), (16, 9), (17, 9), (18, 10), (19, 11), (20, 12),
                 (20, 9), (20, 10), (20, 11), (18, 12), (19, 12)]
    bot_arrow = [(20, 19), (19, 21), (18, 22), (17, 22), (16, 22), (15, 22), (14, 22), (13, 21), (12, 20), (11, 19),
                 (11, 22), (11, 21), (11, 20), (13, 19), (12, 19)]
    draw_glow_points(img, top_arrow + bot_arrow, (116, 248, 168), (44, 164, 98))
    fill_rect(img, 15, 15, 16, 16, (255, 228, 132))
    return img


def make_void_sigil():
    img = new_canvas()
    # 8-point spiked obsidian & crimson-gold star sigil with swirling abyssal vortex
    pts = [(16, 2), (20, 7), (26, 6), (24, 12), (29, 16), (24, 20), (26, 26), (20, 24),
           (16, 29), (12, 24), (6, 26), (8, 20), (3, 16), (8, 12), (6, 6), (12, 7)]
    shade_stone_tablet(img, pts, base=(24, 16, 36), rim=(178, 46, 76), rim_hi=(242, 98, 124), rim_sh=(92, 18, 38))
    # Dark void core + swirling magenta/violet spiral arms
    fill_Waiting_circle(img, 15.5, 15.5, 5.2, (10, 6, 18))
    spiral = [
        (15, 10), (16, 10), (18, 11), (19, 12), (20, 14),
        (21, 16), (20, 18), (19, 19), (17, 20), (15, 21),
        (13, 20), (12, 19), (11, 17), (10, 15), (11, 13), (12, 12),
        (14, 13), (17, 14), (17, 17), (14, 17)
    ]
    draw_glow_points(img, spiral, (216, 112, 255), (128, 38, 185))
    px(img, 15, 15, (255, 210, 255))
    px(img, 16, 16, (180, 70, 240))
    return img


def draw_pouch_body(img, main_col, dark_col, hi_col, trim_col, emblem_fn, top_fn):
    # Plump rounded drawstring pouch body (y=10..27, x=6..25)
    for y in range(10, 28):
        half_w = 6.5 if y < 13 else min(10.0, 6.2 + (y - 12) * 0.42 - max(0, y - 23) * 0.9)
        for x in range(32):
            dx = x - 15.5
            if abs(dx) <= half_w:
                if y in (12, 13):
                    # Drawstring collar
                    col = (232, 188, 92) if (x + y) % 2 == 0 else (168, 118, 42)
                elif y == 26 or abs(dx) >= half_w - 1.1:
                    col = trim_col
                else:
                    light = (14.0 - x) * 1.6 + (17.0 - y) * 1.4
                    if light > 12:
                        col = hi_col
                    elif light < -10:
                        col = dark_col
                    else:
                        col = main_col
                px(img, x, y, col)
    # Top ruffled neck (y=7..11)
    for y in range(7, 12):
        w = 4.5 + (11 - y) * 0.45
        for x in range(32):
            if abs(x - 15.5) <= w:
                px(img, x, y, hi_col if x < 16 else dark_col)
    if top_fn:
        top_fn(img)
    if emblem_fn:
        emblem_fn(img)
    add_outline(img, (18, 13, 10, 255))


def make_provision_pouch():
    img = new_canvas()
    def top_items(im):
        # Golden apple & bread peeking out top
        fill_Waiting_circle(im, 13.5, 7.5, 2.8, (246, 202, 72))
        px(im, 12, 6, (255, 244, 170))
        px(im, 14, 4, (92, 184, 64))
        fill_ellipse(im, 18.0, 8.0, 3.2, 2.2, (214, 148, 72))
        px(im, 17, 7, (244, 194, 118))
    def emblem(im):
        # Brass buckle & ruby heart/ration crest on front
        fill_rect(im, 13, 17, 18, 22, (224, 176, 78))
        fill_rect(im, 14, 18, 17, 21, (216, 48, 68))
        px(im, 14, 18, (255, 138, 152))
    draw_pouch_body(
        img,
        main_col=(156, 94, 52),
        dark_col=(108, 60, 32),
        hi_col=(196, 128, 76),
        trim_col=(82, 44, 22),
        emblem_fn=emblem,
        top_fn=top_items,
    )
    return img


def make_forager_pouch():
    img = new_canvas()
    def top_items(im):
        # Golden wheat stalks & red berries peeking from pouch
        for dx, dy in [(-3, -2), (-2, -3), (-1, -4), (1, -3), (2, -4), (3, -2)]:
            px(im, 15 + dx, 9 + dy, (244, 208, 88))
        px(im, 12, 8, (224, 52, 64))
        px(im, 18, 8, (224, 52, 64))
    def emblem(im):
        # Golden leaf / sprout emblem
        leaf = [(15, 23), (15, 22), (15, 21), (16, 20), (16, 19), (16, 18),
                (13, 20), (14, 20), (14, 19), (17, 19), (18, 18), (17, 18), (15, 17), (16, 17)]
        for x, y in leaf:
            px(im, x, y, (238, 206, 96))
        px(im, 15, 18, (255, 242, 168))
    draw_pouch_body(
        img,
        main_col=(64, 126, 62),
        dark_col=(40, 86, 40),
        hi_col=(96, 164, 88),
        trim_col=(128, 86, 46),
        emblem_fn=emblem,
        top_fn=top_items,
    )
    return img


def make_gem_pouch():
    img = new_canvas()
    def top_items(im):
        # Sparkling diamond (cyan), emerald (green), ruby (red) peeking out top
        fill_Waiting_circle(im, 12.5, 8.0, 2.2, (88, 236, 255))
        px(im, 12, 7, (225, 255, 255))
        fill_Waiting_circle(im, 16.0, 7.2, 2.3, (76, 232, 118))
        px(im, 15, 6, (210, 255, 220))
        fill_Waiting_circle(im, 19.2, 8.2, 2.1, (242, 58, 88))
        px(im, 19, 7, (255, 180, 195))
    def emblem(im):
        # Faceted diamond emblem in gold setting
        pts = [(15, 16), (16, 16), (14, 17), (17, 17), (13, 18), (18, 18),
               (13, 19), (18, 19), (14, 20), (17, 20), (15, 21), (16, 21)]
        for x, y in pts:
            px(im, x, y, (238, 192, 86))
        for y in range(17, 21):
            for x in range(14, 18):
                if (x, y) not in pts:
                    px(im, x, y, (108, 240, 255))
        px(im, 15, 17, (235, 255, 255))
    draw_pouch_body(
        img,
        main_col=(52, 76, 148),
        dark_col=(32, 48, 102),
        hi_col=(82, 114, 196),
        trim_col=(214, 168, 74),
        emblem_fn=emblem,
        top_fn=top_items,
    )
    return img


def make_cartographer_pouch():
    img = new_canvas()
    def top_items(im):
        # Rolled parchment map scroll sticking out diagonally at top
        for x in range(10, 22):
            px(im, x, 7, (244, 228, 184))
            px(im, x, 8, (222, 198, 146))
            px(im, x, 9, (196, 168, 116))
        # Red ribbon band on scroll
        fill_rect(im, 15, 7, 16, 9, (212, 48, 58))
    def emblem(im):
        # Brass compass rose on front
        fill_Waiting_circle(im, 15.5, 19.5, 4.2, (222, 176, 78))
        fill_Waiting_circle(im, 15.5, 19.5, 2.8, (32, 42, 56))
        for d in [-2, -1, 0, 1, 2]:
            px(im, 15 + d, 19, (245, 218, 130))
            px(im, 15, 19 + d, (245, 218, 130))
        px(im, 15, 17, (236, 64, 72))  # North needle
        px(im, 15, 19, (255, 250, 210))
    draw_pouch_body(
        img,
        main_col=(178, 132, 82),
        dark_col=(126, 86, 48),
        hi_col=(216, 168, 112),
        trim_col=(92, 58, 30),
        emblem_fn=emblem,
        top_fn=top_items,
    )
    return img


def make_crafting_kit():
    img = new_canvas()
    # Portable oak & brass-cornered 3x3 crafting board (x=5..26, y=6..26)
    fill_rect(img, 5, 6, 26, 26, (104, 64, 34))
    fill_rect(img, 6, 7, 25, 25, (168, 114, 62))
    # Brass corner brackets
    for cx, cy in [(5, 6), (23, 6), (5, 23), (23, 23)]:
        fill_rect(img, cx, cy, cx + 3, cy + 3, (232, 186, 84))
        px(img, cx + 1, cy + 1, (255, 236, 164))
    # 3x3 crafting grid squares
    for gx in range(3):
        for gy in range(3):
            sx = 8 + gx * 6
            sy = 9 + gy * 5
            fill_rect(img, sx, sy, sx + 4, sy + 3, (214, 162, 98))
            fill_rect(img, sx, sy, sx + 4, sy, (238, 194, 132))
            fill_rect(img, sx, sy + 3, sx + 4, sy + 3, (128, 82, 42))
    # Leather handle at top
    fill_rect(img, 12, 3, 19, 5, (122, 72, 38))
    fill_rect(img, 13, 4, 18, 4, (176, 116, 64))
    px(img, 12, 5, (238, 192, 88))
    px(img, 19, 5, (238, 192, 88))
    add_outline(img, (18, 12, 8, 255))
    return img


def make_ember_core():
    img = new_canvas()
    # Forged dark-iron & brass smelter furnace core with roaring magma flame inside
    pts = [(10, 4), (21, 4), (26, 9), (26, 24), (21, 28), (10, 28), (5, 24), (5, 9)]
    shade_stone_tablet(img, pts, base=(52, 56, 68), rim=(214, 152, 64), rim_hi=(252, 212, 118), rim_sh=(124, 76, 28))
    # Inner firebox chamber
    fill_rect(img, 9, 9, 22, 23, (22, 18, 24))
    # Roaring flame layers (crimson -> orange -> gold -> white-hot core)
    fill_ellipse(img, 15.5, 18.5, 5.8, 5.2, (212, 42, 38))
    fill_ellipse(img, 15.5, 19.0, 4.4, 4.2, (248, 124, 32))
    fill_ellipse(img, 15.5, 19.8, 2.8, 3.0, (255, 222, 76))
    fill_ellipse(img, 15.5, 20.5, 1.5, 1.8, (255, 252, 210))
    # Flame tongues at top
    for fx, fy, col in [(13, 12, (248, 124, 32)), (15, 10, (255, 180, 48)), (16, 11, (255, 222, 76)), (18, 12, (248, 124, 32))]:
        px(img, fx, fy, col)
        px(img, fx, fy + 1, (255, 222, 76))
    # Dark iron grate bars across front
    for gx in [11, 15, 20]:
        for gy in range(15, 24):
            px(img, gx, gy, (42, 46, 56))
    return img


def make_ender_chest():
    img = new_canvas()
    # Miniature obsidian & gold-trimmed Ender Chest with glowing Eye of Ender latch
    # Lid (y=6..13) and base (y=14..26)
    fill_rect(img, 5, 6, 26, 26, (22, 36, 42))
    fill_rect(img, 6, 7, 25, 12, (38, 68, 74))
    fill_rect(img, 6, 15, 25, 25, (30, 54, 60))
    # Top bevel highlight & gold rim seam
    fill_rect(img, 6, 7, 25, 8, (64, 112, 118))
    fill_rect(img, 5, 13, 26, 14, (218, 174, 76))
    # Gold corner studs
    for cx, cy in [(6, 7), (24, 7), (6, 24), (24, 24)]:
        fill_rect(img, cx, cy, cx + 1, cy + 1, (242, 204, 102))
    # Glowing Eye of Ender front clasp
    fill_rect(img, 13, 11, 18, 17, (238, 194, 84))
    fill_rect(img, 14, 12, 17, 16, (96, 244, 196))
    fill_rect(img, 15, 13, 16, 15, (18, 42, 38))
    px(img, 14, 12, (220, 255, 240))
    # Floating ender portal sparkles
    for sx, sy in [(8, 4), (23, 4), (4, 18), (27, 17)]:
        px(img, sx, sy, (206, 108, 255))
    add_outline(img, (12, 16, 20, 255))
    return img


def make_builders_belt():
    img = new_canvas()
    # Heavy stitched leather tool belt with ornate gold buckle & hanging steel trowel
    # Belt strap across middle (y=11..20, x=3..28)
    for y in range(11, 21):
        for x in range(4, 28):
            curve = int(abs(x - 15.5) * 0.18)
            yy = y + curve
            if 11 <= yy <= 21:
                if y in (11, 20):
                    col = (218, 168, 82) if x % 2 == 0 else (102, 58, 28)
                elif y < 14:
                    col = (188, 122, 68)
                elif y > 18:
                    col = (114, 66, 34)
                else:
                    col = (154, 94, 48)
                px(img, x, yy, col)
    # Side leather tool pouches
    fill_rect(img, 6, 13, 10, 22, (132, 76, 38))
    fill_rect(img, 7, 14, 9, 20, (172, 108, 58))
    fill_rect(img, 21, 13, 25, 22, (132, 76, 38))
    fill_rect(img, 22, 14, 24, 20, (172, 108, 58))
    # Center ornate gold buckle
    fill_rect(img, 12, 9, 19, 22, (238, 192, 84))
    fill_rect(img, 12, 9, 19, 10, (255, 232, 148))
    fill_rect(img, 14, 11, 17, 20, (96, 54, 26))
    fill_rect(img, 15, 11, 16, 20, (214, 164, 68))
    # Steel mason's trowel hanging below buckle
    fill_rect(img, 15, 22, 16, 24, (164, 172, 186))
    for dy, w in enumerate([3, 2, 1, 0]):
        fill_rect(img, 15 - w, 25 + dy, 16 + w, 25 + dy, (206, 214, 228) if dy < 2 else (148, 158, 174))
    add_outline(img, (18, 12, 8, 255))
    return img


def make_tank_upgrade():
    img = new_canvas()
    # Twin brass-capped glass fluid tanks: Left = Aqua Water, Right = Glowing Lava
    for tx, liquid_top, liquid_bot, liquid_hi in [
        (6, (64, 188, 255), (36, 124, 216), (176, 238, 255)),
        (17, (255, 144, 36), (218, 68, 24), (255, 228, 112)),
    ]:
        # Brass top & bottom caps
        fill_rect(img, tx, 5, tx + 8, 7, (232, 186, 84))
        fill_rect(img, tx + 1, 5, tx + 7, 5, (255, 232, 148))
        fill_rect(img, tx, 24, tx + 8, 26, (212, 162, 68))
        # Glass cylinder body
        fill_rect(img, tx + 1, 8, tx + 7, 23, (42, 58, 72))
        # Liquid fill (y=12..23)
        fill_rect(img, tx + 2, 12, tx + 6, 23, liquid_bot)
        fill_rect(img, tx + 2, 12, tx + 6, 14, liquid_top)
        fill_rect(img, tx + 2, 12, tx + 5, 12, liquid_hi)
        # Glass specular highlight bar on left & gauge ticks on right
        for y in range(9, 23):
            px(img, tx + 1, y, (210, 242, 255))
            px(img, tx + 7, y, (124, 158, 178) if y % 3 != 0 else (232, 186, 84))
    # Connecting brass manifold pipe in the middle
    fill_rect(img, 14, 14, 17, 17, (218, 168, 74))
    fill_rect(img, 15, 15, 16, 16, (255, 232, 148))
    add_outline(img, (14, 16, 22, 255))
    return img


def make_battery_upgrade():
    img = new_canvas()
    # Heavy-duty Copper & Dark-Steel Forge Energy Cell with glowing Cyan/Redstone core
    # Top redstone/gold terminals
    fill_rect(img, 10, 3, 13, 6, (228, 52, 64))
    fill_rect(img, 18, 3, 21, 6, (64, 214, 248))
    # Main battery casing (x=7..24, y=6..27)
    fill_rect(img, 7, 6, 24, 27, (52, 58, 72))
    # Copper top & bottom bands
    fill_rect(img, 7, 6, 24, 9, (218, 128, 68))
    fill_rect(img, 8, 6, 23, 7, (248, 176, 112))
    fill_rect(img, 7, 24, 24, 27, (188, 104, 52))
    # Side cooling fins
    for y in range(11, 23, 3):
        fill_rect(img, 6, y, 7, y + 1, (148, 158, 176))
        fill_rect(img, 24, y, 25, y + 1, (148, 158, 176))
    # Central glowing energy core window + lightning bolt
    fill_rect(img, 11, 10, 20, 23, (22, 32, 42))
    fill_rect(img, 12, 11, 19, 22, (48, 198, 224))
    fill_rect(img, 13, 12, 18, 21, (112, 248, 255))
    # Bright golden-white lightning glyph in center
    bolt = [(16, 12), (15, 13), (14, 14), (13, 15), (14, 15), (15, 15), (16, 15), (17, 15),
            (15, 16), (16, 16), (17, 16), (16, 17), (15, 18), (14, 19), (13, 20)]
    for x, y in bolt:
        px(img, x, y, (255, 252, 190))
    add_outline(img, (14, 16, 22, 255))
    return img


def make_xp_storage_upgrade():
    img = new_canvas()
    # Ornate gold & dark-iron reliquary orb glowing with bright emerald/lime XP essence
    fill_Waiting_circle(img, 15.5, 15.5, 10.2, (214, 168, 72))
    fill_Waiting_circle(img, 15.5, 15.5, 8.4, (32, 54, 38))
    fill_Waiting_circle(img, 15.5, 15.5, 7.2, (76, 216, 68))
    fill_Waiting_circle(img, 14.8, 14.8, 5.2, (158, 252, 96))
    fill_Waiting_circle(img, 13.8, 13.5, 2.5, (238, 255, 188))
    # Gold meridian bands & top/bottom finials
    for i in range(6, 26):
        px(img, i, 15, (238, 194, 88))
        px(img, i, 16, (176, 128, 48))
    fill_rect(img, 14, 3, 17, 6, (244, 204, 98))
    fill_rect(img, 12, 25, 19, 27, (214, 168, 72))
    add_outline(img, (14, 20, 14, 255))
    return img


def make_quiver():
    img = new_canvas()
    # Diagonal stitched leather & brass quiver with feathered arrows & firework rocket
    # Fletched arrows & firework sticking out top-right (x=14..27, y=3..15)
    # Firework rocket (red/white stripes + cone)
    for i in range(7):
        col = (232, 52, 64) if i % 2 == 0 else (246, 242, 236)
        fill_rect(img, 15 + i // 2, 5 + i, 17 + i // 2, 5 + i, col)
    fill_rect(img, 14, 3, 17, 4, (224, 44, 54))
    # Two feathered arrows
    for ax, ay in [(21, 4), (25, 8)]:
        for s in range(8):
            px(img, ax - s, ay + s, (196, 148, 82))
        for f in range(3):
            px(img, ax - f, ay + f - 1, (245, 248, 255))
            px(img, ax - f + 1, ay + f, (216, 224, 240))
    # Diagonal leather quiver tube from bottom-left (6, 26) to mid-right (21, 11)
    for t in range(18):
        cx = 7 + t * 0.72
        cy = 25 - t * 0.72
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                if dx * dx + dy * dy <= 11:
                    x, y = int(round(cx + dx)), int(round(cy + dy))
                    if t >= 15:
                        col = (238, 192, 86) if dx + dy < 0 else (178, 128, 48)
                    elif t in (7, 8):
                        col = (218, 168, 74)
                    else:
                        col = (186, 118, 64) if dx + dy < -1 else (112, 62, 32) if dx + dy > 2 else (148, 88, 44)
                    px(img, x, y, col)
    add_outline(img, (18, 12, 8, 255))
    return img


def make_philter():
    img = new_canvas()
    # Brass & leather 3-vial potion rack holding Ruby, Violet, and Cyan potion phials
    vials = [
        (5, (238, 56, 88), (176, 28, 56), (255, 164, 184)),
        (13, (196, 84, 248), (132, 42, 186), (236, 178, 255)),
        (21, (68, 218, 248), (36, 148, 196), (184, 246, 255)),
    ]
    for vx, c_top, c_bot, c_hi in vials:
        # Cork stopper
        fill_rect(img, vx + 1, 5, vx + 4, 7, (198, 146, 84))
        # Glass rim & body
        fill_rect(img, vx, 8, vx + 5, 9, (220, 242, 255))
        fill_rect(img, vx, 10, vx + 5, 25, (52, 68, 84))
        # Glowing potion liquid
        fill_rect(img, vx + 1, 13, vx + 4, 24, c_bot)
        fill_rect(img, vx + 1, 13, vx + 4, 16, c_top)
        px(img, vx + 1, 13, c_hi)
        px(img, vx + 2, 13, c_hi)
        # Glass highlight
        for y in range(10, 24):
            px(img, vx + 1, y, c_hi if y >= 13 else (228, 246, 255))
    # Brass & leather holder strap across all 3 vials
    fill_rect(img, 3, 18, 28, 21, (142, 84, 42))
    fill_rect(img, 3, 18, 28, 18, (232, 186, 84))
    fill_rect(img, 3, 21, 28, 21, (212, 162, 68))
    add_outline(img, (16, 14, 20, 255))
    return img


def make_alembic():
    img = new_canvas()
    # Copper & brass distillation retort flask + condenser coil + receiving vial
    # Burner stand & blaze flame at bottom-left
    fill_rect(img, 5, 24, 17, 26, (92, 98, 112))
    fill_ellipse(img, 11.0, 23.0, 3.2, 2.0, (255, 164, 42))
    px(img, 11, 23, (255, 242, 156))
    # Main copper/glass boiling bulb (center=11, 16, r=6)
    fill_Waiting_circle(img, 11.0, 16.0, 6.0, (210, 238, 252))
    fill_Waiting_circle(img, 11.0, 16.0, 4.8, (54, 68, 86))
    # Bubbling violet potion inside bulb
    for y in range(15, 21):
        for x in range(7, 16):
            if (x - 11.0) ** 2 + (y - 16.0) ** 2 <= 22:
                px(img, x, y, (214, 92, 252) if y <= 16 else (156, 48, 204))
    px(img, 9, 14, (246, 204, 255))
    px(img, 12, 13, (246, 204, 255))
    # Copper dome & curved swan-neck condenser tube over to right receiving phial
    fill_rect(img, 9, 7, 13, 11, (224, 142, 74))
    for i in range(12):
        tx = 12 + i
        ty = 7 + (i * i) // 16
        fill_rect(img, tx, ty, tx + 1, ty + 2, (238, 172, 92) if i < 6 else (206, 126, 62))
    # Receiving phial on right (x=21..26, y=16..26)
    fill_rect(img, 21, 16, 26, 26, (210, 238, 252))
    fill_rect(img, 22, 19, 25, 25, (224, 96, 255))
    fill_rect(img, 20, 25, 27, 26, (224, 176, 78))
    add_outline(img, (16, 14, 20, 255))
    return img


def draw_ingot_base(img, top_col, top_hi, front_col, side_col, deep_col, outline_col=(18, 12, 14, 255)):
    """Draws a crisp 32x32 angled 3D ingot bar with top face, front bevel, left/right bevels."""
    d = ImageDraw.Draw(img)
    # Top face polygon
    top_poly = [(9, 9), (25, 9), (22, 16), (6, 16)]
    # Front-bottom face
    front_poly = [(6, 16), (22, 16), (20, 22), (4, 22)]
    # Right side face
    right_poly = [(25, 9), (27, 15), (20, 22), (22, 16)]
    # Left bevel lip
    left_poly = [(9, 9), (6, 16), (4, 22), (4, 15)]

    d.polygon(left_poly, fill=(*side_col, 255))
    d.polygon(front_poly, fill=(*front_col, 255))
    d.polygon(right_poly, fill=(*deep_col, 255))
    d.polygon(top_poly, fill=(*top_col, 255))

    # Top edge specular ridge & ridge line along y=16
    for x in range(9, 25):
        px(img, x, 9, top_hi)
        if x < 18:
            px(img, x, 10, top_hi)
    for x in range(6, 22):
        px(img, x, 16, top_hi if x < 15 else top_col)
    for x in range(5, 20):
        px(img, x, 21, deep_col)
    add_outline(img, outline_col)


def make_backpack_ingot():
    img = new_canvas()
    draw_ingot_base(
        img,
        top_col=(224, 172, 78),
        top_hi=(255, 236, 154),
        front_col=(168, 114, 44),
        side_col=(196, 142, 62),
        deep_col=(114, 72, 26),
        outline_col=(22, 14, 8, 255),
    )
    # Leather strap & glowing cyan expedition gem wrapped around center of ingot
    for y in range(9, 22):
        sx = 14 - (y - 9) // 4
        fill_rect(img, sx, y, sx + 3, y, (116, 66, 34) if y < 16 else (82, 44, 22))
        px(img, sx, y, (238, 192, 88))
        px(img, sx + 3, y, (238, 192, 88))
    # Glowing cyan gem in center of top face
    fill_rect(img, 14, 12, 16, 14, (84, 242, 255))
    px(img, 14, 12, (230, 255, 255))
    return img


def make_crimson_ingot():
    img = new_canvas()
    draw_ingot_base(
        img,
        top_col=(188, 28, 48),
        top_hi=(255, 108, 124),
        front_col=(118, 14, 30),
        side_col=(152, 20, 38),
        deep_col=(68, 8, 20),
        outline_col=(20, 6, 12, 255),
    )
    # Molten crimson-orange veins & glowing core on top face
    veins = [(11, 12), (12, 12), (13, 13), (14, 13), (15, 12), (16, 12), (17, 13), (18, 14), (19, 13), (20, 12)]
    for x, y in veins:
        px(img, x, y, (255, 168, 56))
    fill_rect(img, 14, 12, 16, 14, (255, 76, 92))
    px(img, 15, 13, (255, 234, 176))
    return img


def make_endgame_ingot():
    img = new_canvas()
    draw_ingot_base(
        img,
        top_col=(242, 196, 72),
        top_hi=(255, 248, 184),
        front_col=(52, 44, 66),
        side_col=(186, 138, 48),
        deep_col=(30, 24, 42),
        outline_col=(14, 10, 22, 255),
    )
    # Gilded trim along bottom dark-netherite bevel + glowing ruby/crimson Endgame crest on top
    for x in range(6, 20):
        px(img, x, 19, (218, 166, 62))
    crest = [(13, 12), (14, 11), (15, 11), (16, 11), (17, 12),
             (12, 13), (13, 13), (14, 13), (15, 13), (16, 13), (17, 13), (18, 13),
             (14, 14), (15, 14), (16, 14), (14, 15), (15, 16)]
    for x, y in crest:
        px(img, x, y, (228, 36, 64))
    px(img, 15, 12, (255, 196, 210))
    px(img, 15, 13, (255, 124, 144))
    return img


def make_camp_kit():
    img = new_canvas()
    # Rolled red-and-cream expedition bedroll with dark leather straps, gold buckles & lantern
    # Main cylinder roll (x=4..27, y=10..23)
    for y in range(10, 24):
        for x in range(4, 28):
            dy = y - 16.5
            if abs(dy) <= 6.2 - (0.8 if x in (4, 27) else 0):
                if x >= 22:
                    # Spiral rolled end on right showing cream blanket & red cloth layers
                    dist = ((x - 24.0) ** 2 + (y - 16.5) ** 2) ** 0.5
                    if dist <= 6.0:
                        col = (242, 226, 186) if int(dist * 1.6) % 2 == 0 else (188, 38, 48)
                        px(img, x, y, col)
                else:
                    # Red wool roll shaded top-to-bottom
                    col = (224, 58, 68) if dy < -2 else (142, 24, 34) if dy > 2.5 else (186, 38, 48)
                    px(img, x, y, col)
    # Two leather straps & gold buckles at x=8..10 and x=17..19
    for sx in [8, 17]:
        fill_rect(img, sx, 9, sx + 2, 24, (98, 56, 28))
        fill_rect(img, sx + 1, 9, sx + 1, 24, (146, 86, 44))
        fill_rect(img, sx, 14, sx + 2, 18, (238, 192, 84))
        px(img, sx + 1, 16, (98, 56, 28))
    # Top leather carry handle (x=10..17, y=6..9)
    fill_rect(img, 10, 6, 17, 7, (146, 86, 44))
    px(img, 10, 8, (108, 62, 32))
    px(img, 17, 8, (108, 62, 32))
    add_outline(img, (18, 12, 10, 255))
    return img


def make_field_guide():
    img = new_canvas()
    # Ornate leather-bound expedition journal with brass corners, cream pages, red ribbon & glowing compass emblem
    # Book cover (x=6..25, y=4..27)
    fill_rect(img, 6, 4, 25, 27, (124, 68, 36))
    fill_rect(img, 8, 5, 24, 26, (168, 102, 54))
    # Dark leather spine on left (x=6..9) with raised hubs
    fill_rect(img, 6, 4, 9, 27, (86, 44, 22))
    for hy in [7, 13, 19, 24]:
        fill_rect(img, 6, hy, 9, hy + 1, (218, 168, 74))
    # Cream parchment page block along right edge (x=24..26, y=6..26)
    fill_rect(img, 24, 6, 25, 26, (244, 232, 196))
    for py in range(7, 26, 2):
        px(img, 25, py, (214, 196, 152))
    # Gilded brass top-right and bottom-right corner guards
    for cy in [4, 24]:
        fill_rect(img, 21, cy, 24, cy + 3, (238, 192, 84))
        px(img, 23, cy + 1, (255, 238, 164))
    # Glowing cyan/gold compass crest in center of cover (center=16, 15)
    fill_Waiting_circle(img, 16.0, 15.0, 4.5, (232, 186, 82))
    fill_Waiting_circle(img, 16.0, 15.0, 3.0, (34, 48, 64))
    for d in [-2, -1, 0, 1, 2]:
        px(img, 16 + d, 15, (112, 246, 255))
        px(img, 16, 15 + d, (112, 246, 255))
    px(img, 16, 15, (255, 252, 210))
    # Red bookmark ribbon hanging below bottom edge
    fill_rect(img, 15, 27, 17, 30, (216, 44, 58))
    px(img, 16, 30, (16, 12, 10, 0))
    add_outline(img, (18, 12, 8, 255))
    return img


GENERATORS = [
    ("pickup_rune", make_pickup_rune),
    ("gathering_rune", make_gathering_rune),
    ("deep_pockets_rune", make_deep_pockets_rune),
    ("provision_pouch", make_provision_pouch),
    ("restock_rune", make_restock_rune),
    ("crafting_kit", make_crafting_kit),
    ("ember_core", make_ember_core),
    ("ender_chest", make_ender_chest),
    ("builders_belt", make_builders_belt),
    ("void_sigil", make_void_sigil),
    ("tank_upgrade", make_tank_upgrade),
    ("battery_upgrade", make_battery_upgrade),
    ("xp_storage_upgrade", make_xp_storage_upgrade),
    ("quiver", make_quiver),
    ("philter", make_philter),
    ("alembic", make_alembic),
    ("forager_pouch", make_forager_pouch),
    ("gem_pouch", make_gem_pouch),
    ("cartographer_pouch", make_cartographer_pouch),
]

EXTRA_ITEM_ICONS = [
    ("backpack_ingot", make_backpack_ingot),
    ("crimson_ingot", make_crimson_ingot),
    ("endgame_ingot", make_endgame_ingot),
    ("camp_kit", make_camp_kit),
    ("field_guide", make_field_guide),
]


def main():
    UPGRADE_TEX.mkdir(parents=True, exist_ok=True)
    all_icons = [(name, fn, True) for name, fn in GENERATORS] + [(name, fn, False) for name, fn in EXTRA_ITEM_ICONS]
    cols = 5
    rows = (len(all_icons) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 96 + 24, rows * 96 + 24), (20, 24, 30, 255))
    draw = ImageDraw.Draw(sheet)

    for idx, (name, fn, is_upgrade) in enumerate(all_icons):
        icon = fn()
        if is_upgrade:
            out_upgrade = UPGRADE_TEX / f"{name}.png"
            icon.save(out_upgrade)
            out_root = ITEM_TEX / f"{name}.png"
            if out_root.exists():
                icon.save(out_root)
        else:
            out_root = ITEM_TEX / f"{name}.png"
            icon.save(out_root)

        c = idx % cols
        r = idx // cols
        ox = 20 + c * 96
        oy = 16 + r * 96
        scaled = icon.resize((64, 64), Image.Resampling.NEAREST)
        sheet.alpha_composite(scaled, (ox, oy))
        draw.text((ox, oy + 68), name.replace("_", " "), fill=(235, 224, 196, 255))

    PREVIEW_PATH.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(PREVIEW_PATH)
    print(f"Saved {len(all_icons)} standalone 32x32 icons and preview at {PREVIEW_PATH}")


if __name__ == "__main__":
    main()
