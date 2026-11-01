package com.dragonblockarcanedba.client.render.layer;

/**
 * Duck interface mixed into LivingEntityRenderState: carries the ragdoll-lite
 * tumble strength for an entity that died to a DBA hit (0 means no tumble).
 * Same pattern as DbaPlayerState on AvatarRenderState.
 */
public interface DbaTumbleState {
    float dba$getTumble();
    void dba$setTumble(float strength);
}
