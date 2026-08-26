package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.config.DbaConfig;
import com.dragonblockarcanedba.client.render.AlphaModulatingVertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Redirect(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"
        )
    )
    private <S extends EntityRenderState> void dba$redirectSubmit(
        EntityRenderer<?, S> renderer,
        S state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState cameraState
    ) {
        if (DbaConfig.firstPersonHalfTransparency) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.options != null && mc.options.getCameraType().isFirstPerson()) {
                String pkg = renderer.getClass().getPackage().getName();
                if (pkg.startsWith("com.dragonblockarcanedba.client.render")) {
                    SubmitNodeCollector wrappedCollector = (SubmitNodeCollector) java.lang.reflect.Proxy.newProxyInstance(
                        SubmitNodeCollector.class.getClassLoader(),
                        new Class<?>[]{SubmitNodeCollector.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("submitCustomGeometry") && args != null && args.length == 3) {
                                PoseStack p = (PoseStack) args[0];
                                RenderType r = (RenderType) args[1];
                                SubmitNodeCollector.CustomGeometryRenderer origRenderer = (SubmitNodeCollector.CustomGeometryRenderer) args[2];
                                collector.submitCustomGeometry(p, r, (pose, buffer) -> {
                                    origRenderer.render(pose, new AlphaModulatingVertexConsumer(buffer, 0.5f));
                                });
                                return null;
                            }
                            return method.invoke(collector, args);
                        }
                    );
                    renderer.submit(state, poseStack, wrappedCollector, cameraState);
                    return;
                }
            }
        }
        renderer.submit(state, poseStack, collector, cameraState);
    }
}
