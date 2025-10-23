package net.oxcodsnet.beltborne_lanterns.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.oxcodsnet.beltborne_lanterns.common.config.BLConfigs;
import net.oxcodsnet.beltborne_lanterns.common.physics.LanternSwingManager;

/**
 * Common per-tick logic for lantern client-side updates.
 */
public final class LanternClientLogic {
    private LanternClientLogic() {}

    public static void tickLanternPhysics(Minecraft mc) {
        if (mc.level == null) return;
        final float dt = 1.0f / 20.0f;
        for (Player p : mc.level.players()) {
            if (BLClientAbstractions.clientHasLantern(p)) {
                LanternSwingManager.tickPlayer(p, dt, BLConfigs.get().rotXDeg);
            }
        }
    }
}

