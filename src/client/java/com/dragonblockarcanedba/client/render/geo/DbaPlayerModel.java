package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Race;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * GeckoLib model provider for DBA race characters.
 * <p>
 * Resolves the correct .geo.json model, .animation.json, and texture
 * for each race. Falls back to a default humanoid model/animation if
 * no custom asset exists for the race.
 * <p>
 * Asset path conventions (matching the Blender → Blockbench → GeckoLib pipeline):
 * <ul>
 *   <li>Geometry: {@code geo/{race_id}.geo.json}</li>
 *   <li>Animation: {@code animations/{race_id}.animation.json}</li>
 *   <li>Texture: {@code textures/entity/races/{race_id}.png}</li>
 * </ul>
 * <p>
 * Standard MC player height = 1.8 blocks (~30 pixels in Blockbench at 16px/block).
 * Models from Meshy should target ~1000 faces after Blender Decimate (ratio ~0.003).
 */
public class DbaPlayerModel extends GeoModel<DbaPlayerAnimatable> {

    private static final String FALLBACK_RACE = "default_humanoid";

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        Identifier raceId = renderState.getOrDefaultGeckolibData(DbaGeoRenderer.RACE_ID_TICKET, Identifier.fromNamespaceAndPath("dragonblockarcanedba", "human"));
        String raceKey = raceId.getPath();

        Identifier raceModel = DragonBlockArcaneDBA.id("geo/" + raceKey + ".geo.json");
        if (resourceExists(raceModel)) {
            return raceModel;
        }
        return DragonBlockArcaneDBA.id("geo/" + FALLBACK_RACE + ".geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Identifier raceId = renderState.getOrDefaultGeckolibData(DbaGeoRenderer.RACE_ID_TICKET, Identifier.fromNamespaceAndPath("dragonblockarcanedba", "human"));
        Identifier formId = renderState.getGeckolibData(DbaGeoRenderer.FORM_ID_TICKET);
        String raceKey = raceId.getPath();

        // 1. Check form override texture first if transformed
        if (formId != null) {
            var form = DbaRegistries.getForm(formId);
            if (form != null && form.getModelOverride() != null) {
                return form.getModelOverride();
            }
        }

        // 2. Check if a race-specific texture exists
        Identifier raceTexture = DragonBlockArcaneDBA.id("textures/entity/races/" + raceKey + ".png");
        if (resourceExists(raceTexture)) {
            return raceTexture;
        }

        // 3. Fallback to default
        return DragonBlockArcaneDBA.id("textures/entity/races/" + FALLBACK_RACE + ".png");
    }

    @Override
    public Identifier getAnimationResource(DbaPlayerAnimatable animatable) {
        // Returns the single default_humanoid.animation.json which contains
        // animations for all entities/races
        return DragonBlockArcaneDBA.id("animations/" + FALLBACK_RACE + ".animation.json");
    }

    private boolean resourceExists(Identifier id) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(id);
            return resource.isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}
