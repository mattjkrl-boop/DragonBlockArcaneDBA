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
    /** Missing or unset charge keeps the attack instant, which is how it fires today. */
    public static final int DEFAULT_CHARGE_TICKS = 0;
    /** Missing or unset size keeps today's projectile, beam, disk, laser, spiral, and explosion scale. */
    public static final double DEFAULT_SIZE = 1.0;
    /** Safety cap so a crafted charge cannot lock the slot for more than a minute. */
    public static final int MAX_CHARGE_TICKS = 1200;
    /** Safety cap so a crafted size cannot grow a hit volume without bound. */
    public static final double MAX_SIZE = 16.0;

    public static final KiTechnique EMPTY = new KiTechnique(KiTechniqueType.BLAST, 0, 0xFF00AAFF, false, true);

    public final KiTechniqueType type;
    public final int usedPercent;   // 1–100
    public final int color;         // ARGB
    public final boolean isBarrage; // BLAST only
    public final boolean isEmpty;
    public int chargeTicks;
    public double size;

    public KiTechnique(KiTechniqueType type, int usedPercent, int color, boolean isBarrage, boolean isEmpty,
                       int chargeTicks, double size) {
        this.type = type;
        this.usedPercent = usedPercent;
        this.color = color;
        this.isBarrage = isBarrage;
        this.isEmpty = isEmpty;
        this.chargeTicks = Math.max(0, Math.min(chargeTicks, MAX_CHARGE_TICKS));
        double safeSize = (size > 0.0 && !Double.isNaN(size)) ? size : DEFAULT_SIZE;
        this.size = Math.min(safeSize, MAX_SIZE);
    }

    public KiTechnique(KiTechniqueType type, int usedPercent, int color, boolean isBarrage, boolean isEmpty) {
        this(type, usedPercent, color, isBarrage, isEmpty, DEFAULT_CHARGE_TICKS, DEFAULT_SIZE);
    }

    public KiTechnique(KiTechniqueType type, int usedPercent, int color, boolean isBarrage) {
        this(type, usedPercent, color, isBarrage, false);
    }

    public int getChargeTicks() {
        return chargeTicks;
    }

    public void setChargeTicks(int chargeTicks) {
        this.chargeTicks = Math.max(0, Math.min(chargeTicks, MAX_CHARGE_TICKS));
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        double safeSize = (size > 0.0 && !Double.isNaN(size)) ? size : DEFAULT_SIZE;
        this.size = Math.min(safeSize, MAX_SIZE);
    }

    /** Packet and component id. Barrier stays the lowercase id the craft screen sends. */
    public String typeId() {
        return type == KiTechniqueType.BARRIER ? "barrier" : type.name();
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
        tag.putString("type", typeId());
        tag.putInt("usedPercent", usedPercent);
        tag.putInt("color", color);
        tag.putBoolean("isBarrage", isBarrage);
        tag.putBoolean("isEmpty", isEmpty);
        tag.putInt("chargeTicks", chargeTicks);
        tag.putDouble("size", size);
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
        int chargeTicks = tag.getIntOr("chargeTicks", DEFAULT_CHARGE_TICKS);
        double size = tag.getDoubleOr("size", DEFAULT_SIZE);
        return new KiTechnique(type, usedPercent, color, isBarrage, false, chargeTicks, size);
    }
}
