package com.waldron.adventurerspack;
import net.minecraft.world.entity.player.Player;

/** Shared flight selection without loading the optional animation library. */
public final class WingFlightState {
 private WingFlightState() {}
 public static boolean shouldFlap(Player player) {
  return player.isAlive() && !player.isSpectator()
    && (player.isFallFlying() || player.getAbilities().flying);
 }
}
