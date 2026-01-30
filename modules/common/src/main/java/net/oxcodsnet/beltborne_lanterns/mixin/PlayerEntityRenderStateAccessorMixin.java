package net.oxcodsnet.beltborne_lanterns.mixin;

import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.oxcodsnet.beltborne_lanterns.common.client.RenderStatePlayerUuidAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(PlayerRenderState.class)
public class PlayerEntityRenderStateAccessorMixin implements RenderStatePlayerUuidAccess {
    @Unique
    private UUID bl$playerUuid;

    @Override
    public UUID bl$getPlayerUuid() { return bl$playerUuid; }

    @Override
    public void bl$setPlayerUuid(UUID uuid) { this.bl$playerUuid = uuid; }
}

