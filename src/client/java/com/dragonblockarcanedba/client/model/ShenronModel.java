package com.dragonblockarcanedba.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import com.dragonblockarcanedba.client.render.ShenronRenderer;

public class ShenronModel extends EntityModel<ShenronRenderer.ShenronRenderState> {
    private static final int NUM_SEGMENTS = 20;
    private final ModelPart head;
    private final ModelPart leftWhisker;
    private final ModelPart rightWhisker;
    private final ModelPart jaw;
    private final ModelPart[] segments = new ModelPart[NUM_SEGMENTS];
    
    private ModelPart leftArm;
    private ModelPart rightArm;
    private ModelPart leftLeg;
    private ModelPart rightLeg;

    public ShenronModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leftWhisker = this.head.getChild("left_whisker");
        this.rightWhisker = this.head.getChild("right_whisker");
        this.jaw = this.head.getChild("jaw");
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            this.segments[i] = root.getChild("segment_" + i);
            if (i == 2) {
                this.leftArm = this.segments[i].getChild("left_arm");
                this.rightArm = this.segments[i].getChild("right_arm");
            }
            if (i == 14) {
                this.leftLeg = this.segments[i].getChild("left_leg");
                this.rightLeg = this.segments[i].getChild("right_leg");
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // --- HEAD CONSTRUCTION ---
        PartDefinition head = partdefinition.addOrReplaceChild("head", 
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-12.0F, -12.0F, -12.0F, 24.0F, 24.0F, 24.0F), 
            PartPose.offset(0.0F, -30.0F, 0.0F));

        // Snout
        head.addOrReplaceChild("snout", 
            CubeListBuilder.create().texOffs(0, 64)
                .addBox(-7.0F, -4.0F, -30.0F, 14.0F, 8.0F, 18.0F), 
            PartPose.ZERO);

        // Jaw (slightly open)
        head.addOrReplaceChild("jaw", 
            CubeListBuilder.create().texOffs(64, 64)
                .addBox(-7.0F, 4.0F, -29.0F, 14.0F, 6.0F, 18.0F), 
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F));

        // Antlers/Horns
        head.addOrReplaceChild("left_horn", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F)
                .addBox(-6.0F, -12.0F, -1.5F, 6.0F, 3.0F, 3.0F), 
            PartPose.offsetAndRotation(-9.0F, -12.0F, -3.0F, -0.4F, 0.0F, -0.3F));
            
        head.addOrReplaceChild("right_horn", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F)
                .addBox(0.0F, -12.0F, -1.5F, 6.0F, 3.0F, 3.0F), 
            PartPose.offsetAndRotation(9.0F, -12.0F, -3.0F, -0.4F, 0.0F, 0.3F));

        // Long thin whiskers
        head.addOrReplaceChild("left_whisker", 
            CubeListBuilder.create().texOffs(192, 0)
                .addBox(-1.0F, -1.0F, -24.0F, 2.0F, 2.0F, 24.0F), 
            PartPose.offsetAndRotation(-7.0F, 2.0F, -28.0F, 0.4F, -0.6F, 0.0F));
            
        head.addOrReplaceChild("right_whisker", 
            CubeListBuilder.create().texOffs(192, 0)
                .addBox(-1.0F, -1.0F, -24.0F, 2.0F, 2.0F, 24.0F), 
            PartPose.offsetAndRotation(7.0F, 2.0F, -28.0F, 0.4F, 0.6F, 0.0F));

        // --- BODY SEGMENTS ---
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            PartDefinition segment = partdefinition.addOrReplaceChild("segment_" + i, 
                CubeListBuilder.create().texOffs(0, 128)
                    .addBox(-12.0F, -12.0F, -12.0F, 24.0F, 24.0F, 24.0F), 
                PartPose.ZERO);

            // Spiky mane along the back
            if (i < NUM_SEGMENTS - 1) {
                segment.addOrReplaceChild("mane_" + i,
                    CubeListBuilder.create().texOffs(128, 64)
                        .addBox(-3.0F, -20.0F, -9.0F, 6.0F, 8.0F, 18.0F),
                    PartPose.ZERO);
            }

            // Gradually thinning tail attached to the final segment using CubeDeformation
            if (i == NUM_SEGMENTS - 1) {
                // Pitch the entire tail downwards so it buries into the dirt
                PartPose tailPose = PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.6F, 0.0F, 0.0F);
                
                segment.addOrReplaceChild("tail_1", 
                    CubeListBuilder.create().texOffs(0, 128)
                        .addBox(-12.0F, -12.0F, 10.0F, 24.0F, 24.0F, 24.0F, new CubeDeformation(-2.0F)),
                    tailPose);
                segment.addOrReplaceChild("tail_2", 
                    CubeListBuilder.create().texOffs(0, 128)
                        .addBox(-12.0F, -12.0F, 27.0F, 24.0F, 24.0F, 24.0F, new CubeDeformation(-5.0F)),
                    tailPose);
                segment.addOrReplaceChild("tail_3", 
                    CubeListBuilder.create().texOffs(0, 128)
                        .addBox(-12.0F, -12.0F, 38.0F, 24.0F, 24.0F, 24.0F, new CubeDeformation(-8.0F)),
                    tailPose);
                segment.addOrReplaceChild("tail_4", 
                    CubeListBuilder.create().texOffs(0, 128)
                        .addBox(-12.0F, -12.0F, 43.0F, 24.0F, 24.0F, 24.0F, new CubeDeformation(-11.0F)),
                    tailPose);
            }

            // Front Arms (Segment 2)
            if (i == 2) {
                segment.addOrReplaceChild("left_arm",
                    CubeListBuilder.create().texOffs(128, 128)
                        .addBox(-24.0F, -3.0F, -3.0F, 20.0F, 6.0F, 6.0F) 
                        .addBox(-26.0F, 0.0F, -6.0F, 8.0F, 6.0F, 12.0F), 
                    PartPose.offsetAndRotation(-10.0F, 4.0F, 0.0F, 0.0F, 0.4F, -0.4F));
                segment.addOrReplaceChild("right_arm",
                    CubeListBuilder.create().texOffs(128, 128)
                        .addBox(4.0F, -3.0F, -3.0F, 20.0F, 6.0F, 6.0F) 
                        .addBox(18.0F, 0.0F, -6.0F, 8.0F, 6.0F, 12.0F), 
                    PartPose.offsetAndRotation(10.0F, 4.0F, 0.0F, 0.0F, -0.4F, 0.4F));
            }
            
            // Back Legs (Segment 14)
            if (i == 14) {
                segment.addOrReplaceChild("left_leg",
                    CubeListBuilder.create().texOffs(128, 128)
                        .addBox(-24.0F, -3.0F, -3.0F, 20.0F, 6.0F, 6.0F) 
                        .addBox(-26.0F, 0.0F, -6.0F, 8.0F, 6.0F, 12.0F), 
                    PartPose.offsetAndRotation(-10.0F, 4.0F, 0.0F, 0.0F, -0.4F, -0.4F));
                segment.addOrReplaceChild("right_leg",
                    CubeListBuilder.create().texOffs(128, 128)
                        .addBox(4.0F, -3.0F, -3.0F, 20.0F, 6.0F, 6.0F) 
                        .addBox(18.0F, 0.0F, -6.0F, 8.0F, 6.0F, 12.0F), 
                    PartPose.offsetAndRotation(10.0F, 4.0F, 0.0F, 0.0F, 0.4F, 0.4F));
            }
        }

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(ShenronRenderer.ShenronRenderState state) {
        super.setupAnim(state);
        
        // Idle animation timer
        float idleTime = state.ageInTicks * 0.05F; 
        
        // 1. HEAD POSITION & ROTATION: Staring down, bobbing slightly
        this.head.x = 0.0F;
        this.head.y = -152.0F + (float)Math.sin(idleTime) * 3.0F;
        this.head.z = -10.0F;
        this.head.yRot = 0.0F;
        this.head.xRot = 0.2F + (float)Math.sin(idleTime * 0.5F) * 0.05F;

        // Jaw breathes slowly
        this.jaw.xRot = 0.1F + (float)Math.sin(state.ageInTicks * 0.1) * 0.1F;
        
        // Iterate through all segments to create a perfect Idle Spiral
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            ModelPart currentSegment = this.segments[i];
            
            float angle = i * 0.45F;
            float aliveOffset = (float)Math.sin(idleTime + i * 0.5F) * 1.5F;
            
            // Gradually transition from radius 0 (head) to radius 40 (main coil), expanding at the bottom
            float radius = 40.0F;
            if (i < 4) {
                radius = Math.max(0, i) * 10.0F; 
            } else if (i > 10) {
                radius = 40.0F + (i - 10) * 3.0F; // Expands as it rests on ground
            }

            // Smooth parabolic swoop down to the ground
            float yProgress = Math.max(0, 16 - i) / 16.0F; // i=0 -> 1.0, i=16 -> 0.0
            float currentY = -12.0F - 120.0F * yProgress * yProgress + aliveOffset;

            float currentX = (float)Math.sin(angle) * radius;
            float currentZ = (float)Math.cos(angle) * radius;
            
            // Calculate previous point to perfectly align segment tangency
            float prevAngle = (i - 1) * 0.45F;
            float prevI = i - 1;
            float prevRadius = 40.0F;
            if (prevI < 4) {
                prevRadius = Math.max(0, prevI) * 10.0F;
            } else if (prevI > 10) {
                prevRadius = 40.0F + (prevI - 10) * 3.0F;
            }
            
            float prevYProgress = Math.max(0, 16 - prevI) / 16.0F;
            float prevY = -12.0F - 120.0F * prevYProgress * prevYProgress + (float)Math.sin(idleTime + prevI * 0.5F) * 1.5F;
            
            float prevX = (float)Math.sin(prevAngle) * prevRadius;
            float prevZ = (float)Math.cos(prevAngle) * prevRadius;
            
            // Connect the first segment exactly to the head
            if (i == 0) {
                prevX = this.head.x;
                prevZ = this.head.z;
                prevY = this.head.y;
            }

            currentSegment.x = currentX;
            currentSegment.z = currentZ;
            currentSegment.y = currentY;
            
            // Yaw: Point front of segment perfectly towards the previous segment
            float dx = prevX - currentX;
            float dz = prevZ - currentZ;
            currentSegment.yRot = (float)Math.atan2(dx, dz) + (float)Math.PI;
            
            // Pitch: Point perfectly towards previous segment's Y height
            float dy = prevY - currentY;
            float horizontalDist = (float)Math.sqrt(dx*dx + dz*dz);
            if (horizontalDist > 0.001F) {
                currentSegment.xRot = (float)Math.atan2(dy, horizontalDist);
            } else {
                currentSegment.xRot = (float)Math.PI / 2.0F; // Point straight up
            }

            // Animate arms/legs paddling slowly
            if (i == 2) {
                this.leftArm.xRot = (float) Math.sin(idleTime) * 0.2F;
                this.rightArm.xRot = (float) Math.sin(idleTime + Math.PI) * 0.2F;
            }
            if (i == 14) {
                this.leftLeg.xRot = (float) Math.cos(idleTime) * 0.2F;
                this.rightLeg.xRot = (float) Math.cos(idleTime + Math.PI) * 0.2F;
            }
        }
        
        // Whiskers wave in wind
        this.leftWhisker.yRot = 0.6F;
        this.rightWhisker.yRot = -0.6F;
        this.leftWhisker.xRot = 0.4F + (float) Math.sin(idleTime * 1.5F) * 0.2F;
        this.rightWhisker.xRot = 0.4F + (float) Math.sin(idleTime * 1.5F + Math.PI) * 0.2F;
    }
}
