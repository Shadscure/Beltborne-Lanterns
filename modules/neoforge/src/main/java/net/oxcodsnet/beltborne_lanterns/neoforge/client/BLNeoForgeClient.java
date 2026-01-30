package net.oxcodsnet.beltborne_lanterns.neoforge.client;

import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfig;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.LambDynLightsCompat;
import net.oxcodsnet.beltborne_lanterns.common.client.BLClientAbstractions;
import net.oxcodsnet.beltborne_lanterns.common.client.ClientBeltPlayers;
import net.oxcodsnet.beltborne_lanterns.common.client.LanternBeltFeatureRenderer;
import net.oxcodsnet.beltborne_lanterns.common.client.LanternClientLogic;
import net.oxcodsnet.beltborne_lanterns.common.client.LanternClientScreens;
import net.oxcodsnet.beltborne_lanterns.common.client.ui.LanternDebugScreen;
import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.network.BeltSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.network.LampConfigSyncPayload;
import net.oxcodsnet.beltborne_lanterns.common.network.ToggleLanternPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import net.oxcodsnet.beltborne_lanterns.common.config.BLLampConfigAccess;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.physics.LanternSwingManager;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.UUID;

@EventBusSubscriber(modid = BLMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BLNeoForgeClient {
    // no per-loader state; use common ClientBeltPlayers

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
                InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_L,
                "category.beltborne_lanterns"
        );
        toggleDebugKey = new KeyMapping(
                "key.beltborne_lanterns.toggle_debug",
                InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_K,
                "category.beltborne_lanterns"
        );
        openDebugEditorKey = new KeyMapping(
                "key.beltborne_lanterns.open_debug",
                InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_P,
                "category.beltborne_lanterns"
        );
        toggleLanternKey = new KeyMapping(
                "key.beltborne_lanterns.toggle_lantern",
                InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_B,
                "category.beltborne_lanterns"
        );
        event.register(openConfigKey);
        event.register(toggleDebugKey);
        event.register(openDebugEditorKey);
        event.register(toggleLanternKey);
        // Optional: LambDynamicLights integration (prefer typed LDL4 when present)
        try {
            if (!net.oxcodsnet.beltborne_lanterns.neoforge.compat.LDL4NeoForge.tryInit()) {
                // Fallback to LDL3 reflection (safe no-op if not present)
                net.oxcodsnet.beltborne_lanterns.common.LambDynLightsCompat.init();
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
        public static void onEntityLeave(EntityLeaveLevelEvent event) {
            if (event.getEntity() instanceof Player) {
                LanternSwingManager.removePlayer(event.getEntity().getUUID());
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post e) {
            Minecraft mc = Minecraft.getInstance();
            if (openConfigKey != null && openConfigKey.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(LanternClientScreens.openConfig(mc.screen));
                }
            }

            if (toggleDebugKey != null && toggleDebugKey.consumeClick()) {
                if (BLClientConfigAccess.get().debug) {
                    BLClientAbstractions.setDebugDrawEnabled(!BLClientAbstractions.isDebugDrawEnabled());
                }
            }

            if (openDebugEditorKey != null && openDebugEditorKey.consumeClick()) {
                if (BLClientConfigAccess.get().debug) {
                    if (mc.screen == null) {
                        mc.setScreen(new LanternDebugScreen());
                    }
                }
            }

            if (toggleLanternKey != null && toggleLanternKey.consumeClick()) {
                // Send our toggle request to the server using a direct custom payload packet.
                // PacketDistributor lacks a sendToServer overload in this environment, so use the
                // vanilla client network handler to transmit the payload.
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(new ToggleLanternPayload()));
                }
            }

            LanternClientLogic.tickLanternPhysics(mc);

            // Retry LDL integration on ticks until successful (handles init order)
            if (!net.oxcodsnet.beltborne_lanterns.common.LambDynLightsCompat.isInitialized()) {
                if (!net.oxcodsnet.beltborne_lanterns.neoforge.compat.LDL4NeoForge.tryInit()) {
                    net.oxcodsnet.beltborne_lanterns.common.LambDynLightsCompat.init();
                }
            }
        }
    }
}
