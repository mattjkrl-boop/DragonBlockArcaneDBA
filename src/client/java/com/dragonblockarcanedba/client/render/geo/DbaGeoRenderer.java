package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Form;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * GeckoLib renderer that replaces the vanilla player model entirely.
 * <p>
 * This renderer:
 * 1. Replaces Steve/Alex with the race's custom GeckoLib .geo.json model
 * 2. Uses DbaPlayerAnimatable for animation controllers (idle, walk, attack, transform)
 * 3. Per-bone color tinting is handled via getRenderColor() override
 *    which will apply skin/hair color blending from the player's chosen colors
 * <p>
 * Standard MC player height = 1.8 blocks. Models should be designed to match.
 */
public class DbaGeoRenderer extends GeoReplacedEntityRenderer<DbaPlayerAnimatable, Player, AvatarRenderState> {

    private static final DbaPlayerAnimatable ANIMATABLE = new DbaPlayerAnimatable();

    public DbaGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new DbaPlayerModel(), ANIMATABLE);
    }

    /**
     * Applies the player's skin color as the render tint.
     * GeckoLib calls this to get the ARGB color multiplied onto the model.
     * <p>
     * For now, this applies a uniform skin color tint. Per-bone tinting
     * (hair vs body vs avoid) will be implemented via a GeoRenderLayer
     * once custom models with distinct bone groups are imported.
     */
    @Override
    public int getRenderColor(DbaPlayerAnimatable animatable, Player entity, float partialTick) {
        if (entity instanceof PlayerStatsAccessor accessor) {
            int[] skinRgb = ColorBlending.parseHex(accessor.dba$getSkinColor(), ColorBlending.WHITE);

            // Check if current form has a skin color override and blend
            Identifier formId = accessor.dba$getActiveFormId();
            if (formId != null) {
                Form form = DbaRegistries.getForm(formId);
                if (form != null && form.getSkinColorOverride() != null) {
                    int[] formSkin = ColorBlending.parseHex(form.getSkinColorOverride(), ColorBlending.WHITE);
                    skinRgb = ColorBlending.blendTransformation(skinRgb, formSkin);
                }
            }

            return ColorBlending.toArgb(skinRgb);
        }
        return 0xFFFFFFFF; // White (no tint)
    }
}
