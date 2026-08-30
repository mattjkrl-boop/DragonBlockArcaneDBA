package com.dragonblockarcanedba.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import com.dragonblockarcanedba.client.render.PorungaRenderer;

public class PorungaModel extends EntityModel<PorungaRenderer.PorungaRenderState> {
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart upperTorso;
    private final ModelPart lowerTorso;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart[] tailSegments = new ModelPart[10];

    public PorungaModel(ModelPart root) {
        super(root);
        this.upperTorso = root.getChild("upper_torso");
        this.head = this.upperTorso.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.lowerTorso = root.getChild("lower_torso");
        this.leftArm = this.upperTorso.getChild("left_arm");
        this.rightArm = this.upperTorso.getChild("right_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        
        for (int i = 0; i < 10; i++) {
            if (i == 0) {
                this.tailSegments[i] = this.lowerTorso.getChild("tail_0");
            } else {
                this.tailSegments[i] = this.tailSegments[i-1].getChild("tail_" + i);
            }
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Massive upper torso (wider)
        PartDefinition upperTorso = partdefinition.addOrReplaceChild("upper_torso", 
            CubeListBuilder.create().texOffs(0, 0)
                .addBox(-24.0F, -24.0F, -12.0F, 48.0F, 24.0F, 24.0F), 
            PartPose.offset(0.0F, -60.0F, 0.0F));

        // Shoulder Spikes
        upperTorso.addOrReplaceChild("left_shoulder_spike", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F), 
            PartPose.offsetAndRotation(-14.0F, -24.0F, 0.0F, 0.0F, 0.0F, -0.6F));
        
        upperTorso.addOrReplaceChild("right_shoulder_spike", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -10.0F, -2.0F, 4.0F, 10.0F, 4.0F), 
            PartPose.offsetAndRotation(14.0F, -24.0F, 0.0F, 0.0F, 0.0F, 0.6F));

        // Back Fin / Spikes
        upperTorso.addOrReplaceChild("back_fin", 
            CubeListBuilder.create().texOffs(150, 0)
                .addBox(-1.0F, -8.0F, 10.0F, 2.0F, 24.0F, 12.0F), 
            PartPose.ZERO);

        // Head
        PartDefinition head = upperTorso.addOrReplaceChild("head", 
            CubeListBuilder.create().texOffs(0, 48)
                .addBox(-10.0F, -10.0F, -16.0F, 20.0F, 20.0F, 20.0F), 
            PartPose.offset(0.0F, -28.0F, -6.0F));

        // Snout
        head.addOrReplaceChild("snout", 
            CubeListBuilder.create().texOffs(80, 48)
                .addBox(-8.0F, -2.0F, -24.0F, 16.0F, 10.0F, 8.0F), 
            PartPose.ZERO);

        // Jaw
        head.addOrReplaceChild("jaw", 
            CubeListBuilder.create().texOffs(80, 68)
                .addBox(-8.0F, 2.0F, -23.0F, 16.0F, 6.0F, 10.0F), 
            PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.2F, 0.0F, 0.0F));

        // Horns
        head.addOrReplaceChild("left_horn", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -14.0F, -2.0F, 4.0F, 14.0F, 4.0F), 
            PartPose.offsetAndRotation(-6.0F, -8.0F, -8.0F, -0.6F, 0.0F, -0.2F));
            
        head.addOrReplaceChild("right_horn", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -14.0F, -2.0F, 4.0F, 14.0F, 4.0F), 
            PartPose.offsetAndRotation(6.0F, -8.0F, -8.0F, -0.6F, 0.0F, 0.2F));

        // Head Spikes (Cheeks)
        head.addOrReplaceChild("left_cheek_spike", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -2.0F, -8.0F, 4.0F, 4.0F, 8.0F), 
            PartPose.offsetAndRotation(-10.0F, 2.0F, -8.0F, 0.0F, -0.6F, 0.0F));
            
        head.addOrReplaceChild("right_cheek_spike", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -2.0F, -8.0F, 4.0F, 4.0F, 8.0F), 
            PartPose.offsetAndRotation(10.0F, 2.0F, -8.0F, 0.0F, 0.6F, 0.0F));

        // Lower Torso - make it a child of root so we can scale upper torso independently
        PartDefinition lowerTorso = partdefinition.addOrReplaceChild("lower_torso", 
            CubeListBuilder.create().texOffs(0, 88)
                .addBox(-16.0F, 0.0F, -10.0F, 32.0F, 24.0F, 20.0F), 
            PartPose.offset(0.0F, -60.0F, 0.0F)); // Same pivot as upperTorso!

        // Arms (Defined biceps, forearms, hands, claws, and arm spikes)
        PartDefinition leftArm = upperTorso.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(100, 100)
                .addBox(-8.0F, -4.0F, -6.0F, 12.0F, 16.0F, 12.0F), 
            PartPose.offsetAndRotation(-28.0F, -12.0F, 0.0F, 0.0F, 0.0F, 0.2F));
            
        leftArm.addOrReplaceChild("left_arm_spike", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F), 
            PartPose.offsetAndRotation(-6.0F, 2.0F, 0.0F, 0.0F, 0.0F, -1.2F));
            
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm",
            CubeListBuilder.create().texOffs(100, 130)
                .addBox(-6.0F, 0.0F, -5.0F, 10.0F, 16.0F, 10.0F),
            PartPose.offsetAndRotation(-1.0F, 12.0F, 0.0F, -0.4F, 0.0F, 0.0F));
            
        PartDefinition leftHand = leftForearm.addOrReplaceChild("left_hand",
            CubeListBuilder.create().texOffs(100, 160)
                .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 10.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.2F, 0.0F, 0.0F));
            
        leftHand.addOrReplaceChild("claw_1", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, 10.0F, -2.0F, 0.2F, 0.0F, 0.2F));
        leftHand.addOrReplaceChild("claw_2", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(2.0F, 10.0F, -2.0F, 0.2F, 0.0F, -0.2F));
        leftHand.addOrReplaceChild("claw_3", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 0.2F, 0.0F, 0.0F));

        PartDefinition rightArm = upperTorso.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(100, 100)
                .addBox(-4.0F, -4.0F, -6.0F, 12.0F, 16.0F, 12.0F), 
            PartPose.offsetAndRotation(28.0F, -12.0F, 0.0F, 0.0F, 0.0F, -0.2F));
            
        rightArm.addOrReplaceChild("right_arm_spike", 
            CubeListBuilder.create().texOffs(128, 0)
                .addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F), 
            PartPose.offsetAndRotation(6.0F, 2.0F, 0.0F, 0.0F, 0.0F, 1.2F));
            
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm",
            CubeListBuilder.create().texOffs(100, 130)
                .addBox(-4.0F, 0.0F, -5.0F, 10.0F, 16.0F, 10.0F),
            PartPose.offsetAndRotation(1.0F, 12.0F, 0.0F, -0.4F, 0.0F, 0.0F));
            
        PartDefinition rightHand = rightForearm.addOrReplaceChild("right_hand",
            CubeListBuilder.create().texOffs(100, 160)
                .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 10.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.2F, 0.0F, 0.0F));
            
        rightHand.addOrReplaceChild("claw_1", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, 10.0F, -2.0F, 0.2F, 0.0F, 0.2F));
        rightHand.addOrReplaceChild("claw_2", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(2.0F, 10.0F, -2.0F, 0.2F, 0.0F, -0.2F));
        rightHand.addOrReplaceChild("claw_3", CubeListBuilder.create().texOffs(128, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 0.2F, 0.0F, 0.0F));

        // Tail Segments - 10 segments for a long smooth tail!
        int[] tailSizes = {30, 27, 24, 21, 18, 15, 12, 9, 6, 3};
        int[] tailLengths = {20, 20, 20, 20, 20, 20, 20, 20, 20, 20};
        
        PartDefinition currentParent = lowerTorso; // since lowerTorso is now a local var
        float yOffset = 24.0F; // bottom of lower_torso
        float zOffset = 0.0F; // perfectly centered
        
        for (int i = 0; i < 10; i++) {
            int size = tailSizes[i];
            int length = tailLengths[i];
            int ty = 150 + (i * 20);
            
            currentParent = currentParent.addOrReplaceChild("tail_" + i, 
                CubeListBuilder.create().texOffs(0, ty)
                    .addBox(-size/2.0F, 0.0F, -size/2.0F, (float)size, (float)length, (float)size), 
                PartPose.offset(0.0F, yOffset, zOffset));
                
            yOffset = (float)length; 
            zOffset = 0.0F;
        }

        return LayerDefinition.create(meshdefinition, 512, 512);
    }

    @Override
    public void setupAnim(PorungaRenderer.PorungaRenderState state) {
        super.setupAnim(state);
        
        float idleTime = state.ageInTicks * 0.05F; 
        
        // Hovering upper body, much higher to avoid clipping (raised significantly)
        float hoverY = -250.0F + (float)Math.sin(idleTime) * 4.0F;
        this.upperTorso.y = hoverY;
        this.lowerTorso.y = hoverY; // Match since it's a sibling now
        
        // Heavy hunch forward like the reference image
        this.upperTorso.xRot = 0.6F + (float)Math.sin(idleTime * 0.5F) * 0.02F; 
        this.lowerTorso.xRot = 0.4F + (float)Math.sin(idleTime * 0.5F) * 0.02F; 

        // Head looks up/forward to counteract the heavy hunch
        this.head.yRot = (float) Math.sin(idleTime * 0.3F) * 0.05F; 
        this.head.xRot = -0.6F;

        // Jaw breathes
        this.jaw.xRot = 0.1F + (float)Math.sin(state.ageInTicks * 0.1) * 0.1F;

        // Arms - Biceps down/slightly forward, Forearms bent heavily INWARD across the chest!
        this.leftArm.xRot = -0.3F;
        this.leftArm.yRot = 0.2F;
        this.leftArm.zRot = -0.1F; // Hug closer to body
        this.leftForearm.xRot = -0.6F; // Bent forward
        this.leftForearm.yRot = 0.6F;  // Bent inward
        this.leftForearm.zRot = 0.4F;  // Rolled inward
        
        this.rightArm.xRot = -0.3F;
        this.rightArm.yRot = -0.2F;
        this.rightArm.zRot = 0.1F; // Hug closer to body
        this.rightForearm.xRot = -0.6F; // Bent forward
        this.rightForearm.yRot = -0.6F; // Bent inward
        this.rightForearm.zRot = -0.4F; // Rolled inward
        
        // Scale the upper torso to be massive!
        this.upperTorso.xScale = 3.0F;
        this.upperTorso.yScale = 3.0F;
        this.upperTorso.zScale = 3.0F;
        
        // Lower torso bridges the gap (32 width * 2.8 = ~90, vs upper 48 * 3 = 144)
        this.lowerTorso.xScale = 2.8F;
        this.lowerTorso.yScale = 2.2F;
        this.lowerTorso.zScale = 2.8F;
        
        // Tight spiral tail directly beneath the body!
        for (int i = 0; i < 10; i++) {
            ModelPart tail = this.tailSegments[i];
            
            // Keep local scale at 1.0F! The box sizes themselves handle the tapering.
            // Inherits the 2.8x scale from lowerTorso automatically!
            tail.xScale = 1.0F; 
            tail.yScale = 1.0F;
            tail.zScale = 1.0F;
            
            if (i == 0) {
                // Squeeze the Z-axis of the entire tail chain so it matches the lower torso's depth!
                // lowerTorso depth = 20. tail_0 depth = 30. (20 / 30 = 0.666F)
                tail.zScale = 0.666F;
            }
            
            // Spiral logic: Drop down first, then curl tightly up (xRot) and sideways (yRot)
            float curlX = -0.1F; 
            float curlY = 0.0F;
            
            if (i > 1) {
                curlX = -0.5F; // Heavy curve forward
                curlY = 0.4F;  // Heavy curve sideways (creates the spiral coil)
            }
            if (i > 5) {
                curlX = -0.6F; // Even tighter curl at the end
                curlY = 0.6F;
            }
            
            tail.xRot = curlX + (float)Math.sin(idleTime + i) * 0.02F;
            tail.yRot = curlY + (float)Math.cos(idleTime + i) * 0.02F; 
            tail.zRot = 0.1F;
        }
    }
}
