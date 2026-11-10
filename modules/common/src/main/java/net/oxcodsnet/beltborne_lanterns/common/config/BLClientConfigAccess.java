package net.oxcodsnet.beltborne_lanterns.common.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.InteractionResult;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;

/**
 * Common accessor for the client config using Cloth AutoConfig.
 * Provides lazy registration and keeps the common BLConfigs snapshot in sync.
 */
public final class BLClientConfigAccess {
    public static final int CURRENT_CONFIG_VERSION = 2;
    private static volatile ConfigHolder<BLClientConfig> HOLDER;

    private BLClientConfigAccess() {}

    private static boolean looksLikeOldScale(BLClientConfig clientConfig) {
        return Math.abs(clientConfig.offsetX100) <= 200 &&
                Math.abs(clientConfig.offsetY100) <= 200 &&
                Math.abs(clientConfig.offsetZ100) <= 200 &&
                Math.abs(clientConfig.pivotX100)  <= 200 &&
                Math.abs(clientConfig.pivotY100)  <= 200 &&
                Math.abs(clientConfig.pivotZ100)  <= 200 &&
                clientConfig.scale100 <= 100;
    }

    private static void ensureInit() {
        if (HOLDER != null) return;
        synchronized (BLClientConfigAccess.class) {
            if (HOLDER != null) return;
            AutoConfig.register(BLClientConfig.class, GsonConfigSerializer::new);
            ConfigHolder<BLClientConfig> holder = AutoConfig.getConfigHolder(BLClientConfig.class);
            BLClientConfig clientConfig = holder.getConfig();

            // Migrate from 1/100 to 1/1000 precision once if old values are detected
            boolean migrated = clientConfig.configVersion < CURRENT_CONFIG_VERSION;
            if (migrated) {
                if (clientConfig.configVersion == 0 && looksLikeOldScale(clientConfig)) {
                    clientConfig.offsetX100 *= 10;
                    clientConfig.offsetY100 *= 10;
                    clientConfig.offsetZ100 *= 10;
                    clientConfig.pivotX100  *= 10;
                    clientConfig.pivotY100  *= 10;
                    clientConfig.pivotZ100  *= 10;
                    clientConfig.scale100   *= 10;
                }
                clientConfig.configVersion = CURRENT_CONFIG_VERSION;
            }
            clientConfig.extraLampLight.clear(); // Clear existing to avoid duplicates on re-init

            java.util.Map<String, BLClientConfig.ExtraLampEntry> tempMap = new java.util.LinkedHashMap<>();

            // Add/overwrite lamps from BLLampConfigAccess (server-synced/custom)
            BLLampConfigAccess.get().extraLampLight.forEach(entry -> {
                if (entry == null || entry.id == null) return;
                tempMap.put(entry.id, new BLClientConfig.ExtraLampEntry(entry.id, entry.luminance));
            });

            // Convert tempMap to List and set it to clientConfig.extraLampLight
            clientConfig.extraLampLight.addAll(tempMap.values());
            if (migrated) holder.save();
            // Seed common snapshot
            BLConfigs.set(copyToCommon(holder.getConfig()));
            // Keep snapshot synced on save
            holder.registerSaveListener((h, cfg) -> {
                BLMod.LOGGER.info("Client config saved: offset=({},{},{}) scale={}", cfg.offsetX100, cfg.offsetY100, cfg.offsetZ100, cfg.scale100);
                BLConfigs.set(copyToCommon(cfg));
                BLLampConfig lampCfg = BLLampConfigAccess.get();
                lampCfg.extraLampLight.clear();
                cfg.extraLampLight.forEach(entry -> {
                    if (entry == null || entry.id == null) return;
                    lampCfg.extraLampLight.add(new BLClientConfig.ExtraLampEntry(entry.id, entry.luminance));
                });
                BLLampConfigAccess.save();
                // Rebuild LampRegistry from updated config
                LampRegistry.init();
                return InteractionResult.SUCCESS;
            });
            BLMod.LOGGER.info(
                    "Client config loaded [startup]: offset=({},{},{}) scale={}",
                    clientConfig.offsetX100,
                    clientConfig.offsetY100,
                    clientConfig.offsetZ100,
                    clientConfig.scale100
            );
            HOLDER = holder;
        }
    }

    public static BLClientConfig get() {
        ensureInit();
        return HOLDER.getConfig();
    }

    public static void save() {
        ensureInit();
        HOLDER.save();
    }

    private static BLConfig copyToCommon(BLClientConfig c) {
        BLConfig out = new BLConfig();
        out.offsetX100 = c.offsetX100;
        out.offsetY100 = c.offsetY100;
        out.offsetZ100 = c.offsetZ100;
        out.pivotX100 = c.pivotX100;
        out.pivotY100 = c.pivotY100;
        out.pivotZ100 = c.pivotZ100;
        out.rotXDeg = c.rotXDeg;
        out.rotYDeg = c.rotYDeg;
        out.rotZDeg = c.rotZDeg;
        out.scale100 = c.scale100;
        return out;
    }
}
