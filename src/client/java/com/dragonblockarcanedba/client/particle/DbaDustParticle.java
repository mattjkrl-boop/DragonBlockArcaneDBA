package com.dragonblockarcanedba.client.particle;

import com.dragonblockarcanedba.particle.DbaDustOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;

/** Theme dust particle: a soft additive mote tinted per particle by DbaDustOptions. */
public class DbaDustParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    public DbaDustParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                           SpriteSet sprites, DbaDustOptions options) {
        super(level, x, y, z, dx, dy, dz, sprites.first());
        this.sprites = sprites;
        this.xd = dx; this.yd = dy; this.zd = dz;
        this.lifetime = 14 + this.random.nextInt(11);
        this.gravity = 0.0f;
        this.friction = 0.94f;
        this.hasPhysics = false;
        this.baseSize = 0.13f * Math.max(0.2f, options.scale()) * (0.85f + this.random.nextFloat() * 0.3f);
        this.quadSize = baseSize;
        this.roll = this.oRoll = this.random.nextFloat() * (float) Math.PI * 2f;
        setColor(options.red(), options.green(), options.blue());
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) return;
        float t = this.age / (float) this.lifetime;
        this.quadSize = Math.max(0.01f, baseSize * (1f + 0.35f * t));
        setAlpha(1f - t * t);
        this.oRoll = this.roll;
        this.roll += 0.03f;
        setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() {
        return DbaGlowParticle.ADDITIVE;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return 0xF000F0;
    }
}
