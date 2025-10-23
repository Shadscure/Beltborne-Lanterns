package net.oxcodsnet.beltborne_lanterns.common.client;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Small indirection layer for platform-specific client bits.
 * Platforms set the providers at client init.
 */
public final class BLClientAbstractions {
    private static volatile Function<Player, Item> lampProvider = p -> null;
    private static final AtomicBoolean DEBUG_FLAG = new AtomicBoolean(false);
    private BLClientAbstractions() {}

    public static void init(Function<Player, Item> lampFunc) {
        lampProvider = Objects.requireNonNull(lampFunc);
    }

    public static boolean clientHasLantern(Player player) {
        return lampProvider.apply(player) != null;
    }

    public static Item clientLamp(Player player) {
        return lampProvider.apply(player);
    }

    public static boolean isDebugDrawEnabled() {
        return DEBUG_FLAG.get();
    }

    public static void setDebugDrawEnabled(boolean enabled) {
        DEBUG_FLAG.set(enabled);
    }
}
