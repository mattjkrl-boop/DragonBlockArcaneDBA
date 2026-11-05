package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.entity.ModelDisplayEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

/**
 * The dynamic GeoModel behind the one generic ModelDisplayEntity
 * (Models Pass): the asset set is not fixed per renderer, it is
 * the model name the server synced onto the entity, stashed into
 * the render state by addAdditionalStateData and resolved here
 * to the standard asset paths (geckolib model, entity texture,
 * geckolib animation file), so any def can name any authored
 * model set in data with no new renderer code.
 */
public class ModelDisplayGeoModel extends GeoModel<ModelDisplayEntity> {
    public static final DataTicket<String> MODEL_NAME =
            DataTicket.create("dba_model_display_name", String.class);
    private static final String FALLBACK = "celestial_sword";

    @Override
    public void addAdditionalStateData(ModelDisplayEntity animatable, Object renderData,
                                       GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, renderData, renderState);
        String name = animatable.getModelName();
        renderState.addGeckolibData(MODEL_NAME, name == null || name.isEmpty() ? FALLBACK : name);
    }

    private static String name(GeoRenderState renderState) {
        String name = renderState.getOrDefaultGeckolibData(MODEL_NAME, FALLBACK);
        return name == null || name.isEmpty() ? FALLBACK : name;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return DragonBlockArcaneDBA.id("geckolib/models/entity/" + name(renderState) + ".geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return DragonBlockArcaneDBA.id("textures/entity/" + name(renderState) + ".png");
    }

    @Override
    public Identifier getAnimationResource(ModelDisplayEntity animatable) {
        String name = animatable.getModelName();
        if (name == null || name.isEmpty()) {
            name = FALLBACK;
        }
        return DragonBlockArcaneDBA.id("geckolib/animations/entity/" + name + ".animation.json");
    }
}
