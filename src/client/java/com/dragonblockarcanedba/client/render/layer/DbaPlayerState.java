package com.dragonblockarcanedba.client.render.layer;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

public interface DbaPlayerState {
    void dba$extractFromPlayer(AbstractClientPlayer player, float partialTicks);
    boolean dba$hasTail();
    float dba$getTailAgeInTicks();
    double[] dba$getTailLatencyPos(int bufferOffset, float partialTicks);

    boolean dba$isSprinting();
    boolean dba$isCrouching();
    boolean dba$isSwimming();
    boolean dba$isFlying();
    float dba$getHorizontalSpeed();
    float dba$getYawVelocity();
    float dba$getBodyYawVelocity();
    float dba$getLocalVelocityX();
    float dba$getLocalVelocityZ();
    float dba$getLocalVelocityY();
    float dba$getHeadYawRel();

    Identifier dba$getRaceId();
    Identifier dba$getActiveFormId();
    int dba$getSkinColor();
    int dba$getHairColor();
    boolean dba$isInOtherworld();
    String dba$getActiveEmote();
}

