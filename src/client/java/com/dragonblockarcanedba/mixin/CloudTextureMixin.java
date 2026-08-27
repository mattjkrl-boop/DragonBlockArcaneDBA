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
            this.levelRenderState.cloudHeight = 275.0f;
        }
    }

    /**
     * In the Otherworld, renders moving celestial golden clouds 150-200 blocks higher in the sky (Y=275).
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

            FramePass framePass = frameGraphBuilder.addPass("clouds");
            if (this.targets.clouds != null) {
                this.targets.clouds = framePass.readsAndWrites(this.targets.clouds);
            } else {
                this.targets.main = framePass.readsAndWrites(this.targets.main);
            }

            framePass.executes(() -> {
                int goldenColor = 0xE6FFE070; // Celestial golden hue
                float cloudY = 275.0f; // High in the heavens (164 blocks above Check-In Station at Y=111)

                // Realistic altitude speed gradient: lower altitude views clouds moving faster
                double camY = cameraPos.y();
                double speed = Math.max(0.60, Math.min(1.50, 1.40 - (camY - 100.0) * 0.005));

                double baseTime = ((double) time + partialTicks) * speed;
                long layerTime = (long) Math.floor(baseTime);
                float layerPartial = (float) (baseTime - layerTime);

                this.cloudRenderer.render(
                    goldenColor,
                    cloudStatus,
                    cloudY,
                    cloudRange,
                    cameraPos,
                    layerTime,
                    layerPartial
                );
            });
        }
    }
}
