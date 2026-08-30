package com.dragonblockarcanedba.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
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

/**
 * Universal Dragon Ball Trailing Tail Layer.
 * - Authentically anchored at the sacrum/lower back (Z = +2px, Y = +10.5px).
 * - Extends backwards (+Z) with true forward-kinematic articulation.
 * - Dynamic transformation colors (Super Saiyan golden fur, SSJ Blue cyan, SSJ God magenta, SSJ4 crimson, Cell orange stinger, Arcosian golden form).
 * - Smooth damped kinematics with zero jittering/spasms during character spin or idle.
 */
public class TrailingTailLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {

    private static final Identifier WHITE_TEXTURE =
            Identifier.parse("dragonblockarcanedba:textures/entity/ki_white.png");

    private static final java.util.Map<Integer, Float> PREV_BODY_ROT =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Integer, Float> SMOOTHED_SPIN_DRAG =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final int SAIYAN_SEGMENTS = 8;
    private static final int ARCOSIAN_SEGMENTS = 8;
    private static final int BIO_SEGMENTS = 8;

    private final ModelPart[] saiyanSegments;
    private final ModelPart[] arcosianSegments;
    private final ModelPart[] bioAndroidSegments;

    public TrailingTailLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);
        this.saiyanSegments = new ModelPart[SAIYAN_SEGMENTS];
        for (int i = 0; i < SAIYAN_SEGMENTS; i++) {
            this.saiyanSegments[i] = createSaiyanSegment(i);
        }

        this.arcosianSegments = new ModelPart[ARCOSIAN_SEGMENTS];
        for (int i = 0; i < ARCOSIAN_SEGMENTS; i++) {
            this.arcosianSegments[i] = createArcosianSegment(i);
        }

        this.bioAndroidSegments = new ModelPart[BIO_SEGMENTS];
        for (int i = 0; i < BIO_SEGMENTS; i++) {
            this.bioAndroidSegments[i] = createBioAndroidSegment(i);
        }
    }

    private ModelPart bake(String name, CubeListBuilder builder) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(name, builder, PartPose.ZERO);
        return root.bake(64, 64);
    }

    // ==================== PROCEDURAL CODE GEOMETRY (EXTENDING ALONG +Z) ====================

    private ModelPart createSaiyanSegment(int index) {
        float progress = index / (float) (SAIYAN_SEGMENTS - 1);
        float width = Math.max(1.6F, 2.6F - progress * 0.8F);
        float depth = Math.max(1.6F, 2.5F - progress * 0.75F);
        float length = 3.0F;

        CubeListBuilder builder = CubeListBuilder.create();

        if (index == SAIYAN_SEGMENTS - 1) {
            // Fluffy rounded tufted tip of the monkey tail (classic DBZ Goku/Vegeta look)
            float tipWidth = 2.4F;
            float tipDepth = 2.4F;
            builder.texOffs(0, 0).addBox(-tipWidth / 2.0F, -tipDepth / 2.0F, 0.0F, tipWidth, tipDepth, length * 0.8F);
            // Angled tuft crown extending out
            builder.texOffs(0, 0).addBox(-tipWidth * 0.35F, -tipDepth * 0.35F, length * 0.8F, tipWidth * 0.7F, tipDepth * 0.7F, length * 0.4F);
        } else {
            // Main fur segment extending along +Z
            builder.texOffs(0, 0).addBox(-width / 2.0F, -depth / 2.0F, 0.0F, width, depth, length);
        }

        return bake("saiyan_seg_" + index, builder);
    }

    private ModelPart createArcosianSegment(int index) {
        float progress = index / (float) (ARCOSIAN_SEGMENTS - 1);
        // Heavy muscular alien tail tapering down
        float width = Math.max(1.8F, 3.8F - progress * 2.0F);
        float depth = Math.max(1.7F, 3.6F - progress * 1.9F);
        float length = 3.2F;

        CubeListBuilder builder = CubeListBuilder.create();
        // Main muscular body extending along +Z
        builder.texOffs(0, 0).addBox(-width / 2.0F, -depth / 2.0F, 0.0F, width, depth, length);

        // Segmented dorsal armor ridge on top of tail
        if (index < ARCOSIAN_SEGMENTS - 1) {
            builder.texOffs(0, 0).addBox(-width * 0.28F, -depth / 2.0F - 0.4F, 0.2F, width * 0.56F, 0.5F, length - 0.4F);
        }

        // Smooth bulbous alien tip
        if (index == ARCOSIAN_SEGMENTS - 1) {
            float tipW = 1.8F;
            builder.texOffs(0, 0).addBox(-tipW / 2.0F, -tipW / 2.0F, length, tipW, tipW, 1.4F);
        }

        return bake("arcosian_seg_" + index, builder);
    }

    private ModelPart createBioAndroidSegment(int index) {
        float progress = index / (float) (BIO_SEGMENTS - 1);
        float width = Math.max(1.8F, 3.6F - progress * 1.6F);
        float depth = Math.max(1.8F, 3.4F - progress * 1.5F);
        float length = 3.2F;

        CubeListBuilder builder = CubeListBuilder.create();

        if (index < BIO_SEGMENTS - 2) {
            // Armored green carapace segment along +Z
            builder.texOffs(0, 0).addBox(-width / 2.0F, -depth / 2.0F, 0.0F, width, depth, length);
            // Prominent dorsal chitin crest plate on top of segment
            builder.texOffs(0, 0).addBox(-width * 0.3F, -depth / 2.0F - 0.5F, 0.3F, width * 0.6F, 0.6F, length - 0.6F);
        } else if (index == BIO_SEGMENTS - 2) {
            // Flared stinger collar / socket ring
            float collarW = 3.0F;
            float collarD = 3.0F;
            builder.texOffs(0, 0).addBox(-collarW / 2.0F, -collarD / 2.0F, 0.0F, collarW, collarD, length * 0.85F);
            builder.texOffs(0, 0).addBox(-collarW / 2.0F - 0.3F, -collarD / 2.0F - 0.3F, length * 0.35F, collarW + 0.6F, collarD + 0.6F, length * 0.5F);
        } else {
            // Iconic DBZ Cell Absorption Stinger
            // Orange conical stinger base along +Z
            float coneW = 2.0F;
            float coneD = 2.0F;
            builder.texOffs(0, 0).addBox(-coneW / 2.0F, -coneD / 2.0F, 0.0F, coneW, coneD, length * 0.7F);
            // Tapered middle needle
            float midW = 1.2F;
            builder.texOffs(0, 0).addBox(-midW / 2.0F, -midW / 2.0F, length * 0.7F, midW, midW, length * 0.5F);
            // Sharp metallic needle puncture tip
            builder.texOffs(0, 0).addBox(-0.35F, -0.35F, length * 1.2F, 0.7F, 0.7F, length * 0.7F);
        }

        return bake("bio_seg_" + index, builder);
    }

    // ==================== MULTI-JOINT PHYSICS & RENDERING ====================

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            AvatarRenderState state,
            float yRot,
            float xRot
    ) {
        if (!(state instanceof DbaPlayerState dbaState)) {
            return;
        }

        if (!dbaState.dba$hasTail()) {
            return;
        }

        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        ModelPart body = humanoidModel.body;

        Identifier raceId = dbaState.dba$getRaceId();
        String race = raceId != null ? raceId.getPath().toLowerCase() : "saiyan";

        ModelPart[] segments;
        int segmentCount;
        float segLengthMeters;

        if (race.contains("bio_android") || race.contains("cell")) {
            segments = this.bioAndroidSegments;
            segmentCount = BIO_SEGMENTS;
            segLengthMeters = 3.2F / 16.0F;
        } else if (race.contains("arcosian")) {
            segments = this.arcosianSegments;
            segmentCount = ARCOSIAN_SEGMENTS;
            segLengthMeters = 3.2F / 16.0F;
        } else {
            segments = this.saiyanSegments;
            segmentCount = SAIYAN_SEGMENTS;
            segLengthMeters = 3.0F / 16.0F;
        }

        float age = dbaState.dba$getTailAgeInTicks();
        float speedFactor = Mth.clamp(dbaState.dba$getHorizontalSpeed() * 2.5F, 0.0F, 1.0F);
        boolean isSprinting = dbaState.dba$isSprinting();
        boolean isCrouching = dbaState.dba$isCrouching();
        boolean isSwimming = dbaState.dba$isSwimming();
        boolean isFlying = dbaState.dba$isFlying();
        float vertVel = Mth.clamp(dbaState.dba$getLocalVelocityY(), -1.0F, 1.0F);

        // True body walking movement from Minecraft entity state (NOT head angles!)
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;

        // Form identification for dynamic tail transformations
        Identifier formId = dbaState.dba$getActiveFormId();
        String formStr = formId != null ? formId.getPath().toLowerCase() : "";

        // Tail base color determination - full Dragon Ball anime canonical palette
        int baseTailColor;
        if (race.contains("arcosian")) {
            if (formStr.contains("golden")) {
                baseTailColor = 0xFFFFD700; // Radiant 24k Golden Form
            } else {
                int skin = dbaState.dba$getSkinColor();
                baseTailColor = skin != 0 ? skin : 0xFFE8DCF5; // Arcosian customized skin or pearl lavender
            }
        } else if (race.contains("bio_android") || race.contains("cell")) {
            baseTailColor = 0xFF2E7D32; // Cell emerald carapace
        } else {
            // Saiyan & Half-Saiyan monkey tail: dynamically matches DBZ transformations!
            if (formStr.contains("ssj4") || formStr.contains("super_saiyan_4")) {
                baseTailColor = 0xFFC41528; // SSJ4 primal crimson fur
            } else if (formStr.contains("super_saiyan_blue") || formStr.contains("ssj_blue") || formStr.contains("ssjb")) {
                baseTailColor = 0xFF00E5FF; // SSJ Blue divine cyan fur
            } else if (formStr.contains("super_saiyan_god") || formStr.contains("ssj_god")) {
                baseTailColor = 0xFFFF204E; // SSJ God fiery magenta fur
            } else if (formStr.contains("ultra_instinct")) {
                baseTailColor = 0xFFE8EEF8; // Ultra Instinct platinum silver fur
            } else if (formStr.contains("super_saiyan") || formStr.contains("ssj")) {
                baseTailColor = 0xFFFFD700; // Super Saiyan radiant golden fur!
            } else if (formStr.contains("kaioken")) {
                baseTailColor = 0xFF992222; // Kaioken aura infused crimson-brown fur
            } else {
                int hair = dbaState.dba$getHairColor();
                baseTailColor = hair != 0 ? hair : 0xFF6B4226;
            }
        }

        // Thread-safe capture of current body part orientation
        final float bodyX = body.x;
        final float bodyY = body.y;
        final float bodyZ = body.z;
        final float bodyXRot = body.xRot;
        final float bodyYRot = body.yRot;
        final float bodyZRot = body.zRot;

        // Ultra-smooth physical kinematics: zero chaotic interference, zero turning lag spikes
        float moveWeight = Mth.clamp(speedFactor + limbSwingAmount * 0.5F, 0.0F, 1.0F);

        // Subtle centrifugal drag when the body turns/spins in the world:
        // Sourced exclusively from state.bodyRot changes across frames (0 while only looking around).
        float currentBodyRot = state.bodyRot;
        Float prevRot = PREV_BODY_ROT.put(state.id, currentBodyRot);
        float deltaBodyRot = 0.0F;
        if (prevRot != null) {
            deltaBodyRot = Mth.wrapDegrees(currentBodyRot - prevRot);
            if (Math.abs(deltaBodyRot) > 45.0F) {
                deltaBodyRot = 0.0F; // Ignore sudden teleports or respawns
            }
        }

        if (PREV_BODY_ROT.size() > 64) {
            PREV_BODY_ROT.clear();
            SMOOTHED_SPIN_DRAG.clear();
        }

        float targetDrag = -Mth.clamp(deltaBodyRot * 0.0035F, -0.018F, 0.018F);
        float prevDrag = SMOOTHED_SPIN_DRAG.getOrDefault(state.id, 0.0F);
        final float smoothedSpinDrag = Mth.lerp(0.08F, prevDrag, targetDrag);
        SMOOTHED_SPIN_DRAG.put(state.id, smoothedSpinDrag);

        RenderType renderType = RenderTypes.entitySolid(WHITE_TEXTURE);

        final int renderLight = Math.max(packedLight, 0x00D000D0);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            stack.pushPose();

            // Transform relative to humanoid torso
            stack.translate(bodyX / 16.0F, bodyY / 16.0F, bodyZ / 16.0F);
            if (bodyZRot != 0.0F) {
                stack.mulPose(com.mojang.math.Axis.ZP.rotation(bodyZRot));
            }
            if (bodyYRot != 0.0F) {
                stack.mulPose(com.mojang.math.Axis.YP.rotation(bodyYRot));
            }
            if (bodyXRot != 0.0F) {
                stack.mulPose(com.mojang.math.Axis.XP.rotation(bodyXRot));
            }

            // Anchor tail accurately to the lower base of the spine/pelvis on the exterior back surface:
            // Y = 10.2 pixels down from neck = 0.640F meters
            // Z = 2.64 pixels back from torso center = 0.165F meters (cleanly outside torso, never intersecting)
            stack.translate(0.0F, 0.640F, 0.165F);

            // True forward-kinematics skeletal chaining extending along +Z (backwards):
            int pushedPoses = 0;

            for (int i = 0; i < segmentCount; i++) {
                float progress = i / (float) (segmentCount - 1);
                stack.pushPose();
                pushedPoses++;

                float yaw = 0.0F;
                float pitch = 0.0F;
                float roll = 0.0F;

                if (isSwimming) {
                    // Smooth aquatic propulsion wave
                    yaw = Mth.sin(age * 0.18F - progress * 2.5F) * (0.14F * progress);
                    pitch = -0.06F + Mth.cos(age * 0.12F - progress * 1.5F) * (0.04F * progress);
                    roll = Mth.sin(age * 0.14F + progress) * (0.03F * progress);
                } else if (race.contains("bio_android") || race.contains("cell")) {
                    // Cell's segmented stinger tail: stable armored drape with subtle breathing ripple
                    float cellWave = Mth.sin(age * 0.04F - progress * 1.2F) * (0.018F * progress);
                    float walkSway = Mth.sin(limbSwing * 0.6F) * (limbSwingAmount * 0.025F * progress);
                    float lateralDamp = isSprinting ? 0.4F : 1.0F;
                    float spinDrag = smoothedSpinDrag * progress;
                    yaw = (cellWave + walkSway) * lateralDamp + spinDrag;
                    float basePitch = (i == 0 ? 0.06F : (i < 4 ? 0.08F : (i < 6 ? -0.04F : -0.08F)));
                    float speedStream = (speedFactor * 0.12F + (isSprinting ? 0.05F : 0.0F)) * (1.0F - progress * 0.35F);
                    float inertiaPitch = -vertVel * 0.08F * progress;
                    pitch = basePitch - speedStream + inertiaPitch;
                } else if (race.contains("arcosian")) {
                    // Frieza / Arcosian tail: graceful, fluid alien tail
                    float arcosianWave = Mth.sin(age * 0.05F - progress * 1.6F) * (0.03F * progress);
                    float walkSway = Mth.sin(limbSwing * 0.6F) * (limbSwingAmount * 0.04F * progress);
                    float lateralDamp = isSprinting ? 0.4F : 1.0F;
                    float spinDrag = smoothedSpinDrag * progress;
                    yaw = 0.03F + (arcosianWave + walkSway) * lateralDamp + spinDrag;
                    float basePitch = (i == 0 ? 0.14F : (i < 4 ? 0.08F : (i < 6 ? -0.05F : -0.11F)));
                    float speedStream = (speedFactor * 0.14F + (isSprinting ? 0.06F : 0.0F)) * (1.0F - progress * 0.35F);
                    float inertiaPitch = -vertVel * 0.10F * progress;
                    pitch = basePitch - speedStream + inertiaPitch;
                    roll = arcosianWave * 0.12F;
                } else {
                    // Saiyan & Half-Saiyan monkey tail: classic natural DBZ drape with silky smooth idle wave
                    // Single coherent traveling wave propagating smoothly from root to tip:
                    float idleWave = Mth.sin(age * 0.05F - progress * 1.6F) * (0.025F * progress);

                    // Body footstep sway (synchronized with left/right leg stride):
                    float walkSway = Mth.sin(limbSwing * 0.6F) * (limbSwingAmount * 0.035F * progress);

                    // Side curve (natural relaxed hang to one side):
                    float baseSideCurve = 0.035F * (1.0F - progress * 0.4F);

                    // High-speed sprint tightens lateral sway into a focused aerodynamic stream:
                    float lateralDamp = isSprinting ? 0.35F : 1.0F;

                    // Subtle centrifugal drag when the body turns/spins:
                    float spinDrag = smoothedSpinDrag * progress;

                    yaw = baseSideCurve + (idleWave + walkSway) * lateralDamp + spinDrag;

                    // Naturally curves down from hips, levels off, and hooks up at the tip:
                    float basePitch = (i == 0 ? 0.12F : (i < 3 ? 0.08F : (i < 5 ? 0.04F : (i < 7 ? -0.08F : -0.10F))));

                    // Speed-based streamlining (faster player moves, higher tail trails backward):
                    float speedStream = (speedFactor * 0.16F + (isSprinting ? 0.06F : 0.0F)) * (1.0F - progress * 0.35F);

                    // Inertia from vertical body movement (jumping / falling):
                    float inertiaPitch = -vertVel * 0.12F * progress;

                    // Body posture adjustments:
                    float crouchAdjust = isCrouching ? -0.04F * progress : 0.0F;
                    float flyStream = isFlying ? -0.10F * (1.0F - progress * 0.5F) : 0.0F;

                    pitch = basePitch - speedStream + inertiaPitch + crouchAdjust + flyStream;

                    roll = idleWave * 0.10F;
                }

                // Apply joint rotations
                stack.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
                stack.mulPose(com.mojang.math.Axis.XP.rotation(pitch));
                if (roll != 0.0F) {
                    stack.mulPose(com.mojang.math.Axis.ZP.rotation(roll));
                }

                // Subtle shading / tip coloring per segment
                int segmentColor = baseTailColor;
                if (race.contains("bio_android") || race.contains("cell")) {
                    if (i == segmentCount - 1) {
                        segmentColor = 0xFFFF7700; // Iconic Cell Orange stinger cone
                    } else if (i == segmentCount - 2) {
                        segmentColor = 0xFF1B4D20; // Stinger socket collar
                    } else if (i % 2 == 1) {
                        segmentColor = 0xFF388E3C;
                    } else {
                        segmentColor = 0xFF2E7D32;
                    }
                } else if (race.contains("arcosian")) {
                    if (formStr.contains("golden")) {
                        if (i == segmentCount - 1) {
                            segmentColor = 0xFF8822CC; // Purple jewel tip in Golden Form
                        } else if (i % 2 == 1) {
                            segmentColor = 0xFFFFC800;
                        } else {
                            segmentColor = 0xFFFFD700;
                        }
                    } else {
                        if (i == segmentCount - 1) {
                            segmentColor = 0xFF8844AA; // Purple alien tail tip
                        } else if (i % 2 == 1) {
                            segmentColor = 0xFFD8C4EE;
                        }
                    }
                } else {
                    // Saiyan tail
                    if (i == segmentCount - 1) {
                        if (formStr.contains("super_saiyan") || formStr.contains("ssj")) {
                            segmentColor = 0xFFFFEA66; // Bright tip on golden fur
                        } else {
                            segmentColor = 0xFF543118; // Darker brown tuft tip
                        }
                    }
                }

                // Render current segment
                segments[i].render(
                        stack,
                        buffer,
                        renderLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        segmentColor
                );

                // Translate backwards (+Z) along segment length to the base of the next joint
                stack.translate(0.0F, 0.0F, segLengthMeters);
            }

            // Pop all chained segment poses
            for (int p = 0; p < pushedPoses; p++) {
                stack.popPose();
            }

            stack.popPose();
        });
    }
}
