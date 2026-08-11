package com.dragonblockarcanedba.ki;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents a configured Ki Technique in one of the player's 3 slots.
 *
 * Fields:
 * - type         : what kind of attack (BLAST, BEAM, DISK, etc.)
 * - usedPercent  : 1–100, how much of current Ki to spend on this attack
 * - color        : ARGB int for particle color (e.g. 0xFF00AAFF = blue)
 * - isBarrage    : BLAST only — fires many small balls instead of one large
 * - isEmpty      : true if this slot has no technique configured
 *
 * Damage formula:
 *   kiPower = currentKi × (usedPercent / 100.0)
 *   damage  = kiPower × (1.0 + willpower × 0.1)
 *
 * Ki Explosion only:
 *   damage     = kiPower × 1.5  (100% Ki forced, radius scales with kiPower)
 *   selfDamage = kiPower × 0.95
 */
public class KiTechnique {
    public static final KiTechnique EMPTY = new KiTechnique(KiTechniqueType.BLAST, 0, 0xFF00AAFF, false, true);

    public final KiTechniqueType type;
    public final int usedPercent;   // 1–100
    public final int color;         // ARGB
    public final boolean isBarrage; // BLAST only
    public final boolean isEmpty;

    public KiTechnique(KiTechniqueType type, int usedPercent, int color, boolean isBarrage, boolean isEmpty) {
        this.type = type;
        this.usedPercent = usedPercent;
        this.color = color;
        this.isBarrage = isBarrage;
        this.isEmpty = isEmpty;
    }

    public KiTechnique(KiTechniqueType type, int usedPercent, int color, boolean isBarrage) {
        this(type, usedPercent, color, isBarrage, false);
    }

    /** Returns display name shown on HUD slot. */
    public String displayName() {
        if (isEmpty) return "Empty";
        String base = type.displayName();
        if (type == KiTechniqueType.BLAST && isBarrage) base = "Ki Barrage";
        return base + " (" + usedPercent + "%)";
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putInt("usedPercent", usedPercent);
        tag.putInt("color", color);
        tag.putBoolean("isBarrage", isBarrage);
        tag.putBoolean("isEmpty", isEmpty);
        return tag;
    }

    public static KiTechnique fromNbt(CompoundTag tag) {
        if (tag == null || tag.isEmpty() || tag.getBooleanOr("isEmpty", true)) {
            return EMPTY;
        }
        KiTechniqueType type = KiTechniqueType.fromString(tag.getStringOr("type", "BLAST"));
        int usedPercent = tag.getIntOr("usedPercent", 50);
        int color = tag.getIntOr("color", 0xFF00AAFF);
        boolean isBarrage = tag.getBooleanOr("isBarrage", false);
        return new KiTechnique(type, usedPercent, color, isBarrage, false);
    }
}
