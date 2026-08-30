package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
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

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.HashSet;
import java.util.Set;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("RETURN"))
    private void dba$init(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.Custom3DModelLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.TrailingTailLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.RaceFeatureLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void dba$extractState(net.minecraft.world.entity.Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (state instanceof com.dragonblockarcanedba.client.render.layer.DbaPlayerState dbaState && avatar instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            dbaState.dba$extractFromPlayer(player, partialTicks);

            // Suppress vanilla cuboid model parts so native 3D polygonal mesh renders cleanly
            var model = ((AvatarRenderer<?>)(Object) this).getModel();
            model.head.visible = false;
            model.hat.visible = false;
            model.body.visible = false;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;
            model.jacket.visible = false;
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void dba$renderCustomHand(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            Identifier texture,
            ModelPart armPart,
            boolean slim,
            CallbackInfo ci
    ) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var accessor = (PlayerStatsAccessor) mc.player;
        Identifier raceId = accessor.dba$getRaceId();
        String race = raceId != null ? raceId.getPath().toLowerCase() : "universal_humanoid";

        com.dragonblockarcanedba.client.render.model.ObjMesh mesh =
                com.dragonblockarcanedba.client.render.model.Custom3DModelRegistry.getModelForRace(race);
        if (mesh == null) return;

        var playerModel = ((AvatarRenderer<?>)(Object) this).getModel();
        boolean isRight = (armPart == playerModel.rightArm);
        var armLimb = mesh.getLimb(isRight ? "rightarm" : "leftarm");
        if (armLimb == null) return;

        int skinColor = 0xFFE0BD;
        String hex = accessor.dba$getSkinColor();
        if (hex != null && !hex.isEmpty()) {
            try {
                if (hex.startsWith("#")) hex = hex.substring(1);
                skinColor = Integer.parseInt(hex, 16);
            } catch (Exception ignored) {}
        }

        float r = ((skinColor >> 16) & 0xFF) / 255.0f;
        float g = ((skinColor >> 8) & 0xFF) / 255.0f;
        float b = (skinColor & 0xFF) / 255.0f;
        if (skinColor == 0) { r = 0.85f; g = 0.85f; b = 0.85f; }

        Identifier modelTexture = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/model_3d/texture.png");
        RenderType renderType = RenderTypes.entityCutout(modelTexture, false);

        final float finalR = r;
        final float finalG = g;
        final float finalB = b;

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());
            stack.pushPose();
            armPart.resetPose();
            armPart.xRot = 0.0f;
            armPart.yRot = 0.0f;
            armPart.zRot = isRight ? 0.1f : -0.1f;
            armPart.translateAndRotate(stack);
            armLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, finalR, finalG, finalB, 1.0f);
            stack.popPose();
        });

        ci.cancel();
    }

    @Unique
    private static final Set<Identifier> dba$checkedMissing = new HashSet<>();

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void dba$getTextureLocation(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            net.minecraft.world.entity.Entity entity = level.getEntity(state.id);
            if (entity instanceof PlayerStatsAccessor accessor) {
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
            }
        }

        // Physically recolored DBZ skin using exact user chosen skin and hair colors
        if (state instanceof com.dragonblockarcanedba.client.render.layer.DbaPlayerState dbaState) {
            int skin = dbaState.dba$getSkinColor();
            int hair = dbaState.dba$getHairColor();
            Identifier raceId = dbaState.dba$getRaceId();
            String racePath = raceId != null ? raceId.getPath() : "universal_humanoid";
            cir.setReturnValue(com.dragonblockarcanedba.client.render.DynamicSkinManager.getOrGenerateSkin(racePath, skin, hair));
        }
    }

    /**
     * Checks if a texture resource actually exists.
     * Caches misses to avoid spamming the resource manager every frame.
     */
    @Unique
    private static boolean dba$isTextureAvailable(Identifier texture) {
        if (texture == null) return false;
        if ("minecraft".equals(texture.getNamespace())) return true;
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
