package net.oxcodsnet.beltborne_lanterns.neoforge.compat;

import Type;
import dev.lambdaurora.lambdynlights.api.DynamicLightsContext;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.api.entity.EntityLightSourceManager;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;
import dev.yumi.commons.event.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.LambDynLightsCompat;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.client.BLClientAbstractions;

/**
 * NeoForge service-based initializer for LambDynamicLights 4.x API.
 */
public final class LDL4NeoForgeInitializer implements DynamicLightsInitializer {
    @Override
    public void onInitializeDynamicLights(DynamicLightsContext context) {
        EntityLightSourceManager mgr = context.entityLightSourceManager();
        Event event = mgr.onRegisterEvent();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(BLMod.MOD_ID, "player_lantern");

        EntityLuminance luminance = new EntityLuminance() {
            @Override
            public Type type() { return Type.VALUE; }

            @Override
            public int getLuminance(ItemLightSourceManager items, Entity entity) {
                if (entity instanceof Player player) {
                    Item lamp = BLClientAbstractions.clientLamp(player);
                    return lamp != null ? LampRegistry.getLuminance(lamp) : 0;
                }
                return 0;
            }
        };

        event.register(id, (EntityLightSourceManager.OnRegister) (ctx -> ctx.register(EntityType.PLAYER, luminance)));
        BLMod.LOGGER.info("Dynamic lights: integrated via LambDynamicLights 4.x (API entrypoint)");
        LambDynLightsCompat.markInitialized();
    }
}

