package net.oxcodsnet.beltborne_lanterns.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.oxcodsnet.beltborne_lanterns.BLMod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Payload for syncing additional lamp luminance settings from server to client.
 */
public record LampConfigSyncPayload(Map<Identifier, Integer> lamps) implements CustomPacketPayload {
    public static final Type<LampConfigSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(BLMod.MOD_ID, "lamp_config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LampConfigSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public LampConfigSyncPayload decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readVarInt();
            Map<Identifier, Integer> map = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                Identifier id = buf.readIdentifier();
                int lum = buf.readVarInt();
                map.put(id, lum);
            }
            return new LampConfigSyncPayload(map);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, LampConfigSyncPayload value) {
            buf.writeVarInt(value.lamps().size());
            for (var entry : value.lamps().entrySet()) {
                buf.writeIdentifier(entry.getKey());
                buf.writeVarInt(entry.getValue());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}