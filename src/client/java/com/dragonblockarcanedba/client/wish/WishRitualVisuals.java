package com.dragonblockarcanedba.client.wish;

import com.dragonblockarcanedba.client.fx.MaterialEmitters;
import com.dragonblockarcanedba.network.WishRitualStartPayload;
import com.dragonblockarcanedba.wish.DragonWishRitual;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Balls travel onto a floating heptagon, then a rising material column and
 * circular emission fronts gather until the dragon appears. The presentation
 * is material emission only: plasma and soul clusters mark the ball paths,
 * plasma, soul, and vapor rise through the column, and mist and plasma fronts
 * replace the old ring and burst geometry. Ritual timing and mechanics are
 * unchanged.
 */
public final class WishRitualVisuals {
    private static final float MAX_AGE = DragonWishRitual.SCREEN_AT + 40.0f;
    private static final List<Ritual> ACTIVE = new ArrayList<>();

    private WishRitualVisuals() {}

    public static void begin(WishRitualStartPayload payload) {
        BlockPos[] balls = payload.balls();
        if (balls == null || balls.length != DragonWishRitual.BALLS) {
            return;
        }
        Vec3 center = DragonWishRitual.centerOf(balls);
        int[] slots = DragonWishRitual.assignSlots(balls, center);
        Vec3[] from = new Vec3[DragonWishRitual.BALLS];
        for (int i = 0; i < balls.length; i++) {
            BlockPos ball = balls[i];
            from[i] = new Vec3(ball.getX() + 0.5, ball.getY() + 0.5, ball.getZ() + 0.5);
        }
        ACTIVE.add(new Ritual(payload.namek(), payload.gameTime(), center, from, slots,
                seedOf(payload.namek(), payload.gameTime(), center)));
    }

    /**
     * Frame-stable emission seed derived only from synced payload state: the
     * ritual start time, the Namek flag, and the center computed from the
     * synced ball positions. Every client that received the payload mixes
     * the same values, so every client seeds the same material variation.
     */
    private static long seedOf(boolean namek, long gameTime, Vec3 center) {
        long seed = gameTime * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        seed ^= (long) Math.floor(center.x) * 0xBF58476D1CE4E5B9L;
        seed ^= (long) Math.floor(center.y) * 0x2545F4914F6CDD1DL;
        seed ^= (long) Math.floor(center.z) * 0x94D049BB133111EBL;
        if (namek) {
            seed ^= 0x6A09E667F3BCC909L;
        }
        return seed;
    }

    public static void render(LevelRenderContext context) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || context.levelState() == null) {
            return;
        }
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long gameTime = context.levelState().gameTime;
        ACTIVE.removeIf(ritual -> (gameTime - ritual.startGameTime) + partial > MAX_AGE);
        for (int i = 0; i < ACTIVE.size(); i++) {
            Ritual ritual = ACTIVE.get(i);
            float age = (gameTime - ritual.startGameTime) + partial;
            if (age < 0.0f) {
                continue;
            }
            if (minecraft.player != null
                    && minecraft.player.position().distanceToSqr(ritual.center) > 192.0 * 192.0) {
                ritual.lastEmitNanos = 0L;
                continue;
            }
            emit(level, ritual, age);
        }
    }

    private static void emit(ClientLevel level, Ritual ritual, float age) {
        Palette palette = ritual.namek ? Palette.NAMEK : Palette.EARTH;
        float travelT = Math.min(1.0f, age / DragonWishRitual.FORMATION_TICKS);
        float ease = smooth(travelT);
        double arc = Math.sin(travelT * Math.PI) * 1.7;
        float spin = age > DragonWishRitual.FORMATION_TICKS ? (age - DragonWishRitual.FORMATION_TICKS) * 0.012f : 0.0f;
        float ballAlpha = 1.0f;
        if (age > DragonWishRitual.DRAGON_AT) {
            ballAlpha = 1.0f - smooth((age - DragonWishRitual.DRAGON_AT) / 16.0f);
        }
        ritual.ballEnvelope = Math.max(0.0f, ballAlpha);
        if (ballAlpha > 0.02f) {
            for (int i = 0; i < DragonWishRitual.BALLS; i++) {
                Vec3 target = DragonWishRitual.ringPoint(ritual.center, ritual.slots[i]);
                if (spin != 0.0f) {
                    target = rotateY(target, ritual.center, spin);
                }
                Vec3 at = lerp(ritual.from[i], target, ease);
                ritual.currentBallPositions[i] = new Vec3(at.x, at.y + arc, at.z);
            }
        }
        ritual.formationEnvelope = ease > 0.35f && ballAlpha > 0.02f
                ? smooth((ease - 0.35f) / 0.65f) * ballAlpha * 0.85f
                : 0.0f;

        if (age < DragonWishRitual.FORMATION_TICKS) {
            ritual.columnEnvelope = 0.0f;
            ritual.gateEnvelope = 0.0f;
            ritual.burstEnvelope = 0.0f;
            tickEmitters(level, ritual, palette);
            return;
        }

        float summonT = Math.min(1.0f, (age - DragonWishRitual.FORMATION_TICKS) / DragonWishRitual.SUMMON_TICKS);
        float columnFade = age <= DragonWishRitual.DRAGON_AT
                ? 1.0f
                : 1.0f - smooth((age - DragonWishRitual.DRAGON_AT) / (DragonWishRitual.REVEAL_TICKS + 28.0f));
        float fadeIn = smooth(Math.min(1.0f, (age - DragonWishRitual.FORMATION_TICKS) / 12.0f));
        ritual.columnEnvelope = fadeIn * Math.max(0.0f, columnFade);
        ritual.columnHeight = 6.0f + 22.0f * smooth(summonT);
        ritual.columnGroundY = ritual.center.y - 0.5;
        ritual.columnPhase = age * 0.16f;
        ritual.groundRadius = 1.2f + 5.4f * smooth(summonT);
        ritual.gateEnvelope = summonT > 0.2f
                ? smooth((summonT - 0.2f) / 0.8f) * ritual.columnEnvelope
                : 0.0f;
        ritual.gateRadius = 3.4f + summonT * 1.4f;

        float burstAge = age - DragonWishRitual.DRAGON_AT;
        if (burstAge >= 0.0f && burstAge < 18.0f) {
            float u = burstAge / 18.0f;
            ritual.burstEnvelope = (1.0f - u) * 0.9f;
            ritual.burstFrontRadius = 0.8f + u * 8.0f;
            ritual.burstCoreCenter = new Vec3(ritual.center.x,
                    ritual.center.y + DragonWishRitual.RING_LIFT * 0.45, ritual.center.z);
            ritual.burstCoreRadius = 0.5f + u * 2.4f;
        } else {
            ritual.burstEnvelope = 0.0f;
        }
        tickEmitters(level, ritual, palette);
    }

    private static void tickEmitters(ClientLevel level, Ritual ritual, Palette palette) {
        ensureEmitters(ritual, level, palette);
        long now = System.nanoTime();
        float delta = ritual.lastEmitNanos == 0L
                ? 0.0f
                : Math.min(0.05f, (now - ritual.lastEmitNanos) / 1_000_000_000.0f);
        ritual.lastEmitNanos = now;
        float distScale = MaterialEmitters.distanceScale(ritual.center);
        tick(ritual.ballPlasma, delta, ritual.ballEnvelope * distScale);
        tick(ritual.ballSoul, delta, ritual.ballEnvelope * distScale);
        tick(ritual.formationMist, delta, ritual.formationEnvelope * distScale);
        tick(ritual.formationPlasma, delta, ritual.formationEnvelope * distScale);
        tick(ritual.columnPlasma, delta, ritual.columnEnvelope * distScale);
        tick(ritual.columnSoul, delta, ritual.columnEnvelope * distScale);
        tick(ritual.columnVapor, delta, ritual.columnEnvelope * distScale);
        tick(ritual.groundMist, delta, ritual.columnEnvelope * 0.75f * distScale);
        tick(ritual.groundPlasma, delta, ritual.columnEnvelope * 0.75f * distScale);
        tick(ritual.gateMist, delta, ritual.gateEnvelope * distScale);
        tick(ritual.gatePlasma, delta, ritual.gateEnvelope * distScale);
        tick(ritual.burstFrontPlasma, delta, ritual.burstEnvelope * distScale);
        tick(ritual.burstFrontMist, delta, ritual.burstEnvelope * distScale);
        tick(ritual.burstCoreSoul, delta, ritual.burstEnvelope * distScale);
    }

    private static void tick(MaterialEmitters.Emitter emitter, float delta, float envelope) {
        if (emitter != null) {
            emitter.tick(delta, envelope);
        }
    }

    private static void ensureEmitters(Ritual ritual, ClientLevel level, Palette palette) {
        if (ritual.emitterLevel == level && ritual.ballPlasma != null) {
            return;
        }
        ritual.emitterLevel = level;
        int seed = seedBase(ritual);
        ritual.ballPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(palette.bodyColor, palette.radius * 3.0f, seed + 101,
                        new Vec3(0.0, 0.035, 0.0), 0.018f),
                ballSource(ritual), 135.0f, 190);
        ritual.ballSoul = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(palette.soulColor, 1.05f, seed + 211,
                        new Vec3(0.0, 0.045, 0.0), 0.014f),
                ballSource(ritual), 72.0f, 110);
        ritual.formationMist = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.mist(palette.mistColor, 1.35f, seed + 307,
                        new Vec3(0.0, 0.012, 0.0), 0.010f),
                formationFrontSource(ritual), 58.0f, 90);
        ritual.formationPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(palette.ringColor, 1.15f, seed + 401,
                        new Vec3(0.0, 0.020, 0.0), 0.012f),
                formationFrontSource(ritual), 88.0f, 130);
        ritual.columnPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(palette.ringColor, 1.45f, seed + 503,
                        new Vec3(0.0, 0.050, 0.0), 0.016f),
                columnSource(ritual, 0), 175.0f, 250);
        ritual.columnSoul = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(palette.soulColor, 1.10f, seed + 601,
                        new Vec3(0.0, 0.035, 0.0), 0.012f),
                columnSource(ritual, 1), 112.0f, 170);
        ritual.columnVapor = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.vapor(palette.mistColor, 1.55f, seed + 701,
                        new Vec3(0.0, 0.025, 0.0), 0.014f),
                columnSource(ritual, 2), 72.0f, 110);
        ritual.groundMist = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.mist(palette.mistColor, 1.45f, seed + 809,
                        new Vec3(0.0, 0.010, 0.0), 0.010f),
                groundFrontSource(ritual), 76.0f, 115);
        ritual.groundPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(palette.ringColor, 1.10f, seed + 907,
                        new Vec3(0.0, 0.018, 0.0), 0.012f),
                groundFrontSource(ritual), 92.0f, 140);
        ritual.gateMist = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.mist(palette.mistColor, 1.30f, seed + 1009,
                        Vec3.ZERO, 0.010f),
                gateFrontSource(ritual), 66.0f, 100);
        ritual.gatePlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(palette.ringColor, 1.05f, seed + 1103,
                        Vec3.ZERO, 0.012f),
                gateFrontSource(ritual), 96.0f, 145);
        ritual.burstFrontPlasma = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.plasma(0xFFF5D1, 1.35f, seed + 1201,
                        Vec3.ZERO, 0.018f),
                burstFrontSource(ritual), 245.0f, 330);
        ritual.burstFrontMist = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.mist(palette.mistColor, 1.60f, seed + 1301,
                        Vec3.ZERO, 0.014f),
                burstFrontSource(ritual), 92.0f, 130);
        ritual.burstCoreSoul = new MaterialEmitters.Emitter(level,
                MaterialEmitters.Preset.soul(0xFFF0C7, 1.50f, seed + 1409,
                        new Vec3(0.0, 0.030, 0.0), 0.020f),
                burstCoreSource(ritual), 145.0f, 190);
    }

    private static int seedBase(Ritual ritual) {
        return (int) (ritual.seed ^ (ritual.seed >>> 32));
    }

    /** Emission points clustered inside the current ball positions. */
    private static MaterialEmitters.Source ballSource(Ritual ritual) {
        Palette palette = ritual.namek ? Palette.NAMEK : Palette.EARTH;
        return random -> {
            Vec3 pos = ritual.currentBallPositions[random.nextInt(ritual.currentBallPositions.length)];
            return sphereSample(random, pos, palette.radius * 0.62);
        };
    }

    /** Emission points along the formation front at the floating ball ring. */
    private static MaterialEmitters.Source formationFrontSource(Ritual ritual) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = DragonWishRitual.RING_RADIUS + (random.nextDouble() - 0.5) * 0.24;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 pos = new Vec3(
                    ritual.center.x + cos * radius,
                    ritual.center.y + DragonWishRitual.RING_LIFT + (random.nextDouble() - 0.5) * 0.18,
                    ritual.center.z + sin * radius);
            return new MaterialEmitters.Sample(pos, new Vec3(cos * 0.050, 0.0, sin * 0.050));
        };
    }

    /**
     * Emission points through the summoning column. Kind 0 is the dense
     * plasma core, kind 1 is the rising soul stream near the old mote path,
     * and kind 2 is the wider vapor body.
     */
    private static MaterialEmitters.Source columnSource(Ritual ritual, int kind) {
        return random -> {
            double height = Math.max(0.1, ritual.columnHeight);
            double y = ritual.columnGroundY + random.nextDouble() * height;
            if (kind == 1) {
                double angle = ritual.columnPhase * 1.35 + random.nextDouble() * 1.4 + y * 0.32;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                Vec3 pos = new Vec3(ritual.center.x + cos * 1.45, y, ritual.center.z + sin * 1.45);
                return new MaterialEmitters.Sample(pos, new Vec3(-sin * 0.045, 0.180, cos * 0.045));
            }
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = (kind == 0 ? 0.58 : 1.05) * Math.sqrt(random.nextDouble());
            Vec3 pos = new Vec3(
                    ritual.center.x + Math.cos(angle) * radius,
                    y,
                    ritual.center.z + Math.sin(angle) * radius);
            return new MaterialEmitters.Sample(pos, new Vec3(0.0, kind == 0 ? 0.260 : 0.090, 0.0));
        };
    }

    /** Emission points along the expanding ground front. */
    private static MaterialEmitters.Source groundFrontSource(Ritual ritual) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = ritual.groundRadius + (random.nextDouble() - 0.5) * 0.26;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 pos = new Vec3(
                    ritual.center.x + cos * radius,
                    ritual.columnGroundY + 0.12 + (random.nextDouble() - 0.5) * 0.10,
                    ritual.center.z + sin * radius);
            return new MaterialEmitters.Sample(pos, new Vec3(cos * 0.070, 0.0, sin * 0.070));
        };
    }

    /** Emission points along the two vertical gate fronts. */
    private static MaterialEmitters.Source gateFrontSource(Ritual ritual) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = (random.nextBoolean() ? 1.0 : 0.72) * ritual.gateRadius;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 pos = new Vec3(
                    ritual.center.x + cos * radius,
                    ritual.center.y + 2.2 + sin * radius,
                    ritual.center.z + (random.nextDouble() - 0.5) * 0.18);
            return new MaterialEmitters.Sample(pos, new Vec3(cos * 0.040, sin * 0.040, 0.0));
        };
    }

    /** Emission points along the expanding arrival front. */
    private static MaterialEmitters.Source burstFrontSource(Ritual ritual) {
        return random -> {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 pos = new Vec3(
                    ritual.center.x + cos * ritual.burstFrontRadius,
                    ritual.center.y + 1.2 + (random.nextDouble() - 0.5) * 0.16,
                    ritual.center.z + sin * ritual.burstFrontRadius);
            return new MaterialEmitters.Sample(pos, new Vec3(cos * 0.280, 0.0, sin * 0.280));
        };
    }

    /** Emission points inside the expanding arrival core volume. */
    private static MaterialEmitters.Source burstCoreSource(Ritual ritual) {
        return random -> sphereSample(random, ritual.burstCoreCenter, ritual.burstCoreRadius);
    }

    private static MaterialEmitters.Sample sphereSample(RandomSource random, Vec3 center, double radius) {
        double u = random.nextDouble() * 2.0 - 1.0;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double rr = Math.max(0.0, radius) * Math.cbrt(random.nextDouble());
        double flat = Math.sqrt(Math.max(0.0, 1.0 - u * u)) * rr;
        return new MaterialEmitters.Sample(new Vec3(
                center.x + Math.cos(angle) * flat,
                center.y + u * rr,
                center.z + Math.sin(angle) * flat), Vec3.ZERO);
    }

    private static Vec3 rotateY(Vec3 point, Vec3 center, float yaw) {
        double dx = point.x - center.x;
        double dz = point.z - center.z;
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        return new Vec3(center.x + dx * c - dz * s, point.y, center.z + dx * s + dz * c);
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, float t) {
        return new Vec3(
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t
        );
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }

    private static final class Palette {
        static final Palette EARTH = new Palette(0xFF750F, 0xFFCC40, 0xFFE9A8, 0xFFF3C4, 0.38f);
        static final Palette NAMEK = new Palette(0x2EC751, 0x66FF9E, 0xB8FFD0, 0xD8FFE4, 0.52f);

        final int bodyColor;
        final int ringColor;
        final int soulColor;
        final int mistColor;
        final float radius;

        Palette(int bodyColor, int ringColor, int soulColor, int mistColor, float radius) {
            this.bodyColor = bodyColor;
            this.ringColor = ringColor;
            this.soulColor = soulColor;
            this.mistColor = mistColor;
            this.radius = radius;
        }
    }

    private static final class Ritual {
        final boolean namek;
        final long startGameTime;
        final Vec3 center;
        final Vec3[] from;
        final int[] slots;
        final long seed;
        final Vec3[] currentBallPositions;
        float ballEnvelope;
        float formationEnvelope;
        float columnEnvelope;
        double columnGroundY;
        float columnHeight;
        float columnPhase;
        float groundRadius;
        float gateEnvelope;
        float gateRadius;
        float burstEnvelope;
        float burstFrontRadius;
        Vec3 burstCoreCenter;
        float burstCoreRadius;
        long lastEmitNanos;
        ClientLevel emitterLevel;
        MaterialEmitters.Emitter ballPlasma;
        MaterialEmitters.Emitter ballSoul;
        MaterialEmitters.Emitter formationMist;
        MaterialEmitters.Emitter formationPlasma;
        MaterialEmitters.Emitter columnPlasma;
        MaterialEmitters.Emitter columnSoul;
        MaterialEmitters.Emitter columnVapor;
        MaterialEmitters.Emitter groundMist;
        MaterialEmitters.Emitter groundPlasma;
        MaterialEmitters.Emitter gateMist;
        MaterialEmitters.Emitter gatePlasma;
        MaterialEmitters.Emitter burstFrontPlasma;
        MaterialEmitters.Emitter burstFrontMist;
        MaterialEmitters.Emitter burstCoreSoul;

        Ritual(boolean namek, long startGameTime, Vec3 center, Vec3[] from, int[] slots, long seed) {
            this.namek = namek;
            this.startGameTime = startGameTime;
            this.center = center;
            this.from = from;
            this.slots = slots;
            this.seed = seed;
            this.currentBallPositions = new Vec3[from.length];
            System.arraycopy(from, 0, this.currentBallPositions, 0, from.length);
            this.burstCoreCenter = center;
        }
    }
}
