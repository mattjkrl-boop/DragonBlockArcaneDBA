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

/**
 * Universal Race Feature Layer.
 * Renders the celestial DBZ Angel Halo when the player is deceased in Otherworld.
 * Hair, spikes, and wings are native to models/skins rather than attached Java procedural overlays.
 */
public class RaceFeatureLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {

    public static final Identifier BASE_PLAYER_TEXTURE =
            Identifier.parse("dragonblockarcanedba:textures/entity/player/base.png");
    private static final Identifier WHITE_TEXTURE =
            Identifier.parse("dragonblockarcanedba:textures/entity/ki_white.png");

    private final ModelPart halo;

    public RaceFeatureLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);
        this.halo = createHalo();
    }

    private ModelPart bake(String name, CubeListBuilder builder) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(name, builder, PartPose.ZERO);
        return root.bake(64, 64);
    }

    private ModelPart createHalo() {
        CubeListBuilder builder = CubeListBuilder.create();
        // Golden ring floating above the head at Y = -12.0F
        builder.texOffs(0, 0).addBox(-3.5F, -12.0F, -3.5F, 7.0F, 0.8F, 1.0F);
        builder.texOffs(0, 0).addBox(-3.5F, -12.0F, 2.5F, 7.0F, 0.8F, 1.0F);
        builder.texOffs(0, 0).addBox(-3.5F, -12.0F, -2.5F, 1.0F, 0.8F, 5.0F);
        builder.texOffs(0, 0).addBox(2.5F, -12.0F, -2.5F, 1.0F, 0.8F, 5.0F);
        return bake("halo", builder);
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
        if (!(state instanceof DbaPlayerState dbaState)) {
            return;
        }

        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        ModelPart head = humanoidModel.head;

        // Render DBZ Golden Angel Halo when deceased in Otherworld
        if (dbaState.dba$isInOtherworld()) {
            RenderType haloType = RenderTypes.entitySolid(WHITE_TEXTURE);
            collector.submitCustomGeometry(poseStack, haloType, (pose, buffer) -> {
                PoseStack stack = new PoseStack();
                stack.last().pose().set(pose.pose());
                stack.last().normal().set(pose.normal());

                stack.pushPose();
                head.translateAndRotate(stack);
                this.halo.render(
                        stack,
                        buffer,
                        0x00F000F0, // Luminous celestial glow
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        0xFFFFD700 // Pure golden yellow
                );
                stack.popPose();
            });
        }
    }
}
