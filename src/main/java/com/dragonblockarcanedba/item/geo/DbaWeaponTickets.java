package com.dragonblockarcanedba.item.geo;

import com.geckolib.constant.dataticket.DataTicket;

/**
 * Custom GeckoLib data tickets for weapon rendering. The client weapon renderer stamps these
 * into the render state from the actual holder of the item being rendered, and the animation
 * predicate in DbaGeoWeapon reads only these tickets. That is what makes weapon animation
 * per holder: a remote player's weapon animates from that player's own swinging and using
 * state on every client, instead of from the local player's state.
 */
public final class DbaWeaponTickets {
    private DbaWeaponTickets() {}

    /** True while the holder of the rendered stack is mid swing with it in either hand. */
    public static final DataTicket<Boolean> HOLDER_SWINGING = DataTicket.create("dba_holder_swinging", Boolean.class);

    /** Charge progress 0 to 1 while the holder is using (holding) the rendered stack, or -1 when not using it. */
    public static final DataTicket<Float> USE_RATIO = DataTicket.create("dba_use_ratio", Float.class);
}
