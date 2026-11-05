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
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void scaleModelForRender(RenderPassInfo info, float width, float height) {
        float scale = (Float) info.getOrDefaultGeckolibData(DISPLAY_SCALE, 1.0f);
        super.scaleModelForRender(info, width * scale, height * scale);
    }
}
