package com.waldron.adventurerspack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

public final class EndgameAxeItem extends AxeItem {
    public EndgameAxeItem() {
        super(Endgame.TIER, 41.0F, -3.0F, new Item.Properties().fireResistant().rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        if (!p.isShiftKeyDown()) return InteractionResultHolder.pass(p.getItemInHand(hand));
        if (!level.isClientSide) Endgame.transform(p, hand);
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) {
            if (!ctx.getLevel().isClientSide) Endgame.transform(ctx.getPlayer(), ctx.getHand());
            return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
        }
        return super.useOn(ctx);
    }

    @Override
    public void appendHoverText(ItemStack s, Level l, List<Component> lines, TooltipFlag f) {
        Endgame.tooltip(s, lines, "Axe");
    }

    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }
}
