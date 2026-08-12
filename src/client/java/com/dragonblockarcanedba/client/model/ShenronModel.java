package com.dragonblockarcanedba.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import com.dragonblockarcanedba.client.render.ShenronRenderer;

public class ShenronModel extends EntityModel<ShenronRenderer.ShenronRenderState> {
    private static final int NUM_SEGMENTS = 16;
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
            if (i == 12) {
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
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), 
            PartPose.offset(0.0F, -20.0F, 0.0F));

        // Snout
        head.addOrReplaceChild("snout", 
            CubeListBuilder.create().texOffs(0, 32)
                .addBox(-5.0F, -2.0F, -20.0F, 10.0F, 6.0F, 12.0F), 
            PartPose.ZERO);

        // Jaw (slightly open)
        head.addOrReplaceChild("jaw", 
            CubeListBuilder.create().texOffs(44, 32)
                .addBox(-5.0F, 4.0F, -19.0F, 10.0F, 4.0F, 12.0F), 
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2F, 0.0F, 0.0F));

        // Antlers/Horns
        head.addOrReplaceChild("left_horn", 
            CubeListBuilder.create().texOffs(0, 50)
                .addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                .addBox(-4.0F, -8.0F, -1.0F, 4.0F, 2.0F, 2.0F), 
            PartPose.offsetAndRotation(-6.0F, -8.0F, -2.0F, -0.4F, 0.0F, -0.3F));
            
        head.addOrReplaceChild("right_horn", 
            CubeListBuilder.create().texOffs(0, 50)
                .addBox(-1.0F, -12.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                .addBox(0.0F, -8.0F, -1.0F, 4.0F, 2.0F, 2.0F), 
            PartPose.offsetAndRotation(6.0F, -8.0F, -2.0F, -0.4F, 0.0F, 0.3F));

        // Long thin whiskers
        head.addOrReplaceChild("left_whisker", 
            CubeListBuilder.create().texOffs(8, 50)
                .addBox(-0.5F, -0.5F, -16.0F, 1.0F, 1.0F, 16.0F), 
            PartPose.offsetAndRotation(-5.0F, 2.0F, -18.0F, 0.4F, -0.6F, 0.0F));
            
        head.addOrReplaceChild("right_whisker", 
            CubeListBuilder.create().texOffs(8, 50)
                .addBox(-0.5F, -0.5F, -16.0F, 1.0F, 1.0F, 16.0F), 
            PartPose.offsetAndRotation(5.0F, 2.0F, -18.0F, 0.4F, 0.6F, 0.0F));

        // --- BODY SEGMENTS ---
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            PartDefinition segment = partdefinition.addOrReplaceChild("segment_" + i, 
                CubeListBuilder.create().texOffs(64, 0)
                    .addBox(-7.0F, -7.0F, -7.0F, 14.0F, 14.0F, 14.0F), 
                PartPose.ZERO);

            // Spiky mane along the back
            segment.addOrReplaceChild("mane_" + i,
                CubeListBuilder.create().texOffs(32, 48)
                    .addBox(-2.0F, -12.0F, -5.0F, 4.0F, 5.0F, 10.0F),
                PartPose.ZERO);

            // Front Arms (Segment 2)
            if (i == 2) {
                segment.addOrReplaceChild("left_arm",
                    CubeListBuilder.create().texOffs(80, 48)
                        .addBox(-14.0F, -2.0F, -2.0F, 14.0F, 4.0F, 4.0F) 
                        .addBox(-16.0F, 0.0F, -4.0F, 6.0F, 4.0F, 8.0F), 
                    PartPose.offsetAndRotation(-7.0F, 2.0F, 0.0F, 0.0F, 0.4F, -0.4F));
                segment.addOrReplaceChild("right_arm",
                    CubeListBuilder.create().texOffs(80, 48)
                        .addBox(0.0F, -2.0F, -2.0F, 14.0F, 4.0F, 4.0F) 
                        .addBox(10.0F, 0.0F, -4.0F, 6.0F, 4.0F, 8.0F), 
                    PartPose.offsetAndRotation(7.0F, 2.0F, 0.0F, 0.0F, -0.4F, 0.4F));
            }
            
            // Back Legs (Segment 12)
            if (i == 12) {
                segment.addOrReplaceChild("left_leg",
                    CubeListBuilder.create().texOffs(80, 48)
                        .addBox(-14.0F, -2.0F, -2.0F, 14.0F, 4.0F, 4.0F) 
                        .addBox(-16.0F, 0.0F, -4.0F, 6.0F, 4.0F, 8.0F), 
                    PartPose.offsetAndRotation(-7.0F, 2.0F, 0.0F, 0.0F, -0.4F, -0.4F));
                segment.addOrReplaceChild("right_leg",
                    CubeListBuilder.create().texOffs(80, 48)
                        .addBox(0.0F, -2.0F, -2.0F, 14.0F, 4.0F, 4.0F) 
                        .addBox(10.0F, 0.0F, -4.0F, 6.0F, 4.0F, 8.0F), 
                    PartPose.offsetAndRotation(7.0F, 2.0F, 0.0F, 0.0F, 0.4F, 0.4F));
            }
        }

        return LayerDefinition.create(meshdefinition, 256, 128);
    }

    @Override
    public void setupAnim(ShenronRenderer.ShenronRenderState state) {
        super.setupAnim(state);
        
        // Time multiplier for animation speed
        float time = state.ageInTicks * 0.1F; 
        
        // 1. HEAD POSITION & ROTATION: Center of the rendering coords
        this.head.x = 0.0F;
        this.head.y = -20.0F;
        this.head.z = 0.0F;
        this.head.yRot = 0.0F;
        this.head.xRot = 0.0F;

        // Jaw opens and closes based on speed
        this.jaw.xRot = 0.1F + (state.speed * 0.5F);

        float partialTicks = state.ageInTicks - (float)((int)state.ageInTicks);
        
        // Grab the head's immediate past position
        double[] headPos = state.getLatencyPos(5, partialTicks);
        
        // Iterate through all 16 segments
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            ModelPart currentSegment = this.segments[i];
            
            // The further back the segment, the further back in time we look in the buffer
            int bufferDelay = 5 + (i * 3); 
            double[] historicalPos = state.getLatencyPos(bufferDelay, partialTicks);
            
            // Calculate the difference between the segment's target position and the head
            double rotDiff = historicalPos[0] - headPos[0];
            rotDiff = net.minecraft.util.Mth.wrapDegrees(rotDiff);
            
            double heightDiff = historicalPos[1] - headPos[1];
            
            // Apply rotations based strictly on the entity's actual historical path
            currentSegment.yRot = (float)(-rotDiff * (Math.PI / 180.0));
            currentSegment.y = (float)(heightDiff * 16.0); // Convert block height to pixel height
            
            // Pitch calculations to angle the body up and down
            currentSegment.xRot = (float)(historicalPos[2] * (Math.PI / 180.0));
            
            // Force the segment strictly backwards on the Z axis relative to the turning radius
            currentSegment.z = 12.0F * (i + 1); 
            currentSegment.x = (float)(Math.sin(currentSegment.yRot) * currentSegment.z);

            // Animate arms/legs paddling
            if (i == 2) {
                this.leftArm.xRot = (float) Math.sin(time) * 0.2F;
                this.rightArm.xRot = (float) Math.sin(time + Math.PI) * 0.2F;
            }
            
            if (i == 12) {
                this.leftLeg.xRot = (float) Math.cos(time) * 0.2F;
                this.rightLeg.xRot = (float) Math.cos(time + Math.PI) * 0.2F;
            }
        }
        
        // Whiskers wave in wind
        this.leftWhisker.xRot = 0.4F + (float) Math.sin(time * 1.5F) * 0.2F;
        this.rightWhisker.xRot = 0.4F + (float) Math.sin(time * 1.5F + Math.PI) * 0.2F;
    }
}
