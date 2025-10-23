package net.oxcodsnet.beltborne_lanterns.common.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.oxcodsnet.beltborne_lanterns.BLMod;

import java.util.UUID;

public record BeltSyncPayload(UUID playerUuid, ResourceLocation lampId) implements CustomPacketPayload {
    public static final Type<BeltSyncPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(BLMod.MOD_ID, "belt_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeltSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public BeltSyncPayload decode(RegistryFriendlyByteBuf buf) {
            UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf);
            boolean has = buf.readBoolean();
            ResourceLocation id = has ? buf.readResourceLocation() : null;
            return new BeltSyncPayload(uuid, id);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, BeltSyncPayload value) {
            UUIDUtil.STREAM_CODEC.encode(buf, value.playerUuid());
            if (value.lampId() != null) {
                buf.writeBoolean(true);
                buf.writeResourceLocation(value.lampId());
            } else {
                buf.writeBoolean(false);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
