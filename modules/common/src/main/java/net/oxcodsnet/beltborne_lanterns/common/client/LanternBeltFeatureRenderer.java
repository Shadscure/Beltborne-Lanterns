package net.oxcodsnet.beltborne_lanterns.common.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.oxcodsnet.beltborne_lanterns.common.LampRegistry;
import net.oxcodsnet.beltborne_lanterns.common.config.BLConfig;
import net.oxcodsnet.beltborne_lanterns.common.config.BLConfigs;
import net.oxcodsnet.beltborne_lanterns.common.physics.LanternSwingManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class LanternBeltFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {

    // The lantern model might need a rotation adjustment to face forward.
    private static final float MODEL_Y_ROTATION_DEGREES = 180f;
    private static boolean UUID_LOOKUP_WARNED = false;

    @SuppressWarnings("unchecked")
    public LanternBeltFeatureRenderer(RenderLayerParent<?, ?> context) {
        // Cast to the exact generic pair expected by the superclass.
        super((RenderLayerParent<AvatarRenderState, PlayerModel>) context);
    }

    @Override
    public void submit(PoseStack matrices,
                       SubmitNodeCollector collector,
                       int light,
                       AvatarRenderState state,
                       float limbAngle,
                       float limbDistance) {
        Minecraft mc = Minecraft.getInstance();
        // MC 1.21+ feature renderers receive a render-state, not the entity.
        // Determine the rendered player's UUID from the state when possible.
        UUID subject = null;
        if (mc.level != null) {
            var entity = mc.level.getEntity(state.id);
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                subject = player.getUUID();
            }
        }
        // If unavailable, try to obtain UUID from the render state via reflection to be resilient to mapping changes
        try {
            Class<?> cls = state.getClass();
            // Candidate fields: prefer direct UUID; otherwise GameProfile
            String[] uuidFieldCandidates = {"uuid", "profileId", "gameProfileId", "playerUuid"};
            for (String name : uuidFieldCandidates) {
                try {
                    var f = ReflectUtilBL.findFieldRecursive(cls, name);
                    f.setAccessible(true);
                    Object v = f.get(state);
                    if (v instanceof UUID u) {
                        subject = u;
                        break;
                    }
                } catch (NoSuchFieldException ignored2) { /* try next */ }
            }
            if (subject == null) {
                String[] profileFieldCandidates = {"gameProfile", "profile"};
                for (String name : profileFieldCandidates) {
                    try {
                        var f = ReflectUtilBL.findFieldRecursive(cls, name);
                        f.setAccessible(true);
                        Object v = f.get(state);
                        // Avoid compile-time dep: use reflection to call getId
                        if (v != null) {
                            try {
                                var m = v.getClass().getMethod("getId");
                                Object id = m.invoke(v);
                                if (id instanceof UUID u) {
                                    subject = u;
                                    break;
                                }
                            } catch (ReflectiveOperationException ignored3) {
                            }
                        }
                    } catch (NoSuchFieldException ignored4) { /* try next */ }
                }
            }
        } catch (Throwable ignored) {
        }

        if (subject == null && mc.player != null) {
            // Fallback: use local player if state field mapping changes
            subject = mc.player.getUUID();
            if (!UUID_LOOKUP_WARNED) {
                net.oxcodsnet.beltborne_lanterns.BLMod.LOGGER.warn("Beltborne Lanterns: falling back to local player UUID in feature renderer; mixin may be needed for this mapping.");
                UUID_LOOKUP_WARNED = true;
            }
        }

        if (subject == null) return;

        // Only render if that specific player actually has a belt lamp (client cache)
        var lampItem = ClientBeltPlayers.getLamp(subject);
        if (lampItem == null) return;

        BLConfig c = BLConfigs.get();

        matrices.pushPose();

        // Attach to torso (ModelPart#rotate(MatrixStack) was removed in 1.21.5)
        applyModelPart(matrices, this.getParentModel().body);

        final float offX = c.fOffsetX();
        final float offY = c.fOffsetY();
        final float offZ = c.fOffsetZ();
        final float pivX = c.fPivotX();
        final float pivY = c.fPivotY();
        final float pivZ = c.fPivotZ();
        final float s = c.fScale();

        matrices.translate(offX, offY, offZ);

        if (BLClientAbstractions.isDebugDrawEnabled()) {
            matrices.pushPose();
            matrices.translate(pivX, pivY, pivZ);
            BLDebugRender.drawAxesAndAnchor(matrices, null, 0.25f);
            matrices.popPose();
        }

        matrices.translate(pivX, pivY, pivZ);
        matrices.mulPose(Axis.YP.rotationDegrees(MODEL_Y_ROTATION_DEGREES));
        matrices.scale(s, s, s);

        float dynX = LanternSwingManager.getXDeg(subject);
        float dynZ = LanternSwingManager.getZDeg(subject);
        float baseX = LanternSwingManager.getBaseXDeg(subject);
        matrices.mulPose(Axis.XP.rotationDegrees(baseX + dynX));
        matrices.mulPose(Axis.YP.rotationDegrees(c.rotYDeg));
        matrices.mulPose(Axis.ZP.rotationDegrees(c.rotZDeg + dynZ));

        matrices.translate(-pivX, -pivY, -pivZ);

        BlockState blockState = LampRegistry.getState(lampItem);
        collector.submitBlock(matrices, blockState, light, OverlayTexture.NO_OVERLAY, state.outlineColor);

        matrices.popPose();
    }
    private static void applyModelPart(PoseStack matrices, ModelPart part) {
        // Apply the part's origin and rotation to the matrix stack
        part.translateAndRotate(matrices);
    }
}
// Reflection helpers (package-private)
final class ReflectUtilBL {
    private ReflectUtilBL() {
    }

    static java.lang.reflect.Field findFieldRecursive(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
