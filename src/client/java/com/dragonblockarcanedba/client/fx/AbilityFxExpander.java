package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.particle.AbilityFxOptions;
import com.dragonblockarcanedba.util.SeedHash;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic recipe expansion (Rebuild Phase 3). A recipe plus an
 * event context plus a seed expands to an exact list of particle
 * spawns: same definition, same recipe, same context, same seed,
 * same list, on every client, every time. All randomness comes from
 * one splitmix stream seeded per expansion (SeedHash discipline);
 * the draw order per particle is fixed (shape position, shape
 * velocity, velocity jitter, lifetime, size), so the stream can
 * never drift between clients.
 *
 * This class is pure math over the definition: it reads no game
 * state, which is what lets the harness prove determinism outside
 * the game. Callers own the presentation decisions around it:
 * distance scaling folds into countScale before the call, and the
 * shared budget (FxBudget) clips the count through countLimit.
 * Budget clipping may differ under load; the expansion parameters
 * themselves never do.
 */
public final class AbilityFxExpander {
    private AbilityFxExpander() {}

    /** Where an event happened and what the expansion needs to know about it. */
    public record Ctx(Vec3 origin, Vec3 dir, float power, double radiusHint) {
        public Ctx {
            dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : dir.normalize();
        }
    }

    /** One fully resolved particle spawn: options plus position and velocity. */
    public record Spawn(AbilityFxOptions options, double x, double y, double z,
                        double vx, double vy, double vz) {}

    /** Splitmix stream: the only randomness expansion is allowed. */
    static final class FxRandom {
        private long state;

        FxRandom(long seed) {
            this.state = seed;
        }

        double next() {
            state += 0x9E3779B97F4A7C15L;
            return (SeedHash.mix(state) >>> 40) * (1.0 / (1 << 24));
        }
    }

    /**
     * Expands a burst recipe. countScale folds the caller's
     * distance, density, particleRate, and power factors into the
     * count; countLimit (the budget's answer) caps it when >= 0.
     */
    public static List<Spawn> expandBurst(AbilityDef def, AbilityDef.FxRecipe recipe, Ctx ctx,
                                          int seed, float countScale, int countLimit) {
        int scaled = Math.round(recipe.count() * Math.max(0f, countScale));
        int n = countLimit >= 0 ? Math.min(scaled, countLimit) : scaled;
        if (n <= 0) {
            return List.of();
        }
        List<Spawn> out = new ArrayList<>(n);
        FxRandom rng = new FxRandom(seed);
        for (int i = 0; i < n; i++) {
            out.add(sample(def, recipe, ctx, rng, seed + i * 1013));
        }
        return out;
    }

    /** Expands exactly one particle (the continuous-emission path, one call per spawn). */
    public static Spawn expandOne(AbilityDef def, AbilityDef.FxRecipe recipe, Ctx ctx, int seed) {
        return sample(def, recipe, ctx, new FxRandom(seed), seed);
    }

    /** Spawns one expanded particle into the level. */
    public static void spawn(ClientLevel level, Spawn s) {
        level.addParticle(s.options(), s.x(), s.y(), s.z(), s.vx(), s.vy(), s.vz());
    }

    /**
     * The caps-driven distance factor for recipe emission: full
     * detail inside the definition's fullDetailDistance, a linear
     * fall to 0.35 at cullDistance, nothing beyond. A pure function
     * of the data and the distance, so every client thins an effect
     * by the same rule (the same curve shape as FxEnvelope, driven
     * by the per-ability caps instead of global distances).
     */
    public static float detailFactor(AbilityDef def, double distance) {
        AbilityDef.Caps caps = def.caps();
        if (distance >= caps.cullDistance()) {
            return 0f;
        }
        if (distance <= caps.fullDetailDistance()) {
            return 1f;
        }
        double t = (distance - caps.fullDetailDistance())
                / (caps.cullDistance() - caps.fullDetailDistance());
        return (float) (1.0 - 0.65 * t);
    }

    /**
     * The radius a packet-driven expansion assumes when a recipe
     * leaves its radius unset: derived from the definition alone,
     * since the packet carries no carrier state. Renderer-local
     * expansion passes live values instead (the cloud's current
     * radius), which is the one place the two routes differ.
     */
    public static double defaultRadiusHint(AbilityDef def, AbilityDef.FxEvent event) {
        float scale = def.visual().scale();
        return switch (def.carrier()) {
            case WAVE -> def.wave() != null ? def.wave().radius() * scale : 1.0;
            case CLOUD -> {
                if (def.cloud() == null) {
                    yield 1.0;
                }
                yield (event == AbilityDef.FxEvent.CARRIER_END
                        ? def.cloud().radiusEnd()
                        : event == AbilityDef.FxEvent.CARRIER_HIT
                        ? def.cloud().strikeRadius()
                        : def.cloud().radiusStart()) * scale;
            }
            case PROJECTILE -> 0.0;
        };
    }

    private static double radius(AbilityDef.FxRecipe recipe, Ctx ctx) {
        double r = recipe.radius() >= 0 ? recipe.radius() : ctx.radiusHint();
        return r > 0 ? r : 1.0;
    }

    private static Spawn sample(AbilityDef def, AbilityDef.FxRecipe r, Ctx ctx,
                                FxRandom rng, int particleSeed) {
        AbilityDef.VisualSpec vis = def.visual();
        Vec3 o = ctx.origin();
        Vec3 dir = ctx.dir();
        double speed = r.speed();
        double px = o.x, py = o.y, pz = o.z;
        double vx = 0, vy = 0, vz = 0;

        switch (r.shape()) {
            case POINT -> {
                double[] u = unit(rng);
                double mag = rng.next() * 0.15;
                px += u[0] * mag;
                py += u[1] * mag;
                pz += u[2] * mag;
                double[] w = unit(rng);
                double f = speed * (0.4 + 0.6 * rng.next());
                vx = w[0] * f + dir.x * speed;
                vy = w[1] * f + dir.y * speed;
                vz = w[2] * f + dir.z * speed;
            }
            case RING -> {
                double rad = radius(r, ctx);
                double a = rng.next() * Math.PI * 2.0;
                double rr = rad * (0.94 + 0.12 * rng.next());
                double cos = Math.cos(a), sin = Math.sin(a);
                px += cos * rr;
                pz += sin * rr;
                py += (rng.next() - 0.5) * 0.12;
                vx = cos * speed;
                vy = speed * 0.2;
                vz = sin * speed;
            }
            case DISC -> {
                double rad = radius(r, ctx);
                double a = rng.next() * Math.PI * 2.0;
                double rr = rad * Math.sqrt(rng.next());
                double cos = Math.cos(a), sin = Math.sin(a);
                px += cos * rr;
                pz += sin * rr;
                py += (rng.next() - 0.5) * 0.2;
                vx = cos * speed * 0.25;
                vy = speed;
                vz = sin * speed * 0.25;
            }
            case CONE -> {
                double[] u = unit(rng);
                px += u[0] * 0.2;
                py += u[1] * 0.2;
                pz += u[2] * 0.2;
                // Basis around the cone axis.
                Vec3 w = dir;
                Vec3 helper = Math.abs(w.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 uAxis = w.cross(helper).normalize();
                Vec3 vAxis = w.cross(uAxis);
                double half = Math.toRadians(r.angle());
                double cosT = 1.0 - rng.next() * (1.0 - Math.cos(half));
                double sinT = Math.sqrt(Math.max(0.0, 1.0 - cosT * cosT));
                double around = rng.next() * Math.PI * 2.0;
                double f = speed * (0.7 + 0.3 * rng.next());
                Vec3 d = w.scale(cosT)
                        .add(uAxis.scale(Math.cos(around) * sinT))
                        .add(vAxis.scale(Math.sin(around) * sinT));
                vx = d.x * f;
                vy = d.y * f;
                vz = d.z * f;
            }
            case SPHERE_SHELL -> {
                double rad = radius(r, ctx);
                double[] u = unit(rng);
                px += u[0] * rad;
                py += u[1] * rad;
                pz += u[2] * rad;
                vx = u[0] * speed;
                vy = u[1] * speed;
                vz = u[2] * speed;
            }
            case BAND -> {
                double[] band = bandSample(def, r, ctx, rng);
                px = band[0];
                py = band[1];
                pz = band[2];
                vx = band[3];
                vy = band[4];
                vz = band[5];
            }
        }

        double spread = r.spread();
        if (spread > 0) {
            vx += (rng.next() * 2.0 - 1.0) * spread;
            vy += (rng.next() * 2.0 - 1.0) * spread;
            vz += (rng.next() * 2.0 - 1.0) * spread;
        } else {
            // Keep the draw order identical when spread is zero, so a
            // data edit to spread never reshuffles the other rolls.
            rng.next();
            rng.next();
            rng.next();
        }

        double uLife = rng.next();
        double uSize = rng.next();
        int lifetime = (int) Math.round(r.lifetimeMin() + (r.lifetimeMax() - r.lifetimeMin()) * uLife);
        float size = (float) ((r.sizeMin() + (r.sizeMax() - r.sizeMin()) * uSize) * vis.scale());
        AbilityFxOptions options = new AbilityFxOptions(r.entry(), r.resolveColor(vis),
                size, (float) (size * r.sizeEndScale()), lifetime,
                (float) r.gravity(), (float) r.drag(), particleSeed);
        return new Spawn(options, px, py, pz, vx, vy, vz);
    }

    /**
     * The along-carrier band: samples hug the carrier's silhouette,
     * which each carrier defines from the same parameters its
     * renderer draws. Wave: points along the traveling arc band.
     * Cloud: points on the domain's perimeter at low height.
     * Projectile: points along the trailing segment behind the
     * head. Draws: wave 3, cloud 2, projectile 4 (documented per
     * branch; the stream is per recipe, so branches never mix).
     */
    private static double[] bandSample(AbilityDef def, AbilityDef.FxRecipe r, Ctx ctx, FxRandom rng) {
        Vec3 o = ctx.origin();
        Vec3 dir = ctx.dir();
        double speed = r.speed();
        float scale = def.visual().scale();
        switch (def.carrier()) {
            case WAVE -> {
                if (def.wave() != null) {
                    AbilityDef.WaveParams wave = def.wave();
                    double arc = Math.toRadians(wave.arcDegrees());
                    double a = (rng.next() - 0.5) * arc;
                    double cos = Math.cos(a), sin = Math.sin(a);
                    double rx = dir.x * cos - dir.z * sin;
                    double rz = dir.z * cos + dir.x * sin;
                    double rad = wave.radius() * scale * (0.92 + 0.16 * rng.next());
                    double y = rng.next() * wave.height() * scale;
                    return new double[]{o.x + rx * rad, o.y + y, o.z + rz * rad,
                            dir.x * speed, speed, dir.z * speed};
                }
            }
            case CLOUD -> {
                double rad = radius(r, ctx);
                double a = rng.next() * Math.PI * 2.0;
                double yFrac = rng.next();
                double height = def.cloud() != null ? def.cloud().height() * scale * 0.35 : 1.0;
                double cos = Math.cos(a), sin = Math.sin(a);
                return new double[]{o.x + cos * rad, o.y + yFrac * height, o.z + sin * rad,
                        cos * speed * 0.3, speed * 0.5, sin * speed * 0.3};
            }
            case PROJECTILE -> {
                double t = rng.next();
                double len = def.projectile() != null ? 1.6 * def.projectile().lengthScale() : 1.6;
                double back = 0.2 + t * len;
                double range = 0.12 * (1.0 + t);
                double jx = (rng.next() * 2.0 - 1.0) * range;
                double jy = (rng.next() * 2.0 - 1.0) * range;
                double jz = (rng.next() * 2.0 - 1.0) * range;
                return new double[]{o.x - dir.x * back + jx, o.y - dir.y * back + jy,
                        o.z - dir.z * back + jz,
                        -dir.x * speed, -dir.y * speed, -dir.z * speed};
            }
        }
        // A band recipe on a carrier without band parameters reads as a ring.
        double rad = radius(r, ctx);
        double a = rng.next() * Math.PI * 2.0;
        rng.next();
        return new double[]{o.x + Math.cos(a) * rad, o.y, o.z + Math.sin(a) * rad,
                Math.cos(a) * speed, speed * 0.2, Math.sin(a) * speed};
    }

    /** A unit vector from three draws; degenerate draws fall back to up. */
    private static double[] unit(FxRandom rng) {
        double x = rng.next() * 2.0 - 1.0;
        double y = rng.next() * 2.0 - 1.0;
        double z = rng.next() * 2.0 - 1.0;
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len < 1e-6) {
            return new double[]{0, 1, 0};
        }
        return new double[]{x / len, y / len, z / len};
    }
}
