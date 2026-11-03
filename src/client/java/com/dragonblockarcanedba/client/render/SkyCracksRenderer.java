package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.SkyCracksEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * The sky billboard used to paint a flat red lightning picture hundreds of
 * blocks above the cursed storm. That picture is not drawn. The bolts live
 * on the storm dome and on the strike itself.
 */
public class SkyCracksRenderer extends EntityRenderer<SkyCracksEntity, SkyCracksRenderer.SkyCracksRenderState> {

    public SkyCracksRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class SkyCracksRenderState extends EntityRenderState {
        public float age = 0;
        public long seed = 0;
    }

    @Override
    public SkyCracksRenderState createRenderState() {
        return new SkyCracksRenderState();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(SkyCracksEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public void extractRenderState(SkyCracksEntity entity, SkyCracksRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(SkyCracksRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);
    }
}
