package net.oxcodsnet.beltborne_lanterns.common.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Loader-agnostic client-side cache of players who currently have a belt lamp.
 * Platform layers should call {@link #setLamp(UUID, Item)} from their network handlers.
 */
public final class ClientBeltPlayers {
    private static final Map<UUID, Item> BELT_PLAYERS = new ConcurrentHashMap<>();

    private ClientBeltPlayers() {}

    public static void setLamp(UUID uuid, Item lamp) {
        if (lamp != null) BELT_PLAYERS.put(uuid, lamp); else BELT_PLAYERS.remove(uuid);
    }

    public static boolean hasLantern(Player player) {
        return BELT_PLAYERS.containsKey(player.getUUID());
    }

    public static Item getLamp(Player player) {
        return BELT_PLAYERS.get(player.getUUID());
    }

    public static Item getLamp(UUID uuid) {
        return BELT_PLAYERS.get(uuid);
    }

    public static void clear() {
        BELT_PLAYERS.clear();
    }
}
