package net.oxcodsnet.beltborne_lanterns.common;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.oxcodsnet.beltborne_lanterns.BLMod;
import net.oxcodsnet.beltborne_lanterns.common.config.BLLampConfig;
import net.oxcodsnet.beltborne_lanterns.common.config.BLLampConfigAccess;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of lamp items that can be attached to the belt and their
 * corresponding block states for rendering.
 */
public final class LampRegistry {
    private record LampData(BlockState state, int luminance) {}

    private static final Map<Item, LampData> LAMPS = new LinkedHashMap<>();
    public static final TagKey<Item> EXTRA_LAMPS_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BLMod.MOD_ID, "lamps"));

    private LampRegistry() {}

    /**
     * Registers vanilla lamps and any additional items provided via the
     * {@code beltborne_lanterns:lamps} item tag. The tag is intended as an
     * extension point so data packs or other mods can supply their own
     * supported lamps without code changes.
     */
    public static void init() {
        // Clear previous entries so tag reloads can rebuild the registry
        LAMPS.clear();

        // Built-in vanilla lamps
        register(Items.LANTERN, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));
        register(Items.SOUL_LANTERN, Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));
        registerCopperLanterns();

        // Dynamically register any additional tagged items (MC 1.21+: iterateEntries)
        try {
            int discovered = 0;
            for (Holder<Item> entry : BuiltInRegistries.ITEM.getTagOrEmpty(EXTRA_LAMPS_TAG)) {
                discovered++;
                Identifier id = BuiltInRegistries.ITEM.getKey(entry.value());
                BLMod.LOGGER.debug(" - {}", id);
                Item item = entry.value();
                if (LAMPS.containsKey(item)) continue;
                if (item instanceof BlockItem blockItem) {
                    BlockState state = blockItem.getBlock().defaultBlockState();
                    if (state.hasProperty(BlockStateProperties.HANGING)) {
                        state = state.setValue(BlockStateProperties.HANGING, false);
                    }
                    register(item, state);
                }
            }
            if (discovered > 0) {
                BLMod.LOGGER.debug("Found {} entries in #{}:lamps", discovered, BLMod.MOD_ID);
            }
        } catch (IllegalStateException notBound) {
            // Tags are not yet bound (e.g. very early client init). Skip gracefully.
            BLMod.LOGGER.debug("Tags not bound yet; skipping tag-based lamps for now");
        }

        // Register additional lamps from config with custom luminance
        var cfg = BLLampConfigAccess.get();
        cfg.extraLampLight.forEach(entry -> {
            Identifier id = Identifier.tryParse(entry.id);
            if (id == null) return;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == Items.AIR) return;
            if (!(item instanceof BlockItem blockItem)) return;
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HANGING)) {
                state = state.setValue(BlockStateProperties.HANGING, false);
            }
            register(item, state, entry.luminance);
        });

        // Final summary (info level): total, builtin vs extras
        int total = LAMPS.size();
        int builtin = 0;
        if (LAMPS.containsKey(Items.LANTERN)) builtin++;
        if (LAMPS.containsKey(Items.SOUL_LANTERN)) builtin++;
        int extras = Math.max(0, total - builtin);
        BLMod.LOGGER.info("Lamp registry ready: {} items ({} builtin, {} extra)", total, builtin, extras);
    }

    private static void registerCopperLanterns() {
        registerCopperVariant("copper_lantern");
        registerCopperVariant("exposed_copper_lantern");
        registerCopperVariant("weathered_copper_lantern");
        registerCopperVariant("oxidized_copper_lantern");
        registerCopperVariant("waxed_copper_lantern");
        registerCopperVariant("waxed_exposed_copper_lantern");
        registerCopperVariant("waxed_weathered_copper_lantern");
        registerCopperVariant("waxed_oxidized_copper_lantern");
    }

    private static void registerCopperVariant(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(Identifier.DEFAULT_NAMESPACE, path);
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (!(item instanceof BlockItem blockItem)) {
            BLMod.LOGGER.warn("Expected copper lantern item {}, but it was not found or not a block item", id);
            return;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HANGING)) {
            state = state.setValue(BlockStateProperties.HANGING, false);
        }
        register(item, state);
    }

    private static int clampLuminance(int lum) {
        return Math.max(0, Math.min(15, lum));
    }

    public static void register(Item item, BlockState state) {
        LAMPS.put(item, new LampData(state, state.getLightEmission()));
    }

    public static void register(Item item, BlockState state, int luminance) {
        LAMPS.put(item, new LampData(state, clampLuminance(luminance)));
    }

    public static boolean isLamp(ItemStack stack) {
        return stack != null && LAMPS.containsKey(stack.getItem());
    }

    public static boolean isLamp(Item item) {
        return item != null && LAMPS.containsKey(item);
    }

    public static BlockState getState(Item item) {
        LampData data = LAMPS.get(item);
        return data != null ? data.state() : Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false);
    }

    public static int getLuminance(Item item) {
        LampData data = LAMPS.get(item);
        return data != null ? data.luminance() : 0;
    }

    public static Identifier getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Item getById(Identifier id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }

    public static Set<Item> items() {
        return Collections.unmodifiableSet(LAMPS.keySet());
    }

}
