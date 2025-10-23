package net.oxcodsnet.beltborne_lanterns.common.persistence;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-persistent storage of which players have the belt lantern equipped.
 * Stores full ItemStack NBT to preserve enchantments, names, etc.
 */
public final class BeltLanternSave extends SavedData {
    private static final String SAVE_NAME = "beltborne_lanterns_belts";
    private static final String PLAYERS_KEY = "players";

    private final Map<UUID, ItemStack> playersWithLamps = new HashMap<>();

    private static final SavedData.Factory<BeltLanternSave> TYPE = new SavedData.Factory<>(
            BeltLanternSave::new,
            (nbt, lookup) -> BeltLanternSave.fromNbt(nbt, lookup),
            null
    );

    public static BeltLanternSave get(MinecraftServer server) {
        var psManager = server.overworld().getDataStorage();
        return psManager.computeIfAbsent(TYPE, SAVE_NAME);
    }

    public boolean has(UUID uuid) {
        return playersWithLamps.containsKey(uuid);
    }

    /**
     * Returns the item type of the stored lamp, or null.
     */
    public Item get(UUID uuid) {
        ItemStack stack = playersWithLamps.get(uuid);
        return stack != null ? stack.getItem() : null;
    }

    /**
     * Returns a copy of the stored lamp stack, or null.
     */
    public ItemStack getStack(UUID uuid) {
        ItemStack stack = playersWithLamps.get(uuid);
        return stack != null ? stack.copy() : null;
    }

    /**
     * Persists a full lamp stack (stored as a single-item copy), or clears when null.
     */
    public void set(UUID uuid, ItemStack lamp) {
        if (lamp != null && !lamp.isEmpty()) {
            playersWithLamps.put(uuid, lamp.copyWithCount(1));
        } else {
            playersWithLamps.remove(uuid);
        }
        setDirty();
    }

    /**
     * Convenience setter by item type (no NBT).
     */
    public void set(UUID uuid, Item lamp) {
        if (lamp != null) {
            playersWithLamps.put(uuid, new ItemStack(lamp));
        } else {
            playersWithLamps.remove(uuid);
        }
        setDirty();
    }

    public static BeltLanternSave fromNbt(CompoundTag nbt) {
        // Fallback path (legacy) – will not decode full stacks due to missing registry lookup
        BeltLanternSave save = new BeltLanternSave();
        CompoundTag map = nbt.getCompound(PLAYERS_KEY);
        for (String key : map.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                if (map.contains(key, Tag.TAG_STRING)) {
                    ResourceLocation id = ResourceLocation.tryParse(map.getString(key));
                    if (id != null) {
                        save.playersWithLamps.put(uuid, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)));
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return save;
    }

    public static BeltLanternSave fromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        BeltLanternSave save = new BeltLanternSave();
        CompoundTag map = nbt.getCompound(PLAYERS_KEY);
        for (String key : map.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                Tag el = map.get(key);
                if (el != null && !(el instanceof StringTag)) { // new format: full encoded stack
                    ItemStack stack = ItemStack.parse(registryLookup, el).orElse(ItemStack.EMPTY);
                    if (!stack.isEmpty()) {
                        save.playersWithLamps.put(uuid, stack);
                    }
                } else if (el instanceof StringTag) {
                    ResourceLocation id = ResourceLocation.tryParse(map.getString(key));
                    if (id != null) {
                        save.playersWithLamps.put(uuid, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id)));
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return save;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        CompoundTag map = new CompoundTag();
        for (Map.Entry<UUID, ItemStack> e : playersWithLamps.entrySet()) {
            Tag encoded = e.getValue().save(registryLookup);
            map.put(e.getKey().toString(), encoded);
        }
        nbt.put(PLAYERS_KEY, map);
        return nbt;
    }
}
