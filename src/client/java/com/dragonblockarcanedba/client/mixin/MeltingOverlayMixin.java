package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.effect.DbaEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders custom immersive screen overlays when the local player has custom status effects:
 * - Dark Faded / Movement Curse / Storm of Darkness: Pulsing pitch-black void darkness screen fade.
 * - Melting: Dark purple corrosive venom vignette.
 * - Temporal Stasis: Shimmering silver-cyan time distortion screen warp.
 * - Sorrow Rift: Weeping dark violet void tears overlay.
 * - Evil Dread: The dark closes in: a heavy murk under fully black edges,
 *   so the victim's world shrinks to a dim tunnel while the gaze eyes
 *   burn through it. Strength is tracked statically and advanced by
 *   whole game ticks, in over about a second, out a little faster.
 */
@Mixin(Hud.class)
public class MeltingOverlayMixin {

    private static float dba$dreadStrength = 0.0f;
    private static long dba$dreadLastTick = -1L;

    private static void dba$advanceDread(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            dba$dreadStrength = 0.0f;
            dba$dreadLastTick = -1L;
            return;
        }
        long now = mc.level.getGameTime();
        if (dba$dreadLastTick < 0L) dba$dreadLastTick = now;
        int steps = (int) Math.min(20L, now - dba$dreadLastTick);
        dba$dreadLastTick = now;
        boolean has = DbaEffects.EVIL_DREAD_HOLDER != null && mc.player.hasEffect(DbaEffects.EVIL_DREAD_HOLDER);
        for (int i = 0; i < steps; i++) {
            dba$dreadStrength = has
                ? Math.min(1.0f, dba$dreadStrength + 0.05f)
                : Math.max(0.0f, dba$dreadStrength - 0.07f);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void dba$renderCustomScreenOverlays(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int screenW = guiGraphics.guiWidth();
        int screenH = guiGraphics.guiHeight();

        // 1. Dark Faded / Movement Curse / Storm of Darkness (Abyssal Void Darkness Screen Fade)
        boolean hasDarknessEffect = mc.player.hasEffect(DbaEffects.DARK_FADED_HOLDER)
            || mc.player.hasEffect(DbaEffects.MOVEMENT_CURSE_HOLDER)
            || mc.player.hasEffect(DbaEffects.STORM_OF_DARKNESS_HOLDER);

        if (hasDarknessEffect) {
            float pulse = 0.70f + 0.30f * (float) Math.sin(mc.player.tickCount * 0.35f);
            int alpha = (int) (pulse * 0.88f * 255);
            alpha = Math.max(0, Math.min(255, alpha));

            // Pitch-black darkness overlay with slight abyssal violet undertone
            int darkColor = (alpha << 24) | (0x0A << 16) | (0x00 << 8) | 0x14;
            guiGraphics.fill(0, 0, screenW, screenH, darkColor);
        }

        // 2. Melting Screen Overlay (Dark Purple Corrosive Vignette)
        if (mc.player.hasEffect(DbaEffects.MELTING_HOLDER)) {
            float health = mc.player.getHealth();
            float maxHealth = mc.player.getMaxHealth();
            float healthRatio = maxHealth > 0 ? health / maxHealth : 1.0f;

            float alphaF = 0.15f + 0.60f * (1.0f - healthRatio);
            int alpha = (int) (alphaF * 255);
            alpha = Math.max(0, Math.min(255, alpha));

            int purpleColor = (alpha << 24) | (0x40 << 16) | (0x00 << 8) | 0x60;
            guiGraphics.fill(0, 0, screenW, screenH, purpleColor);
        }

        // 3. Temporal Stasis Screen Overlay (Silver-Cyan Time Freeze)
        if (mc.player.hasEffect(DbaEffects.TEMPORAL_STASIS_HOLDER)) {
            float timePulse = 0.5f + 0.3f * (float) Math.cos(mc.player.tickCount * 0.4f);
            int alpha = (int) (timePulse * 255);
            alpha = Math.max(0, Math.min(200, alpha));

            int stasisColor = (alpha << 24) | (0x00 << 16) | (0xEE << 8) | 0xFF;
            guiGraphics.fill(0, 0, screenW, screenH, stasisColor);
        }

        // 4. Sorrow Rift Screen Overlay (Weeping Void Distortion)
        if (mc.player.hasEffect(DbaEffects.SORROW_RIFT_HOLDER)) {
            float sorrowPulse = 0.4f + 0.25f * (float) Math.sin(mc.player.tickCount * 0.25f);
            int alpha = (int) (sorrowPulse * 255);
            alpha = Math.max(0, Math.min(180, alpha));

            int sorrowColor = (alpha << 24) | (0x66 << 16) | (0x00 << 8) | 0x33;
            guiGraphics.fill(0, 0, screenW, screenH, sorrowColor);
        }

        // 5. Evil Dread (Closing Vignette: the world drowns in black)
        dba$advanceDread(mc);
        if (dba$dreadStrength > 0.01f
            && com.dragonblockarcanedba.config.DbaEffectsConfig.effectDensity() > 0f) {
            float s = dba$dreadStrength;
            float pulse = 0.97f + 0.03f * (float) Math.sin(mc.player.tickCount * 0.21f);
            int baseAlpha = (int) (0.82f * s * pulse * 255f);
            int baseColor = (baseAlpha << 24) | (0x03 << 16) | 0x05;
            guiGraphics.fill(0, 0, screenW, screenH, baseColor);

            // Stepped black bands walking in from every edge: full black
            // at the rim, dying out quadratically before the center, so
            // sight closes to a dim tunnel instead of a flat fade.
            int bands = 7;
            for (int i = 0; i < bands; i++) {
                float t = i / (float) bands;
                float fall = (1.0f - t) * (1.0f - t);
                int alpha = (int) (s * fall * 255f);
                if (alpha <= 0) continue;
                int c = alpha << 24;
                int x0 = (int) (screenW * 0.085f * i);
                int y0 = (int) (screenH * 0.085f * i);
                int x1 = (int) (screenW * 0.085f * (i + 1));
                int y1 = (int) (screenH * 0.085f * (i + 1));
                guiGraphics.fill(x0, y0, screenW - x0, y1, c);
                guiGraphics.fill(x0, screenH - y1, screenW - x0, screenH - y0, c);
                guiGraphics.fill(x0, y0, x1, screenH - y0, c);
                guiGraphics.fill(screenW - x1, y0, screenW - x0, screenH - y0, c);
            }
        }
    }
}
