package net.oxcodsnet.beltborne_lanterns.common.server;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.BeltState;
import net.oxcodsnet.beltborne_lanterns.common.DynamicLightsCompat;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.compat.CompatibilityLayerRegistry;
import net.oxcodsnet.beltborne_lanterns.common.config.BLLampConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.datapack.BLRuntimeDataPack;
import net.oxcodsnet.beltborne_lanterns.common.network.LampConfigSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.persistence.BeltLanternSave;

/**
 * Common server-side lifecycle and event handling facade shared by all mod loaders.
 */
public final class BeltLanternServerLifecycle {
    private BeltLanternServerLifecycle() {}

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    public static void handleToggleLantern(
            ServerPlayer player,
            BiConsumer<ServerPlayer, Item> broadcaster,
            boolean updateServerDynamicLights
    ) {
        for (var layer : CompatibilityLayerRegistry.getLayers()) {
            if (layer.tryToggleLantern(player)) {
                if (updateServerDynamicLights) {
                    if (BeltState.hasLamp(player)) {
                        DynamicLightsCompat.addFor(player);
                    } else {
                        DynamicLightsCompat.removeFor(player);
                    }
                }
                return;
            }
        }

        ItemStack stack = player.getMainHandItem();
        boolean hasLamp = BeltState.hasLamp(player);
        if (!hasLamp && !LampRegistry.isLamp(stack)) {
            stack = player.getOffhandItem();
            if (!LampRegistry.isLamp(stack)) {
                BLMod.LOGGER.debug(
                        "toggle request from {}: handItem={} lampOnBelt={}",
                        player.getScoreboardName(),
                        player.getMainHandItem().getItem(),
                        hasLamp
                );
                player.displayClientMessage(Component.translatable("message.beltborne_lanterns.hold_lantern"), true);
                return;
            }
        }
        Item nowHas = BeltLanternServer.toggleLantern(player, stack);
        broadcaster.accept(player, nowHas);
        if (nowHas != null) {
            for (var layer : CompatibilityLayerRegistry.getLayers()) {
                layer.syncToggleOn(player);
            }
            if (updateServerDynamicLights) {
                DynamicLightsCompat.addFor(player);
            }
        } else if (updateServerDynamicLights) {
            DynamicLightsCompat.removeFor(player);
        }
    }

    public static void onPlayerJoin(
            ServerPlayer joining,
            MinecraftServer server,
            BiConsumer<ServerPlayer, Item> broadcastState,
            TriConsumer<ServerPlayer, UUID, Item> sendStateTo,
            BiConsumer<ServerPlayer, LampConfigSyncPayload> sendLampConfig,
            boolean updateServerDynamicLights
    ) {
        var persistedStack = BeltLanternSave.get(server).getStack(joining.getUUID());

        for (var layer : CompatibilityLayerRegistry.getLayers()) {
            var slotStack = layer.getBeltStack(joining);
            if (slotStack.isPresent() && LampRegistry.isLamp(slotStack.get())) {
                persistedStack = slotStack.get();
                break;
            }
        }

        Item persisted = persistedStack != null ? persistedStack.getItem() : null;
        BeltState.setLamp(joining, persistedStack);
        broadcastState.accept(joining, persisted);
        if (updateServerDynamicLights && persisted != null) {
            DynamicLightsCompat.addFor(joining);
        }

        if (server.isDedicatedServer()) {
            var lampMap = new LinkedHashMap<ResourceLocation, Integer>();
            BLLampConfigAccess.get().extraLampLight.forEach(entry -> {
                ResourceLocation id = ResourceLocation.tryParse(entry.id);
                if (id != null) lampMap.put(id, entry.luminance);
            });
            sendLampConfig.accept(joining, new LampConfigSyncPayload(lampMap));
        }

        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            Item lamp = BeltState.getLamp(other);
            sendStateTo.accept(joining, other.getUUID(), lamp);
        }
    }

    public static void onPlayerLeave(
            ServerPlayer leaving,
            MinecraftServer server,
            boolean updateServerDynamicLights
    ) {
        BeltLanternSave.get(server).set(leaving.getUUID(), BeltState.getLampStack(leaving));
        BeltState.setLamp(leaving, (ItemStack) null);
        if (updateServerDynamicLights) {
            DynamicLightsCompat.removeFor(leaving);
        }
    }

    public static void onPlayerDeath(
            ServerPlayer oldPlayer,
            ServerPlayer newPlayer,
            boolean keepInventory,
            boolean updateServerDynamicLights
    ) {
        BeltLanternServer.handleDeath(oldPlayer, newPlayer, keepInventory);
        if (updateServerDynamicLights) {
            DynamicLightsCompat.removeFor(oldPlayer);
        }
    }

    public static void onPlayerRespawn(
            ServerPlayer player,
            BiConsumer<ServerPlayer, Item> broadcastState,
            boolean updateServerDynamicLights
    ) {
        Item lamp = BeltState.getLamp(player);
        broadcastState.accept(player, lamp);
        if (updateServerDynamicLights && lamp != null) {
            DynamicLightsCompat.addFor(player);
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        LampRegistry.init();
        boolean dpChanged = BLRuntimeDataPack.writeOrUpdate(server);
        if (dpChanged) {
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "datapack enable \"file/beltborne_lanterns_generated\"");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            } catch (Throwable t) {
                BLMod.LOGGER.info("Runtime datapack updated — please run /reload to apply");
            }
        }
    }

    public static void onDataPackReload(
            MinecraftServer server,
            BiConsumer<ServerPlayer, LampConfigSyncPayload> sendLampConfig
    ) {
        if (server != null) {
            BLRuntimeDataPack.writeOrUpdate(server);
        }
        LampRegistry.init();
        if (server != null && server.isDedicatedServer()) {
            var lampMap = new LinkedHashMap<ResourceLocation, Integer>();
            BLLampConfigAccess.get().extraLampLight.forEach(entry -> {
                ResourceLocation id = ResourceLocation.tryParse(entry.id);
                if (id != null) lampMap.put(id, entry.luminance);
            });
            var payload = new LampConfigSyncPayload(lampMap);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendLampConfig.accept(player, payload);
            }
        }
    }

    public static void onServerStopped(boolean updateServerDynamicLights) {
        BeltState.clearAll();
        if (updateServerDynamicLights) {
            DynamicLightsCompat.clearAll();
        }
    }
}
