package com.waldron.adventurerspack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * Adventurer's Field Guide item and RPG Codex data model.
 * Retains TOPICS for backwards compatibility while providing structured RPG entries,
 * stat bars, item showcases, and lore sections for the Fantasy RPG GuideScreen UI.
 */
public class GuideBook extends Item {
 public GuideBook() {
  super(new Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
 }

 @Override
 public void appendHoverText(ItemStack stack, Level level, java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
  lines.add(net.minecraft.network.chat.Component.literal("Fantasy RPG Codex & Compendium").withStyle(net.minecraft.ChatFormatting.GOLD));
  lines.add(net.minecraft.network.chat.Component.literal("Right-click to inspect all Pack Upgrades, Runes, Addons, Endgame Forms & Armor Sets.").withStyle(net.minecraft.ChatFormatting.GRAY));
 }

 @Override
 public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand hand) {
  if (l.isClientSide) {
   net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
    net.minecraftforge.api.distmarker.Dist.CLIENT,
    () -> () -> com.waldron.adventurerspack.client.GuideScreen.open()
   );
  }
  return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), l.isClientSide);
 }

 public static ItemStack tierPack(int tier) {
  ItemStack s = new ItemStack(AdventurersPack.PACK.get());
  CompoundTag tag = s.getOrCreateTagElement("Pack");
  tag.putInt("Tier", Math.max(0, Math.min(4, tier)));
  return s;
 }

 public static final class Topic {
  public final String nav, title;
  public final String[] lines;
  public Topic(String nav, String title, String... lines) {
   this.nav = nav;
   this.title = title;
   this.lines = lines;
  }
 }

 public record Stat(String label, String value, float fill, int color) {}

 public record Section(String heading, String... bullets) {}

 public static final class Entry {
  public final int category;
  public final String nav, title, badge, subtitle, socketInfo;
  public final int accentColor;
  public final Supplier<ItemStack> icon;
  public final Supplier<ItemStack[]> variants;
  public final Stat[] stats;
  public final Section[] sections;

  public Entry(
   int category,
   String nav,
   String title,
   String badge,
   String subtitle,
   String socketInfo,
   int accentColor,
   Supplier<ItemStack> icon,
   Supplier<ItemStack[]> variants,
   Stat[] stats,
   Section[] sections
  ) {
   this.category = category;
   this.nav = nav;
   this.title = title;
   this.badge = badge;
   this.subtitle = subtitle;
   this.socketInfo = socketInfo;
   this.accentColor = accentColor;
   this.icon = icon;
   this.variants = variants;
   this.stats = stats;
   this.sections = sections;
  }
 }

 public record Category(String name, String subtitle, int color, Supplier<ItemStack> icon) {}

 public static final Category[] CATEGORIES = {
  new Category("All Chronicles", "Complete Adventurer's Compendium", 0xFFFFD770, () -> new ItemStack(AdventurersPack.GUIDE.get())),
  new Category("Pack & Storage", "Tiers, Compartments, Bulk & Controls", 0xFF56FAD8, () -> tierPack(4)),
  new Category("Attachments", "6 Active Workstation & Utility Modules", 0xFFFFB84D, () -> new ItemStack(AdventurersPack.FURNACE.get())),
  new Category("Runes", "5 Passive Arcane Pack Enchantments", 0xFFC878FF, () -> new ItemStack(AdventurersPack.PICKUP.get())),
  new Category("Addons & Pouches", "6 Specialized Banks & 4 Filtered Pouches", 0xFF55DDFF, () -> new ItemStack(AdventurersPack.TANK.get())),
  new Category("Weapons & Tools", "10-Form Endgame Arsenal & Tool Rack", 0xFFFF6070, () -> new ItemStack(AdventurersPack.ENDGAME.get())),
  new Category("Armor & Forge", "Immortal Armor Sets, Ingots, Dock & Camp", 0xFF7FE146, () -> new ItemStack(AdventurersPack.CURSIUM_CHESTPLATE_ELYTRA.get()))
 };

 public static final Entry[] ENTRIES = {
  // ==================== CATEGORY 1: PACK & STORAGE ====================
  new Entry(
   1, "Backpack Tiers", "Adventure's Backpack & Tiers", "CORE GEAR",
   "Five progressive pack tiers that preserve all stored loot, upgrades, and settings when upgraded.",
   "Equip: Pack Socket or Curios Back  |  Open: [E] or [B] or Right-Click",
   0xFF56FAD8,
   () -> tierPack(4),
   () -> new ItemStack[]{tierPack(0), tierPack(1), tierPack(2), tierPack(3), tierPack(4), new ItemStack(AdventurersPack.BACKPACK_INGOT.get())},
   new Stat[]{
    new Stat("Traveler (T1)", "27 / tab (108)", 0.42f, 0xFFB8A078),
    new Stat("Explorer (T2)", "36 / tab (144)", 0.57f, 0xFF66CC88),
    new Stat("Ranger (T3)", "45 / tab (180)", 0.71f, 0xFF55BBFF),
    new Stat("Runic (T4)", "54 / tab (216)", 0.86f, 0xFFC878FF),
    new Stat("Relic (T5)", "63 / tab (252)", 1.00f, 0xFFFFD770),
    new Stat("Upgrade Sockets", "6 Gear · 5 Rune · 6 Addon", 1.00f, 0xFF56FAD8)
   },
   new Section[]{
    new Section("EQUIPPING & OPENING",
     "Right-click a pack in your hand to open that specific bag directly, or place it into the Pack Socket on the center Equipment panel (or a Curios Back slot) to wear it on your back.",
     "Press [E] or Open Backpack [B] at any time to open your worn pack. When both the Pack Socket and Curios are filled, the Pack Socket takes priority.",
     "Click [Vanilla inventory] at the top-right of the pack screen (or toggle Vanilla Inventory mode under Controls) to switch seamlessly to Minecraft's default inventory."
    ),
    new Section("TIER PROGRESSION & CAPACITIES",
     "Crafting the next backpack tier on a crafting table transfers 100% of your stored items, bulk stacks, fluids, energy, XP, pouches, and installed upgrades.",
     "Compartment Slots (4 Tabs): Traveler holds 27 per tab (108 total), Explorer 36 (144), Ranger 45 (180), Runic 54 (216), and Relic 63 (252).",
     "Base Bulk Capacity (18 Slots): Traveler 4,096/slot, Explorer 8,192/slot, Ranger 16,384/slot, Runic 32,768/slot, and Relic 65,536/slot."
    )
   }
  ),
  new Entry(
   1, "Compartments & Routing", "Four Smart Compartments", "STORAGE SYSTEM",
   "Customizable color-coded storage tabs with automated item routing, search filtering, and slot protection.",
   "Tabs: Mine, Build, Loot, Tools  |  Protect Slot: [Alt + Click]",
   0xFF56FAD8,
   () -> tierPack(2),
   () -> new ItemStack[]{tierPack(0), new ItemStack(Items.DIAMOND_ORE), new ItemStack(Items.STONE_BRICKS), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.GOLDEN_APPLE)},
   new Stat[]{
    new Stat("Compartments", "4 Custom Tabs", 1.0f, 0xFF56FAD8),
    new Stat("Max Tab Size", "63 Slots (Relic)", 1.0f, 0xFFFFD770),
    new Stat("Routing Rules", "7 Smart Filters", 0.88f, 0xFFC878FF),
    new Stat("Tab Palette", "8 Accent Colors", 0.80f, 0xFF7FE146)
   },
   new Section[]{
    new Section("SEARCH, TRANSFER & PROTECTION",
     "Click any of the four tabs (default: Mine, Build, Loot, Tools) to switch compartments. Use the top Search Bar to dim non-matching items and highlight tabs containing matches in bright teal.",
     "Shift-click moves stacks between your inventory and the active compartment.",
     "Hold [Alt + Click] on any compartment slot to mark it with a golden star (*). Protected slots are ignored by hoppers, hotbar restocking, and automatic meal consumption."
    ),
    new Section("SMART ROUTING & CUSTOMIZATION",
     "Open the [Controls] workstation to rename the active compartment (up to 12 characters), cycle its accent color bar, and assign its routing rule.",
     "When [Route] is enabled, Quick Deposit runs immediately and every second. Main-inventory items move to compartments already holding matching items or matching bulk storage. Hotbar items, unmatched items, and overflow remain in your inventory."
    )
   }
  ),
  new Entry(
   1, "Bulk & Slot Upgrades", "Bulk Vaults & Slot Upgrades", "DEEP STORAGE",
   "Eighteen massive bulk vaults for raw materials plus five permanent normal-slot stack multipliers.",
   "Protect: [Alt + Click]  |  Builder Select: [Ctrl + Click]  |  Track Goal: [Ctrl + Alt + Click]",
   0xFFFFD770,
   () -> new ItemStack(AdventurersPack.CAPACITY.get()),
   () -> new ItemStack[]{new ItemStack(Items.IRON_INGOT, 16), new ItemStack(Items.GOLD_INGOT, 16), new ItemStack(Items.DIAMOND, 8), new ItemStack(Items.EMERALD, 16), new ItemStack(Items.NETHERITE_INGOT, 4)},
   new Stat[]{
    new Stat("Bulk Vaults", "18 Dedicated Slots", 1.0f, 0xFF56FAD8),
    new Stat("Max Bulk / Slot", "126,976 Items", 1.0f, 0xFFC878FF),
    new Stat("Slot Upgrade T1-T3", "384 / 768 / 1,536", 0.60f, 0xFF66CC88),
    new Stat("Slot Upgrade T4-T5", "3,072 / 6,144", 1.0f, 0xFFFFD770)
   },
   new Section[]{
    new Section("18 BULK STORAGE VAULTS",
     "Located beneath the compartments, each of the 18 bulk slots stores thousands of a single un-tagged stackable item. The teal percentage above each slot shows remaining free capacity.",
     "Left-click with items to deposit; left-click empty-handed to withdraw a full stack; right-click to withdraw 1 item.",
     "[Alt + Click] protects a bulk slot (*) from hoppers, restocking, and voiding. [Ctrl + Click] selects that block for the Builder's Belt (or press [N] to cycle)."
    ),
    new Section("PERMANENT SLOT UPGRADES (TIERS 1–5)",
     "Under [SLOT UPGRADES] on the left panel, unlock 5 sequential upgrades that multiply the stack limit of all normal compartment slots (non-stackable tools remain 1 per slot):",
     "Tier 1 (384/slot): Costs 16 Iron Ingots  ·  Tier 2 (768/slot): Costs 16 Gold Ingots  ·  Tier 3 (1,536/slot): Costs 8 Diamonds.",
     "Tier 4 (3,072/slot): Costs 16 Emeralds  ·  Tier 5 (6,144/slot): Costs 4 Netherite Ingots."
    ),
    new Section("PROJECT SUPPLY TRACKER",
     "Hold [Ctrl + Alt + Click] on any item in the pack to pin it to the [SUPPLIES] tracker under Bulk Storage (up to 4 items). Click a tracked line to raise its goal (64 -> 256 -> 1,000 -> 2,000 -> 4,096 -> Clear)."
    )
   }
  ),
  new Entry(
   1, "Controls & Keybinds", "Presets, Radial Wheels & Keys", "TACTICAL HUD",
   "Activity presets, quick-access radial wheels, offhand carousel, and full keyboard shortcuts.",
   "Quick Wheel: [Hold G]  |  Cycle Preset: [R]  |  Offhand Swap: [Y]",
   0xFF55BBFF,
   () -> new ItemStack(Items.COMPASS),
   () -> new ItemStack[]{tierPack(3), new ItemStack(Items.SHIELD), new ItemStack(Items.TORCH), new ItemStack(Items.TOTEM_OF_UNDYING)},
   new Stat[]{
    new Stat("Activity Presets", "4 Tactical Modes", 1.0f, 0xFF56FAD8),
    new Stat("Offhand Carousel", "4 Quick-Swap Slots", 0.85f, 0xFFFFB84D),
    new Stat("Radial Wheels", "Pack [G] · Tool [V] · Form [Z]", 1.0f, 0xFFC878FF),
    new Stat("Supply Tracker", "4 Live Goal Bars", 0.80f, 0xFF7FE146)
   },
   new Section[]{
    new Section("ACTIVITY PRESETS & QUICK WHEEL",
     "Press [R] or click [Preset] under Controls to cycle: Travel, Mining, Building, and Combat. Hold [G] to open the Quick-Access Wheel and release over any compartment or preset.",
     "Mining Preset: Enables Auto Pickup and automatically draws your best rack tool when striking blocks or tilling.",
     "Building Preset: Enables Auto Pickup and activates Build Mode for placing blocks directly from Bulk Storage.",
     "Combat Preset: Automatically draws your strongest weapon when attacking, morphing an Endgame bow or tool on the rack into the Lance."
    ),
    new Section("OFFHAND CAROUSEL & MASTER KEYBINDS",
     "Place up to 4 utility items (shields, totems, torches, food) in the [Off] slots on the Equipment panel. Press [Y] anytime to cycle your offhand with the next filled slot.",
     "Default Keys: [E / B] Open Pack · [V] Tool Kit (Tap/Hold) · [Z] Endgame Forms (Hold) · [G] Quick Wheel · [R] Cycle Preset · [Y] Offhand Kit · [U] Drink/Throw Philter Vial · [N] Next Bulk Block."
    )
   }
  ),

  // ==================== CATEGORY 2: ATTACHMENTS ====================
  new Entry(
   2, "Artisan's Crafting Kit", "Artisan's Crafting Kit", "ATTACHMENT",
   "Unlocks a built-in 3x3 crafting workstation inside the backpack with automatic ingredient recovery.",
   "Socket: Attachment (1 of 6)  |  Workstation: [Craft]  |  Toggle: [Alt + Right-Click]",
   0xFFFFB84D,
   () -> new ItemStack(AdventurersPack.CRAFTING.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.CRAFTING.get()), new ItemStack(Items.CRAFTING_TABLE)},
   new Stat[]{
    new Stat("Crafting Grid", "3×3 Standard Grid", 1.0f, 0xFFFFB84D),
    new Stat("JEI Transfer", "Full Recipe Autofill", 1.0f, 0xFF56FAD8),
    new Stat("Item Recovery", "Auto-Returns on Close", 1.0f, 0xFF7FE146),
    new Stat("Socket Cost", "1 Attachment Slot", 0.35f, 0xFFD4AF37)
   },
   new Section[]{
    new Section("INSTALLATION & OPERATION",
     "Shift-click the Artisan's Crafting Kit into any of the 6 Attachment sockets on the [Upgrades] panel, then click the [Craft] workstation button.",
     "Supports all standard 3x3 shaped and shapeless recipes as well as JEI recipe transfer (+).",
     "Any ingredients left inside the 3x3 grid automatically return to your inventory when you switch panels or close the backpack."
    )
   }
  ),
  new Entry(
   2, "Ember Core", "Ember Core (Portable Furnace)", "ATTACHMENT",
   "Internal smelting core that cooks ores and food continuously while worn, closed, or docked.",
   "Socket: Attachment (1 of 6)  |  Workstation: [Smelt]  |  Toggle: [Alt + Right-Click]",
   0xFFFF7744,
   () -> new ItemStack(AdventurersPack.FURNACE.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.FURNACE.get()), new ItemStack(Items.BLAST_FURNACE), new ItemStack(Items.COAL), new ItemStack(Items.LAVA_BUCKET)},
   new Stat[]{
    new Stat("Smelting Mode", "All Furnace Recipes", 1.0f, 0xFFFF7744),
    new Stat("Background Tick", "Active Closed & Docked", 1.0f, 0xFF56FAD8),
    new Stat("Experience", "Banks Smelting XP", 1.0f, 0xFF7FE146),
    new Stat("Fuel Support", "All Standard Fuels", 1.0f, 0xFFFFD770)
   },
   new Section[]{
    new Section("CONTINUOUS BACKGROUND SMELTING",
     "Install in an Attachment socket and open the [Smelt] workstation. Load input items and any standard furnace fuel.",
     "The Ember Core continues smelting while you explore with the pack closed, and while the pack is mounted in an Expedition Dock.",
     "Smelting experience is accumulated safely inside the core and awarded proportionally whenever you extract finished items from the output slot."
    )
   }
  ),
  new Entry(
   2, "Ender Chest", "Ender Chest Attachment", "ATTACHMENT",
   "Direct dimensional link to your personal 27-slot vanilla Ender Chest inventory from anywhere.",
   "Socket: Attachment (1 of 6)  |  Workstation: [Ender]  |  Toggle: [Alt + Right-Click]",
   0xFF55DDFF,
   () -> new ItemStack(AdventurersPack.ENDER.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.ENDER.get()), new ItemStack(Items.ENDER_CHEST), new ItemStack(Items.ENDER_EYE)},
   new Stat[]{
    new Stat("Ender Slots", "27 Shared Slots", 1.0f, 0xFF55DDFF),
    new Stat("World Sync", "100% Linked to Placed", 1.0f, 0xFFC878FF),
    new Stat("Cross-Dimension", "Works Everywhere", 1.0f, 0xFF56FAD8),
    new Stat("Nesting Guard", "Prevents Pack-in-Pack", 1.0f, 0xFFFFB84D)
   },
   new Section[]{
    new Section("DIMENSIONAL ENDER ACCESS",
     "Install in an Attachment socket and press [Ender] on the workstation panel to access all 27 slots of your personal Ender Chest without placing a block.",
     "Shares the exact same inventory as any placed vanilla Ender Chest in the world. Backpacks cannot be placed inside."
    )
   }
  ),
  new Entry(
   2, "Builder's Belt", "Builder's Belt", "ATTACHMENT",
   "Channels blocks directly from your Bulk Storage vaults so you can build thousands of blocks uninterrupted.",
   "Socket: Attachment (1 of 6)  |  Select Block: [Ctrl + Click] or [N]  |  Control: [Build mode]",
   0xFFFFB84D,
   () -> new ItemStack(AdventurersPack.BUILDER.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.BUILDER.get()), new ItemStack(Items.BRICKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.COBBLESTONE)},
   new Stat[]{
    new Stat("Block Source", "18 Bulk Vaults", 1.0f, 0xFFFFB84D),
    new Stat("Max Continuous", "Up to 126,976 Blocks", 1.0f, 0xFF56FAD8),
    new Stat("Quick Cycle", "Press [N] In-Game", 1.0f, 0xFF7FE146),
    new Stat("Preset Link", "Auto-On in Building", 1.0f, 0xFFFFD770)
   },
   new Section[]{
    new Section("DIRECT BULK BLOCK PLACEMENT",
     "Install the Builder's Belt in an Attachment socket and turn on [Build mode] under Controls (or switch to the Building activity preset with [R]).",
     "Hold [Ctrl + Click] on any Bulk Storage slot to highlight it in teal as your active building block, or press [N] in-game to cycle through stored bulk blocks.",
     "While holding the backpack in your hand, right-clicking places the selected bulk block directly into the world. Hold [Shift + Right-Click] to open the pack instead."
    )
   }
  ),
  new Entry(
   2, "Cave Torch", "Cave Torch Automator", "ATTACHMENT",
   "Stores 9 stacks of torches or soul torches and automatically lights dark caverns as you explore.",
   "Socket: Attachment (1 of 6)  |  Workstation: [Torches]  |  Toggle: [Alt + Right-Click]",
   0xFFFFD770,
   () -> new ItemStack(AdventurersPack.TORCH.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.TORCH.get()), new ItemStack(Items.TORCH), new ItemStack(Items.SOUL_TORCH), new ItemStack(Items.LANTERN)},
   new Stat[]{
    new Stat("Torch Capacity", "9 Dedicated Slots", 0.90f, 0xFFFFD770),
    new Stat("Light Threshold", "Block Light <= 6", 0.85f, 0xFFFF9933),
    new Stat("Spacing Guard", "5-Block Torch Check", 1.0f, 0xFF56FAD8),
    new Stat("Placement", "Floor & Wall Mount", 1.0f, 0xFF7FE146)
   },
   new Section[]{
    new Section("AUTOMATED CAVERN ILLUMINATION",
     "Install the Cave Torch in an Attachment socket and click [Torches] on the workstation panel to open its 9 dedicated torch slots (accepts Torches and Soul Torches).",
     "As you walk through dark caves or night terrain (block light 6 or lower, with no existing torch within 5 blocks), the pack automatically places a torch on valid floor or wall surfaces nearby.",
     "With the Pickup Rune active, collected torches automatically route into the Cave Torch reserve before general storage. Use [Alt + Right-Click] on the upgrade to pause auto-placement anytime."
    )
   }
  ),
  new Entry(
   2, "Provision Pouch", "Provision Pouch (Auto-Feeder)", "ATTACHMENT / POUCH",
   "Insulated 18-slot food pantry that automatically feeds you safe, optimal meals when hungry.",
   "Socket: Food Pouch Socket or Attachment Socket  |  Workstation: [Pouches -> Food]",
   0xFF7FE146,
   () -> new ItemStack(AdventurersPack.FEEDING.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.FEEDING.get()), new ItemStack(Items.COOKED_BEEF), new ItemStack(Items.GOLDEN_CARROT), new ItemStack(Items.BREAD)},
   new Stat[]{
    new Stat("Pantry Slots", "18 Food Stacks", 1.0f, 0xFF7FE146),
    new Stat("Priority Slot", "Slot 1 Checked First", 1.0f, 0xFFFFD770),
    new Stat("Smart Safety", "Skips Status Effects", 1.0f, 0xFF56FAD8),
    new Stat("Zero Waste", "Never Overfills Hunger", 1.0f, 0xFFFFB84D)
   },
   new Section[]{
    new Section("AUTOMATIC FIELD RATIONS",
     "Can be installed either in the dedicated [Food] socket on the Pouches page OR in any Attachment socket. All 18 food slots are saved directly inside the pouch item.",
     "Whenever your hunger drops, it automatically eats from the pouch (checking Slot 1 first, then remaining pouch slots, then unprotected compartment slots).",
     "Smart Meal Selection: Automatically skips foods that carry potion/status effects (such as Golden Apples, Rotten Flesh, or Pufferfish) and skips meals whose nutrition would exceed your missing hunger."
    )
   }
  ),

  // ==================== CATEGORY 3: RUNES ====================
  new Entry(
   3, "Pickup Rune", "Pickup Rune", "RUNE",
   "Intercepts world item drops directly into your backpack's specialized pouches, bulk vaults, and compartments.",
   "Socket: Rune (1 of 5)  |  Control: [Auto pickup: ON]  |  Toggle: [Alt + Right-Click]",
   0xFFC878FF,
   () -> new ItemStack(AdventurersPack.PICKUP.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.PICKUP.get()), new ItemStack(AdventurersPack.MAGNET.get())},
   new Stat[]{
    new Stat("Insertion Order", "Pouch -> Quiver -> Bulk -> Tab", 1.0f, 0xFFC878FF),
    new Stat("Smart Routing", "Auto Quick Deposit", 1.0f, 0xFF56FAD8),
    new Stat("Preset Synergy", "Auto-On in Mine/Build", 1.0f, 0xFF7FE146),
    new Stat("Socket Cost", "1 Rune Slot", 0.40f, 0xFFD4AF37)
   },
   new Section[]{
    new Section("PRIORITY DROP ROUTING",
     "Install in any of the 5 Rune sockets on [Upgrades] and ensure [Auto pickup] is ON under Controls.",
     "Collected drops bypass your player inventory and file into the pack in strict priority order: (1) Matching Filtered Pouches -> (2) Quiver -> (3) Cave Torches -> (4) Matching Bulk Vault -> (5) Routed or Active Compartment."
    )
   }
  ),
  new Entry(
   3, "Gathering Rune", "Gathering Rune (Magnet)", "RUNE",
   "Projects an arcane magnetic field that pulls nearby item drops within 5 blocks straight to you.",
   "Socket: Rune (1 of 5)  |  Requires: Pickup Rune + Auto Pickup ON",
   0xFFC878FF,
   () -> new ItemStack(AdventurersPack.MAGNET.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.MAGNET.get()), new ItemStack(AdventurersPack.PICKUP.get())},
   new Stat[]{
    new Stat("Magnet Radius", "5.0 Blocks (Configurable)", 0.85f, 0xFFC878FF),
    new Stat("Pulse Rate", "Every 0.5 Seconds", 1.0f, 0xFF56FAD8),
    new Stat("Dependency", "Requires Pickup Rune", 1.0f, 0xFFFFB84D)
   },
   new Section[]{
    new Section("ARCANE ITEM MAGNETISM",
     "Install in a Rune socket alongside an active Pickup Rune with [Auto pickup] enabled.",
     "Automatically draws nearby un-delayed item drops within a 5-block radius (adjustable via magnetRange in config) directly into your backpack."
    )
   }
  ),
  new Entry(
   3, "Deep Pockets Rune", "Deep Pockets Rune", "RUNE",
   "Expands every single Bulk Storage vault by +61,440 items on top of the backpack's tier capacity.",
   "Socket: Rune (1 of 5)  |  Bonus: +61,440 Items per Bulk Slot",
   0xFFC878FF,
   () -> new ItemStack(AdventurersPack.CAPACITY.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.CAPACITY.get()), tierPack(4)},
   new Stat[]{
    new Stat("Traveler Bulk", "4,096 -> 65,536", 0.52f, 0xFFB8A078),
    new Stat("Explorer / Ranger", "69,632 / 77,824", 0.65f, 0xFF55BBFF),
    new Stat("Runic Bulk", "32,768 -> 94,208", 0.78f, 0xFFC878FF),
    new Stat("Relic Bulk", "65,536 -> 126,976", 1.00f, 0xFFFFD770)
   },
   new Section[]{
    new Section("MASSIVE BULK EXPANSION",
     "Adds 61,440 items of capacity to all 18 Bulk Storage slots simultaneously.",
     "Non-Destructive Removal: Removing or disabling the Deep Pockets Rune never deletes items already stored in your bulk vaults; over-capacity slots simply hold their contents and refuse additional deposits until below capacity."
    )
   }
  ),
  new Entry(
   3, "Restock Rune", "Restock Rune", "RUNE",
   "Continuously replenishes partial stacks on your hotbar from unprotected bulk vaults and compartments.",
   "Socket: Rune (1 of 5)  |  Respects: [*] Protected Slots",
   0xFFC878FF,
   () -> new ItemStack(AdventurersPack.RESTOCK.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.RESTOCK.get()), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.TORCH)},
   new Stat[]{
    new Stat("Target Slots", "Hotbar Slots 1–9", 1.0f, 0xFF56FAD8),
    new Stat("Pull Priority", "Bulk Vault -> Compartment", 1.0f, 0xFFC878FF),
    new Stat("Protection", "Skips [*] Starred Slots", 1.0f, 0xFFFFD770)
   },
   new Section[]{
    new Section("AUTOMATIC HOTBAR REPLENISHMENT",
     "Every second, inspects your 9 hotbar slots for partial stackable items and tops them up from matching Bulk Storage slots first, then compartments.",
     "Empty hotbar slots are left untouched so your layout stays clean, and any slot marked with an [Alt + Click] protection star (*) is never drained."
    )
   }
  ),
  new Entry(
   3, "Void Sigil", "Void Sigil", "RUNE",
   "Safely disintegrates excess item pickups only when their matching unprotected Bulk Vault is 100% full.",
   "Socket: Rune (1 of 5)  |  Control: [Void overflow: ON]  |  Respects: [*] Protected Slots",
   0xFFC878FF,
   () -> new ItemStack(AdventurersPack.VOID.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.VOID.get()), new ItemStack(Items.OBSIDIAN)},
   new Stat[]{
    new Stat("Safety Filter", "Full Bulk Match Only", 1.0f, 0xFF7FE146),
    new Stat("Protection", "Never Voids [*] Slots", 1.0f, 0xFFFFD770),
    new Stat("Control Toggle", "Void Overflow Switch", 1.0f, 0xFFC878FF)
   },
   new Section[]{
    new Section("PRECISION OVERFLOW DELETION",
     "Install in a Rune socket and enable [Void overflow] under Controls.",
     "When picking up items automatically, leftover items are deleted ONLY if they match a Bulk Storage slot that is completely full and NOT protected (*). Rare drops and un-tracked items are never voided."
    )
   }
  ),

  // ==================== CATEGORY 4: ADDONS & POUCHES ====================
  new Entry(
   4, "Quiver", "Marksman's Quiver", "STORAGE ADDON",
   "Nine-slot ammunition reserve for arrows and firework rockets with direct bow and Elytra integration.",
   "Socket: Storage Addon (1 of 6)  |  Workstation: [Quiver]",
   0xFF55DDFF,
   () -> new ItemStack(AdventurersPack.QUIVER.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.QUIVER.get()), new ItemStack(Items.ARROW), new ItemStack(Items.SPECTRAL_ARROW), new ItemStack(Items.FIREWORK_ROCKET)},
   new Stat[]{
    new Stat("Ammo Slots", "9 Stacks (576 Arrows)", 1.0f, 0xFF55DDFF),
    new Stat("Bow Priority", "Drawn Before Inventory", 1.0f, 0xFF7FE146),
    new Stat("Elytra Boost", "Right-Click Fires Rocket", 1.0f, 0xFFFFB84D)
   },
   new Section[]{
    new Section("RANGED AMMO & ELYTRA PROPULSION",
     "Install in any of the 6 Storage Addon sockets on [Upgrades] and click [Quiver] to load arrows, tipped arrows, spectral arrows, and firework rockets.",
     "All bows, crossbows, and Endgame bow forms automatically draw ammunition from the Quiver before touching your player inventory.",
     "While Elytra gliding (including Winged Crimson or Winged End Game armor), simply right-click in midair to fire a firework rocket directly from the Quiver!"
    )
   }
  ),
  new Entry(
   4, "Philter & Field Alembic", "Philter Vials & Field Alembic", "ALCHEMY ADDONS",
   "Portable 3-bottle brewing stand paired with 3 quick-draw potion vials for instant mid-combat alchemy.",
   "Sockets: Storage Addon  |  Workstations: [Potions] & [Brew]  |  Quick-Use Key: [U]",
   0xFFAE66D4,
   () -> new ItemStack(AdventurersPack.ALEMBIC.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.PHILTER.get()), new ItemStack(AdventurersPack.ALEMBIC.get()), new ItemStack(Items.BLAZE_POWDER), new ItemStack(Items.POTION)},
   new Stat[]{
    new Stat("Philter Vials", "3 Quick-Draw Vials", 1.0f, 0xFF56FAD8),
    new Stat("Brewing Stand", "3 Bottles + Ingredient", 1.0f, 0xFFAE66D4),
    new Stat("Blaze Fuel", "20 Brews / Powder", 1.0f, 0xFFFFAA25),
    new Stat("Synergy", "Direct Pour Into Vials", 1.0f, 0xFF7FE146)
   },
   new Section[]{
    new Section("PHILTER: QUICK-ACTION POTION VIALS",
     "Install the Philter in a Storage Addon socket and open [Potions]. Place up to 3 potions in the vials (highlighted by a teal selection frame).",
     "Press [U] at any time (even with the pack closed!) or click [Use potion] to immediately drink the selected potion or throw splash/lingering potions. Drinkable potions return their empty glass bottle.",
     "Right-click a vial with an empty cursor and a glass bottle in your inventory to retrieve a stored potion."
    ),
    new Section("FIELD ALEMBIC: PORTABLE BREWING",
     "Install the Field Alembic and open [Brew]. Uses 3 bottle slots, 1 brewing ingredient slot, and Blaze Powder fuel (20 charges per powder). Continues brewing while carried or docked.",
     "When both the Alembic and Philter are installed, click [Pour into vials] to transfer finished potions straight into empty Philter vials!"
    )
   }
  ),
  new Entry(
   4, "Fluid Tank", "Dual Fluid Tanks", "STORAGE ADDON",
   "Two independent 128-bucket fluid reservoirs with live texture gauges, bucket I/O slots, and dock pipe support.",
   "Socket: Storage Addon (1 of 6)  |  Workstation: [Tank]  |  Capacity: 2 × 128,000 mB",
   0xFF55DDFF,
   () -> new ItemStack(AdventurersPack.TANK.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.TANK.get()), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.BUCKET)},
   new Stat[]{
    new Stat("Tank 1 Capacity", "128 Buckets (128,000 mB)", 1.0f, 0xFF55BBFF),
    new Stat("Tank 2 Capacity", "128 Buckets (128,000 mB)", 1.0f, 0xFFFF7744),
    new Stat("Total Fluid", "256 Buckets Combined", 1.0f, 0xFF56FAD8),
    new Stat("Dock Piping", "Auto Fill & Drain", 1.0f, 0xFF7FE146)
   },
   new Section[]{
    new Section("DUAL RESERVOIRS & BUCKET TRANSFER",
     "Install the Fluid Tank in a Storage Addon socket and press [Tank] in the top-right workstation bar. Tank 1 and Tank 2 each hold 128,000 mB (128 buckets) of any fluid and render its live texture and tint.",
     "Place a filled bucket or fluid container in [Input] to empty it into that tank, or place an empty bucket in [Output] to fill it from the tank. Hover either gauge for exact mB readouts.",
     "Stored fluids remain safely saved on the backpack even if you remove the Fluid Tank addon. When docked in an Expedition Dock, external fluid pipes can pump fluids in and out."
    )
   }
  ),
  new Entry(
   4, "Forge Battery & XP Bank", "Forge Battery & Experience Bank", "STORAGE ADDONS",
   "High-capacity 1,000,000 FE energy capacitor and 200,000-point Experience Vault.",
   "Sockets: Storage Addon (1 of 6)  |  Workstations: [Battery] & [XP]",
   0xFF3DDC97,
   () -> new ItemStack(AdventurersPack.BATTERY.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.BATTERY.get()), new ItemStack(AdventurersPack.XP_BANK.get()), new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.EXPERIENCE_BOTTLE)},
   new Stat[]{
    new Stat("Forge Battery", "1,000,000 FE Capacity", 1.0f, 0xFF3DDC97),
    new Stat("FE Transfer Rate", "1,000 FE / Tick", 0.85f, 0xFF56FAD8),
    new Stat("Experience Bank", "200,000 XP Points", 1.0f, 0xFF7FE146),
    new Stat("Dock Cables", "Full FE Pipe I/O", 1.0f, 0xFFFFD770)
   },
   new Section[]{
    new Section("FORGE BATTERY (1,000,000 FE)",
     "Install the Forge Battery and click [Battery]. Place any Forge Energy item in the [Drain] (In) slot to siphon energy into the pack, or in the [Charge] (Out) slot to recharge your powered gear at 1,000 FE/tick.",
     "Continues charging items while closed or docked, and interfaces directly with energy cables when placed in an Expedition Dock."
    ),
    new Section("EXPERIENCE BANK (200,000 XP)",
     "Install the Experience Bank and click [XP]. Stores raw experience points (not levels) so you never lose value to high-level XP scaling.",
     "Use [Store 100], [Take 100], [Store all], and [Take all] to deposit or withdraw XP on demand. Stored energy and XP remain on the pack even if the addon is removed."
    )
   }
  ),
  new Entry(
   4, "Filtered Pouches", "Specialized Pouches & Filters", "POUCH SYSTEM",
   "Four 18-slot portable pouches with customizable 9-slot Whitelist/Blacklist item filters.",
   "Workstation: [Pouches]  |  Sub-Tabs: [Seeds] [Gem] [Food] [Maps]",
   0xFF7FE146,
   () -> new ItemStack(AdventurersPack.GEM.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.FORAGER.get()), new ItemStack(AdventurersPack.GEM.get()), new ItemStack(AdventurersPack.FEEDING.get()), new ItemStack(AdventurersPack.CARTOGRAPHER.get())},
   new Stat[]{
    new Stat("Forager Pouch", "18 Slots (Seeds & Crops)", 1.0f, 0xFF7FE146),
    new Stat("Gem Pouch", "18 Slots (Gems & Ingots)", 1.0f, 0xFF55DDFF),
    new Stat("Provision Pouch", "18 Slots (Food & Rations)", 1.0f, 0xFFFFB84D),
    new Stat("Cartographer", "18 Slots (Maps & Books)", 1.0f, 0xFFD4AF37)
   },
   new Section[]{
    new Section("FOUR DEDICATED POUCH SOCKETS",
     "Open [Pouches] to access 4 dedicated sockets (18 slots each). Contents are stored inside the pouch item itself, so taking a pouch out carries all of its items with it:",
     "Seeds (Forager Pouch): Seeds, saplings, flowers, wheat, sugar cane, bamboo, kelp, mushrooms, and berries.",
     "Gem (Gem Pouch): Diamonds, emeralds, amethyst, quartz, raw ores, nuggets, and ingots.",
     "Food (Provision Pouch): All edible foods with automatic hunger feeding.",
     "Maps (Cartographer Pouch): Maps, compasses, paper, and books. Maps inside the Cartographer Pouch actively update your explored terrain while you carry the pack!"
    ),
    new Section("CUSTOM WHITELIST & BLACKLIST FILTERS",
     "Click [Filter: OFF / WHITELIST / BLACKLIST] on the Pouches screen to narrow which items each pouch accepts. Click any of the 9 filter slots at the bottom with an item to copy its type (without consuming the item); right-click a filter slot to clear it."
    )
   }
  ),

  // ==================== CATEGORY 5: WEAPONS & TOOLS ====================
  new Entry(
   5, "Endgame: 10-Form Arsenal", "Endgame · Mythic 10-Form Weapon", "MYTHIC ARSENAL",
   "One indestructible weapon that morphs seamlessly between 10 melee, ranged, and harvesting forms.",
   "Form Wheel: [Hold Z] + [1–9, 0]  |  Quick Cycle: [Shift + Right-Click]",
   0xFFFF6070,
   () -> new ItemStack(AdventurersPack.ENDGAME.get()),
   () -> new ItemStack[]{
    new ItemStack(AdventurersPack.ENDGAME.get()),
    new ItemStack(AdventurersPack.REQUIEM.get()),
    new ItemStack(AdventurersPack.REQUIEM_SPEAR.get()),
    new ItemStack(AdventurersPack.REQUIEM_SHORTBOW.get()),
    new ItemStack(AdventurersPack.REQUIEM_RECURVE.get()),
    new ItemStack(AdventurersPack.REQUIEM_GREATBOW.get()),
    new ItemStack(AdventurersPack.ENDGAME_PICKAXE.get()),
    new ItemStack(AdventurersPack.ENDGAME_AXE.get()),
    new ItemStack(AdventurersPack.ENDGAME_SHOVEL.get()),
    new ItemStack(AdventurersPack.ENDGAME_HOE.get())
   },
   new Stat[]{
    new Stat("Bound Forms", "10 Instant Morphs", 1.0f, 0xFFFF6070),
    new Stat("Durability", "Infinite (Unbreakable)", 1.0f, 0xFFFFD770),
    new Stat("Enchantments", "Shared Across All 10", 1.0f, 0xFFC878FF),
    new Stat("Mining Tier", "Speed 24.0 (Above Netherite)", 1.0f, 0xFF56FAD8)
   },
   new Section[]{
    new Section("SHAPESHIFTING CONTROLS",
     "Hold the Endgame Forms key [Z] while holding any Endgame form to open the 10-Form Radial Wheel. Hover a node and release [Z], click a node, or press number keys [1–9, 0] to transform with an amethyst chime.",
     "Hold [Shift + Right-Click] to quick-step through forms: Sword -> Staff -> Lance, and cycle between Lance, Pickaxe, Axe, Shovel, and Hoe.",
     "All custom names, NBT tags, and enchantments carry across every form automatically, and no form ever takes durability damage."
    ),
    new Section("CLICK ANY ICON ABOVE TO INSPECT",
     "Select any of the 10 Endgame forms in the showcase bar above to inspect its item tooltip and crafting lineage, or read the dedicated Melee, Ranged, and Tool entries below."
    )
   }
  ),
  new Entry(
   5, "Endgame Melee Forms", "Lance, Sword & Double Spear", "MELEE ARSENAL",
   "Devastating close-quarters forms featuring recallable lightning lance throws and extended sweeping reach.",
   "Lance Throw: [Hold Right-Click 0.5s]  |  Extended Reach: +1.5 Blocks (Lance & Staff)",
   0xFFFF5555,
   () -> new ItemStack(AdventurersPack.REQUIEM.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.ENDGAME.get()), new ItemStack(AdventurersPack.REQUIEM.get()), new ItemStack(AdventurersPack.REQUIEM_SPEAR.get())},
   new Stat[]{
    new Stat("Staff / Spear DMG", "51 Attack (+1.5 Reach)", 1.00f, 0xFFFF7799),
    new Stat("Lance DMG", "48 Attack (+1.5 Reach)", 0.94f, 0xFFFF5555),
    new Stat("Sword DMG", "45 Attack (Fast Sweep)", 0.88f, 0xFFFFAA44),
    new Stat("Lance Recall", "Auto-Returns on Hit", 1.00f, 0xFF56FAD8)
   },
   new Section[]{
    new Section("LANCE FORM (48 DMG · THROW & RECALL)",
     "Grants +1.5 blocks of melee reach and sweeping attacks. Hold [Right-Click] for 0.5 seconds and release to hurl the Lance as a fire-immune piercing projectile!",
     "After striking a target or flying a short distance, the Lance automatically recalls straight back into your hand (or waits nearby if your inventory is completely full)."
    ),
    new Section("SWORD (45 DMG) & STAFF / DOUBLE SPEAR (51 DMG)",
     "Endgame · Sword: Balanced 45 Attack Damage with rapid sweeping strikes.",
     "Endgame · Staff (Double Spear): Deals the highest melee hit at 51 Attack Damage while granting +1.5 blocks of extended reach and sweeping cleaves."
    )
   }
  ),
  new Entry(
   5, "Endgame Bow Forms", "Shortbow, Recurve & Greatbow", "RANGED ARSENAL",
   "Three specialized bow forms engineered for multishot volleys, armor-piercing sniping, and heavy knockback.",
   "Select Form: [Hold Z -> Ranged Nodes 8, 9, 0]  |  Ammo: Uses Quiver First",
   0xFF55DDFF,
   () -> new ItemStack(AdventurersPack.REQUIEM_GREATBOW.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.REQUIEM_SHORTBOW.get()), new ItemStack(AdventurersPack.REQUIEM_RECURVE.get()), new ItemStack(AdventurersPack.REQUIEM_GREATBOW.get())},
   new Stat[]{
    new Stat("Shortbow (12t)", "7.5 DMG · 3-Arrow Spread", 0.72f, 0xFF55DDFF),
    new Stat("Recurve (20t)", "9.3 DMG · Pierces 3 Foes", 0.86f, 0xFF70B8FF),
    new Stat("Greatbow (32t)", "12.0 DMG · Heavy Knockback", 1.00f, 0xFFAA88FF),
    new Stat("Quiver Link", "Draws Arrows from Pack", 1.00f, 0xFF7FE146)
   },
   new Section[]{
    new Section("THREE TACTICAL BOW STYLES",
     "Shortbow (12-Tick Fast Draw): Reaches full draw in just 0.6s and fires a 3-arrow fan spread while spending only 1 arrow.",
     "Recurve Bow (20-Tick Precision Draw): High velocity (3.3), tight accuracy, and pierces through up to 3 enemies on a full draw.",
     "Greatbow (32-Tick Siege Draw): Massive 12.0 base arrow damage, 4.2 velocity, and crushes targets backward with heavy knockback on full draw."
    )
   }
  ),
  new Entry(
   5, "Endgame Tool Forms & Rack", "Harvest Tools & 6-Slot Tool Kit", "HARVEST & RACK",
   "Speed-24 unbreakable harvesting forms paired with a 6-slot auto-swapping Tool Kit rack.",
   "Tool Kit Key: [Tap V] Draw/Stow · [Hold V] Radial Wheel",
   0xFFFFD38A,
   () -> new ItemStack(AdventurersPack.ENDGAME_PICKAXE.get()),
   () -> new ItemStack[]{
    new ItemStack(AdventurersPack.ENDGAME_PICKAXE.get()),
    new ItemStack(AdventurersPack.ENDGAME_AXE.get()),
    new ItemStack(AdventurersPack.ENDGAME_SHOVEL.get()),
    new ItemStack(AdventurersPack.ENDGAME_HOE.get())
   },
   new Stat[]{
    new Stat("Mining Speed", "24.0 (All 4 Tool Forms)", 1.00f, 0xFFFFD38A),
    new Stat("Axe / Shovel DMG", "48 DMG / 34.5 DMG", 0.94f, 0xFF55FF55),
    new Stat("Pick / Hoe DMG", "30 DMG / 18 DMG", 0.65f, 0xFF56FAD8),
    new Stat("Tool Kit Rack", "6 Auto-Draw Slots", 1.00f, 0xFFC878FF)
   },
   new Section[]{
    new Section("ENDGAME HARVEST FORMS",
     "Pickaxe (30 DMG), Axe (48 DMG), Shovel (34.5 DMG), and Hoe (18 DMG) all harvest at speed 24.0 (faster and higher tier than Netherite) and never lose durability."
    ),
    new Section("6-SLOT TOOL KIT & VIRTUAL TOOL HAND",
     "The 6 slots beneath your character pedestal on the Equipment panel hold any tools, shears, tridents, or Endgame.",
     "Tap [V] to draw or stow a rack tool into a dedicated virtual equipment slot opposite your offhand—leaving all 9 of your regular hotbar slots untouched! Hold [V] to pick a specific slot from the radial wheel.",
     "When auto-tool selection or the Mining/Combat preset is active, left-clicking blocks, tilling dirt, or attacking enemies automatically draws the optimal tool or weapon from your rack and returns it when finished."
    )
   }
  ),

  // ==================== CATEGORY 6: ARMOR & FORGE ====================
  new Entry(
   6, "Crimson Armor (Ignis)", "Crimson Armor Set", "IMMORTAL ARMOR",
   "Indestructible fireproof battle plate forged from Crimson Ingots—50% stronger than Netherite.",
   "Material: Crimson Ingot  |  Winged Variant: Unbreakable Elytra Flight",
   0xFFFF5544,
   () -> new ItemStack(AdventurersPack.IGNIS_CHESTPLATE_ELYTRA.get()),
   () -> new ItemStack[]{
    new ItemStack(AdventurersPack.IGNIS_HELMET.get()),
    new ItemStack(AdventurersPack.IGNIS_CHESTPLATE.get()),
    new ItemStack(AdventurersPack.IGNIS_CHESTPLATE_ELYTRA.get()),
    new ItemStack(AdventurersPack.IGNIS_LEGGINGS.get()),
    new ItemStack(AdventurersPack.IGNIS_BOOTS.get()),
    new ItemStack(AdventurersPack.CRIMSON_INGOT.get())
   },
   new Stat[]{
    new Stat("Total Defense", "31 Armor (5 / 12 / 9 / 5)", 0.65f, 0xFFFF5544),
    new Stat("Armor Toughness", "4.5 per Piece (18 Total)", 0.65f, 0xFFFF9944),
    new Stat("Knockback Res.", "15% per Piece (60% Total)", 0.65f, 0xFFFFD770),
    new Stat("Durability", "Infinite + Elytra Glide", 1.00f, 0xFF7FE146)
   },
   new Section[]{
    new Section("CRIMSON PLATE SPECIFICATIONS",
     "Forged from Crimson Ingots, every piece provides 50% more defense, toughness, and knockback resistance than Netherite (Helmet 5, Chestplate 12, Leggings 9, Boots 5; Toughness 4.5; Knockback Res 0.15).",
     "100% Indestructible and Fire Resistant: Never loses durability in combat, fire, or lava.",
     "Winged Crimson Chestplate: Combines full 12-defense chest armor with unbreakable Elytra gliding and animated folding dragon wings."
    )
   }
  ),
  new Entry(
   6, "End Game Armor (Cursium)", "End Game Armor Set", "MYTHIC ARMOR",
   "Supreme indestructible regalia forged from End Game Ingots—300% of Netherite's power with true flight.",
   "Material: End Game Ingot  |  Chestplate Passive: Creative Flight + Elytra Glide",
   0xFF56FAD8,
   () -> new ItemStack(AdventurersPack.CURSIUM_CHESTPLATE_ELYTRA.get()),
   () -> new ItemStack[]{
    new ItemStack(AdventurersPack.CURSIUM_CIRCLET.get()),
    new ItemStack(AdventurersPack.CURSIUM_CHESTPLATE.get()),
    new ItemStack(AdventurersPack.CURSIUM_CHESTPLATE_ELYTRA.get()),
    new ItemStack(AdventurersPack.CURSIUM_SKIRT.get()),
    new ItemStack(AdventurersPack.CURSIUM_BOOTS.get()),
    new ItemStack(AdventurersPack.ENDGAME_INGOT.get())
   },
   new Stat[]{
    new Stat("Total Defense", "60 Armor (9 / 24 / 18 / 9)", 1.00f, 0xFF56FAD8),
    new Stat("Armor Toughness", "9.0 per Piece (36 Total)", 1.00f, 0xFFC878FF),
    new Stat("Knockback Res.", "30% per Piece (100%+ Cap)", 1.00f, 0xFFFFD770),
    new Stat("Flight Mastery", "True Flight + Elytra Wings", 1.00f, 0xFF7FE146)
   },
   new Section[]{
    new Section("ULTIMATE DEFENSE & ASCENDED FLIGHT",
     "Forged from End Game Ingots with triple the stats of Netherite: Defense 9 Helmet, 24 Chestplate, 18 Leggings, 9 Boots (60 total), 9.0 Toughness per piece, 0.30 Knockback Resistance per piece, and 45 Enchantability.",
     "True Creative Flight: Wearing either the End Game Chestplate or Winged End Game Chestplate grants full powered flight in Survival mode!",
     "Winged End Game Chestplate: Also unlocks high-speed Elytra gliding with animated arcane wings and Quiver firework boosting."
    )
   }
  ),
  new Entry(
   6, "Expedition Dock", "Expedition Dock Station", "BASE AUTOMATION",
   "Base-camp docking station that displays your pack, ticks internal workstations, and connects pipes & hoppers.",
   "Place Pack: [Right-Click w/ Pack]  |  Retrieve: [Shift + Right-Click Empty Hand]",
   0xFFFFB84D,
   () -> new ItemStack(AdventurersPack.DOCK_ITEM.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.DOCK_ITEM.get()), tierPack(4), new ItemStack(Items.HOPPER)},
   new Stat[]{
    new Stat("Workstation Tick", "Smelts, Brews & Charges", 1.0f, 0xFFFF7744),
    new Stat("Item Hoppers", "Insert & Extract (Safe [*])", 1.0f, 0xFFFFB84D),
    new Stat("Fluid Piping", "Pumps Tank 1 & Tank 2", 1.0f, 0xFF55DDFF),
    new Stat("Energy Cables", "Charges 1,000,000 FE Bank", 1.0f, 0xFF3DDC97)
   },
   new Section[]{
    new Section("DOCKING & BASE AUTOMATION",
     "Right-click a placed Expedition Dock while holding a backpack to mount it. Right-click the dock to open the full backpack interface in-place; close it and [Shift + Right-Click] with an empty hand to pick the pack back up.",
     "Active Processing: A docked pack continuously runs its Ember Core furnace, Field Alembic brewing stand, and Forge Battery item charger.",
     "Pipe & Hopper Logistics: Hoppers insert and extract items while respecting [*] protected slots. Fluid pipes fill or drain both Fluid Tanks, and Forge Energy cables charge or draw from the Forge Battery."
    )
   }
  ),
  new Entry(
   6, "Fold-Out Camp & Compass", "Fold-Out Camp Kit & Compass", "FIELD EXPEDITION",
   "Deployable 4-block survival campsite that packs back up with all loot and cooking progress intact.",
   "Workstations: [Camp] & [Compass]  |  Camp Area: 3×2 Clear Ground Ahead",
   0xFF7FE146,
   () -> new ItemStack(AdventurersPack.CAMP_KIT.get()),
   () -> new ItemStack[]{new ItemStack(AdventurersPack.CAMP_KIT.get()), new ItemStack(Items.RED_BED), new ItemStack(Items.BARREL), new ItemStack(Items.FURNACE), new ItemStack(Items.LANTERN)},
   new Stat[]{
    new Stat("Camp Footprint", "3×2 Area (3 Blocks Ahead)", 0.85f, 0xFF7FE146),
    new Stat("Pack-Up Range", "Within 10 Blocks", 1.00f, 0xFF56FAD8),
    new Stat("State Memory", "Saves Loot, Fuel & Cook %", 1.00f, 0xFFFFD770),
    new Stat("Built-In Compass", "Zero-Slot HUD Rose", 1.00f, 0xFF55BBFF)
   },
   new Section[]{
    new Section("FOLD-OUT CAMP KIT",
     "Open the [Camp] panel and place a Fold-out Camp Kit into its dedicated slot (does not use an attachment, rune, or addon socket).",
     "Click [Set Up] to deploy an indestructible Bed, Lantern, Supply Barrel, and Camp Furnace 3 blocks ahead on clear 3x2 ground. Interact with the blocks directly or click [Supplies] and [Cooking] inside the pack.",
     "Click [Pack Up] within 10 blocks to fold the entire campsite back into the kit—preserving all barrel items, furnace inputs, fuel, and cooking progress!"
    ),
    new Section("BUILT-IN COMPASS & DIMENSIONAL WAYFINDER",
     "Click [Compass] on the workstation panel at any time to view your cardinal heading, exact degree bearing, and a live compass needle pointing toward World Spawn—no compass item required.",
     "Dimensional Wayfinder Upgrade: Unlock directly in the [Compass] panel for 2 Nether Stars, 4 Netherite Ingots, and 8 Eyes of Ender. Unlocks 10 bindable warp anchors across any dimension (Overworld, Nether, The End, or modded dimensions).",
     "Select any of the 10 slots, rename it (up to 12 characters), and click [Set Here] to save your coordinates and dimension. Click [Warp] to teleport there for 100 XP (from your XP Bank or player levels) or 1 Ender Pearl."
    )
   }
  ),
  new Entry(
   6, "Forged Ingots & Alloys", "Backpack, Crimson & End Game Ingots", "METALLURGY",
   "The three foundational alloys used to forge higher-tier packs, upgrades, immortal armor, and Endgame weapons.",
   "Inspect Recipes: Click Any Ingot Above to View Its 3×3 Crafting Blueprint",
   0xFFFFD770,
   () -> new ItemStack(AdventurersPack.ENDGAME_INGOT.get()),
   () -> new ItemStack[]{
    new ItemStack(AdventurersPack.BACKPACK_INGOT.get()),
    new ItemStack(AdventurersPack.CRIMSON_INGOT.get()),
    new ItemStack(AdventurersPack.ENDGAME_INGOT.get())
   },
   new Stat[]{
    new Stat("Backpack Ingot", "Pack Tiers & Upgrades", 0.70f, 0xFF55BBFF),
    new Stat("Crimson Ingot", "Crimson Armor & Mid-Tier", 0.85f, 0xFFFF5544),
    new Stat("End Game Ingot", "End Game Armor & Relic", 1.00f, 0xFFFFD770)
   },
   new Section[]{
    new Section("THREE TIERS OF METALLURGY",
     "Backpack Ingot: Crafted from Copper, Iron, Gold, Leather, and String. Forms the structural frame for pack upgrades, workstations, and Explorer/Ranger packs.",
     "Crimson Ingot: Fire-resistant alloy forged from Netherite Scrap, Blaze Powder, Magma Cream, Gold, and Redstone. Used to craft the unbreakable Crimson Armor set and Runic tier.",
     "End Game Ingot: Mythic alloy forged from Netherite Ingots, Crimson Ingots, Diamonds, Echo Shards, Amethyst, and Chorus Fruit. Forges the flight-capable End Game Armor set and Relic Pack."
    )
   }
  )
 };

 // Legacy TOPICS array kept intact for compatibility
 public static final Topic[] TOPICS = {
  new Topic("Pack", "Equipping and opening",
   "Right-click a pack you are holding to open that bag. To wear one, put it in the pack socket on the equipment page, or in a Curios slot. The pack socket wins when both are filled.",
   "E, or Open backpack (B), opens the worn pack. With nothing equipped, E still opens the equipment page so you can place a pack.",
   "A worn pack shows on your back. Each bag keeps its own contents. Your inventory is shared. Vanilla inventory, at the top of the pack screen, opens Minecraft's inventory.",
   "h:Tiers",
   "Crafting a higher tier keeps everything already stored. Each compartment holds:",
   "Traveler 27, Explorer 36, Ranger 45, Runic 54, Relic 63.",
   "Storage has a separate Slot Upgrades section: buy Tiers 1-5 in order for 384, 768, 1536, 3072 and 6144 items per normal slot. Costs: 16 iron, 16 gold, 8 diamonds, 16 emeralds, then 4 netherite ingots. Tools stay one per slot. Four compartments make 108 to 252 slots. Upgrades are Attachments, Runes, or Storage addons. Those topics list every item."),
  new Topic("Compartments", "Four compartments",
   "Click a tab to open that compartment. The tabs start as Mine, Build, Loot, and Tools. Search storage dims slots that do not match, and marks tabs that do.",
   "Shift-click moves a stack between your inventory and the open compartment. Alt-click protects a slot. A star marks it, and hoppers, restocking, and voiding leave it alone.",
   "On Controls, name the open compartment (12 characters), choose its color, and set its rule.",
   "h:Routing",
   "Turn on Route to Quick Deposit immediately and every second. Matching main-inventory items go to tabs already holding the same item or matching bulk storage. Hotbar, unmatched items, and overflow stay in your inventory.",
   "Rules are Ore, Blocks, Gear, Loot, Ore and Blocks, Any, or Selected. The starting rules are Mine = Ore, Build = Blocks, Loot = Loot, Tools = Gear."),
  new Topic("Bulk storage", "Bulk slots",
   "Eighteen slots under the compartments each hold one kind of plain stackable item, past a normal stack.",
   "Click with items to deposit. Click with an empty hand to take a stack. Right-click takes one. The count on the item is how many are stored. The teal percent above the slot is free space.",
   "Alt-click protects the slot. Ctrl-click selects that block for the Builder's Belt. Next bulk building block (N) moves the selection to the next block.",
   "h:How much fits",
   "Traveler holds 4,096 per slot. Each tier doubles that: Explorer 8,192, Ranger 16,384, Runic 32,768, Relic 65,536.",
   "The Deep Pockets Rune adds 61,440 on top. A Traveler slot then holds 65,536, Explorer 69,632, Ranger 77,824, Runic 94,208, and Relic 126,976. The config values baseBulkCapacity and upgradedBulkCapacity change these defaults."),
  new Topic("Tool kit", "Six tool slots",
   "The six slots under Equipment take tools, shears, tridents, and Endgame.",
   "Tap your Tool kit key to move the hotbar cursor onto the tool kit slot and draw the first usable tool, or to put it away. You do not need a tool in your hand. Hold the key, aim at a slot, and release. The center, Escape, or right-click cancels. A tool you pick this way stays out until you stow it.",
   "The active tool uses a separate equipment slot opposite the offhand on your hotbar. It leaves all normal inventory slots untouched. A tool the pack draws for you returns to its rack when you stop.",
   "The Mining preset draws the best rack tool when you mine. The Combat preset draws the strongest rack weapon when you attack, and turns an Endgame bow or tool on the rack into the lance. Sword and staff stay as they are. Other presets draw tools while automatic tool selection is on."),
  new Topic("Attachments", "Attachment upgrades",
   "c:Attachment",
   "Attachments fit the six attachment sockets on Upgrades. Every pack opens all six. Shift-click one from your inventory to install it. Only one of each item fits. Alt + right-click an installed attachment to disable or enable it. A disabled one stays in the socket and does nothing.",
   "h:Artisan's Crafting Kit",
   "c:Attachment",
   "Opens the Craft page: a 3x3 grid for normal recipes. Ingredients left in the grid come back when the grid or the pack closes.",
   "Install: attachment socket on Upgrades.",
   "h:Ember Core",
   "c:Attachment",
   "Opens the Smelt page. Normal furnace recipes and fuel. It keeps smelting while the pack is closed, and while the pack is in an Expedition Dock. Taking the output gives the smelting experience for that batch.",
   "Install: attachment socket on Upgrades.",
   "h:Ender Chest",
   "c:Attachment",
   "Opens your personal ender chest, the same storage as a placed one. Packs cannot go inside.",
   "Install: attachment socket on Upgrades.",
   "h:Builder's Belt",
   "c:Attachment",
   "Lets you place the selected bulk block while holding the pack. Turn on Build mode under Controls. Ctrl-click a bulk block to select it, or press N to cycle. Shift + right-click still opens the pack.",
   "Install: attachment socket on Upgrades.",
   "h:Cave Torch",
   "c:Attachment",
   "Opens the Torches page with 9 slots for torches and soul torches. Automatically places torches in dark spots nearby as you explore.",
   "Install: attachment socket on Upgrades.",
   "h:Provision Pouch",
   "c:Attachment",
   "Eighteen food slots. When you are hungry it eats from those slots, trying the first slot first. Foods that carry status effects are skipped, and a meal is skipped if it would fill you past full. It then checks unprotected compartment slots.",
   "Install: the Food socket on Pouches, or an attachment socket. The food is stored on the pouch itself."),
  new Topic("Runes", "Rune upgrades",
   "c:Rune",
   "Runes fit the five rune sockets on Upgrades. Every pack opens all five. Shift-click to install. Only one of each item. Alt + right-click disables or enables it.",
   "h:Pickup Rune",
   "c:Rune",
   "With Auto pickup on, collected drops go into the pack instead of your inventory. Order: matching pouches, then the quiver, then torches, then a matching bulk slot, then a compartment.",
   "Install: rune socket. Then turn Auto pickup on under Controls.",
   "h:Gathering Rune",
   "c:Rune",
   "Pulls nearby drops in from 5 blocks. It needs a Pickup Rune and Auto pickup. The magnetRange config changes the distance.",
   "Install: rune socket.",
   "h:Deep Pockets Rune",
   "c:Rune",
   "Adds 61,440 items to every bulk slot. A Traveler slot goes from 4,096 to 65,536. Higher tiers add the same bonus on top of their doubled base. Removing the rune does not delete items already stored; a full slot just refuses more.",
   "Install: rune socket.",
   "h:Restock Rune",
   "c:Rune",
   "Tops up hotbar stacks you already have. It pulls from a matching bulk slot, then compartments. Protected slots are skipped. Empty hotbar slots stay empty.",
   "Install: rune socket.",
   "h:Void Sigil",
   "c:Rune",
   "With Void overflow on, leftover items are deleted only when they match a bulk slot that is already full and not protected. Nothing else is voided.",
   "Install: rune socket. Then turn Void overflow on under Controls."),
  new Topic("Storage addons", "Storage addon upgrades",
   "c:Storage addon",
   "Quiver, Philter, Field Alembic, Fluid Tank, Forge Battery, and Experience Bank go in the six storage addon sockets on Upgrades. Shift-click to install. One of each. These are not toggled with Alt + right-click.",
   "Forager, Gem, and Cartographer pouches are storage addons too. They install on the Pouches page, not in those six sockets. What you put in them stays on the pouch item when you take it out.",
   "h:Quiver",
   "c:Storage addon",
   "Nine slots for arrows and fireworks. Bows and crossbows take ammo from it first. While gliding, right-click uses a firework from it.",
   "Install: storage addon socket. Open Quiver.",
   "h:Philter",
   "c:Storage addon",
   "Three potion vials. Place a potion in a vial. A drinkable potion returns its bottle.",
   "Install: storage addon socket. Open Potions.",
   "h:Field Alembic",
   "c:Storage addon",
   "Brews with bottles, an ingredient, and blaze powder. It keeps brewing while carried or docked. Finished potions can pour into an installed Philter.",
   "Install: storage addon socket. Open Brew.",
   "h:Fluid Tank",
   "c:Storage addon",
   "Two tanks. Each holds 128 buckets, 128,000 mB, of its own fluid. The gauges draw each fluid's texture and tint.",
   "Install: storage addon socket. Open Tank. Each tank has Input and Output bucket slots. Place a container in either slot to transfer automatically.",
   "h:Forge Battery",
   "c:Storage addon",
   "Stores 1,000,000 FE. The In slot drains an energy item into the battery. The Out slot charges an item from the battery. Each move is 1,000 FE per tick.",
   "Install: storage addon socket. Open Battery. While the pack is in an Expedition Dock, energy pipes can charge and drain it.",
   "h:Experience Bank",
   "c:Storage addon",
   "Holds 200,000 experience points. Store or take 100, or all. It does not pick up experience orbs.",
   "Install: storage addon socket. Open XP.",
   "h:Forager Pouch",
   "c:Storage addon",
   "Eighteen slots for seeds, flowers, saplings, and crops such as wheat, sugar cane, bamboo, kelp, mushrooms, and berries.",
   "Install: Seeds on the Pouches page.",
   "h:Gem Pouch",
   "c:Storage addon",
   "Eighteen slots for gems, raw minerals, nuggets, and ingots.",
   "Install: Gem on the Pouches page.",
   "h:Cartographer Pouch",
   "c:Storage addon",
   "Eighteen slots for maps, compasses, paper, and books.",
   "Install: Maps on the Pouches page."),
  new Topic("Crafting", "Crafting grid",
   "c:Attachment",
   "Requires the Artisan's Crafting Kit in an attachment socket.",
   "Open Craft. The grid uses normal recipes, and JEI can fill it. Ingredients still in the grid return to you when you close the grid or the pack."),
  new Topic("Furnace", "Portable furnace",
   "c:Attachment",
   "Requires the Ember Core in an attachment socket.",
   "Open Smelt. Input, fuel, and output work like a furnace, including the burn time of the fuel. Smelting continues with the pack closed or sitting in a dock.",
   "The experience for a finished recipe is stored until you take the output. Taking part of a stack gives you that share."),
  new Topic("Quiver", "Arrows and fireworks",
   "c:Storage addon",
   "Requires the Quiver in a storage addon socket.",
   "Open Quiver. Nine slots accept arrows and fireworks. Bows and crossbows pull ammo from the quiver before your inventory. While gliding, right-click spends a firework from the quiver.",
   "With a Pickup Rune, collected arrows and fireworks file into the quiver before bulk storage."),
  new Topic("Potions", "Philter vials",
   "c:Storage addon",
   "Requires the Philter in a storage addon socket.",
   "Open Potions. Three vials. Put a potion in one. A drinkable potion returns its glass bottle. The teal outline is the selected vial.",
   "Use potion drinks or throws the selected vial. Next moves the selection. Use philter vial (U) does the same with the pack closed. Splash and lingering potions are thrown. Other potions are drunk, and the bottle comes back.",
   "Right-click a vial to take the potion out. A drinkable potion needs a glass bottle in your inventory."),
  new Topic("Brewing", "Field Alembic",
   "c:Storage addon",
   "Requires the Field Alembic in a storage addon socket.",
   "Open Brew. Three bottle slots, one ingredient slot, and blaze powder. One blaze powder is 20 charges. Brewing uses normal recipes and takes the usual time. It continues while you carry the pack and while the pack is docked.",
   "Pour into vials appears when a Philter is installed. It moves finished potions into empty vials. A drinkable result leaves a glass bottle in the stand."),
  new Topic("Ender chest", "Personal ender storage",
   "c:Attachment",
   "Requires the Ender Chest in an attachment socket.",
   "Open Ender. These 27 slots are your ender chest. A placed ender chest sees the same items. Packs cannot be stored here."),
  new Topic("Pouches", "Filtered pouches",
   "Open Pouches. Four sockets, 18 slots each. The items are saved on the pouch, so they leave with it. Each pouch also has a 9-slot Whitelist/Blacklist filter bar.",
   "h:Seeds",
   "c:Storage addon",
   "Forager Pouch. Seeds, flowers, saplings, wheat, sugar cane, bamboo, kelp, mushrooms, sweet berries, and glow berries.",
   "h:Gem",
   "c:Storage addon",
   "Gem Pouch. Gems, raw minerals, nuggets, and ingots.",
   "h:Food",
   "c:Attachment",
   "Provision Pouch. Any food. Automatic meals try the first slot first, skip foods with status effects, and skip a meal that would overfill you.",
   "h:Maps",
   "c:Storage addon",
   "Cartographer Pouch. Maps, compasses, recovery compasses, paper, and books.",
   "Shift-click a pouch to install it. Shift-click matching supplies, or pick them up with the Pickup Rune, and they fill the pouch before the rest of the pack."),
  new Topic("Camp", "Fold-out camp",
   "The Fold-out Camp Kit is not an attachment, rune, or storage addon. Open Camp and place it in the kit slot.",
   "Set Up places a bed, lantern, barrel, and furnace on clear ground three blocks ahead. It needs a 3 by 2 area of sturdy ground, and the dimension must allow beds.",
   "Use those blocks, or press Supplies and Cooking to open the barrel and furnace.",
   "Pack Up works within 10 blocks, in the same dimension, with nobody sleeping there and nearby containers closed. Barrel contents, furnace items, fuel, and cooking progress are packed into the kit.",
   "Use Pack Up rather than breaking the camp. Survival players cannot break, push, or explode it."),
  new Topic("Controls", "Presets and keys",
   "The Controls page is where pickup, voiding, building, and routing are switched.",
   "Preset cycles Travel, Mining, Building, and Combat. Cycle activity preset (R) does the same. Mining and Building turn auto pickup on. Building also turns build mode on. Any other preset turns build mode off.",
   "Auto pickup needs the Pickup Rune. Void overflow needs the Void Sigil. Build mode needs the Builder's Belt. Route automatically Quick Deposits matching main-inventory items into their existing tabs once per second.",
   "Hold Quick-access wheel (G) and release over a compartment or a preset. The center cancels.",
   "Cycle offhand kit (Y) swaps your offhand with the next filled offhand slot. There are four, on the equipment page. A pack in your offhand is not swapped.",
   "Ctrl + Alt-click an item to track it. Up to four supply lines appear under bulk storage. Click a line to raise the goal, then clear it. The count includes the pack and what you are carrying.",
   "Name, Color, and Rule edit the open compartment.",
   "Rebind every key under Adventure's Backpack in Controls. Defaults: E or B open, V tools, N next block, G wheel, R preset, Y offhand, U potion, Z Endgame forms."),
  new Topic("Fluid tank", "Two fluids",
   "c:Storage addon",
   "Requires the Fluid Tank in a storage addon socket. Press Tank.",
   "Tank 1 and Tank 2 each hold one fluid, 128 buckets, which is 128,000 mB. Each gauge is filled with that fluid's texture and tint.",
   "Each tank has two bucket slots. Input empties a container into that tank. Output fills a container from that tank. Transfers happen automatically; take the resulting bucket from the same slot. Each slot holds one container. Different fluids cannot mix.",
   "Taking the Fluid Tank out of its socket leaves both fluids stored in the pack. Put the addon back to move fluid again. A container left in the slot is returned to you when the addon is removed.",
   "While the pack is in an Expedition Dock, fluid pipes can fill a matching tank, or the first empty tank, and can drain either tank."),
  new Topic("Battery", "Forge Energy",
   "c:Storage addon",
   "Requires the Forge Battery in a storage addon socket. Press Battery.",
   "The battery holds 1,000,000 FE. Put an energy item in In to drain it into the battery. Put an energy item in Out to charge it from the battery. Each transfer moves up to 1,000 FE per tick.",
   "The stored energy stays on the pack if you remove the addon. Items left in the slots are returned to you.",
   "While the pack is in an Expedition Dock, energy pipes can charge and drain the battery."),
  new Topic("Experience", "Experience Bank",
   "c:Storage addon",
   "Requires the Experience Bank in a storage addon socket. Press XP.",
   "The bank holds 200,000 experience points, not levels. Orbs are not collected for you.",
   "Store 100 and Take 100 move up to 100 points between you and the bank. Store all and Take all move as many as you and the bank can hold.",
   "The points stay on the pack if you remove the addon. Install it again to move them."),
  new Topic("Dock", "Expedition Dock",
   "Place an Expedition Dock. Right-click it with a pack to put that pack in. Right-click to open it. Shift-right-click with an empty hand to take the pack out, after closing it. Only one player can have it open.",
   "Hoppers can insert and extract. Protected compartment slots and protected bulk slots are skipped.",
   "A docked Ember Core keeps smelting. A docked Field Alembic keeps brewing. A docked Forge Battery keeps charging the items in its slots. Fluid pipes can use the two tanks, and energy pipes can use the battery, while those addons are installed. Breaking the dock drops the pack."),
  new Topic("Curios", "Back slot",
   "With Curios, this mod adds a back slot. A pack in any Curios slot counts as worn when the equipment socket is empty, and it can show on your back.",
   "The pack socket on the equipment page is used first when both are filled. E and B open that preferred pack. Right-click a pack in your hand to open that bag instead."),
  new Topic("Compass", "Built-in compass and warping",
   "Press Compass. No compass item is required, and none is added to your inventory.",
   "It shows the direction you are facing. The needle follows vanilla compass rules: world spawn in the Overworld, and the usual result in other dimensions.",
   "h:Dimensional Wayfinder",
   "Click Unlock Warping on the Compass page to permanently unlock 10 warp slots on the pack. It costs 2 Nether Stars, 4 Netherite Ingots, and 8 Eyes of Ender from your inventory.",
   "Choose any of the 10 slots, rename it, and click Set Here to bind your current coordinates and dimension. Click Warp to teleport to that anchor across any dimension for 100 XP (from your Experience Bank or levels) or 1 Ender Pearl."),
  new Topic("Endgame", "Ten forms",
   "Endgame is one weapon with ten forms: lance, pickaxe, axe, shovel, hoe, sword, staff, shortbow, recurve bow, and greatbow. The name and enchantments carry to every form. None of them use durability.",
   "Attack, as shown when you hold them: sword 45, staff 51, lance 48, pickaxe 30, axe 48, shovel 34.5, hoe 18. The staff and the lance both add 1.5 blocks of reach. Tools mine at speed 24.",
   "Arrow damage is 7.5 for the shortbow, 9.3 for the recurve, and 12 for the greatbow. Draw time is 12, 20, or 32 ticks. They use arrows and bow enchantments.",
   "h:Choosing a form",
   "Hold Endgame Forms (Z). The wheel order is lance, pickaxe, axe, shovel, hoe, sword, staff, shortbow, recurve, greatbow. Release over a form, or press 1 through 9 or 0. Escape, right-click, or the center cancels.",
   "Shift + right-click steps from sword to staff to lance, then cycles lance, pickaxe, axe, shovel, and hoe. Choose bows from the wheel.",
   "h:What each form does",
   "Shortbow: a full draw fires three arrows and spends one. The side arrows cannot be picked up.",
   "Recurve: a full draw is more accurate and pierces up to three enemies.",
   "Greatbow: a full draw adds strong knockback.",
   "Lance: hold right-click for half a second and release to throw it. It returns after impact or a short flight, and waits nearby if your inventory is full.",
   "Sword and staff sweep. The tool forms mine and till like the tool they are."),
  new Topic("Armor", "Two unbreakable sets",
   "Both sets have no durability.",
   "h:Crimson",
   "Forged from Crimson Ingots. Defense is 5 helmet, 12 chest, 9 legs, and 5 boots. Toughness is 4.5. Knockback resistance is 0.15. The winged chestplate flies like an elytra and does not wear down.",
   "h:End Game",
   "Forged from End Game Ingots. Defense is 9, 24, 18, and 9. Toughness is 9. Knockback resistance is 0.3. The chestplate lets you fly while you wear it. The winged chestplate also flies like an elytra.",
   "Equip either set in your armor slots. The pack screen shows those slots beside you.")
 };
}
