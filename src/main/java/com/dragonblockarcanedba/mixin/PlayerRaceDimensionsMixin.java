package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.config.RaceConfig;
import com.dragonblockarcanedba.config.RaceConfigManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PlayerRaceDimensionsMixin {

    @Inject(method = "getDefaultDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;", at = @At("HEAD"), cancellable = true)
    private void dba$modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (pose == Pose.STANDING && (Object) this instanceof PlayerStatsAccessor accessor) {
            Identifier raceId = accessor.dba$getRaceId();
            if (raceId != null) {
                RaceConfig config = RaceConfigManager.getConfig(raceId.getPath());
                EntityDimensions defaultDim = EntityDimensions.scalable(config.hitboxWidth(), config.hitboxHeight()).withEyeHeight(config.eyeHeight());
                cir.setReturnValue(defaultDim);
            }
        }
    }
}
