package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Race;
import com.dragonblockarcanedba.registry.Form;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void dba$getTextureLocation(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            net.minecraft.world.entity.Entity entity = level.getEntity(state.id);
            if (entity instanceof PlayerStatsAccessor accessor) {
                // Check form override texture first
                Identifier formId = accessor.dba$getActiveFormId();
                if (formId != null) {
                    Form form = DbaRegistries.getForm(formId);
                    if (form != null && form.getModelOverride() != null) {
                        cir.setReturnValue(form.getModelOverride());
                        return;
                    }
                }
                
                // Check race texture
                Identifier raceId = accessor.dba$getRaceId();
                Race race = DbaRegistries.getRace(raceId);
                if (race != null && race.getBaseTexture() != null) {
                    cir.setReturnValue(race.getBaseTexture());
                }
            }
        }
    }
}
