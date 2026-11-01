package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.render.layer.DbaStaggerState;
import com.dragonblockarcanedba.client.render.layer.DbaTumbleState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements DbaTumbleState, DbaStaggerState {

    @Unique
    private float dba$tumble = 0f;

    @Unique
    private float dba$stagger = 0f;

    @Unique
    private float dba$staggerProgress = 1f;

    @Override
    public float dba$getTumble() {
        return this.dba$tumble;
    }

    @Override
    public void dba$setTumble(float strength) {
        this.dba$tumble = strength;
    }

    @Override
    public float dba$getStagger() {
        return this.dba$stagger;
    }

    @Override
    public void dba$setStagger(float strength) {
        this.dba$stagger = strength;
    }

    @Override
    public float dba$getStaggerProgress() {
        return this.dba$staggerProgress;
    }

    @Override
    public void dba$setStaggerProgress(float progress) {
        this.dba$staggerProgress = progress;
    }
}
