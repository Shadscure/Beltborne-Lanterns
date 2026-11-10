package net.oxcodsnet.beltborne_lanterns.common.client;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.client.ui.LanternDebugScreen;
import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.physics.LanternSwingManager;

/**
 * Common client-side lifecycle and event handling facade shared by all mod loaders.
 */
public final class BeltLanternClientLifecycle {
    private BeltLanternClientLifecycle() {}

    public static void onClientJoin() {
        ClientBeltPlayers.clear();
        LanternSwingManager.clearAll();
        LampRegistry.clearServerOverride();
        LampRegistry.init();
    }

    public static void onClientDisconnect() {
        ClientBeltPlayers.clear();
        LanternSwingManager.clearAll();
        LampRegistry.clearServerOverride();
        LampRegistry.init();
    }

    public static void onEntityUnload(Entity entity) {
        if (entity instanceof Player player) {
            LanternSwingManager.removePlayer(player.getUUID());
        }
    }

    public static void onBeltSync(UUID uuid, Identifier lampId) {
        Item lamp = lampId != null ? BuiltInRegistries.ITEM.getValue(lampId) : null;
        ClientBeltPlayers.setLamp(uuid, lamp);
    }

    public static void onLampConfigSync(Map<Identifier, Integer> lamps) {
        LampRegistry.setServerOverride(lamps);
        LampRegistry.init();
    }

    public static void onClientTick(
            Minecraft client,
            boolean openConfigPressed,
            boolean toggleDebugPressed,
            boolean openDebugEditorPressed,
            boolean toggleLanternPressed,
            Runnable sendTogglePacket
    ) {
        if (openConfigPressed) {
            if (client.screen == null) {
                client.setScreen(LanternClientScreens.openConfig(client.screen));
            }
        }

        if (toggleDebugPressed) {
            if (BLClientConfigAccess.get().debug) {
                BLClientAbstractions.setDebugDrawEnabled(!BLClientAbstractions.isDebugDrawEnabled());
            }
        }

        if (openDebugEditorPressed) {
            if (BLClientConfigAccess.get().debug) {
                if (client.screen == null) {
                    client.setScreen(new LanternDebugScreen());
                }
            }
        }

        if (toggleLanternPressed) {
            sendTogglePacket.run();
        }

        LanternClientLogic.tickLanternPhysics(client);
    }
}
