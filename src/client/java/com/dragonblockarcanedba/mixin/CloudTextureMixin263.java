package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.client.compat.DbaCloudLayers263;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The 26.3 port of CloudTextureMixin (which stays the 26.2 implementation).
 *
 * 26.3 split CloudRenderer.render into prepare(...) (same arguments, builds the
 * mesh) and render(status, RenderPass) / renderOit(...) (draw). This mixin hooks
 * the vanilla renderer instance: when vanilla prepares its clouds in the
 * Otherworld, the four dedicated layer renderers in DbaCloudLayers263 are prepared
 * with their own heights, speeds, and camera offsets; when vanilla draws, the
 * layers draw into the same pass. The result is the same five golden layers as
 * 26.2, with the vanilla renderer providing the 210 layer (golden via
 * CloudColorMixin). DbaMixinPlugin applies this mixin on 26.3 and up only.
 *
 * The handlers for render and renderOit declare the pass arguments as Object
 * because those types do not exist on the 26.2 floor this code compiles against;
 * the actual draw is forwarded reflectively by DbaCloudLayers263.
 */
@Mixin(CloudRenderer.class)
public class CloudTextureMixin263 {

    @Unique
    private boolean dba$inOtherworld() {
        Minecraft client = Minecraft.getInstance();
        return client.level != null && client.level.dimension().toString().contains("otherworld");
    }

    @Inject(method = "prepare", at = @At("TAIL"))
    private void dba$prepareExtraLayers(int cloudColor, CloudStatus cloudStatus, float cloudHeight, int cloudRange,
                                        Vec3 cameraPos, long time, float partialTicks, CallbackInfo ci) {
        CloudRenderer self = (CloudRenderer) (Object) this;
        if (DbaCloudLayers263.isLayer(self)) return;
        if (!dba$inOtherworld()) return;
        DbaCloudLayers263.syncTextures(self);
        DbaCloudLayers263.prepareLayers(cloudRange, cloudStatus, cameraPos, time, partialTicks);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dba$drawExtraLayers(CloudStatus cloudStatus, Object renderPass, CallbackInfo ci) {
        CloudRenderer self = (CloudRenderer) (Object) this;
        if (DbaCloudLayers263.isLayer(self)) return;
        if (!dba$inOtherworld()) return;
        DbaCloudLayers263.drawLayers(cloudStatus, renderPass);
    }

    @Inject(method = "renderOit", at = @At("HEAD"))
    private void dba$drawExtraLayersOit(CloudStatus cloudStatus, Object stage, Object textureView, Object params, CallbackInfo ci) {
        CloudRenderer self = (CloudRenderer) (Object) this;
        if (DbaCloudLayers263.isLayer(self)) return;
        if (!dba$inOtherworld()) return;
        DbaCloudLayers263.drawLayersOit(cloudStatus, stage, textureView, params);
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void dba$endExtraLayerFrames(CallbackInfo ci) {
        CloudRenderer self = (CloudRenderer) (Object) this;
        if (DbaCloudLayers263.isLayer(self)) return;
        DbaCloudLayers263.endFrames();
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void dba$closeExtraLayers(CallbackInfo ci) {
        CloudRenderer self = (CloudRenderer) (Object) this;
        if (DbaCloudLayers263.isLayer(self)) return;
        DbaCloudLayers263.closeLayers();
    }
}
