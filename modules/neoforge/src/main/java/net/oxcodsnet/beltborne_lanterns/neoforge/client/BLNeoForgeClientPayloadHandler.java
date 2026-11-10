package net.oxcodsnet.beltborne_lanterns.neoforge.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oxcodsnet.beltborne_lanterns.common.client.BeltLanternClientLifecycle;
import net.oxcodsnet.beltborne_lanterns.common.network.BeltSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.network.LampConfigSyncPayload;

/**
 * Client-side network payload handlers for NeoForge.
 */
public final class BLNeoForgeClientPayloadHandler {
    private BLNeoForgeClientPayloadHandler() {}

    public static void handleBeltSync(BeltSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BeltLanternClientLifecycle.onBeltSync(payload.playerUuid(), payload.lampId()));
    }

    public static void handleLampConfigSync(LampConfigSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BeltLanternClientLifecycle.onLampConfigSync(payload.lamps()));
    }
}
