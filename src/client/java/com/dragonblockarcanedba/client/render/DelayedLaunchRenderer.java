package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.DelayedLaunchEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class DelayedLaunchRenderer extends EntityRenderer<DelayedLaunchEntity, EntityRenderState> {
    public DelayedLaunchRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(DelayedLaunchEntity entity, EntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        // Render absolutely nothing since this is an invisible marker entity
        super.submit(state, poseStack, collector, cameraState);
    }
}
