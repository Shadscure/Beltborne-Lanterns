package net.oxcodsnet.beltborne_lanterns.common;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Runtime-only per-player state: which lamp (with NBT) is
 * "equipped on the belt". Stored as an ItemStack to preserve all data.
 */
public final class BeltState {
    private static final Map<UUID, ItemStack> PLAYER_LAMPS = new ConcurrentHashMap<>();

    private BeltState() {}

    public static boolean hasLamp(UUID uuid) {
        return PLAYER_LAMPS.containsKey(uuid);
    }

    public static boolean hasLamp(Player player) {
        return hasLamp(player.getUUID());
    }

    /**
     * Returns the lamp item type currently equipped, or null.
     * Kept for convenience when only item identity is required (e.g., networking/FX).
     */
    public static Item getLamp(UUID uuid) {
        ItemStack stack = PLAYER_LAMPS.get(uuid);
        return stack != null ? stack.getItem() : null;
    }

    /**
     * Returns the lamp item type currently equipped, or null.
     */
    public static Item getLamp(Player player) {
        return getLamp(player.getUUID());
    }

    /**
     * Returns a copy of the stored lamp stack (count=as stored), or null.
     */
    public static ItemStack getLampStack(UUID uuid) {
        ItemStack stack = PLAYER_LAMPS.get(uuid);
        return stack != null ? stack.copy() : null;
    }

    public static ItemStack getLampStack(Player player) {
        return getLampStack(player.getUUID());
    }

    /**
     * Sets the equipped lamp to the given stack (stored as a single-item copy), or clears when null.
     */
    public static void setLamp(UUID uuid, ItemStack lamp) {
        if (lamp != null && !lamp.isEmpty()) {
            PLAYER_LAMPS.put(uuid, lamp.copyWithCount(1));
        } else {
            PLAYER_LAMPS.remove(uuid);
        }
    }

    public static void setLamp(Player player, ItemStack lamp) {
        setLamp(player.getUUID(), lamp);
    }

    /**
     * Convenience: sets from item type only (no NBT).
     */
    public static void setLamp(UUID uuid, Item lamp) {
        if (lamp != null) {
            PLAYER_LAMPS.put(uuid, new ItemStack(lamp));
        } else {
            PLAYER_LAMPS.remove(uuid);
        }
    }

    public static void setLamp(Player player, Item lamp) {
        setLamp(player.getUUID(), lamp);
    }
}

