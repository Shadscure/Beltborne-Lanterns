package net.oxcodsnet.beltborne_lanterns.fabric;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.network.BeltSyncPayload;

import java.util.UUID;

public final class BeltNetworking {
    private BeltNetworking() {}

    public static void broadcastBeltState(ServerPlayer subject, Item lamp) {
        // Send to watchers + the subject themselves
        for (ServerPlayer target : PlayerLookup.tracking(subject)) {
            ServerPlayNetworking.send(target, new BeltSyncPayload(subject.getUUID(), lamp != null ? LampRegistry.getId(lamp) : null));
        }
        ServerPlayNetworking.send(subject, new BeltSyncPayload(subject.getUUID(), lamp != null ? LampRegistry.getId(lamp) : null));
    }

    public static void sendTo(ServerPlayer target, UUID subjectUuid, Item lamp) {
        ServerPlayNetworking.send(target, new BeltSyncPayload(subjectUuid, lamp != null ? LampRegistry.getId(lamp) : null));
    }
}
