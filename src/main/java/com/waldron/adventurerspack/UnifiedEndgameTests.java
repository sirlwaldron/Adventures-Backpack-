package com.waldron.adventurerspack;

import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AdventurersPack.ID)
@PrefixGameTestTemplate(false)
public class UnifiedEndgameTests {
    @GameTest(template = "empty")
    public static void everyPairPreservesOneWeapon(GameTestHelper h) {
        for (int from = 0; from < 10; ++from) {
            for (int to = 0; to < 10; ++to) {
                ItemStack s = new ItemStack(Requiem.item(from));
                s.setDamageValue(1234);
                s.enchant(Enchantments.UNBREAKING, 3);
                s.setHoverName(Component.literal("Endgame custom"));
                s.getOrCreateTag().putUUID("AdventurersPackLease", UUID.randomUUID());
                ItemStack out = Requiem.transformed(s, to);
                h.assertTrue(out.getCount() == 1 && Requiem.form(out) == to && out.getTag().equals(s.getTag()), "Every pair retains identity, enchantments and wear");
                out.getOrCreateTag().putInt("probe", 1);
                h.assertFalse(s.getTag().contains("probe"), "Tags copied independently");
            }
        }

        h.assertTrue(Arrays.stream(Requiem.WHEEL_ORDER).distinct().count() == 10L, "Wheel has ten unique forms");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lanceCanSelectAnyForm(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        p.getInventory().selected = 0;

        for (int to = 0; to < 10; ++to) {
            if (to != 5) {
                for (int i = 0; i < 10; ++i) {
                    p.getCooldowns().removeCooldown(Requiem.item(i));
                }

                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AdventurersPack.ENDGAME.get()));
                h.assertTrue(Requiem.transform(p, 0, 5, to) && Requiem.form(p.getMainHandItem()) == to, "Lance opens every form");

                for (int i = 0; i < 10; ++i) {
                    h.assertTrue(p.getCooldowns().isOnCooldown(Requiem.item(i)), "Shared cooldown spans all forms");
                }
            }
        }

        h.succeed();
    }

    @GameTest(template = "empty")
    public static void shortcutCannotBypassCooldown(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        p.getInventory().selected = 0;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AdventurersPack.ENDGAME.get()));
        Endgame.transform(p, InteractionHand.MAIN_HAND);
        h.assertTrue(Requiem.form(p.getMainHandItem()) == 6, "Shortcut selects pickaxe");
        Endgame.transform(p, InteractionHand.MAIN_HAND);
        h.assertTrue(Requiem.form(p.getMainHandItem()) == 6, "Shortcut respects shared cooldown");
        h.assertFalse(Requiem.transform(p, 0, 6, 0), "Radial cannot bypass shortcut cooldown");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidStacksAndTargetsRejected(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        p.getInventory().selected = 0;
        ItemStack s = new ItemStack(AdventurersPack.ENDGAME.get(), 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        h.assertFalse(Requiem.transform(p, 0, 5, 0), "Malformed stacked weapons cannot be destroyed by transform");
        h.assertTrue(s.getCount() == 2, "Rejected source untouched");
        h.assertTrue(Requiem.transformed(new ItemStack(Items.STICK), 5).isEmpty(), "Unrelated item rejected");
        h.assertTrue(Requiem.transformed(new ItemStack(AdventurersPack.ENDGAME.get()), 10).isEmpty(), "Out of range rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staffHitKeepsNormalWear(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        Cow target = EntityType.COW.create(h.getLevel());
        target.setPos(h.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        ItemStack staff = new ItemStack(AdventurersPack.REQUIEM_SPEAR.get());
        staff.hurtEnemy(target, p);
        h.assertTrue(staff.getDamageValue() == 0, "Impact visuals do not wear an unbreakable spear");
        h.succeed();
    }
}
