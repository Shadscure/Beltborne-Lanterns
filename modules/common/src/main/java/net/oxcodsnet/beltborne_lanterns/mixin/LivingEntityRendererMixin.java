package net.oxcodsnet.beltborne_lanterns.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.oxcodsnet.beltborne_lanterns.common.client.RenderStateUUIDMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void bl$captureUUID(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof Player && state instanceof PlayerRenderState) {
            if (state instanceof net.oxcodsnet.beltborne_lanterns.common.client.RenderStatePlayerUuidAccess acc) {
                acc.bl$setPlayerUuid(entity.getUUID());
            }
            RenderStateUUIDMap.put(state, entity.getUUID());
        }
    }
}
