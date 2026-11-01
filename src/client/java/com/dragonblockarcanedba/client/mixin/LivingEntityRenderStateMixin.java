package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.client.render.layer.DbaTumbleState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements DbaTumbleState {

    @Unique
    private float dba$tumble = 0f;

    @Override
    public float dba$getTumble() {
        return this.dba$tumble;
    }

    @Override
    public void dba$setTumble(float strength) {
        this.dba$tumble = strength;
    }
}
