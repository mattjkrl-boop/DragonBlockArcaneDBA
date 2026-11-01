package com.dragonblockarcanedba.client.render.layer;

/**
 * Duck interface mixed into LivingEntityRenderState: carries the boss stagger
 * strength and elapsed progress for an entity that took a heavy DBA hit
 * (strength 0 means no stagger). Same pattern as DbaTumbleState.
 */
public interface DbaStaggerState {
    float dba$getStagger();
    void dba$setStagger(float strength);
    float dba$getStaggerProgress();
    void dba$setStaggerProgress(float progress);
}
