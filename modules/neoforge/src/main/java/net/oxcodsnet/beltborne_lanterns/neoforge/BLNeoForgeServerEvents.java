package net.oxcodsnet.beltborne_lanterns.neoforge;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.BeltState;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.compat.CompatibilityLayer;
import net.oxcodsnet.beltborne_lanterns.common.compat.CompatibilityLayerRegistry;
import net.oxcodsnet.beltborne_lanterns.common.config.BLLampConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.persistence.BeltLanternSave;
import net.oxcodsnet.beltborne_lanterns.common.server.BeltLanternServer;
import net.oxcodsnet.beltborne_lanterns.neoforge.BeltNetworking;
import net.oxcodsnet.beltborne_lanterns.common.network.LampConfigSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.DynamicLightsCompat;

/**
 * Server-side interaction + sync logic for NeoForge.
 */
@EventBusSubscriber(modid = BLMod.MOD_ID)
public final class BLNeoForgeServerEvents {
    private BLNeoForgeServerEvents() {}

    private static MinecraftServer server(ServerPlayer player) {
        return Objects.requireNonNull(((ServerLevel) player.level()).getServer(), "Missing server instance");
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        LampRegistry.init();
        var server = e.getServer();
        // Write runtime datapack with tag entries from config and suggest reload if changed
        boolean dpChanged = net.oxcodsnet.beltborne_lanterns.common.datapack.BLRuntimeDataPack.writeOrUpdate(server);
        if (dpChanged) {
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            } catch (Throwable t) {
                net.oxcodsnet.beltborne_lanterns.BLMod.LOGGER.info("Runtime datapack updated — please run /reload to apply");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joining)) return;
        MinecraftServer server = server(joining);
        // Restore from persistent save (full stack with NBT) and broadcast
        var persistedStack = BeltLanternSave.get(server).getStack(joining.getUUID());

        // If a compatibility layer has a belt stack, prefer that as source of truth
        for (var layer : CompatibilityLayerRegistry.getLayers()) {
            var slotStack = layer.getBeltStack(joining);
            if (slotStack.isPresent() && LampRegistry.isLamp(slotStack.get())) {
                persistedStack = slotStack.get();
                break;
            }
        }

        Item persisted = persistedStack != null ? persistedStack.getItem() : null;
        BeltState.setLamp(joining, persistedStack);
        BeltNetworking.broadcastBeltState(joining, persisted);
        if (persisted != null) {
            // Ensure dynamic light is active on join when lamp is equipped
            DynamicLightsCompat.addFor(joining);
        }
        // If on a dedicated server, send its lamp config to the joining player.
        // In single player, the client's config is trusted as the source of truth.
        if (server.isDedicatedServer()) {
            var lampMap = new java.util.LinkedHashMap<ResourceLocation, Integer>();
            BLLampConfigAccess.get().extraLampLight.forEach(entry -> {
                ResourceLocation id = ResourceLocation.tryParse(entry.id);
                if (id != null) lampMap.put(id, entry.luminance);
            });
            PacketDistributor.sendToPlayer(joining, new LampConfigSyncPayload(lampMap));
        }
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            Item lamp = BeltState.getLamp(other);
            BeltNetworking.sendTo(joining, other.getUUID(), lamp);
        }

    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer leaving)) return;
        // Persist full stack with NBT on disconnect
        BeltLanternSave.get(server(leaving)).set(leaving.getUUID(), BeltState.getLampStack(leaving));
        // Clean up dynamic light source when leaving
        DynamicLightsCompat.removeFor(leaving);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;
        if (!event.isWasDeath()) return;

        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
        boolean keep = oldPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        BeltLanternServer.handleDeath(oldPlayer, newPlayer, keep);
        // Remove dynamic light from the dying player entity
        DynamicLightsCompat.removeFor(oldPlayer);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Item lamp = BeltState.getLamp(player);
        BeltNetworking.broadcastBeltState(player, lamp);
        if (lamp != null) {
            // Re-create dynamic light for the new player entity
            DynamicLightsCompat.addFor(player);
        }
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Keep runtime datapack in sync on reload
            net.oxcodsnet.beltborne_lanterns.common.datapack.BLRuntimeDataPack.writeOrUpdate(server);
        }
        LampRegistry.init();
    }
}
