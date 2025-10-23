package net.oxcodsnet.beltborne_lanterns.common.persistence;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.DataResult;
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

    private static final SavedDataType<BeltLanternSave> TYPE = new SavedDataType<>(
            SAVE_NAME,
            (SavedData.Context ctx) -> new BeltLanternSave(),
            BeltLanternSave::codec,
            (net.minecraft.util.datafix.DataFixTypes) null
    );

    private static Codec<BeltLanternSave> codec(SavedData.Context ctx) {
        return Codec.of(new Encoder<>() {
            @Override
            public <T> DataResult<T> encode(BeltLanternSave value, DynamicOps<T> ops, T prefix) {
                CompoundTag nbt = new CompoundTag();
                var world = ctx.levelOrThrow();
                var registries = new RegistrySetBuilder().build(world.registryAccess());
                value.writeNbt(nbt, registries);
                return CompoundTag.CODEC.encodeStart(ops, nbt);
            }
        }, new Decoder<>() {
            @Override
            public <T> DataResult<Pair<BeltLanternSave, T>> decode(DynamicOps<T> ops, T input) {
                return CompoundTag.CODEC.parse(ops, input).map(nbt -> {
                    var world = ctx.levelOrThrow();
                    var registries = new RegistrySetBuilder().build(world.registryAccess());
                    return Pair.of(BeltLanternSave.fromNbt(nbt, registries), input);
                });
            }
        });
    }

    public static BeltLanternSave get(MinecraftServer server) {
        var psManager = server.overworld().getDataStorage();
        return (BeltLanternSave) psManager.computeIfAbsent(TYPE);
    }

    public BeltLanternSave() {}

    public BeltLanternSave(SavedData.Context ctx) {}

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
        CompoundTag map = nbt.getCompoundOrEmpty(PLAYERS_KEY);
        for (String key : map.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                map.getString(key).ifPresent(str -> {
                    ResourceLocation id = ResourceLocation.tryParse(str);
                    if (id != null) {
                        save.playersWithLamps.put(uuid, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)));
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
                if (el != null && !(el instanceof StringTag)) { // new format: full encoded stack
                    ItemStack.CODEC.parse(ops, el).result().ifPresent(stack -> {
                        if (!stack.isEmpty()) {
                            save.playersWithLamps.put(uuid, stack);
                        }
                    });
                } else {
                    map.getString(key).ifPresent(str -> {
                        ResourceLocation id = ResourceLocation.tryParse(str);
                        if (id != null) {
                            save.playersWithLamps.put(uuid, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id)));
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
