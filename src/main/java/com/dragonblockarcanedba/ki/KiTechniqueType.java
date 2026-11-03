package com.dragonblockarcanedba.ki;

/**
 * The type of Ki Technique — determines how the attack fires.
 */
public enum KiTechniqueType {
    /** Spherical projectile. Can be toggled to BARRAGE mode (many small balls). */
    BLAST,
    /** Spiraling charged beam, continuous while held. */
    SPIRAL_BEAM,
    /** A disk summoned above the player, launches forward. */
    DISK,
    /** Sustained constant beam channel. */
    BEAM,
    /** Twin rapid-fire eye lasers — spam-castable. */
    LASER,
    /** AoE last-resort explosion. Costs 100% Ki, damages self. */
    EXPLOSION,
    /** Shell around the caster. Spends its ki share over time. */
    BARRIER;

    public static KiTechniqueType fromString(String s) {
        try {
            return KiTechniqueType.valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BLAST;
        }
    }

    public String displayName() {
        return switch (this) {
            case BLAST -> "Ki Blast";
            case SPIRAL_BEAM -> "Spiral Beam";
            case DISK -> "Ki Disk";
            case BEAM -> "Ki Beam";
            case LASER -> "Ki Laser";
            case EXPLOSION -> "Ki Explosion";
            case BARRIER -> "Ki Barrier";
        };
    }
}
