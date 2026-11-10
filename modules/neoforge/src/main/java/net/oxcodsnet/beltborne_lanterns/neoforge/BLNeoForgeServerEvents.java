package net.oxcodsnet.beltborne_lanterns.neoforge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.server.BeltLanternServerLifecycle;

/**
 * Server-side interaction + sync logic for NeoForge.
 */
@EventBusSubscriber(modid = BLMod.MOD_ID)
public final class BLNeoForgeServerEvents {
    private BLNeoForgeServerEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        BeltLanternServerLifecycle.onServerStarted(e.getServer());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;
        MinecraftServer server = joining.getServer();
        BeltLanternServerLifecycle.onPlayerJoin(
                joining,
                server,
                BeltNetworking::broadcastBeltState,
                BeltNetworking::sendTo,
                PacketDistributor::sendToPlayer,
                true
        );
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer leaving)) return;
        BeltLanternServerLifecycle.onPlayerLeave(leaving, leaving.getServer(), true);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BeltLanternServerLifecycle.onServerStopped(true);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;
        if (!event.isWasDeath()) return;

        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
        boolean keep = oldPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        BeltLanternServerLifecycle.onPlayerDeath(oldPlayer, newPlayer, keep, true);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BeltLanternServerLifecycle.onPlayerRespawn(player, BeltNetworking::broadcastBeltState, true);
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        BeltLanternServerLifecycle.onDataPackReload(server, PacketDistributor::sendToPlayer);
    }
}
