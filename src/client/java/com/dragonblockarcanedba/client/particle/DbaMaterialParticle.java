package com.dragonblockarcanedba.client.particle;

import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

/**
 * Particle for the no-shapes material family. The sprite flipbook carries
 * the material detail; this class carries the behavior: seeded turbulence,
 * drag, buoyancy or gravity from the profile, spin, velocity-responsive
 * scale, density fade in and out, and a ground response for the materials
 * that should settle, creep, or bounce instead of falling through terrain.
 */
public class DbaMaterialParticle extends SingleQuadParticle {
    public enum GroundMode { NONE, SETTLE, BOUNCE, HUG }

    public record Profile(int minLife, int maxLife, float size, float growth, float gravity,
                          float drag, float turbulence, float turbulenceRate, float spin,
                          float alphaMax, float fadeIn, float fadePower, boolean additive,
                          boolean emissive, GroundMode groundMode, float speedScale) {}

    private static final Profile PLASMA = new Profile(10, 22, 0.17f, 0.10f, -0.004f, 0.90f,
            0.0060f, 0.55f, 0.08f, 1.00f, 0.12f, 1.15f, true, true, GroundMode.NONE, 1.40f);
    private static final Profile SMOKE = new Profile(34, 72, 0.34f, 1.90f, -0.012f, 0.94f,
            0.0035f, 0.22f, 0.025f, 0.62f, 0.18f, 1.35f, false, false, GroundMode.SETTLE, 0.05f);
    private static final Profile VAPOR = new Profile(24, 54, 0.30f, 1.25f, -0.008f, 0.95f,
            0.0040f, 0.28f, 0.030f, 0.50f, 0.16f, 1.25f, false, false, GroundMode.HUG, 0.08f);
    private static final Profile DUST = new Profile(20, 44, 0.26f, 1.10f, 0.018f, 0.93f,
            0.0030f, 0.30f, 0.040f, 0.78f, 0.10f, 1.20f, false, false, GroundMode.SETTLE, 0.10f);
    private static final Profile MIST = new Profile(55, 105, 1.05f, 0.55f, -0.001f, 0.985f,
            0.0012f, 0.12f, 0.008f, 0.34f, 0.22f, 1.10f, false, false, GroundMode.HUG, 0.03f);
    private static final Profile SOUL = new Profile(34, 70, 0.20f, 0.35f, -0.022f, 0.965f,
            0.0050f, 0.32f, 0.050f, 0.92f, 0.15f, 1.20f, true, true, GroundMode.NONE, 0.20f);
    private static final Profile DARKNESS = new Profile(30, 62, 0.36f, 1.55f, -0.004f, 0.94f,
            0.0040f, 0.24f, 0.030f, 0.86f, 0.18f, 1.30f, false, false, GroundMode.SETTLE, 0.05f);
    private static final Profile GLASS_DUST = new Profile(18, 36, 0.11f, -0.15f, 0.160f, 0.97f,
            0.0015f, 0.50f, 0.220f, 0.95f, 0.05f, 1.05f, true, true, GroundMode.BOUNCE, 0.35f);

    public static Profile profileFor(int material) {
        return switch (material) {
            case DbaMaterialOptions.PLASMA -> PLASMA;
            case DbaMaterialOptions.SMOKE -> SMOKE;
            case DbaMaterialOptions.VAPOR -> VAPOR;
            case DbaMaterialOptions.DUST -> DUST;
            case DbaMaterialOptions.MIST -> MIST;
            case DbaMaterialOptions.SOUL -> SOUL;
            case DbaMaterialOptions.DARKNESS -> DARKNESS;
            case DbaMaterialOptions.GLASS_DUST -> GLASS_DUST;
            default -> PLASMA;
        };
    }

    private final SpriteSet sprites;
    private final Profile profile;
    private final float baseSize;
    private final float phase;
    private final float spinDirection;

    public DbaMaterialParticle(ClientLevel level, double x, double y, double z,
                               double dx, double dy, double dz, SpriteSet sprites,
                               DbaMaterialOptions options, Profile profile) {
        super(level, x, y, z, dx, dy, dz, sprites.first());
        this.sprites = sprites;
        this.profile = profile;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        int span = Math.max(1, profile.maxLife() - profile.minLife() + 1);
        this.lifetime = profile.minLife() + this.random.nextInt(span);
        this.gravity = profile.gravity();
        this.friction = profile.drag();
        this.hasPhysics = profile.groundMode() != GroundMode.NONE;
        this.baseSize = profile.size() * options.scale() * (0.82f + this.random.nextFloat() * 0.36f);
        this.quadSize = baseSize;
        this.phase = ((options.seed() & 0x3FF) / 1023.0f) * (float) (Math.PI * 2.0)
                + this.random.nextFloat() * (float) Math.PI;
        this.spinDirection = (options.seed() & 1) == 0 ? 1f : -1f;
        this.roll = this.oRoll = this.random.nextFloat() * (float) Math.PI * 2f;
        setColor(options.red(), options.green(), options.blue());
        setAlpha(profile.alphaMax() * 0.25f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        double prevXd = this.xd;
        double prevYd = this.yd;
        double prevZd = this.zd;
        super.tick();
        if (!this.isAlive()) return;

        float t = this.age / (float) this.lifetime;
        if (this.onGround && profile.groundMode() != GroundMode.NONE) {
            applyGroundResponse(prevXd, prevYd, prevZd);
        } else if (profile.turbulence() > 0f) {
            double wave = this.age * profile.turbulenceRate() + this.phase;
            this.xd += Math.sin(wave) * profile.turbulence();
            this.zd += Math.cos(wave * 0.83 + this.phase * 1.7) * profile.turbulence();
            this.yd += Math.sin(wave * 0.61 + this.phase * 2.3) * profile.turbulence() * 0.45;
        }

        double speed = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        float stretch = 1f + (float) Math.min(1.2, speed * profile.speedScale());
        this.quadSize = Math.max(0.01f, baseSize * (1f + profile.growth() * t) * stretch);

        float fadeIn = profile.fadeIn() <= 0f ? 1f : smoothstep(t / profile.fadeIn());
        float fadeOut = (float) Math.pow(Math.max(0f, 1f - t), profile.fadePower());
        setAlpha(profile.alphaMax() * Math.min(fadeIn, fadeOut));

        this.oRoll = this.roll;
        this.roll += profile.spin() * spinDirection;
        setSpriteFromAge(sprites);
    }

    private void applyGroundResponse(double prevXd, double prevYd, double prevZd) {
        switch (profile.groundMode()) {
            case SETTLE -> {
                this.yd = 0.0;
                this.xd *= 0.80;
                this.zd *= 0.80;
            }
            case HUG -> {
                this.yd = 0.0;
                this.xd *= 0.94;
                this.zd *= 0.94;
            }
            case BOUNCE -> {
                if (prevYd < -0.05) {
                    this.yd = -prevYd * 0.34;
                    this.xd = prevXd * 0.78;
                    this.zd = prevZd * 0.78;
                } else {
                    this.yd = 0.0;
                }
            }
            case NONE -> {
            }
        }
    }

    private static float smoothstep(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return c * c * (3f - 2f * c);
    }

    @Override
    protected Layer getLayer() {
        return profile.additive() ? DbaGlowParticle.ADDITIVE : Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return profile.emissive() ? 0xF000F0 : super.getLightCoords(partialTick);
    }
}
