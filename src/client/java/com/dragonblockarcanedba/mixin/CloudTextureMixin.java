package com.dragonblockarcanedba.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to render actual Minecraft moving clouds in the Otherworld across multiple
 * celestial elevation tiers (Y=95, 135, 175, 215) with golden anime cloud coloring.
 */
@Mixin(LevelRenderer.class)
public class CloudTextureMixin {
    @Shadow @Final private CloudRenderer cloudRenderer;
    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private LevelTargetBundle targets;

    /**
     * Ensures cloudColor is valid in Otherworld so LevelRenderer does not skip the cloud pass.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void ensureOtherworldCloudColor(
        GraphicsResourceAllocator allocator,
        DeltaTracker deltaTracker,
        boolean bl,
        CameraRenderState cameraRenderState,
        Matrix4fc matrix4fc,
        GpuBufferSlice gpuBufferSlice,
        Vector4f vector4f,
        boolean bl2,
        CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.dimension().toString().contains("otherworld")) {
            this.levelRenderState.cloudColor = 0xE6FFE070;
            this.levelRenderState.cloudHeight = 210.0f;
        }
    }

    @Unique private CloudRenderer otherworldLayer1;
    @Unique private CloudRenderer otherworldLayer2;
    @Unique private CloudRenderer otherworldLayer3;
    @Unique private CloudRenderer otherworldLayer4;

    @Unique
    private void syncLayerTexture(CloudRenderer layer) {
        if (layer == null) return;
        CloudRendererAccessor vanillaAcc = (CloudRendererAccessor) this.cloudRenderer;
        CloudRendererAccessor layerAcc = (CloudRendererAccessor) layer;
        CloudRenderer.TextureData tex = vanillaAcc.getTexture();
        if (tex != null && layerAcc.getTexture() == null) {
            layerAcc.setTexture(tex);
            layerAcc.setNeedsRebuild(true);
        }
    }

    /**
     * In the Otherworld, renders 5 distinct moving Minecraft cloud layers in the sky (base line 200 + [0..2], then +2..4, +1..3, +3..5, +3..8)
     * using dedicated CloudRenderer instances to prevent GPU fence contention.
     */
    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true)
    private void renderOtherworldMultiLayerClouds(
        FrameGraphBuilder frameGraphBuilder,
        CloudStatus cloudStatus,
        Vec3 cameraPos,
        long time,
        float partialTicks,
        int cloudColor,
        float cloudHeight,
        int cloudRange,
        CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.level.dimension().toString().contains("otherworld")) {
            ci.cancel();

            if (this.otherworldLayer1 == null) this.otherworldLayer1 = new CloudRenderer();
            if (this.otherworldLayer2 == null) this.otherworldLayer2 = new CloudRenderer();
            if (this.otherworldLayer3 == null) this.otherworldLayer3 = new CloudRenderer();
            if (this.otherworldLayer4 == null) this.otherworldLayer4 = new CloudRenderer();

            syncLayerTexture(this.otherworldLayer1);
            syncLayerTexture(this.otherworldLayer2);
            syncLayerTexture(this.otherworldLayer3);
            syncLayerTexture(this.otherworldLayer4);

            FramePass framePass = frameGraphBuilder.addPass("clouds");
            if (this.targets.clouds != null) {
                this.targets.clouds = framePass.readsAndWrites(this.targets.clouds);
            } else {
                this.targets.main = framePass.readsAndWrites(this.targets.main);
            }

            framePass.executes(() -> {
                int goldenColor = 0xE6FFE070; // Celestial golden hue
                // Base line 200.0, then Layer 0 in 0-2 range (201.2), Layer 1 in +2-4 range (204.0),
                // Layer 2 in +1-3 range (205.8), Layer 3 in +3-5 range (210.0), Layer 4 in +3-8 range (215.5)
                float[] heights = { 201.2f, 204.0f, 205.8f, 210.0f, 215.5f };
                // Realistic altitude speed gradient: lower layers move faster across the sky
                double[] speeds = { 1.45, 1.15, 0.90, 0.70, 0.50 };
                double[] offsets = { 0.0, 350.0, 750.0, 1200.0, 1600.0 };

                double baseTime = (double) time + partialTicks;

                // 50-100 block horizontal offsets so each cloud layer spawns at completely different X-Z coordinates
                Vec3 cam0 = cameraPos;
                Vec3 cam1 = cameraPos.add(68.0, 0.0, 52.0);
                Vec3 cam2 = cameraPos.add(-88.0, 0.0, 94.0);
                Vec3 cam3 = cameraPos.add(92.0, 0.0, -78.0);
                Vec3 cam4 = cameraPos.add(-72.0, 0.0, -84.0);

                // 1. Base Layer 0 (rendered on primary cloudRenderer)
                double t0 = baseTime * speeds[0] + offsets[0];
                long lt0 = (long) Math.floor(t0);
                float lp0 = (float) (t0 - lt0);
                this.cloudRenderer.render(goldenColor, cloudStatus, heights[0], cloudRange, cam0, lt0, lp0);

                // 2. Layer 1 (rendered on dedicated otherworldLayer1 with +68X, +52Z offset)
                if (this.otherworldLayer1 != null && ((CloudRendererAccessor) this.otherworldLayer1).getTexture() != null) {
                    double t1 = baseTime * speeds[1] + offsets[1];
                    long lt1 = (long) Math.floor(t1);
                    float lp1 = (float) (t1 - lt1);
                    this.otherworldLayer1.render(goldenColor, cloudStatus, heights[1], cloudRange, cam1, lt1, lp1);
                }

                // 3. Layer 2 (rendered on dedicated otherworldLayer2 with -88X, +94Z offset)
                if (this.otherworldLayer2 != null && ((CloudRendererAccessor) this.otherworldLayer2).getTexture() != null) {
                    double t2 = baseTime * speeds[2] + offsets[2];
                    long lt2 = (long) Math.floor(t2);
                    float lp2 = (float) (t2 - lt2);
                    this.otherworldLayer2.render(goldenColor, cloudStatus, heights[2], cloudRange, cam2, lt2, lp2);
                }

                // 4. Layer 3 (rendered on dedicated otherworldLayer3 with +92X, -78Z offset)
                if (this.otherworldLayer3 != null && ((CloudRendererAccessor) this.otherworldLayer3).getTexture() != null) {
                    double t3 = baseTime * speeds[3] + offsets[3];
                    long lt3 = (long) Math.floor(t3);
                    float lp3 = (float) (t3 - lt3);
                    this.otherworldLayer3.render(goldenColor, cloudStatus, heights[3], cloudRange, cam3, lt3, lp3);
                }

                // 5. Layer 4 (rendered on dedicated otherworldLayer4 with -72X, -84Z offset)
                if (this.otherworldLayer4 != null && ((CloudRendererAccessor) this.otherworldLayer4).getTexture() != null) {
                    double t4 = baseTime * speeds[4] + offsets[4];
                    long lt4 = (long) Math.floor(t4);
                    float lp4 = (float) (t4 - lt4);
                    this.otherworldLayer4.render(goldenColor, cloudStatus, heights[4], cloudRange, cam4, lt4, lp4);
                }
            });
        }
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void endOtherworldLayerFrames(CallbackInfo ci) {
        if (this.otherworldLayer1 != null) this.otherworldLayer1.endFrame();
        if (this.otherworldLayer2 != null) this.otherworldLayer2.endFrame();
        if (this.otherworldLayer3 != null) this.otherworldLayer3.endFrame();
        if (this.otherworldLayer4 != null) this.otherworldLayer4.endFrame();
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void closeOtherworldLayers(CallbackInfo ci) {
        if (this.otherworldLayer1 != null) { this.otherworldLayer1.close(); this.otherworldLayer1 = null; }
        if (this.otherworldLayer2 != null) { this.otherworldLayer2.close(); this.otherworldLayer2 = null; }
        if (this.otherworldLayer3 != null) { this.otherworldLayer3.close(); this.otherworldLayer3 = null; }
        if (this.otherworldLayer4 != null) { this.otherworldLayer4.close(); this.otherworldLayer4 = null; }
    }
}
