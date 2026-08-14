package com.dragonblockarcanedba.mixin;

import com.dragonblockarcanedba.item.CurseBladeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class ClientLevelWeatherMixin {

    private boolean dba$isCurseStormActive() {
        Level level = (Level) (Object) this;
        if (!level.isClientSide()) return false;
        
        for (Player p : level.players()) {
            if (p.isUsingItem() && p.getUseItem().getItem() instanceof CurseBladeItem) {
                // If the player is channeling within 60 blocks, we render storm
                Player localPlayer = net.minecraft.client.Minecraft.getInstance().player;
                if (localPlayer != null && p.distanceToSqr(localPlayer) < 3600) {
                    return true;
                }
            }
        }
        return false;
    }

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void dba$getRainLevel(float f, CallbackInfoReturnable<Float> cir) {
        if (dba$isCurseStormActive()) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void dba$getThunderLevel(float f, CallbackInfoReturnable<Float> cir) {
        if (dba$isCurseStormActive()) {
            cir.setReturnValue(1.0f);
        }
    }
}
