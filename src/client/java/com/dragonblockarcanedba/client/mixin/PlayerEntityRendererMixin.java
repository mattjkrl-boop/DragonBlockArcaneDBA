package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Race;
import com.dragonblockarcanedba.registry.Form;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin {

    @Unique
    private static final Set<Identifier> dba$checkedMissing = new HashSet<>();

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void dba$getTextureLocation(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        // If GeckoLib is handling the player model rendering, skip this vanilla texture override.
        // GeckoLib resolves textures through DbaPlayerModel.getTextureResource() instead.
        // This mixin is kept as a fallback for cases where GeckoLib isn't rendering
        // (e.g., if GeckoLib fails to load or for other mods that check player texture).
        var level = Minecraft.getInstance().level;
        if (level != null) {
            net.minecraft.world.entity.Entity entity = level.getEntity(state.id);
            if (entity instanceof PlayerStatsAccessor accessor) {
                // Check form override texture first
                Identifier formId = accessor.dba$getActiveFormId();
                if (formId != null) {
                    Form form = DbaRegistries.getForm(formId);
                    if (form != null && form.getModelOverride() != null) {
                        Identifier tex = form.getModelOverride();
                        if (dba$isTextureAvailable(tex)) {
                            cir.setReturnValue(tex);
                            return;
                        }
                    }
                }
                
                // Check race texture
                Identifier raceId = accessor.dba$getRaceId();
                Race race = DbaRegistries.getRace(raceId);
                if (race != null && race.getBaseTexture() != null) {
                    Identifier tex = race.getBaseTexture();
                    if (dba$isTextureAvailable(tex)) {
                        cir.setReturnValue(tex);
                    }
                }
            }
        }
    }

    /**
     * Checks if a texture resource actually exists.
     * Caches misses to avoid spamming the resource manager every frame.
     */
    @Unique
    private static boolean dba$isTextureAvailable(Identifier texture) {
        if (texture == null) return false;
        // Vanilla textures always exist
        if ("minecraft".equals(texture.getNamespace())) return true;
        // Already known missing — skip
        if (dba$checkedMissing.contains(texture)) return false;

        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                return true;
            }
        } catch (Exception ignored) {}

        dba$checkedMissing.add(texture);
        return false;
    }
}
