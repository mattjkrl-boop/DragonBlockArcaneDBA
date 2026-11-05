package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.item.geo.DbaGeoWeapon;
import net.minecraft.world.item.Item;

/**
 * Plain item after the weapon attack strip (Nov 4, 2026). The weapon still
 * exists in hand and in the creative menu with its geo model and texture;
 * every ability behavior (use actions, charges, stances, custom melee,
 * Ki costs, cooldowns) was removed and will be rebuilt fresh from the
 * saved attack designs.
 */
public class WhisStaffItem extends Item implements DbaGeoWeapon {
    public WhisStaffItem(Properties properties) {
        super(properties);
    }
}
