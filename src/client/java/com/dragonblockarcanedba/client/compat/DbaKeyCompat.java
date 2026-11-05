package com.dragonblockarcanedba.client.compat;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Default key codes resolved at runtime. 26.2 uses GLFW key codes, 26.3
 * moved to new codes behind the same InputConstants names, and javac
 * inlines constants, so reading the field reflectively keeps one jar right
 * on both. The keyboard type is implied by the three-argument KeyMapping
 * constructor, which exists on both versions (InputConstants.Type.KEYSYM
 * does not exist on 26.3).
 */
public final class DbaKeyCompat {
    private DbaKeyCompat() {}

    public static int key(String constantName) {
        try {
            return InputConstants.class.getField(constantName).getInt(null);
        } catch (ReflectiveOperationException e) {
            return InputConstants.UNKNOWN.getValue();
        }
    }
}
