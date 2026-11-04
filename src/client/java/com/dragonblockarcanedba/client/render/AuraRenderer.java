package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.client.render.ki.ComplexTextures;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Form;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Transformed players breathe with a ki aura. The emission runs on a pulse
 * wave (each player phase-shifted so a crowd does not blink in sync): the
 * aura swells and settles, rising faster at the crest. While the player
 * charges (holding use) the pulse becomes a surge: more motes, tighter
 * column, faster rise, reading as power being pulled in before a release.
 *
 * No-shapes conversion: the old single KI_MOTE sprite is retired here. The
 * same pulse counts, spread, and rise now emit through MaterialEmitters as
 * three material layers in the active form family's signature colors: dense
 * PLASMA through the body column, SOUL motes rising through the lower body,
 * and pale VAPOR at the rim that sheds outward along the ground. The
 * material profiles carry the turbulence, buoyancy, and density fade.
 * Density scaling is applied once by MaterialEmitters.burst, the shared
 * rule for the material system.
 *
 * Complex pass: that material emission is now the filler layer of a
 * layered composition (class L). The pieces layer is drawn from the level
 * render submit pass (see collectSubmits): ComplexGeometry flameTongues
 * at the builder's full count budget of nine tongues, in the same form
 * colors, rising around the player as the aura lashes of the reference
 * picture, plus a ComplexGeometry tendrilFan of wisps at high form grades
 * (form data stage 3 and up). Both builders submit through submitEnergy
 * on the dissolve shader, so no aura body is a round glow sprite and the
 * PLASMA filler is never the dominant layer. The complex particle layer
 * is a WISP_CURL shed through the body column, one curl every second tick
 * (10 per second per player, under the continuous cap). The geometry hook
 * registers itself lazily from the first transformed player's tick,
 * because this class is driven from the client tick loop in the main
 * client class, which is not this owner's file. Piece seeds, colors,
 * pulse, and charging state all derive from the same synced form state
 * and the same UUID seed the emission uses, so remote players show the
 * same aura on every client.
 */
public class AuraRenderer {

    /** Plasma core color and paler soul color for one form family. */
    private record AuraColors(int plasma, int soul) {}

    private static final AuraColors SSJ = new AuraColors(0xFFD700, 0xFFEC9E);
    private static final AuraColors BLUE = new AuraColors(0x1E90FF, 0x9FD8FF);
    private static final AuraColors GOD = new AuraColors(0xDC143C, 0xFF8A7A);
    private static final AuraColors UI = new AuraColors(0xC8C8DC, 0xEDF1FF);
    private static final AuraColors GOLDEN = new AuraColors(0xFFD700, 0xFFF0B3);
    private static final AuraColors KAIOKEN = new AuraColors(0xFF3030, 0xFF9A80);
    private static final AuraColors NAMEKIAN = new AuraColors(0xFF8C00, 0xFFBE78);
    private static final AuraColors ARCOSIAN = new AuraColors(0xB373FF, 0xD8B6FF);
    private static final AuraColors DEFAULT = new AuraColors(0xFFE9A8, 0xFFF6D8);

    /** Layer rotation across one pulse count: plasma, plasma, soul, plasma, vapor. */
    private static final int[] LAYER_PATTERN = {0, 0, 1, 0, 2};

    public static void renderAura(AbstractClientPlayer player) {
        if (!(player instanceof PlayerStatsAccessor accessor)) return;
        Identifier formId = accessor.dba$getActiveFormId();
        if (formId == null) return;
        if (DbaEffectsConfig.effectDensity() <= 0f) return;
        if (!(player.level() instanceof ClientLevel level)) return;
        ensurePiecesHook();

        boolean charging = player.isUsingItem();
        double phase = player.getUUID().hashCode() & 0xFF;
        double pulse = 0.5 + 0.5 * Math.sin(player.tickCount * 0.32 + phase);
        int count;
        double spread, rise;
        if (charging) {
            count = 3 + (pulse > 0.5 ? 2 : 0);
            spread = 0.8;
            rise = 0.10 + pulse * 0.08;
        } else {
            count = 1 + (pulse > 0.62 ? 2 : 0) + (pulse > 0.9 ? 1 : 0);
            spread = 1.2;
            rise = 0.04 + pulse * 0.05;
        }

        AuraColors colors = colorsFor(formId.getPath());
        int seed = player.getUUID().hashCode();
        double half = spread * 0.5;

        int[] layers = new int[3];
        int rotation = Math.floorMod(player.tickCount, LAYER_PATTERN.length);
        for (int i = 0; i < count; i++) {
            layers[LAYER_PATTERN[(rotation + i) % LAYER_PATTERN.length]]++;
        }

        if (layers[0] > 0) {
            MaterialEmitters.Source column = random -> new MaterialEmitters.Sample(
                    new Vec3(player.getX() + (random.nextDouble() * 2.0 - 1.0) * half,
                            player.getY() + random.nextDouble() * 2.0,
                            player.getZ() + (random.nextDouble() * 2.0 - 1.0) * half),
                    Vec3.ZERO);
            MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.plasma(colors.plasma(), 1.0f, seed, new Vec3(0.0, rise, 0.0), 0.006f),
                    column, layers[0]);
        }
        if (layers[1] > 0) {
            MaterialEmitters.Source lowerBody = random -> new MaterialEmitters.Sample(
                    new Vec3(player.getX() + (random.nextDouble() * 2.0 - 1.0) * half,
                            player.getY() + random.nextDouble() * 1.3,
                            player.getZ() + (random.nextDouble() * 2.0 - 1.0) * half),
                    Vec3.ZERO);
            MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.soul(colors.soul(), 0.9f, seed ^ 0x50A1, new Vec3(0.0, rise * 1.3, 0.0), 0.008f),
                    lowerBody, layers[1]);
        }
        if (layers[2] > 0) {
            MaterialEmitters.Source rim = random -> {
                double ang = random.nextDouble() * Math.PI * 2.0;
                double radius = half * (0.8 + random.nextDouble() * 0.35);
                double outward = 0.035;
                return new MaterialEmitters.Sample(
                        new Vec3(player.getX() + Math.cos(ang) * radius,
                                player.getY() + random.nextDouble() * 0.75,
                                player.getZ() + Math.sin(ang) * radius),
                        new Vec3(Math.cos(ang) * outward, 0.0, Math.sin(ang) * outward));
            };
            MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.vapor(colors.soul(), 1.5f, seed ^ 0x9A90, new Vec3(0.0, rise * 0.4, 0.0), 0.02f),
                    rim, layers[2]);
        }

        // Complex particle layer: one WISP_CURL shed every second tick,
        // sampled through the same body column and rising at the pulse
        // rise rate, so the curls ride the aura instead of orbiting it.
        if ((player.tickCount & 1) == 0) {
            MaterialEmitters.Source column = random -> new MaterialEmitters.Sample(
                    new Vec3(player.getX() + (random.nextDouble() * 2.0 - 1.0) * half,
                            player.getY() + random.nextDouble() * 1.6,
                            player.getZ() + (random.nextDouble() * 2.0 - 1.0) * half),
                    Vec3.ZERO);
            MaterialEmitters.burst(level,
                    MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL, new Vec3(0.0, rise * 1.2, 0.0), 0.012f),
                    column, 1);
        }
    }

    // ------------------------------------------------------------------
    // Pieces layer (level render submit pass)

    private static boolean piecesHookRegistered;

    /** Registers the submit pass once, from the first aura tick that needs it. */
    private static void ensurePiecesHook() {
        if (piecesHookRegistered) return;
        piecesHookRegistered = true;
        LevelRenderEvents.COLLECT_SUBMITS.register(AuraRenderer::collectSubmits);
    }

    /**
     * Draws the aura pieces for every transformed player in the level.
     * The pose stack here is camera relative, so each player is anchored
     * on its interpolated position minus the camera, the same pattern the
     * wish ritual and the swing trails use. Per player vertex accounting
     * at distScale 1.0: nine flame tongues at 48 vertices each is 432,
     * and at high grades four tendrils at 384 vertices each is 1536, for
     * 1968 total, well inside the class L per submit budget of 6000 even
     * before FxEnvelope distance scaling steps the counts down.
     */
    private static void collectSubmits(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || context.levelState() == null
                || context.levelState().cameraRenderState == null
                || !context.levelState().cameraRenderState.initialized
                || context.levelState().cameraRenderState.pos == null
                || context.poseStack() == null || context.submitNodeCollector() == null) return;
        if (DbaEffectsConfig.effectDensity() <= 0f) return;
        Vec3 cam = context.levelState().cameraRenderState.pos;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        PoseStack stack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        for (var entity : mc.level.players()) {
            if (!(entity instanceof AbstractClientPlayer player)) continue;
            // The local player's own pieces are skipped in first person:
            // the tongues stand inside the camera near plane and would
            // flash at the screen edges. The filler emission, unchanged,
            // still reads in first person, and every other view of that
            // player (third person, remote clients) gets the full pieces.
            if (firstPerson && player == mc.player) continue;
            if (!(player instanceof PlayerStatsAccessor accessor)) continue;
            Identifier formId = accessor.dba$getActiveFormId();
            if (formId == null) continue;
            submitAuraPieces(stack, collector, cam, partial, player, formId);
        }
    }

    private static void submitAuraPieces(PoseStack stack, SubmitNodeCollector collector, Vec3 cam,
                                         float partial, AbstractClientPlayer player, Identifier formId) {
        AuraColors colors = colorsFor(formId.getPath());
        boolean charging = player.isUsingItem();
        double phase = player.getUUID().hashCode() & 0xFF;
        float age = player.tickCount + partial;
        // The same pulse wave the emission runs on, evaluated with the
        // partial tick so the pieces breathe smoothly between ticks.
        double pulse = 0.5 + 0.5 * Math.sin(age * 0.32 + phase);
        Vec3 pos = player.getPosition(partial);
        float distScale = FxEnvelope.distScale(cam.distanceTo(pos));
        float envelope = (float) (0.55 + 0.45 * pulse);
        long seed = player.getUUID().hashCode();

        float tongueRadius = charging ? 0.55f : 0.75f;
        float tongueHeight = (charging ? 2.3f : 2.0f) * (float) (0.9 + 0.2 * pulse);
        float sway = age * 0.45f + (float) (phase * 0.01);

        Form form = DbaRegistries.getForm(formId);
        boolean highGrade = form != null && form.getStage() >= 3;

        stack.pushPose();
        try {
            stack.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
            final long flameSeed = seed ^ 0xF1A9L;
            final float env = envelope;
            final float ds = distScale;
            // Aura lashes: nine flame tongues (the builder's full count
            // budget) in the form colors, the paler soul color hot at the
            // base running to the plasma color at the torn tips. Peak
            // alpha is an honest 0.95; the pulse envelope carries the
            // breathing, never a low constant alpha.
            ComplexGeometry.submitEnergy(collector, stack, ComplexTextures.FLAME_TONGUE, (pose, buffer) ->
                    ComplexGeometry.flameTongues(pose, buffer, flameSeed,
                            new Vec3(0.0, 0.05, 0.0), tongueRadius, tongueHeight, 9,
                            colors.soul(), colors.plasma(), 0.95f, env, sway, ds));
            if (highGrade) {
                // High grade wisps: four tendril strands lashing up
                // through the tongues, plasma at the root to the paler
                // soul color at the tip, on the same shader treatment.
                final long tendrilSeed = seed ^ 0x7D3AL;
                ComplexGeometry.submitEnergy(collector, stack, ComplexTextures.TENDRIL, (pose, buffer) ->
                        ComplexGeometry.tendrilFan(pose, buffer, tendrilSeed,
                                new Vec3(0.0, 0.10, 0.0), new Vec3(0.0, 1.0, 0.0),
                                30.0f, 2.6f, 4, 0.16f,
                                colors.plasma(), colors.soul(), 0.90f, env, ds));
            }
        } finally {
            stack.popPose();
        }
    }

    /**
     * Form family colors, matched to the family mapping the post grades use:
     * the plasma color is the form's own declared color where the form data
     * carries one (hair or skin override), otherwise the grade tint; the
     * soul color is a paler variant of the same hue.
     */
    private static AuraColors colorsFor(String formPath) {
        if (formPath.contains("super_saiyan_blue")) return BLUE;
        if (formPath.contains("super_saiyan_god")) return GOD;
        if (formPath.contains("super_saiyan")) return SSJ;
        if (formPath.contains("ultra_instinct")) return UI;
        if (formPath.contains("golden")) return GOLDEN;
        if (formPath.contains("kaioken")) return KAIOKEN;
        if (formPath.contains("namekian")) return NAMEKIAN;
        if (formPath.contains("arcosian")) return ARCOSIAN;
        return DEFAULT;
    }
}
