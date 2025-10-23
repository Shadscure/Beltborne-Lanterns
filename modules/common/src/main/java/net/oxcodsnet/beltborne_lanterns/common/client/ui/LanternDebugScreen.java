package net.oxcodsnet.beltborne_lanterns.common.client.ui;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.oxcodsnet.beltborne_lanterns.common.client.BLClientAbstractions;
import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfig;
import net.oxcodsnet.beltborne_lanterns.common.config.BLClientConfigAccess;

/**
 * Simple in-game debug UI to tweak lantern FeatureRenderer transforms.
 * Loader-agnostic: relies only on Minecraft client + AutoConfig.
 */
public class LanternDebugScreen extends Screen {
    private static final float[] STEP_PRESETS = new float[]{0.005f, 0.01f, 0.025f, 0.05f, 0.1f};
    private int stepIndex = 1;
    private boolean prevDebugEnabled;

    private EditBox copyPreviewField;
    private Button stepButton;

    public LanternDebugScreen() {
        super(Component.literal("Lantern Debug"));
    }

    @Override
    protected void init() {
        prevDebugEnabled = BLClientAbstractions.isDebugDrawEnabled();
        BLClientAbstractions.setDebugDrawEnabled(true);

        int left = 20;
        int top = 80; // push controls below the header text
        int row = 0;
        int colGap = 78;

        // Right column for scale/step/copy to save horizontal space
        int rightColWidth = 240;
        int rightX = this.width - 20 - rightColWidth;
        int rightRow = 0;

        // Helper lambdas
        Runnable save = BLClientConfigAccess::save;
        BLClientConfig cfg = BLClientConfigAccess.get();

        // Section: Offset
        addRenderableWidget(Button.builder(Component.literal("Offset X-"), b -> { cfg.offsetX100 -= scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("X+"), b -> { cfg.offsetX100 += scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;
        addRenderableWidget(Button.builder(Component.literal("Offset Y-"), b -> { cfg.offsetY100 -= scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Y+"), b -> { cfg.offsetY100 += scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;
        addRenderableWidget(Button.builder(Component.literal("Offset Z-"), b -> { cfg.offsetZ100 -= scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Z+"), b -> { cfg.offsetZ100 += scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;

        // Section: Pivot
        addRenderableWidget(Button.builder(Component.literal("Pivot X-"), b -> { cfg.pivotX100 -= scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("X+"), b -> { cfg.pivotX100 += scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;
        addRenderableWidget(Button.builder(Component.literal("Pivot Y-"), b -> { cfg.pivotY100 -= scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Y+"), b -> { cfg.pivotY100 += scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;
        addRenderableWidget(Button.builder(Component.literal("Pivot Z-"), b -> { cfg.pivotZ100 -= scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Z+"), b -> { cfg.pivotZ100 += scaledStep100(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;

        // Section: Rotation
        addRenderableWidget(Button.builder(Component.literal("Rot X-"), b -> { cfg.rotXDeg -= scaledRotStepDeg(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("X+"), b -> { cfg.rotXDeg += scaledRotStepDeg(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;
        addRenderableWidget(Button.builder(Component.literal("Rot Y-"), b -> { cfg.rotYDeg -= scaledRotStepDeg(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Y+"), b -> { cfg.rotYDeg += scaledRotStepDeg(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;
        addRenderableWidget(Button.builder(Component.literal("Rot Z-"), b -> { cfg.rotZDeg -= scaledRotStepDeg(); save.run(); refreshCopyPreview(); }).bounds(left, top + row * 22, 74, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Z+"), b -> { cfg.rotZDeg += scaledRotStepDeg(); save.run(); refreshCopyPreview(); }).bounds(left + colGap, top + row * 22, 50, 20).build());
        row++;

        // Section (right): Scale + Step
        addRenderableWidget(Button.builder(Component.literal("Scale -"), b -> {
            cfg.scale100 = Math.max(1, cfg.scale100 - scaledStep100());
            save.run();
            refreshCopyPreview();
        }).bounds(rightX, top + rightRow * 22, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Scale +"), b -> {
            cfg.scale100 += scaledStep100();
            save.run();
            refreshCopyPreview();
        }).bounds(rightX + 74, top + rightRow * 22, 70, 20).build());
        stepButton = addRenderableWidget(Button.builder(Component.literal("Step: " + stepText()), b -> {
            cycleStep();
            stepButton.setMessage(Component.literal("Step: " + stepText()));
        }).bounds(rightX + 148, top + rightRow * 22, 90, 20).build());
        rightRow++;

        // Section (right): Copy preview & button
        int previewWidth = 180;
        copyPreviewField = new EditBox(this.font, rightX, top + rightRow * 22, previewWidth, 20, Component.literal("CopyPreview"));
        copyPreviewField.setEditable(false);
        addRenderableWidget(copyPreviewField);
        addRenderableWidget(Button.builder(Component.literal("Copy"), b -> copyValuesToClipboard())
                .bounds(rightX + previewWidth + 4, top + rightRow * 22, rightColWidth - previewWidth - 4, 20)
                .build());
        refreshCopyPreview();
    }

    private int scaledStep100() {
        // Use 1/1000 precision so 0.005/0.025 steps are exact
        float base = STEP_PRESETS[stepIndex] * 1000f;
        float mul = 1.0f;
        if (hasShiftDown()) mul *= 10f;
        if (hasControlDown()) mul *= 0.1f;
        return Math.max(1, Math.round(base * mul));
    }

    private int scaledRotStepDeg() {
        float base = 5f;
        float mul = 1.0f;
        if (hasShiftDown()) mul *= 3.0f;
        if (hasControlDown()) mul *= 0.2f;
        return Math.max(1, Math.round(base * mul));
    }

    private void cycleStep() {
        stepIndex = (stepIndex + 1) % STEP_PRESETS.length;
    }

    private String stepText() {
        return String.format("%.3f", STEP_PRESETS[stepIndex]);
    }

    private void refreshCopyPreview() {
        BLClientConfig c = BLClientConfigAccess.get();
        String s = String.format("off(%.3f,%.3f,%.3f) piv(%.3f,%.3f,%.3f) rot(%d,%d,%d) sc(%.3f)",
                c.fOffsetX(), c.fOffsetY(), c.fOffsetZ(), c.fPivotX(), c.fPivotY(), c.fPivotZ(), c.rotXDeg, c.rotYDeg, c.rotZDeg, c.fScale());
        if (copyPreviewField != null) copyPreviewField.setValue(s);
    }

    private void copyValuesToClipboard() {
        BLClientConfig c = BLClientConfigAccess.get();
        String jsonish = String.format("{offset:[%.3f,%.3f,%.3f], pivot:[%.3f,%.3f,%.3f], rot:[%d,%d,%d], scale:%.3f}",
                c.fOffsetX(), c.fOffsetY(), c.fOffsetZ(), c.fPivotX(), c.fPivotY(), c.fPivotZ(), c.rotXDeg, c.rotYDeg, c.rotZDeg, c.fScale());
        Minecraft.getInstance().keyboardHandler.setClipboard(jsonish);
    }

    @Override
    public void onClose() {
        BLClientAbstractions.setDebugDrawEnabled(prevDebugEnabled);
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        BLClientConfig cfg = BLClientConfigAccess.get();
        boolean used = false;
        switch (keyCode) {
            case 262: cfg.offsetX100 += scaledStep100(); used = true; break;
            case 263: cfg.offsetX100 -= scaledStep100(); used = true; break;
            case 265: cfg.offsetZ100 -= scaledStep100(); used = true; break;
            case 264: cfg.offsetZ100 += scaledStep100(); used = true; break;
            case 266: cfg.offsetY100 += scaledStep100(); used = true; break;
            case 267: cfg.offsetY100 -= scaledStep100(); used = true; break;
            case 82: cfg.rotXDeg += scaledRotStepDeg(); used = true; break;
            case 70: cfg.rotXDeg -= scaledRotStepDeg(); used = true; break;
            case 84: cfg.rotYDeg += scaledRotStepDeg(); used = true; break;
            case 71: cfg.rotYDeg -= scaledRotStepDeg(); used = true; break;
            case 89: cfg.rotZDeg += scaledRotStepDeg(); used = true; break;
            case 72: cfg.rotZDeg -= scaledRotStepDeg(); used = true; break;
            case 73: cfg.pivotX100 += scaledStep100(); used = true; break;
            case 75: cfg.pivotX100 -= scaledStep100(); used = true; break;
            case 79: cfg.pivotY100 += scaledStep100(); used = true; break;
            case 76: cfg.pivotY100 -= scaledStep100(); used = true; break;
            case 80: cfg.pivotZ100 += scaledStep100(); used = true; break;
            case 59: cfg.pivotZ100 -= scaledStep100(); used = true; break;
            case 85: cfg.scale100 += scaledStep100(); used = true; break;
            case 74: cfg.scale100 = Math.max(1, cfg.scale100 - scaledStep100()); used = true; break;
            default: break;
        }
        if (used) {
            BLClientConfigAccess.save();
            refreshCopyPreview();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBlurredBackground(float delta) {
        // keep world visible
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        BLClientConfig c = BLClientConfigAccess.get();
        int x = 20;
        int y = 10;
        ctx.drawString(this.font, Component.literal("Lantern Debug (hold Shift=×10, Ctrl=×0.1)" ).withStyle(ChatFormatting.YELLOW), x, y, 0xFFFFFF, false);
        y += 14;
        ctx.drawString(this.font, Component.literal(String.format("Offset: X=%.3f Y=%.3f Z=%.3f", c.fOffsetX(), c.fOffsetY(), c.fOffsetZ())), x, y, 0xFFFFFF, false);
        y += 12;
        ctx.drawString(this.font, Component.literal(String.format("Pivot:  X=%.3f Y=%.3f Z=%.3f", c.fPivotX(), c.fPivotY(), c.fPivotZ())), x, y, 0xFFFFFF, false);
        y += 12;
        ctx.drawString(this.font, Component.literal(String.format("Rot:    X=%d Y=%d Z=%d", c.rotXDeg, c.rotYDeg, c.rotZDeg)), x, y, 0xFFFFFF, false);
        y += 12;
        ctx.drawString(this.font, Component.literal(String.format("Scale:  %.3f", c.fScale())), x, y, 0xFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
