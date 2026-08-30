package com.dragonblockarcanedba.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal Dynamic Player Skin & Yardrat Model Texture Generator.
 * Physically tints the exact skin and hair/horns pixels directly on:
 * 1. The 256x256 authentic Dragon Ball Yardrat texture used by Better Player Model (BPM).
 * 2. The 64x64 martial arts base skin fallback texture.
 * Saves the dynamically tinted texture directly into BPM's custom texture folder and registers
 * it dynamically with Minecraft's TextureManager so changes are instant in menus and in-game.
 */
public final class DynamicSkinManager {

    public static final Identifier BASE_SKIN_ID =
            Identifier.parse("dragonblockarcanedba:textures/entity/player/base.png");
    private static final Identifier BASE_MASK_ID =
            Identifier.parse("dragonblockarcanedba:textures/entity/player/base_mask.png");

    public static final Identifier YARDRAT_BASE_ID =
            Identifier.parse("dragonblockarcanedba:textures/entity/player/yardrat_base.png");
    private static final Identifier YARDRAT_MASK_ID =
            Identifier.parse("dragonblockarcanedba:textures/entity/player/yardrat_mask.png");

    private static final Map<String, Identifier> CACHED_SKINS = new ConcurrentHashMap<>();
    private static NativeImage cachedBaseImage = null;
    private static NativeImage cachedMaskImage = null;
    private static NativeImage cachedYardratImage = null;
    private static NativeImage cachedYardratMask = null;

    private DynamicSkinManager() {}

    private static synchronized void ensureBaseLoaded() {
        if (cachedYardratImage != null && cachedYardratMask != null) return;

        var rm = Minecraft.getInstance().getResourceManager();
        try {
            // Load 64x64 base
            if (rm != null) {
                var skinRes = rm.getResource(BASE_SKIN_ID);
                if (skinRes.isPresent()) {
                    try (InputStream is = skinRes.get().open()) {
                        cachedBaseImage = NativeImage.read(is);
                    }
                }
                var maskRes = rm.getResource(BASE_MASK_ID);
                if (maskRes.isPresent()) {
                    try (InputStream is = maskRes.get().open()) {
                        cachedMaskImage = NativeImage.read(is);
                    }
                }

                // Load 256x256 Yardrat base and mask
                var yardratRes = rm.getResource(YARDRAT_BASE_ID);
                if (yardratRes.isPresent()) {
                    try (InputStream is = yardratRes.get().open()) {
                        cachedYardratImage = NativeImage.read(is);
                    }
                }
                var yMaskRes = rm.getResource(YARDRAT_MASK_ID);
                if (yMaskRes.isPresent()) {
                    try (InputStream is = yMaskRes.get().open()) {
                        cachedYardratMask = NativeImage.read(is);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Direct ClassLoader fallback if ResourceManager did not find them yet
        try {
            if (cachedYardratImage == null) {
                InputStream is = DynamicSkinManager.class.getResourceAsStream("/assets/dragonblockarcanedba/textures/entity/player/yardrat_base.png");
                if (is != null) {
                    try (is) {
                        cachedYardratImage = NativeImage.read(is);
                    }
                }
            }
            if (cachedYardratMask == null) {
                InputStream is = DynamicSkinManager.class.getResourceAsStream("/assets/dragonblockarcanedba/textures/entity/player/yardrat_mask.png");
                if (is != null) {
                    try (is) {
                        cachedYardratMask = NativeImage.read(is);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Physically recolors the Yardrat model texture and base skin for the chosen skin & hair color.
     *
     * @param skinColor ARGB or RGB color int
     * @param hairColor ARGB or RGB color int
     * @return Texture Identifier pointing to the dynamically registered texture
     */
    public static Identifier getOrGenerateSkin(int skinColor, int hairColor) {
        int sR = (skinColor >> 16) & 0xFF;
        int sG = (skinColor >> 8) & 0xFF;
        int sB = skinColor & 0xFF;

        int hR = (hairColor >> 16) & 0xFF;
        int hG = (hairColor >> 8) & 0xFF;
        int hB = hairColor & 0xFF;

        // Default fallbacks if color not yet configured
        if (sR == 0 && sG == 0 && sB == 0) {
            sR = 255; sG = 180; sB = 160;
        }
        if (hR == 0 && hG == 0 && hB == 0) {
            hR = 255; hG = 240; hB = 140;
        }

        String key = String.format("%02x%02x%02x_%02x%02x%02x", sR, sG, sB, hR, hG, hB);

        Identifier existing = CACHED_SKINS.get(key);
        if (existing != null) {
            return existing;
        }

        ensureBaseLoaded();

        // 1. Physically recolor 256x256 Yardrat texture for BPM
        if (cachedYardratImage != null && cachedYardratMask != null) {
            try {
                int w = cachedYardratImage.getWidth();
                int h = cachedYardratImage.getHeight();
                NativeImage yardratOut = new NativeImage(w, h, true);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int baseAbgr = cachedYardratImage.getPixel(x, y);
                        int baseArgb = ARGB.fromABGR(baseAbgr);
                        int baseA = ARGB.alpha(baseArgb);

                        if (baseA == 0) {
                            yardratOut.setPixel(x, y, 0);
                            continue;
                        }

                        int maskAbgr = cachedYardratMask.getPixel(x, y);
                        int maskArgb = ARGB.fromABGR(maskAbgr);
                        int maskR = ARGB.red(maskArgb);
                        int maskG = ARGB.green(maskArgb);

                        int baseR = ARGB.red(baseArgb);
                        int baseG = ARGB.green(baseArgb);
                        int baseB = ARGB.blue(baseArgb);

                        int finalR = baseR;
                        int finalG = baseG;
                        int finalB = baseB;

                        if (maskR > 128) {
                            // Luminance-based skin recoloring preserving authentic shading & highlights
                            float lum = (0.299f * baseR + 0.587f * baseG + 0.114f * baseB) / 215.0f;
                            if (lum > 1.25f) lum = 1.25f;
                            finalR = Math.min(255, Math.round(sR * lum));
                            finalG = Math.min(255, Math.round(sG * lum));
                            finalB = Math.min(255, Math.round(sB * lum));
                        } else if (maskG > 128) {
                            // Luminance-based hair / horns / crests recoloring
                            float hLum = (0.299f * baseR + 0.587f * baseG + 0.114f * baseB) / 235.0f;
                            if (hLum > 1.25f) hLum = 1.25f;
                            finalR = Math.min(255, Math.round(hR * hLum));
                            finalG = Math.min(255, Math.round(hG * hLum));
                            finalB = Math.min(255, Math.round(hB * hLum));
                        }

                        int finalArgb = ARGB.color(baseA, finalR, finalG, finalB);
                        yardratOut.setPixel(x, y, ARGB.toABGR(finalArgb));
                    }
                }

                // Register dynamically in Minecraft TextureManager
                DynamicTexture dynamicTexture = new DynamicTexture(() -> "DBA Yardrat Skin " + key, yardratOut);
                Identifier generatedId = Identifier.parse("dragonblockarcanedba:skins/yardrat_" + key);
                Minecraft.getInstance().getTextureManager().register(generatedId, dynamicTexture);

                // Write directly to BPM custom model texture destinations for all 12 races
                java.nio.file.Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
                String[] allRaces = new String[] {
                    "android", "arcosian", "bio_android", "half_saiyan", "human",
                    "majin", "namekian", "neo_tuffle", "saiyan", "tuffle",
                    "universal_humanoid", "yardrat"
                };

                for (String r : allRaces) {
                    try {
                        File bpmDst = configDir.resolve("better_player_model/custom/" + r + "/textures/default.png").toFile();
                        File animDst = new File("Animated/UniversalAnimations/BPM/" + r + "/textures/default.png");
                        for (File dst : new File[]{ bpmDst, animDst }) {
                            if (dst.getParentFile() != null) {
                                dst.getParentFile().mkdirs();
                            }
                            yardratOut.writeToFile(dst);
                        }
                    } catch (Exception ignored) {}
                }

                CACHED_SKINS.put(key, generatedId);
                return generatedId;

            } catch (Exception e) {
                // Fall through to 64x64 base
            }
        }

        // 2. Fallback to 64x64 base skin if Yardrat base image not available
        if (cachedBaseImage != null && cachedMaskImage != null) {
            try {
                int w = cachedBaseImage.getWidth();
                int h = cachedBaseImage.getHeight();
                NativeImage newImage = new NativeImage(w, h, true);

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int baseAbgr = cachedBaseImage.getPixel(x, y);
                        int baseArgb = ARGB.fromABGR(baseAbgr);
                        int baseA = ARGB.alpha(baseArgb);

                        if (baseA == 0) {
                            newImage.setPixel(x, y, 0);
                            continue;
                        }

                        int maskAbgr = cachedMaskImage.getPixel(x, y);
                        int maskArgb = ARGB.fromABGR(maskAbgr);
                        int maskR = ARGB.red(maskArgb);
                        int maskG = ARGB.green(maskArgb);

                        int baseR = ARGB.red(baseArgb);
                        int baseG = ARGB.green(baseArgb);
                        int baseB = ARGB.blue(baseArgb);

                        int finalR = baseR;
                        int finalG = baseG;
                        int finalB = baseB;

                        if (maskR > 128) {
                            finalR = (baseR * sR) / 255;
                            finalG = (baseG * sG) / 255;
                            finalB = (baseB * sB) / 255;
                        } else if (maskG > 128) {
                            finalR = Math.min(255, (baseR * hR * 2) / 255);
                            finalG = Math.min(255, (baseG * hG * 2) / 255);
                            finalB = Math.min(255, (baseB * hB * 2) / 255);
                        }

                        int finalArgb = ARGB.color(baseA, finalR, finalG, finalB);
                        newImage.setPixel(x, y, ARGB.toABGR(finalArgb));
                    }
                }

                DynamicTexture dynamicTexture = new DynamicTexture(() -> "DBA Skin " + key, newImage);
                Identifier generatedId = Identifier.parse("dragonblockarcanedba:skins/" + key);
                Minecraft.getInstance().getTextureManager().register(generatedId, dynamicTexture);

                CACHED_SKINS.put(key, generatedId);
                return generatedId;

            } catch (Exception e) {
                return BASE_SKIN_ID;
            }
        }

        return BASE_SKIN_ID;
    }
}
