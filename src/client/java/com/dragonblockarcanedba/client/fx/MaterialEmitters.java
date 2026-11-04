package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.particle.DbaMaterialOptions;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.util.FxEnvelope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shared client emission toolkit for the no-shapes realism pass.
 *
 * Emitters are data only. A source supplies points along a path, inside a
 * volume, or across a ground area; nothing here draws geometry. Continuous
 * emitters own a time accumulator, so calling tick once per rendered frame
 * produces the same rate at 30, 60, or 144 frames per second. Rates are
 * multiplied by the effects density config and can be distance-scaled by
 * the caller with FxEnvelope, matching the existing effect patterns.
 */
public final class MaterialEmitters {
    private MaterialEmitters() {}

    /** A material look: particle options plus a base velocity and spread. */
    public static final class Preset {
        private final int material;
        private final int color;
        private final float scale;
        private final int seedBase;
        private final ParticleOptions fixedOptions;
        private final Vec3 velocity;
        private final float spread;

        private Preset(int material, int color, float scale, int seedBase,
                       ParticleOptions fixedOptions, Vec3 velocity, float spread) {
            this.material = material;
            this.color = color;
            this.scale = scale;
            this.seedBase = seedBase;
            this.fixedOptions = fixedOptions;
            this.velocity = velocity;
            this.spread = Math.max(0f, spread);
        }

        public static Preset material(int material, int argb, float scale, int seed, Vec3 velocity, float spread) {
            return new Preset(material, argb & 0xFFFFFF, scale, seed, null, velocity, spread);
        }

        public static Preset fixed(ParticleOptions options, Vec3 velocity, float spread) {
            return new Preset(-1, 0, 1f, 0, options, velocity, spread);
        }

        public static Preset plasma(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.PLASMA, argb, scale, seed, velocity, spread);
        }

        public static Preset smoke(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.SMOKE, argb, scale, seed, velocity, spread);
        }

        public static Preset vapor(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.VAPOR, argb, scale, seed, velocity, spread);
        }

        public static Preset dust(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.DUST, argb, scale, seed, velocity, spread);
        }

        public static Preset mist(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.MIST, argb, scale, seed, velocity, spread);
        }

        public static Preset soul(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.SOUL, argb, scale, seed, velocity, spread);
        }

        public static Preset darkness(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.DARKNESS, argb, scale, seed, velocity, spread);
        }

        public static Preset glassDust(int argb, float scale, int seed, Vec3 velocity, float spread) {
            return material(DbaMaterialOptions.GLASS_DUST, argb, scale, seed, velocity, spread);
        }

        public static Preset flame(Vec3 velocity, float spread) {
            return fixed(DbaParticles.FLAME, velocity, spread);
        }

        public static Preset ember(Vec3 velocity, float spread) {
            return fixed(DbaParticles.EMBER, velocity, spread);
        }

        public static Preset ash(Vec3 velocity, float spread) {
            return fixed(DbaParticles.ASH, velocity, spread);
        }

        private ParticleOptions optionsFor(int cursor) {
            if (material < 0) return fixedOptions;
            return DbaMaterialOptions.of(material, color, scale, seedBase + cursor * 1013);
        }
    }

    /** One sampled emission point and the velocity the source itself imparts. */
    public record Sample(Vec3 pos, Vec3 velocity) {}

    /** A source of emission points. Data only; it draws nothing. */
    public interface Source {
        Sample sample(RandomSource random);
    }

    public static Source point(Vec3 pos) {
        return random -> new Sample(pos, Vec3.ZERO);
    }

    /** Points along a straight path; the sample velocity follows the path. */
    public static Source path(Vec3 from, Vec3 to, double pathSpeed) {
        Vec3 tangent = to.subtract(from);
        double len = tangent.length();
        Vec3 dir = len < 1e-6 ? Vec3.ZERO : tangent.scale(1.0 / len);
        return random -> {
            double t = random.nextDouble();
            return new Sample(from.add(tangent.scale(t)), dir.scale(pathSpeed));
        };
    }

    /** Points along a polyline path, segment by segment. */
    public static Source path(List<Vec3> points, double pathSpeed) {
        if (points.isEmpty()) return point(Vec3.ZERO);
        if (points.size() == 1) return point(points.get(0));
        List<Vec3> copy = List.copyOf(points);
        return random -> {
            int i = random.nextInt(copy.size() - 1);
            Vec3 a = copy.get(i);
            Vec3 b = copy.get(i + 1);
            Vec3 tangent = b.subtract(a);
            double len = tangent.length();
            Vec3 dir = len < 1e-6 ? Vec3.ZERO : tangent.scale(1.0 / len);
            return new Sample(a.add(tangent.scale(random.nextDouble())), dir.scale(pathSpeed));
        };
    }

    /** Points inside an axis-aligned volume around a center. */
    public static Source volume(Vec3 center, Vec3 halfExtents) {
        return random -> new Sample(new Vec3(
                center.x + (random.nextDouble() * 2.0 - 1.0) * halfExtents.x,
                center.y + (random.nextDouble() * 2.0 - 1.0) * halfExtents.y,
                center.z + (random.nextDouble() * 2.0 - 1.0) * halfExtents.z), Vec3.ZERO);
    }

    /** Points inside a spherical volume, uniform by radius. */
    public static Source sphereVolume(Vec3 center, double radius) {
        return random -> {
            double u = random.nextDouble() * 2.0 - 1.0;
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.cbrt(random.nextDouble());
            double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
            return new Sample(new Vec3(center.x + Math.cos(ang) * flat,
                    center.y + u * rr, center.z + Math.sin(ang) * flat), Vec3.ZERO);
        };
    }

    /** Points across a ground area at a fixed height. */
    public static Source groundArea(Vec3 center, double radius, double y) {
        return random -> {
            double ang = random.nextDouble() * Math.PI * 2.0;
            double rr = radius * Math.sqrt(random.nextDouble());
            return new Sample(new Vec3(center.x + Math.cos(ang) * rr, y,
                    center.z + Math.sin(ang) * rr), Vec3.ZERO);
        };
    }

    /** Distance scale for a world position, from the local player camera. */
    public static float distanceScale(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return 1f;
        return FxEnvelope.distScale(mc.player.position().distanceTo(pos));
    }

    private static int spawnOne(ClientLevel level, Preset preset, Source source, RandomSource random, int cursor) {
        Sample sample = source.sample(random);
        double spread = preset.spread;
        Vec3 vel = sample.velocity().add(preset.velocity).add(new Vec3(
                (random.nextDouble() * 2.0 - 1.0) * spread,
                (random.nextDouble() * 2.0 - 1.0) * spread,
                (random.nextDouble() * 2.0 - 1.0) * spread));
        level.addParticle(preset.optionsFor(cursor), sample.pos().x, sample.pos().y, sample.pos().z,
                vel.x, vel.y, vel.z);
        return 1;
    }

    /** One-shot burst at an impact, cast, or landing moment. */
    public static int burst(ClientLevel level, Preset preset, Source source, int count) {
        if (level == null || count <= 0) return 0;
        float density = DbaEffectsConfig.effectDensity();
        int n = Math.round(count * density);
        RandomSource random = level.getRandom();
        int spawned = 0;
        for (int i = 0; i < n; i++) spawned += spawnOne(level, preset, source, random, i);
        return spawned;
    }

    /**
     * A continuous emitter with a frame-rate independent accumulator and a
     * hard per-second budget. tick() reads the wall clock; tick(deltaSeconds,
     * envelope) is the deterministic form for client-tick callers.
     */
    public static final class Emitter {
        private final ClientLevel level;
        private final Preset preset;
        private final Source source;
        private final float ratePerSecond;
        private final int budgetPerSecond;
        private float accumulator = 0f;
        private long lastNanos = 0L;
        private long windowStartNanos = 0L;
        private int emittedInWindow = 0;
        private int seedCursor = 0;

        public Emitter(ClientLevel level, Preset preset, Source source, float ratePerSecond, int budgetPerSecond) {
            this.level = level;
            this.preset = preset;
            this.source = source;
            this.ratePerSecond = Math.max(0f, ratePerSecond);
            this.budgetPerSecond = Math.max(1, budgetPerSecond);
        }

        public int tick() {
            long now = System.nanoTime();
            if (lastNanos == 0L) {
                lastNanos = now;
                windowStartNanos = now;
                return 0;
            }
            float delta = Math.min(0.05f, (now - lastNanos) / 1_000_000_000.0f);
            lastNanos = now;
            return tick(delta, 1f);
        }

        public int tick(float deltaSeconds, float envelope) {
            if (level == null || deltaSeconds <= 0f || envelope <= 0.01f) return 0;
            long now = System.nanoTime();
            if (windowStartNanos == 0L || now - windowStartNanos >= 1_000_000_000L) {
                windowStartNanos = now;
                emittedInWindow = 0;
            }
            float density = DbaEffectsConfig.effectDensity();
            accumulator += ratePerSecond * deltaSeconds * envelope * density;
            int want = (int) Math.floor(accumulator);
            accumulator -= want;
            int room = budgetPerSecond - emittedInWindow;
            if (room <= 0) {
                accumulator = 0f;
                return 0;
            }
            int count = Math.min(want, Math.min(room, 96));
            if (count < want) accumulator = 0f; // never build emission debt
            RandomSource random = level.getRandom();
            int spawned = 0;
            for (int i = 0; i < count; i++) {
                spawned += spawnOne(level, preset, source, random, seedCursor++);
            }
            emittedInWindow += spawned;
            return spawned;
        }
    }
}
