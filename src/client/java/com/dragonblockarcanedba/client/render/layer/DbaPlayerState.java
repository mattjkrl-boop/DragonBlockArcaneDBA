package com.dragonblockarcanedba.client.render.layer;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

public interface DbaPlayerState {
    void dba$extractFromPlayer(AbstractClientPlayer player, float partialTicks);
    boolean dba$hasTail();
    float dba$getTailAgeInTicks();
    double[] dba$getTailLatencyPos(int bufferOffset, float partialTicks);

    Identifier dba$getRaceId();
    int dba$getSkinColor();
    int dba$getHairColor();
    boolean dba$isInOtherworld();
}
