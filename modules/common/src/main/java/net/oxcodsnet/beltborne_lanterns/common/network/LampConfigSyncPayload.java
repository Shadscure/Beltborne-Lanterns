package net.oxcodsnet.beltborne_lanterns.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.oxcodsnet.beltborne_lanterns.BLMod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Payload for syncing additional lamp luminance settings from server to client.
 */
public record LampConfigSyncPayload(Map<ResourceLocation, Integer> lamps) implements CustomPacketPayload {
    public static final Type<LampConfigSyncPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BLMod.MOD_ID, "lamp_config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LampConfigSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public LampConfigSyncPayload decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            if (size < 0 || size > 4096) throw new IllegalArgumentException("Invalid lamp config size: " + size);
            Map<ResourceLocation, Integer> map = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                ResourceLocation id = buf.readResourceLocation();
                int lum = Math.max(0, Math.min(15, buf.readVarInt()));
                map.put(id, lum);
            }
            return new LampConfigSyncPayload(map);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, LampConfigSyncPayload value) {
            buf.writeVarInt(value.lamps().size());
            for (var entry : value.lamps().entrySet()) {
                buf.writeResourceLocation(entry.getKey());
                buf.writeVarInt(entry.getValue());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}