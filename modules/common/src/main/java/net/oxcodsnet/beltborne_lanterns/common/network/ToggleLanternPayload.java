package net.oxcodsnet.beltborne_lanterns.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.oxcodsnet.beltborne_lanterns.BLMod;

public record ToggleLanternPayload() implements CustomPacketPayload {
    public static final Type<ToggleLanternPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(BLMod.MOD_ID, "toggle_lantern"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleLanternPayload> CODEC = StreamCodec.unit(new ToggleLanternPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
