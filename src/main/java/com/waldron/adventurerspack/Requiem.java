package com.waldron.adventurerspack;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Requiem {
    public static final String[] NAMES = new String[]{"Sword", "Staff", "Shortbow", "Recurve bow", "Greatbow", "Lance", "Pickaxe", "Axe", "Shovel", "Hoe"};
    public static final int[] WHEEL_ORDER = new int[]{5, 6, 7, 8, 9, 0, 1, 2, 3, 4};

    public static Item item(int form) {
        return switch (form) {
            case 0 -> AdventurersPack.REQUIEM.get();
            case 1 -> AdventurersPack.REQUIEM_SPEAR.get();
            case 2 -> AdventurersPack.REQUIEM_SHORTBOW.get();
            case 3 -> AdventurersPack.REQUIEM_RECURVE.get();
            case 4 -> AdventurersPack.REQUIEM_GREATBOW.get();
            case 5 -> AdventurersPack.ENDGAME.get();
            case 6 -> AdventurersPack.ENDGAME_PICKAXE.get();
            case 7 -> AdventurersPack.ENDGAME_AXE.get();
            case 8 -> AdventurersPack.ENDGAME_SHOVEL.get();
            case 9 -> AdventurersPack.ENDGAME_HOE.get();
            default -> Items.AIR;
        };
    }

    public static int form(ItemStack stack) {
        for (int i = 0; i < NAMES.length; ++i) {
            if (stack.is(item(i))) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack transformed(ItemStack source, int target) {
        if (source.getCount() == 1 && form(source) >= 0 && target >= 0 && target < NAMES.length) {
            net.minecraft.nbt.CompoundTag fullTag = source.serializeNBT();
            var targetItem = item(target);
            var key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(targetItem);
            if (key != null) {
                fullTag.putString("id", key.toString());
            }
            ItemStack out = ItemStack.of(fullTag);
            if (out.isEmpty()) {
                out = new ItemStack(targetItem);
                if (source.hasTag()) {
                    out.setTag(source.getTag().copy());
                }
            }
            return out;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public static boolean transform(Player p, int slot, int expected, int target) {
        if (p.isAlive() && !p.isSpectator() && p.containerMenu == p.inventoryMenu && !p.isUsingItem() && target >= 0 && target < NAMES.length) {
            if (slot != 40 && slot != p.getInventory().selected) {
                return false;
            } else {
                InteractionHand hand = slot == 40 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                ItemStack source = p.getItemInHand(hand);
                int current = form(source);
                if (source.getCount() == 1 && current >= 0 && current == expected && current != target && !p.getCooldowns().isOnCooldown(source.getItem())) {
                    p.setItemInHand(hand, transformed(source, target));

                    for (int i = 0; i < NAMES.length; ++i) {
                        p.getCooldowns().addCooldown(item(i), 8);
                    }

                    p.getInventory().setChanged();
                    p.inventoryMenu.broadcastChanges();
                    p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 0.85F + (float) target * 0.08F);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }
}
