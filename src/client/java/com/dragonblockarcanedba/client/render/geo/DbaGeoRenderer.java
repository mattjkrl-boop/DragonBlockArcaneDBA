package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Form;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import net.minecraft.world.entity.player.Player;

/**
 * GeckoLib renderer that replaces the vanilla player model entirely.
 * <p>
 * This renderer:
 * 1. Replaces Steve/Alex with the race's custom GeckoLib .geo.json model
 * 2. Uses DbaPlayerAnimatable for animation controllers covering ALL vanilla MC
 *    player states (idle, walk, run, jump, fall, sneak, swim, crawl, elytra,
 *    sleep, die, swing, use_item, spin_attack, sit)
 * 3. Per-bone color tinting is handled via getRenderColor() override
 *    which will apply skin/hair color blending from the player's chosen colors
 * <p>
 * Standard MC player height = 1.8 blocks. Models should be designed to match.
 */
public class DbaGeoRenderer extends GeoReplacedEntityRenderer<DbaPlayerAnimatable, Player, AvatarRenderState> {

    // ── DBA mod-specific tickets ──────────────────────────────────────────
    public static final DataTicket<Identifier> RACE_ID_TICKET = DataTicket.create("dba_race_id", Identifier.class);
    public static final DataTicket<Identifier> FORM_ID_TICKET = DataTicket.create("dba_form_id", Identifier.class);
    public static final DataTicket<String> SKIN_COLOR_TICKET = DataTicket.create("dba_skin_color", String.class);
    public static final DataTicket<String> HAIR_COLOR_TICKET = DataTicket.create("dba_hair_color", String.class);

    // ── Player state tickets for animation controller ─────────────────────
    // These supplement the built-in GeckoLib tickets (IS_MOVING, IS_DEAD_OR_DYING,
    // SWINGING_ARM, SPRINTING, IS_CROUCHING, ENTITY_POSE, VELOCITY)
    public static final DataTicket<Boolean> IS_IN_WATER = DataTicket.create("dba_in_water", Boolean.class);
    public static final DataTicket<Boolean> IS_ON_GROUND = DataTicket.create("dba_on_ground", Boolean.class);
    public static final DataTicket<Boolean> IS_FALL_FLYING = DataTicket.create("dba_elytra", Boolean.class);
    public static final DataTicket<Boolean> IS_SLEEPING = DataTicket.create("dba_sleeping", Boolean.class);
    public static final DataTicket<Boolean> IS_VISUALLY_SWIMMING = DataTicket.create("dba_vis_swimming", Boolean.class);
    public static final DataTicket<Boolean> IS_USING_ITEM = DataTicket.create("dba_using_item", Boolean.class);
    public static final DataTicket<Boolean> IS_SPIN_ATTACK = DataTicket.create("dba_spin_attack", Boolean.class);
    public static final DataTicket<Boolean> IS_PASSENGER = DataTicket.create("dba_passenger", Boolean.class);

    private static final DbaPlayerAnimatable ANIMATABLE = new DbaPlayerAnimatable();

    public DbaGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new DbaPlayerModel(), ANIMATABLE);
    }

    /**
     * Extracts all player state data into GeckoLib DataTickets so the
     * animation controllers can read them without needing entity references.
     * <p>
     * This covers every vanilla MC 26.2 player state:
     * idle, walk, run, jump, fall, sneak, sneak_walk, swim, crawl,
     * elytra, sleep, die, swing, use_item, spin_attack, sit
     */
    @Override
    public void extractRenderState(Player entity, AvatarRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        // DBA mod data (race, form, colors)
        if (entity instanceof PlayerStatsAccessor accessor) {
            state.addGeckolibData(RACE_ID_TICKET, accessor.dba$getRaceId());
            state.addGeckolibData(FORM_ID_TICKET, accessor.dba$getActiveFormId());
            state.addGeckolibData(SKIN_COLOR_TICKET, accessor.dba$getSkinColor());
            state.addGeckolibData(HAIR_COLOR_TICKET, accessor.dba$getHairColor());
        }

        // Player animation state data
        state.addGeckolibData(IS_IN_WATER, entity.isInWater());
        state.addGeckolibData(IS_ON_GROUND, entity.onGround());
        state.addGeckolibData(IS_FALL_FLYING, entity.isFallFlying());
        state.addGeckolibData(IS_SLEEPING, entity.isSleeping());
        state.addGeckolibData(IS_VISUALLY_SWIMMING, entity.isVisuallySwimming());
        state.addGeckolibData(IS_USING_ITEM, entity.isUsingItem());
        state.addGeckolibData(IS_SPIN_ATTACK, entity.isAutoSpinAttack());
        state.addGeckolibData(IS_PASSENGER, entity.isPassenger());
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
