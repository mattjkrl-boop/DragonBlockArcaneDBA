package com.dragonblockarcanedba.client.particle;

import com.dragonblockarcanedba.client.compat.DbaPipelineCompat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

/**
 * Full bright particle with optional additive blending. Behaviour (gravity, drag, growth, fade, color)
 * comes from a {@link Style} so one class covers sparks, embers, void smoke and wind.
 */
public class DbaGlowParticle extends SingleQuadParticle {
    // Built through DbaPipelineCompat: the pipeline classes live in different packages on 26.2 and 26.3.
    public static final Layer ADDITIVE = DbaPipelineCompat.buildAdditiveLayer();

    public record Style(float r, float g, float b, float r2, float g2, float b2, int minLife, int maxLife,
                       float size, float growth, float gravity, float drag, boolean additive) {}

    public static final Style SPARK = new Style(1f, 0.95f, 0.7f, 1f, 0.45f, 0.1f, 6, 12, 0.08f, -0.5f, 0.6f, 0.86f, true);
    public static final Style EMBER = new Style(1f, 0.6f, 0.2f, 0.6f, 0.08f, 0.02f, 20, 40, 0.1f, -0.3f, -0.02f, 0.94f, true);
    public static final Style VOID_SMOKE = new Style(0.35f, 0.1f, 0.5f, 0.05f, 0.0f, 0.08f, 20, 36, 0.35f, 1.2f, -0.01f, 0.9f, false);
    public static final Style KI_MOTE = new Style(1f, 0.93f, 0.72f, 1f, 0.72f, 0.35f, 16, 30, 0.11f, 0.25f, -0.015f, 0.96f, true);
    public static final Style WIND = new Style(0.9f, 0.97f, 1f, 0.7f, 0.85f, 1f, 8, 14, 0.25f, 0.8f, 0f, 0.92f, true);

    private final SpriteSet sprites;
    private final Style style;
    private final float baseSize;

    protected DbaGlowParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                              SpriteSet sprites, Style style) {
        super(level, x, y, z, dx, dy, dz, sprites.first());
        this.sprites = sprites;
        this.style = style;
        this.xd = dx; this.yd = dy; this.zd = dz;
        this.lifetime = style.minLife() + this.random.nextInt(Math.max(1, style.maxLife() - style.minLife() + 1));
        this.gravity = style.gravity();
        this.friction = style.drag();
        this.hasPhysics = false;
        this.baseSize = style.size() * (0.8f + this.random.nextFloat() * 0.4f);
        this.quadSize = baseSize;
        this.roll = this.oRoll = this.random.nextFloat() * (float) Math.PI * 2f;
        setColor(style.r(), style.g(), style.b());
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) return;
        float t = this.age / (float) this.lifetime;
        setColor(lerp(style.r(), style.r2(), t), lerp(style.g(), style.g2(), t), lerp(style.b(), style.b2(), t));
        this.quadSize = Math.max(0.01f, baseSize * (1f + style.growth() * t));
        setAlpha(1f - t * t);
        this.oRoll = this.roll;
        this.roll += 0.05f;
        setSpriteFromAge(sprites);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    protected Layer getLayer() {
        return style.additive() ? ADDITIVE : Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return 0xF000F0;
    }
}
