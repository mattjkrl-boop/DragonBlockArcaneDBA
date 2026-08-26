package com.dragonblockarcanedba.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * High-detail procedural multi-joint tail renderer with speed/movement responsive physics.
 * Generates custom code geometry for all tailed races (Saiyan, Half-Saiyan, Arcosian, Bio-Android).
 * Features true skeletal forward kinematics, inertia lag, speed drag, and organic secondary motion.
 */
public class TrailingTailLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {
    private static final Identifier WHITE_TEXTURE =
            Identifier.parse("dragonblockarcanedba:textures/entity/ki_white.png");

    private static final int SAIYAN_SEGMENTS = 8;
    private static final int ARCOSIAN_SEGMENTS = 9;
    private static final int BIO_SEGMENTS = 8;

    private final ModelPart[] saiyanTail = new ModelPart[SAIYAN_SEGMENTS];
    private final ModelPart[] arcosianTail = new ModelPart[ARCOSIAN_SEGMENTS];
    private final ModelPart[] bioTail = new ModelPart[BIO_SEGMENTS];

    public TrailingTailLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);

        for (int i = 0; i < SAIYAN_SEGMENTS; i++) {
            saiyanTail[i] = createSaiyanSegment(i);
        }

        for (int i = 0; i < ARCOSIAN_SEGMENTS; i++) {
            arcosianTail[i] = createArcosianSegment(i);
        }

        for (int i = 0; i < BIO_SEGMENTS; i++) {
            bioTail[i] = createBioAndroidSegment(i);
        }
    }

    private ModelPart bake(String name, CubeListBuilder builder) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(name, builder, PartPose.ZERO);
        return root.bake(64, 64);
    }

    // ==================== PROCEDURAL CODE GEOMETRY ====================

    private ModelPart createSaiyanSegment(int index) {
        float progress = index / (float) (SAIYAN_SEGMENTS - 1);
        float width = Math.max(1.6F, 2.6F - progress * 0.9F);
        float depth = Math.max(1.6F, 2.5F - progress * 0.85F);
        float length = 3.2F;

        CubeListBuilder builder = CubeListBuilder.create();

        if (index == SAIYAN_SEGMENTS - 1) {
            // Fluffy rounded tip of the monkey tail
            float tipWidth = 2.0F;
            float tipDepth = 2.0F;
            builder.texOffs(0, 0).addBox(-tipWidth / 2.0F, 0.0F, -tipDepth / 2.0F, tipWidth, length * 0.85F, tipDepth);
            builder.texOffs(0, 0).addBox(-tipWidth * 0.35F, length * 0.85F, -tipDepth * 0.35F, tipWidth * 0.7F, length * 0.35F, tipDepth * 0.7F);
        } else {
            // Main fur segment
            builder.texOffs(0, 0).addBox(-width / 2.0F, 0.0F, -depth / 2.0F, width, length, depth);
        }

        return bake("saiyan_seg_" + index, builder);
    }

    private ModelPart createArcosianSegment(int index) {
        float progress = index / (float) (ARCOSIAN_SEGMENTS - 1);
        float width = Math.max(1.0F, 2.8F - progress * 1.8F);
        float depth = Math.max(1.0F, 2.6F - progress * 1.7F);
        float length = 3.4F;

        CubeListBuilder builder = CubeListBuilder.create();
        builder.texOffs(0, 0).addBox(-width / 2.0F, 0.0F, -depth / 2.0F, width, length, depth);

        // Sleek tapering tip
        if (index == ARCOSIAN_SEGMENTS - 1) {
            builder.texOffs(0, 0).addBox(-0.5F, length, -0.5F, 1.0F, 1.2F, 1.0F);
        }

        return bake("arcosian_seg_" + index, builder);
    }

    private ModelPart createBioAndroidSegment(int index) {
        float progress = index / (float) (BIO_SEGMENTS - 1);
        float width = Math.max(1.4F, 3.2F - progress * 1.6F);
        float depth = Math.max(1.4F, 3.0F - progress * 1.5F);
        float length = 3.5F;

        CubeListBuilder builder = CubeListBuilder.create();

        if (index < BIO_SEGMENTS - 2) {
            // Segmented carapace core
            builder.texOffs(0, 0).addBox(-width / 2.0F, 0.0F, -depth / 2.0F, width, length, depth);
            // Armored dorsal ridge plate
            builder.texOffs(0, 0).addBox(-width / 2.0F - 0.2F, 0.4F, depth / 2.0F - 0.2F, width + 0.4F, length - 0.8F, 0.8F);
        } else if (index == BIO_SEGMENTS - 2) {
            // Stinger collar base
            float collarW = 2.4F;
            float collarD = 2.4F;
            builder.texOffs(0, 0).addBox(-collarW / 2.0F, 0.0F, -collarD / 2.0F, collarW, length, collarD);
            builder.texOffs(0, 0).addBox(-collarW / 2.0F - 0.3F, length - 1.0F, -collarD / 2.0F - 0.3F, collarW + 0.6F, 1.4F, collarD + 0.6F);
        } else {
            // Segmented absorption stinger / needle cone
            float coneW = 1.2F;
            float coneD = 1.2F;
            builder.texOffs(0, 0).addBox(-coneW / 2.0F, 0.0F, -coneD / 2.0F, coneW, length * 0.6F, coneD);
            // Needle tip
            builder.texOffs(0, 0).addBox(-0.35F, length * 0.6F, -0.35F, 0.7F, length * 0.65F, 0.7F);
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
            float limbSwing,
            float limbSwingAmount
    ) {
        if (!(state instanceof DbaPlayerState dbaState)) {
            return;
        }

        if (!dbaState.dba$hasTail()) {
            return;
        }

        Identifier raceId = dbaState.dba$getRaceId();
        if (raceId == null) {
            return;
        }

        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        ModelPart body = humanoidModel.body;
        String race = raceId.getPath().toLowerCase();

        ModelPart[] segments;
        int segmentCount;
        float segLengthMeters;

        if (race.contains("arcosian")) {
            segments = arcosianTail;
            segmentCount = ARCOSIAN_SEGMENTS;
            segLengthMeters = 3.4F / 16.0F;
        } else if (race.contains("bio_android") || race.contains("cell")) {
            segments = bioTail;
            segmentCount = BIO_SEGMENTS;
            segLengthMeters = 3.5F / 16.0F;
        } else {
            segments = saiyanTail;
            segmentCount = SAIYAN_SEGMENTS;
            segLengthMeters = 3.2F / 16.0F;
        }

        float age = dbaState.dba$getTailAgeInTicks();
        float speed = dbaState.dba$getHorizontalSpeed();
        boolean isSprinting = dbaState.dba$isSprinting();
        boolean isCrouching = dbaState.dba$isCrouching();
        boolean isSwimming = dbaState.dba$isSwimming();
        boolean isFlying = dbaState.dba$isFlying();

        // Local velocities and turning physics
        float bodyYawVelocity = dbaState.dba$getBodyYawVelocity();
        float localVx = dbaState.dba$getLocalVelocityX();
        float localVz = dbaState.dba$getLocalVelocityZ();
        float localVy = dbaState.dba$getLocalVelocityY();
        float headYawRel = dbaState.dba$getHeadYawRel();

        // Effective speed multiplier (0.0 to 1.5+)
        float speedFactor = Math.min(1.5F, (isSprinting || isFlying ? 1.0F : speed * 4.0F));

        // Form identification for dynamic tail transformations
        Identifier formId = dbaState.dba$getActiveFormId();
        String formStr = formId != null ? formId.getPath().toLowerCase() : "";

        // Tail base color determination
        int baseTailColor;
        if (race.contains("arcosian")) {
            if (formStr.contains("golden")) {
                baseTailColor = 0xFFFFD700; // Radiant Golden Form
            } else {
                int skin = dbaState.dba$getSkinColor();
                baseTailColor = skin != 0 ? skin : 0xFFE5D0FF; // Arcosian customized skin or lavender pearl
            }
        } else if (race.contains("bio_android") || race.contains("cell")) {
            baseTailColor = 0xFF4FBC5A; // Bio-Android emerald carapace
        } else {
            if (formStr.contains("ssj4") || formStr.contains("super_saiyan_4") || formStr.contains("ozaru")) {
                baseTailColor = 0xFFBA1B2B; // Super Saiyan 4 primal crimson fur
            } else {
                baseTailColor = 0xFF8A5A38; // Saiyan natural monkey fur brown
            }
        }

        // Thread-safe capture of current body part orientation
        final float bodyX = body.x;
        final float bodyY = body.y;
        final float bodyZ = body.z;
        final float bodyXRot = body.xRot;
        final float bodyYRot = body.yRot;
        final float bodyZRot = body.zRot;

        RenderType renderType = RenderTypes.entitySolid(WHITE_TEXTURE);

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

            // Anchor tail accurately to the lower base of the spine/pelvis (meters)
            stack.translate(0.0F, 0.58F, 0.14F);

            // True forward-kinematics skeletal chaining:
            // Each segment rotates and extends relative to its parent joint!
            int pushedPoses = 0;

            for (int i = 0; i < segmentCount; i++) {
                float progress = i / (float) (segmentCount - 1);
                stack.pushPose();
                pushedPoses++;

                float yaw = 0.0F;
                float pitch = 0.0F;
                float roll = 0.0F;

                // --- 1. Dynamic Physical Forces (Turning Lag, Strafe Drag, Velocity Stream) ---
                // Torso turning inertia: tail lags behind rapid body rotation
                float turningLag = -bodyYawVelocity * 0.024F * (0.25F + progress * 0.75F);

                // Strafe drag: strafing pushes tail in opposite lateral direction
                float strafeDrag = -localVx * 1.6F * progress;

                // Forward/Backward movement stream: moving forward tilts tail backward/up into wind
                float forwardDrag = -localVz * 1.4F * (0.3F + progress * 0.7F);

                // Vertical lift/fall: jumping pushes down, falling lifts up
                float verticalDrag = localVy * 0.6F * progress;

                // Head counter-balance: looking sideways slightly swivels tail in sympathy
                float headCounter = -headYawRel * 0.006F * progress;

                // Stride counter-oscillation during walking/running
                float strideSway = Mth.sin(limbSwing * 0.6662F) * limbSwingAmount * 0.18F * (0.3F + progress * 0.7F);

                // Wind flutter in high-velocity flight or sprint
                float windFlutterYaw = Mth.sin(age * 0.55F - i * 0.75F) * (0.04F + speedFactor * 0.09F) * progress;
                float windFlutterPitch = Mth.cos(age * 0.50F - i * 0.65F) * (0.03F + speedFactor * 0.07F) * progress;

                if (isSwimming) {
                    // Serpentine undulating swim wave
                    yaw = Mth.sin(age * 0.28F - progress * 3.5F) * 0.24F + strafeDrag * 0.5F;
                    pitch = -0.15F + Mth.cos(age * 0.18F - progress * 2.0F) * 0.08F + forwardDrag * 0.4F;
                    roll = Mth.sin(age * 0.20F + progress) * 0.06F;
                } else if (isCrouching) {
                    // Crouching: alert tail arching upward & inward over lower back
                    yaw = Mth.sin(age * 0.10F + i * 0.4F) * 0.07F + turningLag * 0.6F + strafeDrag * 0.4F;
                    pitch = (i == 0 ? -0.75F : -0.22F - progress * 0.15F) + forwardDrag * 0.3F;
                    roll = Mth.cos(age * 0.08F + i * 0.3F) * 0.04F;
                } else if (speedFactor > 0.35F) {
                    // High-Speed Running / Sprinting / Flying: Aerodynamic backward stream
                    yaw = turningLag + strafeDrag + headCounter + strideSway + windFlutterYaw;
                    pitch = (i == 0 ? -0.38F - speedFactor * 0.30F : -0.10F - speedFactor * 0.12F)
                            + forwardDrag + verticalDrag + windFlutterPitch;
                    roll = (turningLag + strafeDrag) * 0.35F + windFlutterYaw * 0.5F;
                } else {
                    // Idle & Walking: Organic breathing sway + stride oscillation + turning inertia
                    float breathWave = Mth.sin(age * 0.08F + i * 0.45F) * (0.05F + progress * 0.10F);
                    float sideSway = Mth.cos(age * 0.06F + i * 0.55F) * (0.07F + progress * 0.14F);

                    if (race.contains("arcosian")) {
                        // Sleek sinuous reptilian whip motion
                        float whipWave = Mth.sin(age * 0.12F - progress * 2.6F) * (0.09F + progress * 0.14F);
                        yaw = sideSway * 1.1F + whipWave + turningLag + strafeDrag + headCounter + strideSway;
                        pitch = (i == 0 ? -0.38F : -0.16F - progress * 0.07F + breathWave) + forwardDrag + verticalDrag;
                        roll = (whipWave + turningLag) * 0.3F;
                    } else if (race.contains("bio_android") || race.contains("cell")) {
                        // Heavy mechanical / chitinous posture with poised stinger
                        yaw = breathWave * 0.7F + turningLag + strafeDrag + headCounter + strideSway * 0.7F;
                        pitch = (i == 0 ? -0.58F : -0.16F - progress * 0.10F + Mth.cos(age * 0.07F + i * 0.3F) * 0.04F)
                                + forwardDrag + verticalDrag;
                        roll = turningLag * 0.25F;
                    } else {
                        // Saiyan: classic curled S-shape monkey tail with relaxed breathing & lively twitches
                        float tipFlick = (i >= 5 ? Mth.sin(age * 0.15F + i * 0.7F) * 0.08F : 0.0F);
                        yaw = sideSway + tipFlick + turningLag + strafeDrag + headCounter + strideSway;
                        pitch = (i == 0 ? -0.46F : (i < 4 ? -0.22F : 0.18F)) + breathWave * 0.8F + forwardDrag + verticalDrag;
                        roll = (sideSway + turningLag) * 0.25F;
                    }
                }

                // Apply joint rotations
                stack.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
                stack.mulPose(com.mojang.math.Axis.XP.rotation(pitch));
                if (roll != 0.0F) {
                    stack.mulPose(com.mojang.math.Axis.ZP.rotation(roll));
                }

                // Subtle shading / tip coloring per segment
                int segmentColor = baseTailColor;
                if (race.contains("bio_android") && i >= segmentCount - 2) {
                    // Dark needle tip for Bio-Android stinger
                    segmentColor = 0xFF1E3A22;
                }

                // Render current segment
                segments[i].render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        segmentColor
                );

                // Translate along segment axis to base of the next joint
                stack.translate(0.0F, segLengthMeters, 0.0F);
            }

            // Pop all chained segment poses
            for (int p = 0; p < pushedPoses; p++) {
                stack.popPose();
            }

            stack.popPose();
        });
    }
}

