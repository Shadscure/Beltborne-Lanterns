package net.oxcodsnet.beltborne_lanterns.common.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Client-side common debug helpers (platform-agnostic).
 */
public final class BLDebugRender {
    private BLDebugRender() {}

    /** Draws axes gizmo and a small cube at origin. */
    public static void drawAxesAndAnchor(PoseStack matrices, MultiBufferSource vertices, float axisLength) {
        VertexConsumer vc = vertices.getBuffer(RenderType.lines());

        // Anchor: small wireframe cube at origin
        float c = axisLength * 0.08f;
        LevelRenderer.renderLineBox(matrices, vc, -c, -c, -c, c, c, c, 1.0f, 1.0f, 1.0f, 1.0f);

        // Axes as thin wireframe boxes from origin
        float t = c * 0.4f; // half-thickness
        // +X axis (red)
        LevelRenderer.renderLineBox(matrices, vc, 0.0, -t, -t, axisLength, t, t, 1.0f, 0.25f, 0.25f, 1.0f);
        // +Y axis (green)
        LevelRenderer.renderLineBox(matrices, vc, -t, 0.0, -t, t, axisLength, t, 0.25f, 1.0f, 0.25f, 1.0f);
        // +Z axis (blue)
        LevelRenderer.renderLineBox(matrices, vc, -t, -t, 0.0, t, t, axisLength, 0.25f, 0.5f, 1.0f, 1.0f);
    }
}

