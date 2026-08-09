package com.dragonblockarcanedba.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

public interface DbaAttackerTracker {
    Player dba$getEffectInflictor(Holder<MobEffect> effect);
}
