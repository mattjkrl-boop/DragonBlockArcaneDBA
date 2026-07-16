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
        // For now, use default humanoid model for all races until custom models are imported
        // When race-specific models exist, this will resolve per-race:
        //   DragonBlockArcaneDBA.id("geo/" + raceKey + ".geo.json")
        return DragonBlockArcaneDBA.id("geo/" + FALLBACK_RACE + ".geo.json");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        // Default race texture — will be tinted by the renderer
        return DragonBlockArcaneDBA.id("textures/entity/races/" + FALLBACK_RACE + ".png");
    }

    @Override
    public Identifier getAnimationResource(DbaPlayerAnimatable animatable) {
        return DragonBlockArcaneDBA.id("animations/" + FALLBACK_RACE + ".animation.json");
    }
}
