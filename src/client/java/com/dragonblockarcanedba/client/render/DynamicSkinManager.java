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

    private static NativeImage getImage(String relativePath) {
        if (LOADED_IMAGES.containsKey(relativePath)) {
            return LOADED_IMAGES.get(relativePath);
        }

        String fullPath = relativePath;
        if (!fullPath.startsWith("textures/")) {
            fullPath = "textures/" + fullPath;
        }
        if (!fullPath.endsWith(".png")) {
            fullPath += ".png";
        }

        Identifier resId = Identifier.fromNamespaceAndPath("dragonblockarcanedba", fullPath);
        var rm = Minecraft.getInstance().getResourceManager();
        if (rm != null) {
            try {
                var res = rm.getResource(resId);
                if (res.isPresent()) {
                    try (InputStream is = res.get().open()) {
                        NativeImage img = NativeImage.read(is);
                        LOADED_IMAGES.put(relativePath, img);
                        return img;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Classpath fallback
        try {
            String cp = "/assets/dragonblockarcanedba/" + fullPath;
            InputStream is = DynamicSkinManager.class.getResourceAsStream(cp);
            if (is != null) {
                try (is) {
                    NativeImage img = NativeImage.read(is);
                    LOADED_IMAGES.put(relativePath, img);
                    return img;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static int[] resolveColors(int skinColor, int hairColor, int eyeColor) {
        int sR = (skinColor >> 16) & 0xFF;
        int sG = (skinColor >> 8) & 0xFF;
        int sB = skinColor & 0xFF;
        int hR = (hairColor >> 16) & 0xFF;
        int hG = (hairColor >> 8) & 0xFF;
        int hB = hairColor & 0xFF;
        int eR = (eyeColor >> 16) & 0xFF;
        int eG = (eyeColor >> 8) & 0xFF;
        int eB = eyeColor & 0xFF;

        if (sR == 0 && sG == 0 && sB == 0) { sR = 255; sG = 180; sB = 160; }
        if (hR == 0 && hG == 0 && hB == 0) { hR = 255; hG = 240; hB = 140; }
        // Black is a real eye color. Callers pass white when the player has not chosen one.

        return new int[]{ sR, sG, sB, hR, hG, hB, eR, eG, eB };
    }

    public static Identifier getOrGenerateSkin(String racePath, int skinColor, int hairColor, int eyeColor) {
        return getOrGenerateSkin(racePath, skinColor, hairColor, eyeColor, false);
    }

    /**
     * @param eyesClosed when true, the face atlas is the closed-eyelid variant.
     *                   Only the eye texels differ from the open face. Both variants
     *                   keep mask B on the eye so the chosen eye color tints them.
     */
    public static Identifier getOrGenerateSkin(String racePath, int skinColor, int hairColor, int eyeColor, boolean eyesClosed) {
        if (racePath == null || racePath.isEmpty()) racePath = "base";

        int[] c = resolveColors(skinColor, hairColor, eyeColor);
        String key = String.format("%s_%s_%02x%02x%02x_%02x%02x%02x_%02x%02x%02x",
                racePath, eyesClosed ? "shut" : "open", c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8]);

        if (CACHED_SKINS.containsKey(key)) {
            return CACHED_SKINS.get(key);
        }

        Identifier skinId = generateAndRegister(racePath, key, c, eyesClosed);
        if (skinId != null) {
            CACHED_SKINS.put(key, skinId);
        }
        return skinId;
    }

    private static NativeImage resolveImage(String racePath, String suffix, String universalStem, String baseStem) {
        NativeImage img = getImage("entity/player/" + racePath + suffix);
        if (img == null && !racePath.equals("universal_humanoid")) {
            img = getImage("entity/player/" + universalStem);
        }
        if (img == null) {
            img = getImage("entity/player/" + baseStem);
        }
        return img;
    }

    private static Identifier generateAndRegister(String racePath, String key, int[] c, boolean eyesClosed) {
        NativeImage baseImage;
        NativeImage maskImage;
        if (eyesClosed) {
            baseImage = getImage("entity/player/" + racePath + "_blink");
            maskImage = getImage("entity/player/" + racePath + "_blink_mask");
            // A missing closed face keeps this race's open face. Do not borrow another race.
            if (baseImage == null || maskImage == null) {
                return generateAndRegister(racePath, key, c, false);
            }
        } else {
            baseImage = resolveImage(racePath, "_base", "universal_humanoid_base", "base");
            maskImage = resolveImage(racePath, "_mask", "universal_humanoid_mask", "base_mask");
        }
        if (baseImage == null || maskImage == null) return null;

        int sR = c[0], sG = c[1], sB = c[2], hR = c[3], hG = c[4], hB = c[5], eR = c[6], eG = c[7], eB = c[8];

        int w = baseImage.getWidth();
        int h = baseImage.getHeight();
        NativeImage out = new NativeImage(w, h, true);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int baseAbgr = baseImage.getPixel(x, y);
                int baseArgb = baseAbgr; // NativeImage.getPixel returns ARGB in 26.x
                int baseA = ARGB.alpha(baseArgb);

                if (baseA == 0) {
                    out.setPixel(x, y, 0);
                    continue;
                }

                int maskAbgr = maskImage.getPixel(x, y);
                int maskArgb = maskAbgr;
                int maskR = ARGB.red(maskArgb);
                int maskG = ARGB.green(maskArgb);
                int maskB = ARGB.blue(maskArgb);

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
                } else if (maskB > 128) {
                    // Iris and blink texels are grayscale. A straight multiply by that gray
                    // (often ~90/255) turns every chosen color into the same dark speck
                    // beside the pupil, so the eyes never appear to change.
                    float eLum = (0.299f * baseR + 0.587f * baseG + 0.114f * baseB) / 255.0f;
                    if (eLum > 1.0f) eLum = 1.0f;
                    float shade = 0.35f + 0.65f * eLum;
                    finalR = Math.min(255, Math.round(eR * shade));
                    finalG = Math.min(255, Math.round(eG * shade));
                    finalB = Math.min(255, Math.round(eB * shade));
                }

                out.setPixel(x, y, ARGB.color(baseA, finalR, finalG, finalB));
            }
        }

        DynamicTexture tex = new DynamicTexture(() -> "DBA Skin " + key, out);
        tex.upload();
        Identifier id = Identifier.parse("dragonblockarcanedba:skins/" + key);
        Minecraft.getInstance().getTextureManager().register(id, tex);
        return id;
    }
}
