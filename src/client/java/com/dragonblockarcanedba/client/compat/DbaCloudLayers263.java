package com.dragonblockarcanedba.client.compat;

import com.dragonblockarcanedba.mixin.CloudRendererAccessor;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * The 26.3 port of the Otherworld multi-layer clouds.
 *
 * On 26.3 vanilla split CloudRenderer.render(color, status, height, range, cam, time,
 * partial) into prepare(...) with the same argument list (builds the mesh) plus
 * render(status, RenderPass) and renderOit(...) (draw the prepared mesh). The four
 * dedicated layer renderers below are driven with the same heights, speeds, time
 * offsets, and camera offsets the 26.2 mixin used; the vanilla renderer itself
 * provides the fifth layer (already golden and raised to 210 by CloudColorMixin).
 * The draw calls are forwarded reflectively because the RenderPass and OIT types
 * only exist on 26.3, while this code compiles against the 26.2 floor.
 */
public final class DbaCloudLayers263 {
    private DbaCloudLayers263() {}

    private static final int GOLDEN = 0xE6FFE070;
    private static final float[] HEIGHTS = {201.2f, 204.0f, 205.8f, 215.5f};
    private static final double[] SPEEDS = {1.45, 1.15, 0.90, 0.50};
    private static final double[] TIME_OFFSETS = {0.0, 350.0, 750.0, 1600.0};
    private static final double[][] CAM_OFFSETS = {{0.0, 0.0}, {68.0, 52.0}, {-88.0, 94.0}, {-72.0, -84.0}};

    private static final CloudRenderer[] LAYERS = new CloudRenderer[4];

    private static Method prepareMethod;
    private static Method renderMethod;
    private static Method renderOitMethod;
    private static boolean resolved;

    public static boolean isLayer(CloudRenderer renderer) {
        for (CloudRenderer layer : LAYERS) {
            if (layer == renderer) return true;
        }
        return false;
    }

    private static void ensureLayers() {
        for (int i = 0; i < LAYERS.length; i++) {
            if (LAYERS[i] == null) LAYERS[i] = new CloudRenderer();
        }
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            prepareMethod = CloudRenderer.class.getMethod("prepare",
                int.class, CloudStatus.class, float.class, int.class, Vec3.class, long.class, float.class);
        } catch (NoSuchMethodException ignored) {
        }
        try {
            Class<?> renderPass = Class.forName("com.mojang.renderpearl.api.commands.RenderPass");
            renderMethod = CloudRenderer.class.getMethod("render", CloudStatus.class, renderPass);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Class<?> oitStage = Class.forName("net.minecraft.client.renderer.oit.OitStage");
            Class<?> gpuTextureView = Class.forName("com.mojang.renderpearl.api.textures.GpuTextureView");
            Class<?> oitParams = Class.forName("net.minecraft.client.renderer.oit.OitRenderPassProvider$Parameters");
            renderOitMethod = CloudRenderer.class.getMethod("renderOit", CloudStatus.class, oitStage, gpuTextureView, oitParams);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    /** Copies the vanilla cloud texture into any layer that does not have it yet, as on 26.2. */
    public static void syncTextures(CloudRenderer vanilla) {
        ensureLayers();
        CloudRendererAccessor vanillaAcc = (CloudRendererAccessor) vanilla;
        CloudRenderer.TextureData tex = vanillaAcc.getTexture();
        if (tex == null) return;
        for (CloudRenderer layer : LAYERS) {
            CloudRendererAccessor layerAcc = (CloudRendererAccessor) layer;
            if (layerAcc.getTexture() == null) {
                layerAcc.setTexture(tex);
                layerAcc.setNeedsRebuild(true);
            }
        }
    }

    /** Prepares the four dedicated layers with their per-layer height, speed, and camera offset. */
    public static void prepareLayers(int cloudRange, CloudStatus status, Vec3 cameraPos, long time, float partialTicks) {
        resolve();
        if (prepareMethod == null) return;
        ensureLayers();
        double baseTime = (double) time + partialTicks;
        for (int i = 0; i < LAYERS.length; i++) {
            CloudRenderer layer = LAYERS[i];
            if (layer == null || ((CloudRendererAccessor) layer).getTexture() == null) continue;
            double t = baseTime * SPEEDS[i] + TIME_OFFSETS[i];
            long lt = (long) Math.floor(t);
            float lp = (float) (t - lt);
            Vec3 cam = cameraPos.add(CAM_OFFSETS[i][0], 0.0, CAM_OFFSETS[i][1]);
            try {
                prepareMethod.invoke(layer, GOLDEN, status, HEIGHTS[i], cloudRange, cam, lt, lp);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    /** Draws the prepared layers into the vanilla render pass. */
    public static void drawLayers(CloudStatus status, Object renderPass) {
        resolve();
        if (renderMethod == null || renderPass == null) return;
        for (CloudRenderer layer : LAYERS) {
            if (layer == null || ((CloudRendererAccessor) layer).getTexture() == null) continue;
            try {
                renderMethod.invoke(layer, status, renderPass);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    /** Draws the prepared layers in the order-independent-transparency path. */
    public static void drawLayersOit(CloudStatus status, Object stage, Object textureView, Object params) {
        resolve();
        if (renderOitMethod == null) return;
        for (CloudRenderer layer : LAYERS) {
            if (layer == null || ((CloudRendererAccessor) layer).getTexture() == null) continue;
            try {
                renderOitMethod.invoke(layer, status, stage, textureView, params);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    public static void endFrames() {
        for (CloudRenderer layer : LAYERS) {
            if (layer != null) layer.endFrame();
        }
    }

    public static void closeLayers() {
        for (int i = 0; i < LAYERS.length; i++) {
            if (LAYERS[i] != null) {
                LAYERS[i].close();
                LAYERS[i] = null;
            }
        }
    }
}
