package net.oxcodsnet.beltborne_lanterns.neoforge.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.oxcodsnet.beltborne_lanterns.common.client.LanternBeltFeatureRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.LambDynLightsCompat;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.client.BLClientAbstractions;
import net.oxcodsnet.beltborne_lanterns.common.client.BeltLanternClientLifecycle;
import net.oxcodsnet.beltborne_lanterns.common.client.ClientBeltPlayers;
import net.oxcodsnet.beltborne_lanterns.common.client.LanternClientScreens;
import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.network.ToggleLanternPayload;
import net.oxcodsnet.beltborne_lanterns.neoforge.compat.LDL4NeoForge;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = BLMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BLNeoForgeClient {
    private BLNeoForgeClient() {}

    // Keybindings
    private static KeyMapping openConfigKey;
    private static KeyMapping toggleDebugKey;
    private static KeyMapping openDebugEditorKey;
    private static KeyMapping toggleLanternKey;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Load the lamp registry from the client's config file on startup.
        // This makes the config screen work before joining a world.
        LampRegistry.init();
        event.enqueueWork(BLClientConfigAccess::get);
        BLMod.LOGGER.info("Client initialization started [NeoForge]");

        // Provide platform bridges for common renderer
        BLClientAbstractions.init(ClientBeltPlayers::getLamp);

        // Register the config screen with NeoForge's extension point
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (mc, parent) -> LanternClientScreens.openConfig(parent)
        );
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        openConfigKey = new KeyMapping(
                "key.beltborne_lanterns.open_config",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L,
                "category.beltborne_lanterns"
        );
        toggleDebugKey = new KeyMapping(
                "key.beltborne_lanterns.toggle_debug",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K,
                "category.beltborne_lanterns"
        );
        openDebugEditorKey = new KeyMapping(
                "key.beltborne_lanterns.open_debug",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P,
                "category.beltborne_lanterns"
        );
        toggleLanternKey = new KeyMapping(
                "key.beltborne_lanterns.toggle_lantern",
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B,
                "category.beltborne_lanterns"
        );
        event.register(openConfigKey);
        event.register(toggleDebugKey);
        event.register(openDebugEditorKey);
        event.register(toggleLanternKey);
        // Optional: LambDynamicLights integration (prefer typed LDL4 when present)
        try {
            if (!LDL4NeoForge.tryInit()) {
                // Fallback to LDL3 reflection (safe no-op if not present)
                LambDynLightsCompat.init();
            }
        } catch (Throwable ignored) {}

        BLMod.LOGGER.info("Client ready [NeoForge].");
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Add our belt lantern feature to all available player skins
        for (var skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer per) {
                per.addLayer(new LanternBeltFeatureRenderer(per));
            }
        }
    }

    @EventBusSubscriber(modid = BLMod.MOD_ID, value = Dist.CLIENT)
    public static final class ClientBus {
        private ClientBus() {}

        @SubscribeEvent
        public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            BeltLanternClientLifecycle.onClientJoin();
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            BeltLanternClientLifecycle.onClientDisconnect();
        }

        @SubscribeEvent
        public static void onEntityLeave(EntityLeaveLevelEvent event) {
            BeltLanternClientLifecycle.onEntityUnload(event.getEntity());
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post e) {
            Minecraft mc = Minecraft.getInstance();
            BeltLanternClientLifecycle.onClientTick(
                    mc,
                    openConfigKey != null && openConfigKey.consumeClick(),
                    toggleDebugKey != null && toggleDebugKey.consumeClick(),
                    openDebugEditorKey != null && openDebugEditorKey.consumeClick(),
                    toggleLanternKey != null && toggleLanternKey.consumeClick(),
                    () -> {
                        if (mc.getConnection() != null) {
                            mc.getConnection().send(new ServerboundCustomPayloadPacket(new ToggleLanternPayload()));
                        }
                    }
            );

            // Retry LDL integration on ticks until successful (handles init order)
            if (!LambDynLightsCompat.isInitialized()) {
                if (!LDL4NeoForge.tryInit()) {
                    LambDynLightsCompat.init();
                }
            }
        }
    }
}
