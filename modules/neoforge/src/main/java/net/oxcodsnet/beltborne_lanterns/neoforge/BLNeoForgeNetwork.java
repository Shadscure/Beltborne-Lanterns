package net.oxcodsnet.beltborne_lanterns.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.network.BeltSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.network.LampConfigSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.network.ToggleLanternPayload;
import net.oxcodsnet.beltborne_lanterns.common.server.BeltLanternServerLifecycle;
import net.oxcodsnet.beltborne_lanterns.neoforge.client.BLNeoForgeClientPayloadHandler;

/**
 * Registers network payloads on the MOD bus.
 *
 * <p>Runs on both dedicated and integrated servers so the toggle payload is
 * handled in singleplayer as well.</p>
 */
@EventBusSubscriber(modid = BLMod.MOD_ID)
public final class BLNeoForgeNetwork {
    private BLNeoForgeNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        // Register payloads (network version "1")
        var registrar = event.registrar("1");

        // C2S Payloads (Client to Server)
        registrar.playToServer(ToggleLanternPayload.ID, ToggleLanternPayload.CODEC, (payload, ctx) -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ctx.enqueueWork(() ->
                    BeltLanternServerLifecycle.handleToggleLantern(player, BeltNetworking::broadcastBeltState, true)
            );
        });

        // S2C Payloads (Server to Client)
        registrar.playToClient(
                BeltSyncPayload.ID,
                BeltSyncPayload.CODEC,
                BLNeoForgeClientPayloadHandler::handleBeltSync
        );
        registrar.playToClient(
                LampConfigSyncPayload.ID,
                LampConfigSyncPayload.CODEC,
                BLNeoForgeClientPayloadHandler::handleLampConfigSync
        );
    }
}
