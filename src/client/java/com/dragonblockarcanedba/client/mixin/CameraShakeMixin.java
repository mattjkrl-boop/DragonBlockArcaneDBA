package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.fx.ScreenShakeHandler;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraShakeMixin {
    @Shadow private float xRot;
    @Shadow private float yRot;

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void dba$applyShake(float partialTick, CallbackInfo ci) {
        float dy = ScreenShakeHandler.yaw(partialTick);
        float dp = ScreenShakeHandler.pitch(partialTick);
        if (dy != 0f || dp != 0f) {
            this.setRotation(this.yRot + dy, this.xRot + dp);
        }
    }

    /** Dev-only preview orbit (DevPreviewCamera); applied before the third-person move so the camera orbits the player. */
    @ModifyArg(method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1),
            index = 0)
    private float dba$devPreviewYaw(float yRot) {
        return yRot + com.dragonblockarcanedba.client.dev.DevPreviewCamera.yawOffset;
    }
}
