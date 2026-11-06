package net.oxcodsnet.beltborne_lanterns.common;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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

    private static volatile Map<Item, LampData> LAMPS = Map.of();
    public static final TagKey<Item> EXTRA_LAMPS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(BLMod.MOD_ID, "lamps"));

    private LampRegistry() {}

    /**
     * Registers vanilla lamps and any additional items provided via the
     * {@code beltborne_lanterns:lamps} item tag. The tag is intended as an
     * extension point so data packs or other mods can supply their own
     * supported lamps without code changes.
     */
    public static void init() {
        Map<Item, LampData> next = new LinkedHashMap<>();

        // Built-in vanilla lamps
        put(next, Items.LANTERN, Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));
        put(next, Items.SOUL_LANTERN, Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false));

        // Dynamically register any additional tagged items (MC 1.21+: iterateEntries)
        try {
            int discovered = 0;
            for (Holder<Item> entry : BuiltInRegistries.ITEM.getTagOrEmpty(EXTRA_LAMPS_TAG)) {
                discovered++;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(entry.value());
                BLMod.LOGGER.debug(" - {}", id);
                Item item = entry.value();
                if (next.containsKey(item)) continue;
                if (item instanceof BlockItem blockItem) {
                    BlockState state = blockItem.getBlock().defaultBlockState();
                    if (state.hasProperty(BlockStateProperties.HANGING)) {
                        state = state.setValue(BlockStateProperties.HANGING, false);
                    }
                    put(next, item, state);
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
            ResourceLocation id = ResourceLocation.tryParse(entry.id);
            if (id == null) return;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == Items.AIR) return;
            if (!(item instanceof BlockItem blockItem)) return;
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HANGING)) {
                state = state.setValue(BlockStateProperties.HANGING, false);
            }
            put(next, item, state, entry.luminance);
        });

        LAMPS = Collections.unmodifiableMap(next);

        // Final summary (info level): total, builtin vs extras
        int total = next.size();
        int builtin = 0;
        if (next.containsKey(Items.LANTERN)) builtin++;
        if (next.containsKey(Items.SOUL_LANTERN)) builtin++;
        int extras = Math.max(0, total - builtin);
        BLMod.LOGGER.info("Lamp registry ready: {} items ({} builtin, {} extra)", total, builtin, extras);
    }

    private static int clampLuminance(int lum) {
        return Math.max(0, Math.min(15, lum));
    }

    private static void put(Map<Item, LampData> target, Item item, BlockState state) {
        target.put(item, new LampData(state, state.getLightEmission()));
    }

    private static void put(Map<Item, LampData> target, Item item, BlockState state, int luminance) {
        target.put(item, new LampData(state, clampLuminance(luminance)));
    }

    public static synchronized void register(Item item, BlockState state) {
        Map<Item, LampData> next = new LinkedHashMap<>(LAMPS);
        put(next, item, state);
        LAMPS = Collections.unmodifiableMap(next);
    }

    public static synchronized void register(Item item, BlockState state, int luminance) {
        Map<Item, LampData> next = new LinkedHashMap<>(LAMPS);
        put(next, item, state, luminance);
        LAMPS = Collections.unmodifiableMap(next);
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

    public static ResourceLocation getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Item getById(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }

    public static Set<Item> items() {
        return LAMPS.keySet();
    }
}
