package com.waldron.adventurerspack;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

public final class Endgame {
    public static final Tier TIER = TierSortingRegistry.registerTier(
        new ForgeTier(4, 4096, 24.0F, 6.0F, 22, BlockTags.NEEDS_DIAMOND_TOOL, () -> Ingredient.of(Items.NETHERITE_INGOT)),
        new ResourceLocation(AdventurersPack.ID, "endgame"),
        List.of(Tiers.NETHERITE),
        List.of()
    );

    public static ItemStack transformed(ItemStack source) {
        int current = Requiem.form(source);
        if (current >= 5 && current <= 9) {
            int next = current == 9 ? 5 : current + 1;
            return Requiem.transformed(source, next);
        } else if (current == 0) {
            return Requiem.transformed(source, 1);
        } else if (current == 1) {
            return Requiem.transformed(source, 5);
        }
        return Requiem.transformed(source, 5);
    }

    public static void transform(Player p, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND && !p.getMainHandItem().isEmpty()) return;
        int slot = hand == InteractionHand.OFF_HAND ? 40 : p.getInventory().selected;
        int current = Requiem.form(p.getItemInHand(hand));
        if (current >= 5 && current <= 9) {
            int next = current == 9 ? 5 : current + 1;
            Requiem.transform(p, slot, current, next);
        } else if (current == 0) {
            Requiem.transform(p, slot, current, 1);
        } else if (current == 1) {
            Requiem.transform(p, slot, current, 5);
        }
    }

    public static void tooltip(ItemStack s, List<Component> lines, boolean pick) {
        tooltip(s, lines, pick ? "Pickaxe" : null);
    }

    public static void tooltip(ItemStack s, List<Component> lines, String toolName) {
        lines.add(Component.literal(toolName != null ? toolName + " form | Faster and tougher than netherite" : "Lance form | Sweeping attacks and extended melee reach"));
        lines.add(Component.literal("Endgame Forms key: choose any of ten forms"));
        lines.add(Component.literal("Shift + right-click: cycle lance & tools"));
        if (toolName == null) {
            lines.add(Component.literal("Hold right-click, release: throw and automatically recall"));
        }
        lines.add(Component.literal("Unbreakable | All ten forms share enchantments"));
    }
}
