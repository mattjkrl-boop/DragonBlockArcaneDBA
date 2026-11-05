package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.entity.ModelDisplayEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Renderer for the generic ModelDisplayEntity (Models Pass): a
 * GeoEntityRenderer over the dynamic ModelDisplayGeoModel, with
 * the AutoGlowingGeoLayer the weapon pipeline uses (each model
 * set's "_glowmask" texture renders emissive; the base texture
 * stays world-lit, so physical matter never glows by itself).
 * Per-entity scale comes from the synced display scale, stashed
 * into the render state at compile time like the weapon
 * renderer's per-holder data and applied at model scaling.
 */
public class ModelDisplayRenderer extends GeoEntityRenderer<ModelDisplayEntity, EntityRenderState> {
    public static final DataTicket<Float> DISPLAY_SCALE =
            DataTicket.create("dba_model_display_scale", Float.class);
    public static final DataTicket<Float> DISPLAY_PITCH =
            DataTicket.create("dba_model_display_pitch", Float.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ModelDisplayRenderer(EntityRendererProvider.Context context) {
        super(context, new ModelDisplayGeoModel());
        withRenderLayer(new AutoGlowingGeoLayer(this));
    }

    @Override
    public void fireCompileRenderStateEvent(ModelDisplayEntity animatable, Void renderData,
                                            EntityRenderState renderState, float partialTick) {
        super.fireCompileRenderStateEvent(animatable, renderData, renderState, partialTick);
        ((GeoRenderState) renderState).addGeckolibData(DISPLAY_SCALE, animatable.getDisplayScale());
        ((GeoRenderState) renderState).addGeckolibData(DISPLAY_PITCH,
                net.minecraft.util.Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()));
    }

    /**
     * GeckoLib applies only yaw to a non-living entity. Pieces are built
     * facing model +Z, so after the yaw a turn about local X by the
     * entity pitch (positive looks down, as in vanilla) tips the nose
     * along the aim: bolts and riders follow their flight line, escorts
     * the owner's look. Pieces that never set a pitch stay level.
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void applyRotations(RenderPassInfo info, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  float nativeScale) {
        super.applyRotations(info, poseStack, nativeScale);
        float pitch = (Float) info.getOrDefaultGeckolibData(DISPLAY_PITCH, 0.0f);
        if (pitch != 0.0f) {
            poseStack.mulPose(new org.joml.Matrix4f().rotation(com.mojang.math.Axis.XP.rotationDegrees(pitch)));
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void scaleModelForRender(RenderPassInfo info, float width, float height) {
        float scale = (Float) info.getOrDefaultGeckolibData(DISPLAY_SCALE, 1.0f);
        super.scaleModelForRender(info, width * scale, height * scale);
    }
}
