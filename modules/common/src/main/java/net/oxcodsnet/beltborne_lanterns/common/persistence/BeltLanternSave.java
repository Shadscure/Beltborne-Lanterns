package net.oxcodsnet.beltborne_lanterns.common.persistence;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.datafixers.util.Pair;

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

    private static final Codec<BeltLanternSave> CODEC = Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(BeltLanternSave value, DynamicOps<T> ops, T prefix) {
            CompoundTag nbt = new CompoundTag();
            CompoundTag map = new CompoundTag();
            // Get NbtOps with registry access if available
            DynamicOps<Tag> nbtOps = getNbtOps(ops);
            for (Map.Entry<UUID, ItemStack> e : value.playersWithLamps.entrySet()) {
                ItemStack stack = e.getValue();
                ItemStack.CODEC.encodeStart(nbtOps, stack).result()
                        .ifPresent(el -> map.put(e.getKey().toString(), el));
            }
            nbt.put(PLAYERS_KEY, map);
            return CompoundTag.CODEC.encodeStart(ops, nbt);
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<BeltLanternSave, T>> decode(DynamicOps<T> ops, T input) {
            return CompoundTag.CODEC.parse(ops, input).map(nbt -> {
                BeltLanternSave save = new BeltLanternSave();
                CompoundTag map = nbt.getCompoundOrEmpty(PLAYERS_KEY);
                // Get NbtOps with registry access if available
                DynamicOps<Tag> nbtOps = getNbtOps(ops);
                for (String key : map.keySet()) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        Tag el = map.get(key);
                        if (el != null && !(el instanceof StringTag)) {
                            // New format: full encoded stack
                            ItemStack.CODEC.parse(nbtOps, el).result().ifPresent(stack -> {
                                if (!stack.isEmpty()) {
                                    save.playersWithLamps.put(uuid, stack);
                                }
                            });
                        } else {
                            // Legacy format: just resource location string
                            map.getString(key).ifPresent(str -> {
                                Identifier id = Identifier.tryParse(str);
                                if (id != null) {
                                    save.playersWithLamps.put(uuid, new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
                                }
                            });
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                return Pair.of(save, input);
            });
        }
    });

    /**
     * Extracts NbtOps with registry access from the given ops if it's a RegistryOps.
     */
    @SuppressWarnings("unchecked")
    private static <T> DynamicOps<Tag> getNbtOps(DynamicOps<T> ops) {
        if (ops instanceof RegistryOps<?> registryOps) {
            // RegistryOps has registry access - create NbtOps with same registry
            return registryOps.withParent(NbtOps.INSTANCE);
        }
        return NbtOps.INSTANCE;
    }

    private static final SavedDataType<BeltLanternSave> TYPE = new SavedDataType<>(
            SAVE_NAME,
            BeltLanternSave::new,
            CODEC,
            null
    );

    public static BeltLanternSave get(MinecraftServer server) {
        var psManager = server.overworld().getDataStorage();
        return psManager.computeIfAbsent(TYPE);
    }

    public BeltLanternSave() {}

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

    // Legacy load method for compatibility
    public static BeltLanternSave fromNbt(CompoundTag nbt) {
        BeltLanternSave save = new BeltLanternSave();
        CompoundTag map = nbt.getCompoundOrEmpty(PLAYERS_KEY);
        for (String key : map.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                map.getString(key).ifPresent(str -> {
                    Identifier id = Identifier.tryParse(str);
                    if (id != null) {
                        save.playersWithLamps.put(uuid, new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
                    }
                });
            } catch (IllegalArgumentException ignored) {}
        }
        return save;
    }

    public static BeltLanternSave fromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        BeltLanternSave save = new BeltLanternSave();
        CompoundTag map = nbt.getCompoundOrEmpty(PLAYERS_KEY);
        var ops = RegistryOps.create(NbtOps.INSTANCE, registryLookup);
        for (String key : map.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                Tag el = map.get(key);
                if (el != null && !(el instanceof StringTag)) {
                    ItemStack.CODEC.parse(ops, el).result().ifPresent(stack -> {
                        if (!stack.isEmpty()) {
                            save.playersWithLamps.put(uuid, stack);
                        }
                    });
                } else {
                    map.getString(key).ifPresent(str -> {
                        Identifier id = Identifier.tryParse(str);
                        if (id != null) {
                            save.playersWithLamps.put(uuid, new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
                        }
                    });
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return save;
    }

    public CompoundTag writeNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        CompoundTag map = new CompoundTag();
        var ops = RegistryOps.create(NbtOps.INSTANCE, registryLookup);
        for (Map.Entry<UUID, ItemStack> e : playersWithLamps.entrySet()) {
            ItemStack stack = e.getValue();
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(el -> map.put(e.getKey().toString(), el));
        }
        nbt.put(PLAYERS_KEY, map);
        return nbt;
    }
}
