package com.waldron.adventurerspack.client.jei;

import com.waldron.adventurerspack.AdventurersPack;
import com.waldron.adventurerspack.PackMenu;
import com.waldron.adventurerspack.client.PackScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@JeiPlugin
public class PackJeiPlugin implements IModPlugin {
    public static final ResourceLocation UID = new ResourceLocation(AdventurersPack.ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(PackScreen.class, new IScreenHandler<PackScreen>() {
            @Override
            public IGuiProperties apply(PackScreen screen) {
                if (screen.getMinecraft() == null || screen.getMinecraft().getWindow() == null) {
                    return null;
                }
                int screenW = screen.getMinecraft().getWindow().getGuiScaledWidth();
                int screenH = screen.getMinecraft().getWindow().getGuiScaledHeight();
                int guiLeft = screen.getActualGuiLeft();
                int guiTop = screen.getActualGuiTop();
                int guiWidth = screen.getActualGuiWidth();
                int guiHeight = screen.getActualGuiHeight();

                return new IGuiProperties() {
                    @Override
                    public Class<? extends Screen> getScreenClass() {
                        return PackScreen.class;
                    }

                    @Override
                    public int getGuiLeft() {
                        return guiLeft;
                    }

                    @Override
                    public int getGuiTop() {
                        return guiTop;
                    }

                    @Override
                    public int getGuiXSize() {
                        return guiWidth;
                    }

                    @Override
                    public int getGuiYSize() {
                        return guiHeight;
                    }

                    @Override
                    public int getScreenWidth() {
                        return screenW;
                    }

                    @Override
                    public int getScreenHeight() {
                        return screenH;
                    }
                };
            }

            @Override
            public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory, PackScreen screen, double mouseX, double mouseY) {
                return findIngredient(factory, screen, mouseX, mouseY);
            }
        });

        registration.addGuiContainerHandler(PackScreen.class, new IGuiContainerHandler<PackScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(PackScreen screen) {
                return Collections.emptyList();
            }

            @Override
            public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory, PackScreen screen, double mouseX, double mouseY) {
                return findIngredient(factory, screen, mouseX, mouseY);
            }
        });
    }

    private static Optional<IClickableIngredient<?>> findIngredient(IClickableIngredientFactory factory, PackScreen screen, double mouseX, double mouseY) {
        Slot s = screen.getSlotAtScreen(mouseX, mouseY);
        if (s != null) {
            ItemStack stack = screen.getItemAtScreen(mouseX, mouseY);
            if (!stack.isEmpty()) {
                float scale = screen.getUiScale();
                int slotScreenX = (int) ((screen.getGuiLeft() + s.x) * scale);
                int slotScreenY = (int) ((screen.getGuiTop() + s.y) * scale);
                int slotScreenSize = Math.max(1, (int) (16 * scale));
                return factory.createBuilder(stack).buildWithArea(slotScreenX, slotScreenY, slotScreenSize, slotScreenSize).map(ci -> ci);
            }
        }
        return Optional.empty();
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            PackMenu.class,
            AdventurersPack.MENU.get(),
            RecipeTypes.CRAFTING,
            PackMenu.CRAFT,
            9,
            PackMenu.PLAYER,
            36
        );

        registration.addRecipeTransferHandler(PackMenu.class, AdventurersPack.MENU.get(), RecipeTypes.SMELTING, 120, 1, PackMenu.PLAYER, 36);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(RecipeTypes.CRAFTING, AdventurersPack.PACK.get(), AdventurersPack.CRAFTING.get());
        registration.addRecipeCatalysts(RecipeTypes.SMELTING, AdventurersPack.FURNACE.get());
        registration.addRecipeCatalysts(RecipeTypes.BREWING, AdventurersPack.ALEMBIC.get());
    }
}
