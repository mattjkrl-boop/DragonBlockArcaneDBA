package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;

/**
 * Screen space post effects driven from client state: a grayscale freeze while the local player is in
 * Temporal Stasis, an eclipse grade while inside Storm of Darkness, and a radial shockwave on heavy impacts.
 * Each effect uses stepped vanilla post chains (uniforms are static in post chain json), advanced per tick.
 */
public final class DbaPostFx {
    private DbaPostFx() {}

    private static final int LEVELS = 4;
    private static final int SHOCK_FRAMES = 8;
    private static int grayLevel;
    private static int eclipseLevel;
    private static int shockFrame = -1;
    private static boolean wasStopped;
    private static boolean failed;

    /** Called from the shake payload handler; strong hits ripple the screen. */
    public static void onShake(float intensity) {
        if (intensity >= 1.2f) shockFrame = 0;
    }

    public static void tick(Minecraft mc) {
        boolean stopped = false, eclipse = false;
        if (mc.player != null) {
            stopped = DbaEffects.TEMPORAL_STASIS_HOLDER != null && mc.player.hasEffect(DbaEffects.TEMPORAL_STASIS_HOLDER);
            eclipse = DbaEffects.STORM_OF_DARKNESS_HOLDER != null && mc.player.hasEffect(DbaEffects.STORM_OF_DARKNESS_HOLDER);
        }
        if (stopped && !wasStopped) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(DbaSounds.TIME_STOP, 1.0f, 0.8f));
        }
        wasStopped = stopped;
        grayLevel = step(grayLevel, stopped);
        eclipseLevel = step(eclipseLevel, eclipse);
        if (shockFrame >= 0 && ++shockFrame >= SHOCK_FRAMES) shockFrame = -1;
    }

    private static int step(int level, boolean on) {
        return on ? Math.min(LEVELS, level + 1) : Math.max(0, level - 1);
    }

    /** Runs right after the world and vanilla post effect, before the GUI. */
    public static void render(Minecraft mc, com.mojang.blaze3d.pipeline.RenderTarget target, GraphicsResourceAllocator pool) {
        if (failed || mc.level == null) return;
        if (com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity() <= 0f) return;
        try {
            if (eclipseLevel > 0) apply(mc, target, pool, "eclipse_" + eclipseLevel);
            if (grayLevel > 0) apply(mc, target, pool, "time_stop_" + grayLevel);
            if (shockFrame >= 0) apply(mc, target, pool, "shockwave_" + shockFrame);
        } catch (RuntimeException e) {
            failed = true;
            com.dragonblockarcanedba.DragonBlockArcaneDBA.LOGGER.error("Disabling DBA post effects after a render failure", e);
        }
    }

    private static void apply(Minecraft mc, com.mojang.blaze3d.pipeline.RenderTarget target, GraphicsResourceAllocator pool, String name) {
        PostChain chain = mc.getShaderManager().getPostChain(
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", name), LevelTargetBundle.MAIN_TARGETS);
        if (chain != null) chain.process(target, pool);
    }
}
