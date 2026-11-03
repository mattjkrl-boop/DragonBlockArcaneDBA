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
    boolean dba$isOnGround();
    float dba$getHorizontalSpeed();
    float dba$getYawVelocity();
    float dba$getBodyYawVelocity();
    float dba$getLocalVelocityX();
    /** Unified flight bank in degrees, computed for every race and form from turn rate and lateral velocity. */
    float dba$getFlightBank();
    float dba$getLocalVelocityZ();
    float dba$getLocalVelocityY();
    float dba$getHeadYawRel();
    boolean dba$isOnLadder();

    Identifier dba$getRaceId();
    Identifier dba$getActiveFormId();
    int dba$getSkinColor();
    int dba$getHairColor();
    int dba$getEyeColor();
    boolean dba$isInOtherworld();
    String dba$getActiveEmote();
    float dba$getPitchVelocity();
    float dba$getHeadPitch();
}

