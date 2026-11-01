package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.item.*;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The one table that defines how every weapon's basic attack looks. One record per
 * weapon: trail color, reach, sweep, fade, alpha, motion kind, and an optional tip
 * particle. SwingTrailRenderer consumes only this table, so tuning a weapon's attack
 * visual is a one-line edit here and adding a weapon is one line too.
 *
 * Kinds: ARC is the standard slash sweep; THRUST is a stab streak along the look
 * direction (spear, trident); FAN is an extra-wide slow gust arc (Bansho Fan);
 * FLASH is a muzzle flash and tracer with no ribbon (Blaster Gun).
 */
public final class WeaponTrailProfiles {
    private WeaponTrailProfiles() {}

    public enum Kind { ARC, THRUST, FAN, FLASH }
    public enum Tip { NONE, SPARK, EMBER, VOID_SMOKE, WIND }

    public record Profile(int color, double reach, float sweepDeg, int fadeTicks, float alpha, Kind kind, Tip tip) {}

    private static final Map<Class<? extends Item>, Profile> BY_CLASS = new IdentityHashMap<>();

    private static void reg(Class<? extends Item> cls, int color, double reach, float sweep, int fade, float alpha, Kind kind, Tip tip) {
        BY_CLASS.put(cls, new Profile(color, reach, sweep, fade, alpha, kind, tip));
    }

    static {
        // Swords: sweep and fade follow the blade's weight and speed.
        reg(KatanaItem.class,             0xFFFFFF, 2.0,  150f, 5, 1.0f,  Kind.ARC, Tip.NONE);
        reg(ZSwordItem.class,             0xBFE6FF, 2.1,  140f, 6, 1.0f,  Kind.ARC, Tip.SPARK);
        reg(SaberItem.class,              0x66CCFF, 2.0,  160f, 4, 1.0f,  Kind.ARC, Tip.SPARK);
        reg(BraveSwordItem.class,         0xFFD54A, 2.1,  145f, 6, 1.0f,  Kind.ARC, Tip.EMBER);
        reg(GrandSwordItem.class,         0xFFB347, 2.35, 165f, 8, 1.05f, Kind.ARC, Tip.EMBER);
        reg(AzureDragonSwordItem.class,   0x3FA9FF, 2.15, 150f, 6, 1.0f,  Kind.ARC, Tip.SPARK);
        reg(DaburaSwordItem.class,        0xC23BFF, 2.05, 145f, 6, 1.0f,  Kind.ARC, Tip.VOID_SMOKE);
        reg(CurseBladeItem.class,         0x8A2BE2, 2.05, 140f, 7, 1.0f,  Kind.ARC, Tip.VOID_SMOKE);
        reg(HollowsEdgeItem.class,        0x5A5A7A, 2.05, 150f, 6, 1.0f,  Kind.ARC, Tip.VOID_SMOKE);
        reg(DimensionalSwordItem.class,   0x9B6BFF, 2.2,  150f, 7, 1.0f,  Kind.ARC, Tip.VOID_SMOKE);
        reg(SpiritSwordItem.class,        0x7DF9FF, 2.4,  155f, 7, 1.1f,  Kind.ARC, Tip.SPARK);
        // Polearms: spear and trident stab, the rest sweep with their real length.
        reg(EvilSpearItem.class,          0xB0003A, 2.7,    0f, 5, 1.0f,  Kind.THRUST, Tip.NONE);
        reg(DevilTridentItem.class,       0xFF2244, 2.7,    0f, 5, 1.0f,  Kind.THRUST, Tip.NONE);
        reg(SickleOfSorrowItem.class,     0x8B0000, 2.3,  185f, 7, 1.0f,  Kind.ARC, Tip.NONE);
        reg(OxKingsAxeItem.class,         0xFF6A2B, 2.4,  150f, 8, 1.05f, Kind.ARC, Tip.EMBER);
        reg(PowerPoleItem.class,          0xFF5040, 2.9,  130f, 5, 0.9f,  Kind.ARC, Tip.NONE);
        // Staff, fan, blaster: previously no attack visual at all.
        reg(WhisStaffItem.class,          0xFFE9A8, 2.6,  120f, 6, 0.9f,  Kind.ARC, Tip.SPARK);
        reg(BanshoFanItem.class,          0xCFF5EE, 2.6,  200f, 8, 0.7f,  Kind.FAN, Tip.WIND);
        reg(BlasterGunItem.class,         0xFFB347, 8.0,    0f, 3, 1.0f,  Kind.FLASH, Tip.SPARK);
    }

    /** The attack visual profile for an item, or null when it has none. */
    public static Profile forItem(Item item) {
        return item == null ? null : BY_CLASS.get(item.getClass());
    }
}
