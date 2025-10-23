package net.oxcodsnet.beltborne_lanterns.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.config.BLConfig;
import net.oxcodsnet.beltborne_lanterns.common.config.BLConfigs;
import net.oxcodsnet.beltborne_lanterns.common.physics.LanternSwingManager;

/**
 * Feature renderer that draws a lamp model attached to the player's belt.
 * This implementation targets the 1.21.1 renderer API, which still exposes
 * the player entity directly when rendering features.
 */
public class LanternBeltFeatureRenderer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final float MODEL_Y_ROTATION_DEGREES = 180f;

    public LanternBeltFeatureRenderer(RenderLayerParent<T, M> context) {
        super(context);
    }

    @Override
    public void render(PoseStack matrices,
                       MultiBufferSource vertexConsumers,
                       int light,
                       T entity,
                       float limbAngle,
                       float limbDistance,
                       float tickDelta,
                       float animationProgress,
                       float headYaw,
                       float headPitch) {
        if (!(entity instanceof Player player)) {
            return;
        }

        Item lampItem = ClientBeltPlayers.getLamp(player);
        if (lampItem == null) {
            return;
        }

        BLConfig config = BLConfigs.get();

        matrices.pushPose();

        // Attach the lantern to the player's torso.
        this.getParentModel().body.translateAndRotate(matrices);

        final float offsetX = config.fOffsetX();
        final float offsetY = config.fOffsetY();
        final float offsetZ = config.fOffsetZ();
        final float pivotX = config.fPivotX();
        final float pivotY = config.fPivotY();
        final float pivotZ = config.fPivotZ();
        final float scale = config.fScale();

        matrices.translate(offsetX, offsetY, offsetZ);

        if (BLClientAbstractions.isDebugDrawEnabled()) {
            matrices.pushPose();
            matrices.translate(pivotX, pivotY, pivotZ);
            BLDebugRender.drawAxesAndAnchor(matrices, vertexConsumers, 0.25f);
            matrices.popPose();
        }

        matrices.translate(pivotX, pivotY, pivotZ);
        matrices.mulPose(Axis.YP.rotationDegrees(MODEL_Y_ROTATION_DEGREES));
        matrices.scale(scale, scale, scale);

        float dynX = LanternSwingManager.getXDeg(player.getUUID());
        float dynZ = LanternSwingManager.getZDeg(player.getUUID());
        float baseX = LanternSwingManager.getBaseXDeg(player.getUUID());
        matrices.mulPose(Axis.XP.rotationDegrees(baseX + dynX));
        matrices.mulPose(Axis.YP.rotationDegrees(config.rotYDeg));
        matrices.mulPose(Axis.ZP.rotationDegrees(config.rotZDeg + dynZ));

        matrices.translate(-pivotX, -pivotY, -pivotZ);

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState blockState = LampRegistry.getState(lampItem);
        dispatcher.renderSingleBlock(blockState, matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);

        matrices.popPose();
    }
}
