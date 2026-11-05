package com.dragonblockarcanedba.client.particle;

import com.dragonblockarcanedba.particle.AbilityFxEntries;
import com.dragonblockarcanedba.particle.AbilityFxOptions;
import com.dragonblockarcanedba.util.SeedHash;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.List;

/**
 * The universal ability particle (Rebuild Phase 3). One class serves
 * every ability effect: the options record carries the texture entry,
 * tint, size curve, lifetime, gravity, and drag, all derived from the
 * effect seed by the recipe expander before spawning, so this class
 * rolls nothing behavior-relevant itself. The only local variation is
 * cosmetic: roll direction and spin come from the per-instance seed
 * through SeedHash, identically on every client.
 *
 * Rendering follows the entry table: lit entries (physical matter)
 * shade with the world's light and collide with the ground on the
 * translucent layer; additive entries (energy) are full bright on
 * the shared additive layer, which never writes depth.
 */
public class AbilityFxParticle extends SingleQuadParticle {
    private final List<TextureAtlasSprite> sprites;
    private final AbilityFxOptions options;
    private final boolean lit;
    private final boolean additive;
    private final float spin;

    public AbilityFxParticle(ClientLevel level, double x, double y, double z,
                             double dx, double dy, double dz, SpriteSet sprites,
                             AbilityFxOptions options) {
        super(level, x, y, z, dx, dy, dz, firstSprite(sprites, options.entry()));
        this.sprites = spriteList(sprites);
        this.options = options;
        this.lit = AbilityFxEntries.isLit(options.entry());
        this.additive = AbilityFxEntries.isAdditive(options.entry());
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = options.lifetime();
        this.gravity = options.gravity();
        this.friction = options.drag();
        this.hasPhysics = this.lit;
        this.quadSize = options.sizeFrom();
        float phase = SeedHash.hash1(options.seed(), 77);
        this.spin = (phase - 0.5f) * 0.35f;
        this.roll = this.oRoll = phase * (float) Math.PI * 2f;
        setColor(options.red(), options.green(), options.blue());
        setAlpha(options.alpha() * 0.3f);
        pickSprite(0f);
    }

    private static List<TextureAtlasSprite> spriteList(SpriteSet sprites) {
        if (sprites instanceof FabricSpriteSet fabric) {
            return fabric.getSprites();
        }
        return List.of(sprites.first());
    }

    private static TextureAtlasSprite firstSprite(SpriteSet sprites, int entry) {
        List<TextureAtlasSprite> list = spriteList(sprites);
        int idx = Math.min(AbilityFxEntries.frameBase(entry), list.size() - 1);
        return list.get(Math.max(0, idx));
    }

    private void pickSprite(float t) {
        if (sprites.isEmpty()) {
            return;
        }
        int frame = Math.min(AbilityFxEntries.FRAMES - 1,
                (int) Math.floor(t * AbilityFxEntries.FRAMES));
        int idx = AbilityFxEntries.frameBase(options.entry()) + frame;
        if (idx < sprites.size()) {
            setSprite(sprites.get(idx));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isAlive()) {
            return;
        }
        float t = this.age / (float) this.lifetime;
        this.quadSize = Math.max(0.01f, options.sizeFrom()
                + (options.sizeTo() - options.sizeFrom()) * t);
        float fadeIn = t >= 0.12f ? 1f : smoothstep(t / 0.12f);
        float fadeOut = (float) Math.pow(Math.max(0f, 1f - t), 1.2);
        setAlpha(options.alpha() * Math.min(fadeIn, fadeOut));
        this.oRoll = this.roll;
        this.roll += spin;
        pickSprite(t);
    }

    private static float smoothstep(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return c * c * (3f - 2f * c);
    }

    @Override
    protected Layer getLayer() {
        return additive ? DbaGlowParticle.ADDITIVE : Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return lit ? super.getLightCoords(partialTick) : 0xF000F0;
    }
}
