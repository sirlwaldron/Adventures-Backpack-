package com.waldron.adventurerspack;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(AdventurersPack.ID)
public class AdventurersPack {
 public static final String ID="adventurerspack";
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
 public static final DeferredRegister<RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ID);
 public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ID);
 public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
 public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
 public static final RegistryObject<net.minecraft.world.level.block.Block> DOCK=BLOCKS.register("expedition_dock",DockBlock::new);
 public static final RegistryObject<Item> DOCK_ITEM=ITEMS.register("expedition_dock",()->new BlockItem(DOCK.get(),new Item.Properties()));
 public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<DockEntity>> DOCK_ENTITY=ENTITIES.register("expedition_dock",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(DockEntity::new,DOCK.get()).build(null));
 public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> PROJECTILES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
 public static final RegistryObject<Item> REQUIEM=ITEMS.register("requiem",()->new RequiemMeleeItem(false));
 public static final RegistryObject<Item> REQUIEM_SPEAR=ITEMS.register("requiem_spear",()->new RequiemMeleeItem(true));
 public static final RegistryObject<Item> REQUIEM_SHORTBOW=ITEMS.register("requiem_shortbow",()->new RequiemBowItem(RequiemBowItem.Style.SHORTBOW,12,2.8f,7.5));
 public static final RegistryObject<Item> REQUIEM_RECURVE=ITEMS.register("requiem_recurve",()->new RequiemBowItem(RequiemBowItem.Style.RECURVE,20,3.3f,9.3));
 public static final RegistryObject<Item> REQUIEM_GREATBOW=ITEMS.register("requiem_greatbow",()->new RequiemBowItem(RequiemBowItem.Style.GREATBOW,32,4.2f,12));
 public static final RegistryObject<Item> ENDGAME=ITEMS.register("endgame",EndgameLanceItem::new);
 public static final RegistryObject<Item> ENDGAME_PICKAXE=ITEMS.register("endgame_pickaxe",EndgamePickaxeItem::new);
 public static final RegistryObject<Item> ENDGAME_AXE=ITEMS.register("endgame_axe",EndgameAxeItem::new);
 public static final RegistryObject<Item> ENDGAME_SHOVEL=ITEMS.register("endgame_shovel",EndgameShovelItem::new);
 public static final RegistryObject<Item> ENDGAME_HOE=ITEMS.register("endgame_hoe",EndgameHoeItem::new);
 public static final RegistryObject<net.minecraft.world.entity.EntityType<EndgameProjectile>> ENDGAME_PROJECTILE=PROJECTILES.register("endgame_lance",()->net.minecraft.world.entity.EntityType.Builder.<EndgameProjectile>of(EndgameProjectile::new,net.minecraft.world.entity.MobCategory.MISC).sized(0.5f,0.5f).clientTrackingRange(8).updateInterval(1).fireImmune().build(ID+":endgame_lance"));
 public static final RegistryObject<Item> PACK=ITEMS.register("adventurers_pack",PackItem::new);
 public static final RegistryObject<Item> PICKUP=upgrade("pickup_rune",true);
 public static final RegistryObject<Item> MAGNET=upgrade("gathering_rune",true);
 public static final RegistryObject<Item> CAPACITY=upgrade("deep_pockets_rune",true);
 public static final RegistryObject<Item> FEEDING=upgrade("provision_pouch",false);
 public static final RegistryObject<Item> RESTOCK=upgrade("restock_rune",true);
 public static final RegistryObject<Item> CRAFTING=upgrade("crafting_kit",false);
 public static final RegistryObject<Item> FURNACE=upgrade("ember_core",false);
 public static final RegistryObject<Item> ENDER=upgrade("ender_chest",false);
 public static final RegistryObject<Item> BUILDER=upgrade("builders_belt",false); public static final RegistryObject<Item> TORCH=upgrade("cave_torch",false);
 public static final RegistryObject<Item> VOID=upgrade("void_sigil",true);
 public static final RegistryObject<Item> TANK=section("tank_upgrade");
 public static final RegistryObject<Item> BATTERY=section("battery_upgrade");
 public static final RegistryObject<Item> XP_BANK=section("xp_storage_upgrade");
 public static final RegistryObject<Item> QUIVER=section("quiver");
 public static final RegistryObject<Item> PHILTER=section("philter");
 public static final RegistryObject<Item> ALEMBIC=section("alembic");
 public static final RegistryObject<Item> FORAGER=ITEMS.register("forager_pouch",()->new PouchItem("forager_pouch",0));
 public static final RegistryObject<Item> GEM=ITEMS.register("gem_pouch",()->new PouchItem("gem_pouch",1));
 public static final RegistryObject<Item> CARTOGRAPHER=ITEMS.register("cartographer_pouch",()->new PouchItem("cartographer_pouch",3));
 public static final RegistryObject<Item> CAMP_KIT=ITEMS.register("camp_kit",()->new Item(new Item.Properties().stacksTo(1)));
 public static final RegistryObject<Item> GUIDE=ITEMS.register("field_guide",GuideBook::new);
 public static final RegistryObject<Item> IGNIS_HELMET=ITEMS.register("ignis_helmet",()->new IgnisArmorItem(ArmorItem.Type.HELMET,false));
 public static final RegistryObject<Item> IGNIS_CHESTPLATE=ITEMS.register("ignis_chestplate",()->new IgnisArmorItem(ArmorItem.Type.CHESTPLATE,false));
 public static final RegistryObject<Item> IGNIS_CHESTPLATE_ELYTRA=ITEMS.register("ignis_chestplate_elytra",()->new IgnisArmorItem(ArmorItem.Type.CHESTPLATE,true));
 public static final RegistryObject<Item> IGNIS_LEGGINGS=ITEMS.register("ignis_leggings",()->new IgnisArmorItem(ArmorItem.Type.LEGGINGS,false));
 public static final RegistryObject<Item> IGNIS_BOOTS=ITEMS.register("ignis_boots",()->new IgnisArmorItem(ArmorItem.Type.BOOTS,false));
 public static final RegistryObject<Item> CURSIUM_CIRCLET=ITEMS.register("cursium_mage_circlet",()->new CursiumArmorItem(ArmorItem.Type.HELMET,false));
 public static final RegistryObject<Item> CURSIUM_CHESTPLATE=ITEMS.register("cursium_mage_chestplate",()->new CursiumArmorItem(ArmorItem.Type.CHESTPLATE,false));
 public static final RegistryObject<Item> CURSIUM_CHESTPLATE_ELYTRA=ITEMS.register("cursium_mage_elytra",()->new CursiumArmorItem(ArmorItem.Type.CHESTPLATE,true));
 public static final RegistryObject<Item> CURSIUM_SKIRT=ITEMS.register("cursium_mage_skirt",()->new CursiumArmorItem(ArmorItem.Type.LEGGINGS,false));
 public static final RegistryObject<Item> CURSIUM_BOOTS=ITEMS.register("cursium_mage_boots",()->new CursiumArmorItem(ArmorItem.Type.BOOTS,false));
 public static final RegistryObject<Item> CRIMSON_INGOT=ITEMS.register("crimson_ingot",()->new Item(new Item.Properties().fireResistant()));
 public static final RegistryObject<Item> ENDGAME_INGOT=ITEMS.register("endgame_ingot",()->new Item(new Item.Properties().fireResistant().rarity(Rarity.EPIC)));
 public static final RegistryObject<Item> BACKPACK_INGOT=ITEMS.register("backpack_ingot",()->new Item(new Item.Properties().rarity(Rarity.RARE)));
 public static final RegistryObject<RecipeSerializer<PackTierRecipe>> TIER_RECIPE=RECIPES.register("pack_tier",PackTierRecipe.Serializer::new);
 public static final RegistryObject<MenuType<PackMenu>> MENU=MENUS.register("pack",()->IForgeMenuType.create(PackMenu::new));
 public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.adventurerspack")).icon(()->new ItemStack(PACK.get())).displayItems((params,output)->{ITEMS.getEntries().forEach(i->{if(i!=PACK)output.accept(i.get());});for(int tier=0;tier<5;tier++){ItemStack pack=new ItemStack(PACK.get());pack.getOrCreateTagElement("Pack").putInt("Tier",tier);output.accept(pack);}}).build());
 private static RegistryObject<Item> upgrade(String name,boolean rune){return ITEMS.register(name,()->new UpgradeItem(name,rune,false));}
 private static RegistryObject<Item> section(String name){return ITEMS.register(name,()->new UpgradeItem(name,false,true));}
 public AdventurersPack(){
  var bus=FMLJavaModLoadingContext.get().getModEventBus(); ITEMS.register(bus);PROJECTILES.register(bus); MENUS.register(bus);BLOCKS.register(bus);ENTITIES.register(bus);RECIPES.register(bus);TABS.register(bus);
  bus.addListener((net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent e)->e.register(PackEquipment.class));
  bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e)->e.enqueueWork(()->{if(net.minecraftforge.fml.ModList.get().isLoaded("azurelib")){mod.azure.azurelib.animation.cache.AzIdentityRegistry.register(IGNIS_CHESTPLATE_ELYTRA.get());mod.azure.azurelib.animation.cache.AzIdentityRegistry.register(CURSIUM_CHESTPLATE_ELYTRA.get());}}));
  Network.init();
  CuriosPacks.init();
  ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,PackConfig.SPEC);
  ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,PackClientConfig.SPEC);
  MinecraftForge.EVENT_BUS.register(new PackEvents());
 }
 public static class UpgradeItem extends Item {
  final String name; public final boolean rune,section;
  UpgradeItem(String name,boolean rune,boolean section){super(new Item.Properties().stacksTo(1));this.name=name;this.rune=rune;this.section=section;}
  @Override public void appendHoverText(ItemStack s,net.minecraft.world.level.Level l,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable(section?"upgrade.adventurerspack.kind.section":rune?"upgrade.adventurerspack.kind.rune":"upgrade.adventurerspack.kind.gear"));lines.add(Component.translatable("upgrade.adventurerspack."+name));if(!section){lines.add(Component.literal(PackData.upgradeEnabled(s)?"Enabled":"Disabled"));lines.add(Component.literal("Alt + right-click installed upgrade to toggle"));}}
 }
}

