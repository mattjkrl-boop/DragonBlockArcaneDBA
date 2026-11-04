package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
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
 * material profiles carry the turbulence, buoyancy, and density fade, so
 * nothing here draws or implies a shell. Density scaling is applied once by
 * MaterialEmitters.burst, the shared rule for the material system.
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
