package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.weapon.model.BoxPart;
import com.dragonblockarcanedba.client.render.weapon.model.ModelPart;
import com.dragonblockarcanedba.client.render.weapon.model.RingPart;
import com.dragonblockarcanedba.entity.CurseChainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Entity Renderer for Curse Chain in Minecraft 26.2.
 * Renders dark spectral chained links with glowing cursed purple runes.
 */
public class CurseChainRenderer extends EntityRenderer<CurseChainEntity, CurseChainRenderer.CurseChainRenderState> {
    private final ModelPart chainModel;

    public CurseChainRenderer(EntityRendererProvider.Context context) {
        super(context);

        chainModel = new ModelPart();

        // Dark iron interlocking chain links
        ModelPart link1 = new RingPart(0.8f, 1.4f, 0.3f, 8).setColor(0xFF1A1A1A);
        link1.setPos(0, -0.6f, 0);

        ModelPart link2 = new RingPart(0.8f, 1.4f, 0.3f, 8).setColor(0xFF1A1A1A);
        link2.setPos(0, 0.6f, 0);
        link2.setRot(0, (float) Math.PI / 2f, 0); // Perpendicular link

        // Glowing purple cursed runes on the links
        ModelPart rune1 = new BoxPart(0.3f, 0.6f, 0.3f).setColor(0xFF800080);
        rune1.setPos(0.9f, 0, 0);

        ModelPart rune2 = new BoxPart(0.3f, 0.6f, 0.3f).setColor(0xFF800080);
        rune2.setPos(-0.9f, 0, 0);

        chainModel.addChild(link1);
        chainModel.addChild(link2);
        chainModel.addChild(rune1);
        chainModel.addChild(rune2);
    }

    @Override
    public CurseChainRenderState createRenderState() {
        return new CurseChainRenderState();
    }

    @Override
    public void extractRenderState(CurseChainEntity entity, CurseChainRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.isAttached = entity.isAttached();
        state.orbitIndex = entity.getOrbitIndex();
    }

    @Override
    public void submit(CurseChainRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        poseStack.pushPose();
        poseStack.scale(0.4f, 0.4f, 0.4f);

        // Dynamic spinning
        float spinSpeed = state.isAttached ? 8.0f : 15.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * spinSpeed + (state.orbitIndex * 36.0f)));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.ageInTicks * 4.0f));

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack localPose = new PoseStack();
            localPose.last().pose().set(pose.pose());
            localPose.last().normal().set(pose.normal());
            chainModel.render(localPose, buffer, KiRenderHelper.FULL_BRIGHT, KiRenderHelper.NO_OVERLAY, null);
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    public static class CurseChainRenderState extends EntityRenderState {
        public float ageInTicks;
        public boolean isAttached;
        public int orbitIndex;
    }
}
