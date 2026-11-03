package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.item.KatanaItem;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Basic-attack trails that are a weapon's own effect.
 *
 * The shared swing used to draw an arc for every weapon on top of that weapon's
 * real attack. That overlay is gone. The katana's white slash stays here because
 * it was made for the katana. Every other weapon shows only its own ability effect.
 */
public final class WeaponTrailProfiles {
    private WeaponTrailProfiles() {}

    public enum Kind { ARC, THRUST, FAN, FLASH }
    public enum Tip { NONE, SPARK, EMBER, VOID_SMOKE, WIND }

    public record Profile(int color, double reach, float sweepDeg, int fadeTicks, float alpha, Kind kind, Tip tip) {}

    private static final Map<Class<? extends Item>, Profile> BY_CLASS = new IdentityHashMap<>();

    static {
        BY_CLASS.put(KatanaItem.class, new Profile(0xFFFFFF, 2.0, 150f, 5, 1.0f, Kind.ARC, Tip.NONE));
    }

    /** The attack visual profile for an item, or null when the weapon has no basic-attack trail. */
    public static Profile forItem(Item item) {
        return item == null ? null : BY_CLASS.get(item.getClass());
    }
}
