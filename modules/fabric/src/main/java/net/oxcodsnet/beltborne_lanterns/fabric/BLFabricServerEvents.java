package net.oxcodsnet.beltborne_lanterns.fabric;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.oxcodsnet.beltborne_lanterns.common.network.ToggleLanternPayload;
import net.oxcodsnet.beltborne_lanterns.common.server.BeltLanternServerLifecycle;

/**
 * Handles all server-side event registrations for Fabric.
 */
public final class BLFabricServerEvents {
    private BLFabricServerEvents() {}

    public static void initialize() {
        // Handle client toggle requests
        ServerPlayNetworking.registerGlobalReceiver(ToggleLanternPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() ->
                    BeltLanternServerLifecycle.handleToggleLantern(player, BeltNetworking::broadcastBeltState, false)
            );
        });

        // When a player joins, sync known belt states of all players to them and restore theirs
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer joining = handler.getPlayer();
            BeltLanternServerLifecycle.onPlayerJoin(
                    joining,
                    server,
                    BeltNetworking::broadcastBeltState,
                    BeltNetworking::sendTo,
                    ServerPlayNetworking::send,
                    false
            );
        });

        // On disconnect, persist the current state for that player
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer leaving = handler.getPlayer();
            BeltLanternServerLifecycle.onPlayerLeave(leaving, server, false);
        });

        // Handle lamp drop/persistence on death and sync after respawn
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            boolean keep = oldPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY);
            BeltLanternServerLifecycle.onPlayerDeath(oldPlayer, newPlayer, keep, false);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            BeltLanternServerLifecycle.onPlayerRespawn(newPlayer, BeltNetworking::broadcastBeltState, false);
        });

        // Lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(BeltLanternServerLifecycle::onServerStarted);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            BeltLanternServerLifecycle.onDataPackReload(server, ServerPlayNetworking::send);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BeltLanternServerLifecycle.onServerStopped(false);
        });
    }
}
