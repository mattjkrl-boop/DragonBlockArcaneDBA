package com.dragonblockarcanedba.mixin.client;

import com.dragonblockarcanedba.client.SaberPhasedState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements SaberPhasedState {
    @Unique
    private boolean dba$isSaberPhased;

    @Override
    public boolean dba$isSaberPhased() {
        return this.dba$isSaberPhased;
    }

    @Override
    public void dba$setSaberPhased(boolean phased) {
        this.dba$isSaberPhased = phased;
    }
}
