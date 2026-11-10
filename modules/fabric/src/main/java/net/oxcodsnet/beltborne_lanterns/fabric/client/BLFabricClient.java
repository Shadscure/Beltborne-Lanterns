package net.oxcodsnet.beltborne_lanterns.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.client.BLClientAbstractions;
import net.oxcodsnet.beltborne_lanterns.common.client.BeltLanternClientLifecycle;
import net.oxcodsnet.beltborne_lanterns.common.client.ClientBeltPlayers;
import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.network.BeltSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.network.LampConfigSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.network.ToggleLanternPayload;
import org.lwjgl.glfw.GLFW;

public final class BLFabricClient implements ClientModInitializer {
    // Keybindings
    private static final KeyMapping.Category BELTBORNE_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BLMod.MOD_ID, "beltborne_lanterns"));
    private static KeyMapping openConfigKey;
    private static KeyMapping toggleDebugKey;
    private static KeyMapping openDebugEditorKey;
    private static KeyMapping toggleLanternKey;

    @Override
    public void onInitializeClient() {
        // Load the lamp registry from the client's config file on startup.
        // This makes the config screen work before joining a world.
        LampRegistry.init();
        BLClientConfigAccess.get(); // loads JSON + seeds BLConfigs (#22)
        BLMod.LOGGER.info("Client initialization started [Fabric]");

        // Rebuild registry after client joins a server (tags/registries are synced at this point)
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(BeltLanternClientLifecycle::onClientJoin);
        });

        // Clear caches on disconnect as well
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(BeltLanternClientLifecycle::onClientDisconnect);
        });

        // Register network receiver: updates local client set
        ClientPlayNetworking.registerGlobalReceiver(BeltSyncPayload.ID, (payload, context) -> {
            context.client().execute(() ->
                    BeltLanternClientLifecycle.onBeltSync(payload.playerUuid(), payload.lampId())
            );
        });

        // This receiver handles lamp configs sent from a dedicated server.
        ClientPlayNetworking.registerGlobalReceiver(LampConfigSyncPayload.ID, (payload, context) -> {
            context.client().execute(() ->
                    BeltLanternClientLifecycle.onLampConfigSync(payload.lamps())
            );
        });

        // Clean up swing manager state when a player entity is unloaded
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            BeltLanternClientLifecycle.onEntityUnload(entity);
        });

        // Optionally register dynamic lights for the belt lantern when LambDynamicLights is present
        boolean hasLamb = FabricLoader.getInstance().isModLoaded("lambdynlights");

        // Keybind to open config (default: L)
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.beltborne_lanterns.open_config",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L,
                BELTBORNE_CATEGORY
        ));

        // Keybind to toggle debug gizmos (default: K)
        toggleDebugKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.beltborne_lanterns.toggle_debug",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
                BELTBORNE_CATEGORY
        ));

        // Keybind to open lantern debug editor (default: P)
        openDebugEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.beltborne_lanterns.open_debug",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P,
                BELTBORNE_CATEGORY
        ));

        // Keybind to toggle belt lantern (default: B)
        toggleLanternKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.beltborne_lanterns.toggle_lantern",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
                BELTBORNE_CATEGORY
        ));

        // Wire platform abstractions so common renderer can query state/debug
        BLClientAbstractions.init(ClientBeltPlayers::getLamp);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BeltLanternClientLifecycle.onClientTick(
                    client,
                    openConfigKey.consumeClick(),
                    toggleDebugKey.consumeClick(),
                    openDebugEditorKey.consumeClick(),
                    toggleLanternKey.consumeClick(),
                    () -> ClientPlayNetworking.send(new ToggleLanternPayload())
            );
        });

        // Final client-ready log (concise, useful to players)
        String dyn = hasLamb ? "enabled" : "disabled";
        BLMod.LOGGER.info("Client ready [Fabric]. Dynamic lights: {}.", dyn);
    }
}
