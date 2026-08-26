package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Race;
import com.dragonblockarcanedba.registry.Form;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;

import java.util.HashSet;
import java.util.Set;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void dba$init(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.TrailingTailLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.RaceFeatureLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void dba$extractState(net.minecraft.world.entity.Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (state instanceof com.dragonblockarcanedba.client.render.layer.DbaPlayerState dbaState && avatar instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            dbaState.dba$extractFromPlayer(player, partialTicks);
        }
    }

    @Unique
    private static final Set<Identifier> dba$checkedMissing = new HashSet<>();

    private static final Identifier DEFAULT_STEVE = Identifier.parse("minecraft:textures/entity/player/wide/steve.png");

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void dba$getTextureLocation(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            net.minecraft.world.entity.Entity entity = level.getEntity(state.id);
            if (entity instanceof PlayerStatsAccessor accessor) {
                Identifier raceId = accessor.dba$getRaceId();
                String racePath = raceId != null ? raceId.getPath().toLowerCase() : "human";

                // Human pick always uses player's own skin
                if ("human".equals(racePath)) {
                    return; // Let vanilla return state.skin.texture()
                }

                // Check form override texture first if available
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

                // Check custom race texture if available
                Race race = DbaRegistries.getRace(raceId);
                if (race != null && race.getBaseTexture() != null) {
                    Identifier tex = race.getBaseTexture();
                    if (dba$isTextureAvailable(tex)) {
                        cir.setReturnValue(tex);
                        return;
                    }
                }

                // Non-human placeholder: default MC person (Steve)
                cir.setReturnValue(DEFAULT_STEVE);
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
