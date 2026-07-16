package com.dragonblockarcanedba.client.render.geo;

/**
 * Utility for parsing hex color strings and blending colors.
 * <p>
 * Used by the GeckoLib renderer to:
 * 1. Parse player-chosen skin/hair colors from hex strings (#FFCC99)
 * 2. Blend form-forced colors (e.g., SSJ gold hair) with the player's chosen color
 * 3. Produce final ARGB values for texture tinting
 */
public final class ColorBlending {

    private ColorBlending() {}

    /**
     * Parses a hex color string (#RRGGBB) into an int array [R, G, B].
     * Returns the fallback if the string is null, empty, or malformed.
     */
    public static int[] parseHex(String hex, int[] fallback) {
        if (hex == null || hex.isEmpty()) return fallback;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            if (clean.length() != 6) return fallback;
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            return new int[]{r, g, b};
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Converts an [R, G, B] array to a packed ARGB int with full alpha.
     */
    public static int toArgb(int[] rgb) {
        return 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /**
     * Converts an [R, G, B] array to a packed ARGB int with specified alpha (0-255).
     */
    public static int toArgb(int[] rgb, int alpha) {
        return (alpha << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /**
     * Blends a form's override color with the player's chosen color.
     * <p>
     * Instead of fully replacing, this produces a merged result:
     * - The form color dominates hue and saturation
     * - The player color influences brightness/warmth
     * <p>
     * blend = 0.0 → pure player color
     * blend = 1.0 → pure form color
     * Default blend for transformations is 0.7 (form-dominant with player warmth)
     *
     * @param playerColor The player's chosen [R,G,B] from character creation
     * @param formColor   The form's forced [R,G,B] override (e.g., SSJ gold = [255, 215, 0])
     * @param blendFactor How much the form color dominates (0.0 to 1.0)
     * @return Merged [R,G,B] result
     */
    public static int[] blend(int[] playerColor, int[] formColor, double blendFactor) {
        blendFactor = Math.max(0.0, Math.min(1.0, blendFactor));
        double playerWeight = 1.0 - blendFactor;

        int r = (int) Math.round(playerColor[0] * playerWeight + formColor[0] * blendFactor);
        int g = (int) Math.round(playerColor[1] * playerWeight + formColor[1] * blendFactor);
        int b = (int) Math.round(playerColor[2] * playerWeight + formColor[2] * blendFactor);

        return new int[]{
            Math.min(255, Math.max(0, r)),
            Math.min(255, Math.max(0, g)),
            Math.min(255, Math.max(0, b))
        };
    }

    /**
     * Convenience blend with default transformation blend factor (0.7 = form-dominant).
     */
    public static int[] blendTransformation(int[] playerColor, int[] formColor) {
        return blend(playerColor, formColor, 0.7);
    }

    /**
     * Multiplies a base texture color by a tint color (normalized multiply).
     * This is how skin/hair tinting works on grayscale textures:
     * result = (textureColor * tintColor) / 255
     *
     * @param textureR Original texture red (0-255)
     * @param textureG Original texture green (0-255)
     * @param textureB Original texture blue (0-255)
     * @param tint     The tint color [R, G, B]
     * @return Tinted [R, G, B]
     */
    public static int[] multiplyTint(int textureR, int textureG, int textureB, int[] tint) {
        return new int[]{
            (textureR * tint[0]) / 255,
            (textureG * tint[1]) / 255,
            (textureB * tint[2]) / 255
        };
    }

    /** Default white (no tint applied). */
    public static final int[] WHITE = {255, 255, 255};

    /** Default SSJ gold for hair tinting. */
    public static final int[] SSJ_GOLD = {255, 215, 0};

    /** Default SSJ Blue for hair tinting. */
    public static final int[] SSJ_BLUE = {30, 144, 255};

    /** Default SSJ God red for hair tinting. */
    public static final int[] SSJ_GOD_RED = {220, 20, 60};

    /** Default Ultra Instinct silver for hair tinting. */
    public static final int[] UI_SILVER = {200, 200, 220};
}
