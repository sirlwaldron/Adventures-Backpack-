package com.waldron.adventurerspack;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.waldron.adventurerspack.client.StaffAnimation;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.context.UseOnContext;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import org.joml.Vector3f;

public class RequiemMeleeItem extends SwordItem {
    private final boolean spear;

    public RequiemMeleeItem(boolean spear) {
        super(Endgame.TIER, spear ? 44 : 38, spear ? -3.0F : -2.5F, new Item.Properties().rarity(Rarity.EPIC).fireResistant());
        this.spear = spear;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (!spear || slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        var b = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        b.putAll(super.getDefaultAttributeModifiers(slot));
        b.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(UUID.fromString("f47c5831-f552-4c7a-882f-560a8d1f7831"), "Requiem spear reach", 1.5, AttributeModifier.Operation.ADDITION));
        return b.build();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(spear ? new StaffAnimation() : com.waldron.adventurerspack.client.RequiemBladeRenderer.extension());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (spear) {
            Level l = target.level();
            if (l instanceof ServerLevel level) {
                DustParticleOptions dust = new DustParticleOptions(new Vector3f(0.85F, 0.025F, 0.1F), 1.1F);
                Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
                double yaw = Math.toRadians(attacker.getYRot());

                for (int i = 0; i < 18; ++i) {
                    double a = -1.2D + (double) i * 2.4D / 17.0D;
                    double across = Math.sin(a) * 0.85D;
                    level.sendParticles(dust, center.x + Math.cos(yaw) * across, center.y + Math.cos(a) * 0.35D, center.z + Math.sin(yaw) * across, 1, 0.025D, 0.025D, 0.025D, 0.0D);
                }

                level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 6, 0.15D, 0.15D, 0.15D, 0.08D);
                level.playSound((Player) null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6F, 0.75F);
            }
        }
        return result;
    }

    @Override public boolean isDamageable(ItemStack stack) { return false; }
    @Override public boolean isBarVisible(ItemStack stack) { return false; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        if (!p.isShiftKeyDown()) return super.use(level, p, hand);
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
    public void appendHoverText(ItemStack s, Level l, List<Component> out, TooltipFlag f) {
        out.add(Component.literal(spear ? "51 damage | Long reach | Sweeping melee" : "45 damage | Swift sweeping melee"));
        out.add(Component.literal("Endgame Forms key: choose any of ten forms"));
        out.add(Component.literal("Shift + right-click: cycle lance & tools"));
        out.add(Component.literal("Unbreakable | All ten forms share enchantments"));
    }
}
