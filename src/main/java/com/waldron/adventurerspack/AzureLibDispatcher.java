package com.waldron.adventurerspack;

import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class AzureLibDispatcher {
    private static final AzCommand IDLE_COMMAND = AzCommand.create("base_controller", "idle", AzPlayBehaviors.LOOP);
    private static final AzCommand FLYING_COMMAND = AzCommand.create("base_controller", "flying", AzPlayBehaviors.LOOP);

    // Weak player keys avoid retaining disconnected players or discarded item identities.
    private record WingState(UUID item, boolean flying, int tick) {}
    private static final Map<Player, WingState> LAST_FLYING_STATE = new WeakHashMap<>();

    public static void tickWingedArmor(Player player, ItemStack stack) {
        // Only dispatch for the stack worn on chest
        if (player.getItemBySlot(EquipmentSlot.CHEST) != stack) return;

        // Ensure stack has az_id
        if (!stack.hasTag() || !stack.getTag().hasUUID("az_id")) return;

        UUID currentAzId = stack.getTag().getUUID("az_id");
        boolean flying = WingFlightState.shouldFlap(player);
        WingState lastState = LAST_FLYING_STATE.get(player);

        // Send only when the pose changes. Replaying either clip on a timer restarts it.
        if (lastState == null || !lastState.item().equals(currentAzId) || lastState.flying() != flying
                || player.tickCount < lastState.tick()) {
            LAST_FLYING_STATE.put(player, new WingState(currentAzId, flying, player.tickCount));

            if (flying) {
                FLYING_COMMAND.sendForItem(player, stack);
            } else {
                IDLE_COMMAND.sendForItem(player, stack);
            }
        }
    }

    /** One pose update for players who just came into view. */
    public static void replay(Player player) {
        LAST_FLYING_STATE.remove(player);
        tickWingedArmor(player, player.getItemBySlot(EquipmentSlot.CHEST));
    }
}
