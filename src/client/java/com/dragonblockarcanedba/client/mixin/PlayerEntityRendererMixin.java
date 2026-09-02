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
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.TrailingHairLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.RaceFeatureLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
        ((LivingEntityRendererInvoker) this).dba$addLayer(new com.dragonblockarcanedba.client.render.layer.BlinkingEyesLayer((net.minecraft.client.renderer.entity.RenderLayerParent)(Object)this));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void dba$extractState(net.minecraft.world.entity.Avatar avatar, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (state instanceof com.dragonblockarcanedba.client.render.layer.DbaPlayerState dbaState && avatar instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            dbaState.dba$extractFromPlayer(player, partialTicks);

            // Suppress vanilla cuboid model parts if the race uses a custom 3D OBJ mesh (Yardrat, Saiyan, Half-Saiyan)
            Identifier raceId = dbaState.dba$getRaceId();
            String race = (raceId != null) ? raceId.getPath().toLowerCase() : "";
            boolean is3D = com.dragonblockarcanedba.client.render.model.Custom3DModelRegistry.hasModelForRace(race);
            var model = ((AvatarRenderer<?>)(Object) this).getModel();

            if (is3D) {
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
            } else {
                model.head.visible = true;
                model.hat.visible = true;
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
                model.leftSleeve.visible = true;
                model.rightSleeve.visible = true;
                model.leftPants.visible = true;
                model.rightPants.visible = true;
                model.jacket.visible = true;
            }
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

        if (!com.dragonblockarcanedba.client.render.model.Custom3DModelRegistry.hasModelForRace(race)) {
            return;
        }

        com.dragonblockarcanedba.client.render.model.ObjMesh mesh =
                com.dragonblockarcanedba.client.render.model.Custom3DModelRegistry.getModelForRace(race);
        if (mesh == null) return;

        var playerModel = ((AvatarRenderer<?>)(Object) this).getModel();
        boolean isRight = (armPart == playerModel.rightArm);
        var armLimb = mesh.getLimb(isRight ? "rightarm" : "leftarm");
        if (armLimb == null) return;

        int skinColor = 0xFFE0BD;
        int hairColor = 0xFF1EB4FF;
        String hex = accessor.dba$getSkinColor();
        if (hex != null && !hex.isEmpty()) {
            try {
                if (hex.startsWith("#")) hex = hex.substring(1);
                skinColor = Integer.parseInt(hex, 16);
            } catch (Exception ignored) {}
        }
        String hHex = accessor.dba$getHairColor();
        if (hHex != null && !hHex.isEmpty()) {
            try {
                if (hHex.startsWith("#")) hHex = hHex.substring(1);
                hairColor = Integer.parseInt(hHex, 16);
            } catch (Exception ignored) {}
        }

        String eHex = accessor.dba$getEyeColor();
        int eyeColor = 0xFFFFFFFF;
        if (eHex != null && !eHex.isEmpty()) {
            try {
                if (eHex.startsWith("#")) eHex = eHex.substring(1);
                eyeColor = Integer.parseInt(eHex, 16);
            } catch (Exception ignored) {}
        }

        Identifier modelTexture = com.dragonblockarcanedba.client.render.DynamicSkinManager.getOrGenerateSkin(race, skinColor, hairColor, eyeColor);
        if (modelTexture == null) {
            modelTexture = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/player/" + race + "_base.png");
        }

        RenderType renderType = RenderTypes.entityCutout(modelTexture, false);

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
            armLimb.render(stack.last().pose(), buffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
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
            int eye = dbaState.dba$getEyeColor();
            Identifier raceId = dbaState.dba$getRaceId();
            String racePath = raceId != null ? raceId.getPath() : "universal_humanoid";
            cir.setReturnValue(com.dragonblockarcanedba.client.render.DynamicSkinManager.getOrGenerateSkin(racePath, skin, hair, eye));
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
