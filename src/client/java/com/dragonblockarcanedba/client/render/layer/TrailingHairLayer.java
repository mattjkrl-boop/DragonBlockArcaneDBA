package com.dragonblockarcanedba.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal Dragon Ball Trailing Hair Layer.
 * - Authentically anchored to the humanoid skull.
 * - Forward-kinematic multi-joint hair spikes for Saiyan, Half-Saiyan, and Super Saiyan 3 mane.
 * - Dynamic head-movement reaction: head yaw turning velocity and pitch nodding whip and sway the hair.
 * - Locomotion stride bounce, vertical inertia, and high-speed slipstream streamlining.
 * - Transformation Ki aura surge oscillation and dynamic live hair coloring.
 */
public class TrailingHairLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {

    private static final Identifier WHITE_TEXTURE =
            Identifier.parse("dragonblockarcanedba:textures/entity/ki_white.png");

    private static final Map<Integer, Float> PREV_HEAD_YAW = new ConcurrentHashMap<>();
    private static final Map<Integer, Float> PREV_HEAD_PITCH = new ConcurrentHashMap<>();
    private static final Map<Integer, Float> SMOOTHED_HEAD_YAW_DRAG = new ConcurrentHashMap<>();
    private static final Map<Integer, Float> SMOOTHED_HEAD_PITCH_DRAG = new ConcurrentHashMap<>();

    // Saiyan Hair Segments
    private final ModelPart[] saiyanCrownSpike;
    private final ModelPart[] saiyanLeftSpike;
    private final ModelPart[] saiyanRightSpike;
    private final ModelPart[] saiyanLeftFlare;
    private final ModelPart[] saiyanRightFlare;
    private final ModelPart[] saiyanOccipital;
    private final ModelPart saiyanLeftBang;
    private final ModelPart saiyanRightBang;

    // Half-Saiyan Hair Segments
    private final ModelPart[] halfSaiyanMainBang;
    private final ModelPart[] halfSaiyanRightBang;
    private final ModelPart[] halfSaiyanCrown;
    private final ModelPart[] halfSaiyanLeftFlare;
    private final ModelPart[] halfSaiyanRightFlare;
    private final ModelPart[] halfSaiyanBack;

    // SSJ3 Flowing Mane Segments
    private final ModelPart[] ssj3ManeCenter;
    private final ModelPart[] ssj3ManeLeft;
    private final ModelPart[] ssj3ManeRight;

    public TrailingHairLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);

        // 1. Saiyan Spikes
        this.saiyanCrownSpike = new ModelPart[3];
        this.saiyanCrownSpike[0] = createBoxPart("saiyan_crown_0", -2.5F, -3.0F, -2.5F, 5.0F, 3.0F, 5.0F);
        this.saiyanCrownSpike[1] = createBoxPart("saiyan_crown_1", -2.0F, -3.5F, -2.0F, 4.0F, 3.5F, 4.0F);
        this.saiyanCrownSpike[2] = createBoxPart("saiyan_crown_2", -1.2F, -3.0F, -1.2F, 2.4F, 3.0F, 2.4F);

        this.saiyanLeftSpike = new ModelPart[2];
        this.saiyanLeftSpike[0] = createBoxPart("saiyan_l_0", -2.0F, -2.8F, -2.0F, 4.0F, 2.8F, 4.0F);
        this.saiyanLeftSpike[1] = createBoxPart("saiyan_l_1", -1.5F, -3.2F, -1.5F, 3.0F, 3.2F, 3.0F);

        this.saiyanRightSpike = new ModelPart[2];
        this.saiyanRightSpike[0] = createBoxPart("saiyan_r_0", -2.0F, -2.8F, -2.0F, 4.0F, 2.8F, 4.0F);
        this.saiyanRightSpike[1] = createBoxPart("saiyan_r_1", -1.5F, -3.2F, -1.5F, 3.0F, 3.2F, 3.0F);

        this.saiyanLeftFlare = new ModelPart[2];
        this.saiyanLeftFlare[0] = createBoxPart("saiyan_lf_0", -1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F);
        this.saiyanLeftFlare[1] = createBoxPart("saiyan_lf_1", -2.8F, -1.2F, -1.2F, 2.8F, 2.4F, 2.4F);

        this.saiyanRightFlare = new ModelPart[2];
        this.saiyanRightFlare[0] = createBoxPart("saiyan_rf_0", -1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F);
        this.saiyanRightFlare[1] = createBoxPart("saiyan_rf_1", 0.0F, -1.2F, -1.2F, 2.8F, 2.4F, 2.4F);

        this.saiyanOccipital = new ModelPart[2];
        this.saiyanOccipital[0] = createBoxPart("saiyan_occ_0", -2.5F, -2.0F, 0.0F, 5.0F, 4.0F, 3.0F);
        this.saiyanOccipital[1] = createBoxPart("saiyan_occ_1", -1.8F, -1.5F, 0.0F, 3.6F, 3.0F, 3.0F);

        this.saiyanLeftBang = createBoxPart("saiyan_lbang", -1.5F, 0.0F, -1.5F, 3.0F, 3.5F, 2.5F);
        this.saiyanRightBang = createBoxPart("saiyan_rbang", -1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 2.5F);

        // 2. Half-Saiyan Spikes (Gohan / Trunks)
        this.halfSaiyanMainBang = new ModelPart[3];
        this.halfSaiyanMainBang[0] = createBoxPart("hs_mbang_0", -1.5F, 0.0F, -1.5F, 3.0F, 2.5F, 3.0F);
        this.halfSaiyanMainBang[1] = createBoxPart("hs_mbang_1", -1.2F, 0.0F, -1.2F, 2.4F, 3.0F, 2.4F);
        this.halfSaiyanMainBang[2] = createBoxPart("hs_mbang_2", -0.8F, 0.0F, -0.8F, 1.6F, 2.5F, 1.6F);

        this.halfSaiyanRightBang = new ModelPart[2];
        this.halfSaiyanRightBang[0] = createBoxPart("hs_rbang_0", -1.2F, 0.0F, -1.2F, 2.4F, 2.2F, 2.4F);
        this.halfSaiyanRightBang[1] = createBoxPart("hs_rbang_1", -0.8F, 0.0F, -0.8F, 1.6F, 2.0F, 1.6F);

        this.halfSaiyanCrown = new ModelPart[2];
        this.halfSaiyanCrown[0] = createBoxPart("hs_crown_0", -2.0F, -3.5F, -2.0F, 4.0F, 3.5F, 4.0F);
        this.halfSaiyanCrown[1] = createBoxPart("hs_crown_1", -1.2F, -3.0F, -1.2F, 2.4F, 3.0F, 2.4F);

        this.halfSaiyanLeftFlare = new ModelPart[2];
        this.halfSaiyanLeftFlare[0] = createBoxPart("hs_l_0", -1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F);
        this.halfSaiyanLeftFlare[1] = createBoxPart("hs_l_1", -2.5F, -1.0F, -1.0F, 2.5F, 2.0F, 2.0F);

        this.halfSaiyanRightFlare = new ModelPart[2];
        this.halfSaiyanRightFlare[0] = createBoxPart("hs_r_0", -1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F);
        this.halfSaiyanRightFlare[1] = createBoxPart("hs_r_1", 0.0F, -1.0F, -1.0F, 2.5F, 2.0F, 2.0F);

        this.halfSaiyanBack = new ModelPart[2];
        this.halfSaiyanBack[0] = createBoxPart("hs_b_0", -2.2F, -2.0F, 0.0F, 4.4F, 3.5F, 2.5F);
        this.halfSaiyanBack[1] = createBoxPart("hs_b_1", -1.5F, -1.5F, 0.0F, 3.0F, 2.5F, 2.5F);

        // 3. SSJ3 Flowing Mane Segments
        this.ssj3ManeCenter = new ModelPart[5];
        for (int i = 0; i < 5; i++) {
            float width = Math.max(2.0F, 4.8F - i * 0.6F);
            float depth = Math.max(2.0F, 3.6F - i * 0.4F);
            this.ssj3ManeCenter[i] = createBoxPart("ssj3_c_" + i, -width / 2.0F, 0.0F, 0.0F, width, 3.5F, depth);
        }

        this.ssj3ManeLeft = new ModelPart[4];
        for (int i = 0; i < 4; i++) {
            float width = Math.max(1.8F, 3.8F - i * 0.5F);
            this.ssj3ManeLeft[i] = createBoxPart("ssj3_l_" + i, -width / 2.0F, 0.0F, 0.0F, width, 3.2F, 2.8F);
        }

        this.ssj3ManeRight = new ModelPart[4];
        for (int i = 0; i < 4; i++) {
            float width = Math.max(1.8F, 3.8F - i * 0.5F);
            this.ssj3ManeRight[i] = createBoxPart("ssj3_r_" + i, -width / 2.0F, 0.0F, 0.0F, width, 3.2F, 2.8F);
        }
    }

    private ModelPart createBoxPart(String name, float x, float y, float z, float w, float h, float d) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder builder = CubeListBuilder.create().texOffs(0, 0).addBox(x, y, z, w, h, d);
        root.addOrReplaceChild(name, builder, PartPose.ZERO);
        return root.bake(64, 64);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            AvatarRenderState state,
            float limbSwing,
            float limbSwingAmount
    ) {
        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        DbaPlayerState dbaState = (state instanceof DbaPlayerState ds) ? ds : null;

        String race = "saiyan";
        int hairColor = 0xFF151515;
        Identifier activeFormId = null;

        if (dbaState != null) {
            Identifier rId = dbaState.dba$getRaceId();
            if (rId != null) {
                race = rId.getPath().toLowerCase();
            }
            activeFormId = dbaState.dba$getActiveFormId();
            int hColor = dbaState.dba$getHairColor();
            if (hColor != 0) hairColor = hColor;
        }

        // Only render coded hair for Saiyan, Half-Saiyan, and Human when not using 3D OBJ model layer
        if (com.dragonblockarcanedba.client.render.model.Custom3DModelRegistry.hasModelForRace(race)) {
            return;
        }
        if (!race.contains("saiyan") && !race.contains("half_saiyan") && !race.equals("human")) {
            return;
        }

        // Active transformation hair color overrides
        if (activeFormId != null) {
            com.dragonblockarcanedba.registry.Form form = com.dragonblockarcanedba.registry.DbaRegistries.getForm(activeFormId);
            if (form != null && form.getHairColorOverride() != null) {
                try {
                    String hex = form.getHairColorOverride();
                    if (hex.startsWith("#")) hex = hex.substring(1);
                    hairColor = 0xFF000000 | Integer.parseInt(hex, 16);
                } catch (Exception ignored) {}
            }
        }

        // Time source guaranteed to advance smoothly even in paused GUI screens
        float age = (dbaState != null && dbaState.dba$getTailAgeInTicks() > 0)
                ? dbaState.dba$getTailAgeInTicks()
                : (System.currentTimeMillis() / 50.0F);

        // 1. Dynamic Head Yaw Turning Inertia
        float currentHeadYaw = state.yRot;
        Float prevYaw = PREV_HEAD_YAW.put(state.id, currentHeadYaw);
        float deltaHeadYaw = 0.0F;
        if (prevYaw != null) {
            deltaHeadYaw = Mth.wrapDegrees(currentHeadYaw - prevYaw);
            if (Math.abs(deltaHeadYaw) > 50.0F) deltaHeadYaw = 0.0F;
        }
        float targetYawDrag = -Mth.clamp(deltaHeadYaw * 0.009F, -0.22F, 0.22F);
        float prevYawDrag = SMOOTHED_HEAD_YAW_DRAG.getOrDefault(state.id, 0.0F);
        float smoothedHeadYawDrag = Mth.lerp(0.14F, prevYawDrag, targetYawDrag);
        SMOOTHED_HEAD_YAW_DRAG.put(state.id, smoothedHeadYawDrag);

        // 2. Dynamic Head Pitch Nodding Inertia
        float currentHeadPitch = state.xRot;
        Float prevPitch = PREV_HEAD_PITCH.put(state.id, currentHeadPitch);
        float deltaHeadPitch = 0.0F;
        if (prevPitch != null) {
            deltaHeadPitch = Mth.wrapDegrees(currentHeadPitch - prevPitch);
            if (Math.abs(deltaHeadPitch) > 50.0F) deltaHeadPitch = 0.0F;
        }
        float targetPitchDrag = -Mth.clamp(deltaHeadPitch * 0.009F, -0.20F, 0.20F);
        float prevPitchDrag = SMOOTHED_HEAD_PITCH_DRAG.getOrDefault(state.id, 0.0F);
        float smoothedHeadPitchDrag = Mth.lerp(0.14F, prevPitchDrag, targetPitchDrag);
        SMOOTHED_HEAD_PITCH_DRAG.put(state.id, smoothedHeadPitchDrag);

        // Clean up entity caches periodically
        if (PREV_HEAD_YAW.size() > 64) {
            PREV_HEAD_YAW.clear();
            PREV_HEAD_PITCH.clear();
            SMOOTHED_HEAD_YAW_DRAG.clear();
            SMOOTHED_HEAD_PITCH_DRAG.clear();
        }

        // Locomotion & Inertia
        float walkBounce = Mth.sin(limbSwing * 1.2F) * (limbSwingAmount * 0.045F);
        float walkSway = Mth.cos(limbSwing * 0.6F) * (limbSwingAmount * 0.030F);
        float vertVel = dbaState != null ? dbaState.dba$getLocalVelocityY() : 0.0F;
        float vertInertia = -Mth.clamp(vertVel * 0.06F, -0.12F, 0.12F);
        boolean isSprinting = dbaState != null && dbaState.dba$isSprinting();
        float speedDrag = isSprinting ? -0.05F : 0.0F;

        // Transformation Ki Surge / Idle Wave
        boolean isTransformed = activeFormId != null;
        float kiFreq = isTransformed ? 0.28F : 0.075F;
        float kiAmp = isTransformed ? 0.055F : 0.022F;
        float auraFlutter = isTransformed ? (Mth.sin(age * 0.50F) * 0.025F) : 0.0F;

        final int finalHairColor = hairColor;
        final String finalRace = race;
        final Identifier finalActiveFormId = activeFormId;

        final float finalYawDrag = smoothedHeadYawDrag;
        final float finalPitchDrag = smoothedHeadPitchDrag;
        final float finalWalkBounce = walkBounce;
        final float finalWalkSway = walkSway;
        final float finalVertInertia = vertInertia;
        final float finalSpeedDrag = speedDrag;
        final float finalAge = age;
        final float finalKiFreq = kiFreq;
        final float finalKiAmp = kiAmp;
        final float finalAuraFlutter = auraFlutter;
        final boolean finalIsTransformed = isTransformed;

        final int renderLight = Math.max(packedLight, isTransformed ? 0x00F000F0 : 0x00D000D0);
        RenderType renderType = RenderTypes.entitySolid(WHITE_TEXTURE);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            stack.pushPose();
            humanoidModel.head.translateAndRotate(stack);

            if (finalRace.contains("half_saiyan")) {
                renderHalfSaiyanHair(stack, buffer, renderLight, finalHairColor,
                        finalYawDrag, finalPitchDrag, finalWalkBounce, finalWalkSway,
                        finalVertInertia, finalSpeedDrag, finalAge, finalKiFreq, finalKiAmp,
                        finalAuraFlutter, finalIsTransformed);
            } else {
                renderSaiyanHair(stack, buffer, renderLight, finalHairColor,
                        finalYawDrag, finalPitchDrag, finalWalkBounce, finalWalkSway,
                        finalVertInertia, finalSpeedDrag, finalAge, finalKiFreq, finalKiAmp,
                        finalAuraFlutter, finalIsTransformed);
            }

            // SSJ3 Flowing Mane
            if (finalActiveFormId != null && finalActiveFormId.getPath().contains("super_saiyan_3")) {
                renderSSJ3Mane(stack, buffer, renderLight, finalHairColor,
                        finalYawDrag, finalPitchDrag, finalWalkBounce, finalWalkSway,
                        finalVertInertia, finalSpeedDrag, finalAge, finalKiFreq, finalKiAmp);
            }

            stack.popPose();
        });
    }

    private void renderSaiyanHair(
            PoseStack stack, VertexConsumer buffer,
            int light, int color, float yawDrag, float pitchDrag, float walkBounce,
            float walkSway, float vertInertia, float speedDrag, float age,
            float kiFreq, float kiAmp, float auraFlutter, boolean transformed
    ) {
        // 1. Central Soaring Crown Spike
        stack.pushPose();
        stack.translate(0.0F, -0.48F, 0.03F);
        for (int i = 0; i < 3; i++) {
            float prog = i / 2.0F;
            float wave = Mth.sin(age * kiFreq - prog * 1.5F) * (kiAmp * (1.0F + prog));
            float yRot = (yawDrag + walkSway + wave * 0.7F) * (1.0F + prog * 0.8F);
            float xRot = -0.32F + (pitchDrag + vertInertia + walkBounce + speedDrag + auraFlutter) * (1.0F + prog * 0.6F);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            this.saiyanCrownSpike[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, -0.18F, 0.02F);
        }
        stack.popPose();

        // 2. Left Crown Spike
        stack.pushPose();
        stack.translate(-0.16F, -0.46F, 0.05F);
        for (int i = 0; i < 2; i++) {
            float prog = i;
            float wave = Mth.sin(age * kiFreq + 1.2F - prog * 1.5F) * kiAmp;
            float yRot = -0.32F + (yawDrag + wave) * (1.0F + prog * 0.7F);
            float xRot = -0.28F + (pitchDrag + vertInertia + speedDrag) * (1.0F + prog * 0.5F);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            stack.mulPose(com.mojang.math.Axis.ZP.rotation(-0.25F));
            this.saiyanLeftSpike[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, -0.16F, 0.02F);
        }
        stack.popPose();

        // 3. Right Crown Spike
        stack.pushPose();
        stack.translate(0.16F, -0.46F, 0.05F);
        for (int i = 0; i < 2; i++) {
            float prog = i;
            float wave = Mth.sin(age * kiFreq + 2.4F - prog * 1.5F) * kiAmp;
            float yRot = 0.32F + (yawDrag + wave) * (1.0F + prog * 0.7F);
            float xRot = -0.28F + (pitchDrag + vertInertia + speedDrag) * (1.0F + prog * 0.5F);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            stack.mulPose(com.mojang.math.Axis.ZP.rotation(0.25F));
            this.saiyanRightSpike[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, -0.16F, 0.02F);
        }
        stack.popPose();

        // 4. Left Flare
        stack.pushPose();
        stack.translate(-0.25F, -0.36F, 0.0F);
        stack.mulPose(com.mojang.math.Axis.ZP.rotation(-0.50F + (yawDrag * 0.8F)));
        this.saiyanLeftFlare[0].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        this.saiyanLeftFlare[1].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();

        // 5. Right Flare
        stack.pushPose();
        stack.translate(0.25F, -0.36F, 0.0F);
        stack.mulPose(com.mojang.math.Axis.ZP.rotation(0.50F + (yawDrag * 0.8F)));
        this.saiyanRightFlare[0].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        this.saiyanRightFlare[1].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();

        // 6. Occipital Spikes
        stack.pushPose();
        stack.translate(0.0F, -0.32F, 0.22F);
        float occPitch = 0.35F + (pitchDrag + speedDrag * 1.5F + vertInertia);
        stack.mulPose(com.mojang.math.Axis.XP.rotation(occPitch));
        this.saiyanOccipital[0].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.translate(0.0F, 0.0F, 0.16F);
        this.saiyanOccipital[1].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();

        // 7. Bangs
        stack.pushPose();
        stack.translate(-0.12F, -0.38F, -0.26F);
        stack.mulPose(com.mojang.math.Axis.YP.rotation(0.12F + yawDrag * 1.2F));
        this.saiyanLeftBang.render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();

        stack.pushPose();
        stack.translate(0.12F, -0.38F, -0.26F);
        stack.mulPose(com.mojang.math.Axis.YP.rotation(-0.12F + yawDrag * 1.2F));
        this.saiyanRightBang.render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();
    }

    private void renderHalfSaiyanHair(
            PoseStack stack, VertexConsumer buffer,
            int light, int color, float yawDrag, float pitchDrag, float walkBounce,
            float walkSway, float vertInertia, float speedDrag, float age,
            float kiFreq, float kiAmp, float auraFlutter, boolean transformed
    ) {
        // 1. Long Iconic Front-Left Sweeping Bang (Teen Gohan / Trunks)
        stack.pushPose();
        stack.translate(-0.10F, -0.42F, -0.25F);
        for (int i = 0; i < 3; i++) {
            float prog = i / 2.0F;
            float wave = Mth.sin(age * kiFreq - prog * 1.8F) * (kiAmp * (1.2F + prog * 1.2F));
            float yRot = (yawDrag * 1.5F + walkSway * 1.2F + wave) * (1.0F + prog * 1.0F);
            float xRot = (pitchDrag * 1.3F + vertInertia * 1.2F + walkBounce + speedDrag) * (1.0F + prog * 0.8F);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            stack.mulPose(com.mojang.math.Axis.ZP.rotation(0.08F));
            this.halfSaiyanMainBang[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, 0.15F, -0.04F);
        }
        stack.popPose();

        // 2. Right Front Bang
        stack.pushPose();
        stack.translate(0.10F, -0.42F, -0.25F);
        for (int i = 0; i < 2; i++) {
            float prog = i;
            float wave = Mth.sin(age * kiFreq + 1.5F - prog * 1.5F) * kiAmp;
            float yRot = (-0.10F + yawDrag * 1.2F + wave);
            float xRot = (pitchDrag + vertInertia + walkBounce);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            this.halfSaiyanRightBang[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, 0.12F, -0.03F);
        }
        stack.popPose();

        // 3. Elevated Crown Crest Spike
        stack.pushPose();
        stack.translate(0.04F, -0.48F, -0.02F);
        for (int i = 0; i < 2; i++) {
            float prog = i;
            float wave = Mth.sin(age * kiFreq + 0.8F - prog * 1.5F) * kiAmp;
            float yRot = (yawDrag + walkSway + wave);
            float xRot = -0.30F + (pitchDrag + vertInertia + speedDrag + auraFlutter);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            this.halfSaiyanCrown[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, -0.18F, 0.02F);
        }
        stack.popPose();

        // 4. Left & Right Flares
        stack.pushPose();
        stack.translate(-0.24F, -0.36F, 0.0F);
        stack.mulPose(com.mojang.math.Axis.ZP.rotation(-0.45F + yawDrag * 0.7F));
        this.halfSaiyanLeftFlare[0].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        this.halfSaiyanLeftFlare[1].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();

        stack.pushPose();
        stack.translate(0.24F, -0.36F, 0.0F);
        stack.mulPose(com.mojang.math.Axis.ZP.rotation(0.45F + yawDrag * 0.7F));
        this.halfSaiyanRightFlare[0].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        this.halfSaiyanRightFlare[1].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();

        // 5. Back Occipital Layer
        stack.pushPose();
        stack.translate(0.0F, -0.32F, 0.20F);
        float occPitch = 0.30F + (pitchDrag + speedDrag + vertInertia);
        stack.mulPose(com.mojang.math.Axis.XP.rotation(occPitch));
        this.halfSaiyanBack[0].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.translate(0.0F, 0.0F, 0.14F);
        this.halfSaiyanBack[1].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
        stack.popPose();
    }

    private void renderSSJ3Mane(
            PoseStack stack, VertexConsumer buffer,
            int light, int color, float yawDrag, float pitchDrag, float walkBounce,
            float walkSway, float vertInertia, float speedDrag, float age,
            float kiFreq, float kiAmp
    ) {
        // Center Flowing Mane (5 chained cascading segments)
        stack.pushPose();
        stack.translate(0.0F, -0.22F, 0.24F);
        for (int i = 0; i < 5; i++) {
            float prog = i / 4.0F;
            float wave = Mth.sin(age * 0.10F - prog * 2.2F) * (0.04F * (1.0F + prog * 1.5F));
            float yRot = (yawDrag * 1.4F + walkSway * 1.5F + wave) * (1.0F + prog * 0.8F);
            float basePitch = 0.18F + prog * 0.08F;
            float xRot = basePitch + (pitchDrag * 1.2F + vertInertia * 1.5F + speedDrag * 2.0F + walkBounce);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            this.ssj3ManeCenter[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, 0.20F, 0.03F);
        }
        stack.popPose();

        // Left Mane Stream
        stack.pushPose();
        stack.translate(-0.16F, -0.18F, 0.22F);
        for (int i = 0; i < 4; i++) {
            float prog = i / 3.0F;
            float wave = Mth.sin(age * 0.10F + 0.5F - prog * 2.0F) * 0.04F;
            float yRot = -0.12F + (yawDrag * 1.3F + wave);
            float xRot = 0.16F + (pitchDrag + vertInertia + speedDrag * 1.8F);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            this.ssj3ManeLeft[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, 0.18F, 0.02F);
        }
        stack.popPose();

        // Right Mane Stream
        stack.pushPose();
        stack.translate(0.16F, -0.18F, 0.22F);
        for (int i = 0; i < 4; i++) {
            float prog = i / 3.0F;
            float wave = Mth.sin(age * 0.10F + 1.0F - prog * 2.0F) * 0.04F;
            float yRot = 0.12F + (yawDrag * 1.3F + wave);
            float xRot = 0.16F + (pitchDrag + vertInertia + speedDrag * 1.8F);
            stack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            stack.mulPose(com.mojang.math.Axis.XP.rotation(xRot));
            this.ssj3ManeRight[i].render(stack, buffer, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, color);
            stack.translate(0.0F, 0.18F, 0.02F);
        }
        stack.popPose();
    }
}
