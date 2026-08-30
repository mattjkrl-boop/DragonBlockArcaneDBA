package com.dragonblockarcanedba.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal Dynamic Model Manager.
 * Generates recolored textures dynamically for any race model entirely in-memory.
 */
public final class DynamicSkinManager {
    private static final Map<String, Identifier> CACHED_SKINS = new ConcurrentHashMap<>();
    private static final Map<String, NativeImage> LOADED_IMAGES = new ConcurrentHashMap<>();

    private DynamicSkinManager() {}

    private static NativeImage getImage(String texturePath) {
        if (LOADED_IMAGES.containsKey(texturePath)) {
            return LOADED_IMAGES.get(texturePath);
        }
        
        Identifier id = Identifier.tryParse(texturePath);
        if (id != null) {
            var rm = Minecraft.getInstance().getResourceManager();
            if (rm != null) {
                try {
                    var res = rm.getResource(id);
                    if (res.isPresent()) {
                        try (InputStream is = res.get().open()) {
                            NativeImage img = NativeImage.read(is);
                            LOADED_IMAGES.put(texturePath, img);
                            return img;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        
        // Classpath fallback
        try {
            String cp = "/assets/" + texturePath.replace(":", "/textures/");
            if (!cp.endsWith(".png")) cp += ".png";
            InputStream is = DynamicSkinManager.class.getResourceAsStream(cp);
            if (is != null) {
                try (is) { 
                    NativeImage img = NativeImage.read(is);
                    LOADED_IMAGES.put(texturePath, img);
                    return img;
                }
            }
        } catch (Exception ignored) {}
        
        return null;
    }

    private static int[] resolveColors(int skinColor, int hairColor) {
        int sR = (skinColor >> 16) & 0xFF;
        int sG = (skinColor >> 8) & 0xFF;
        int sB = skinColor & 0xFF;
        int hR = (hairColor >> 16) & 0xFF;
        int hG = (hairColor >> 8) & 0xFF;
        int hB = hairColor & 0xFF;

        if (sR == 0 && sG == 0 && sB == 0) { sR = 255; sG = 180; sB = 160; }
        if (hR == 0 && hG == 0 && hB == 0) { hR = 255; hG = 240; hB = 140; }

        return new int[]{ sR, sG, sB, hR, hG, hB };
    }

    public static Identifier getOrGenerateSkin(String racePath, int skinColor, int hairColor) {
        if (racePath == null || racePath.isEmpty()) racePath = "base";
        
        int[] c = resolveColors(skinColor, hairColor);
        String key = String.format("%s_%02x%02x%02x_%02x%02x%02x", racePath, c[0], c[1], c[2], c[3], c[4], c[5]);

        if (CACHED_SKINS.containsKey(key)) {
            return CACHED_SKINS.get(key);
        }

        Identifier skinId = generateAndRegister(racePath, key, c);
        if (skinId != null) {
            CACHED_SKINS.put(key, skinId);
        }
        return skinId;
    }

    private static Identifier generateAndRegister(String racePath, String key, int[] c) {
        String baseTexPath = "dragonblockarcanedba:entity/player/" + racePath + "_base";
        String maskTexPath = "dragonblockarcanedba:entity/player/" + racePath + "_mask";
        
        NativeImage baseImage = getImage(baseTexPath);
        if (baseImage == null && !racePath.equals("universal_humanoid")) {
            baseTexPath = "dragonblockarcanedba:entity/player/universal_humanoid_base";
            baseImage = getImage(baseTexPath);
        }
        if (baseImage == null) {
            baseTexPath = "dragonblockarcanedba:entity/player/base";
            baseImage = getImage(baseTexPath);
        }
        
        NativeImage maskImage = getImage(maskTexPath);
        if (maskImage == null && !racePath.equals("universal_humanoid")) {
            maskTexPath = "dragonblockarcanedba:entity/player/universal_humanoid_mask";
            maskImage = getImage(maskTexPath);
        }
        if (maskImage == null) {
            maskTexPath = "dragonblockarcanedba:entity/player/base_mask";
            maskImage = getImage(maskTexPath);
        }

        if (baseImage == null || maskImage == null) return null;

        int sR = c[0], sG = c[1], sB = c[2], hR = c[3], hG = c[4], hB = c[5];

        int w = baseImage.getWidth();
        int h = baseImage.getHeight();
        NativeImage out = new NativeImage(w, h, true);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int baseAbgr = baseImage.getPixel(x, y);
                int baseArgb = ARGB.fromABGR(baseAbgr);
                int baseA = ARGB.alpha(baseArgb);

                if (baseA == 0) {
                    out.setPixel(x, y, 0);
                    continue;
                }

                int maskAbgr = maskImage.getPixel(x, y);
                int maskArgb = ARGB.fromABGR(maskAbgr);
                int maskR = ARGB.red(maskArgb);
                int maskG = ARGB.green(maskArgb);

                int baseR = ARGB.red(baseArgb);
                int baseG = ARGB.green(baseArgb);
                int baseB = ARGB.blue(baseArgb);

                int finalR = baseR, finalG = baseG, finalB = baseB;

                if (maskR > 128) {
                    float lum = (0.299f * baseR + 0.587f * baseG + 0.114f * baseB) / 215.0f;
                    if (lum > 1.25f) lum = 1.25f;
                    finalR = Math.min(255, Math.round(sR * lum));
                    finalG = Math.min(255, Math.round(sG * lum));
                    finalB = Math.min(255, Math.round(sB * lum));
                } else if (maskG > 128) {
                    float hLum = (0.299f * baseR + 0.587f * baseG + 0.114f * baseB) / 235.0f;
                    if (hLum > 1.25f) hLum = 1.25f;
                    finalR = Math.min(255, Math.round(hR * hLum));
                    finalG = Math.min(255, Math.round(hG * hLum));
                    finalB = Math.min(255, Math.round(hB * hLum));
                }

                out.setPixel(x, y, ARGB.toABGR(ARGB.color(baseA, finalR, finalG, finalB)));
            }
        }

        DynamicTexture tex = new DynamicTexture(() -> "DBA Skin " + key, out);
        tex.upload();
        Identifier id = Identifier.parse("dragonblockarcanedba:skins/" + key);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        return id;
    }
}
