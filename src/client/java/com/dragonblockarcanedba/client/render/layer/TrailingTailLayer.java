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

public class TrailingTailLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {
    private static final Identifier WHITE_TEXTURE =
            Identifier.parse("dragonblockarcanedba:textures/entity/ki_white.png");

    private final ModelPart[] saiyanTail = new ModelPart[7];
    private final ModelPart[] arcosianTail = new ModelPart[6];
    private final ModelPart[] bioTail = new ModelPart[8];

    public TrailingTailLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);

        for (int i = 0; i < saiyanTail.length; i++) {
            saiyanTail[i] = createSaiyanSegment(i);
        }

        for (int i = 0; i < arcosianTail.length; i++) {
            arcosianTail[i] = createArcosianSegment(i);
        }

        for (int i = 0; i < bioTail.length; i++) {
            bioTail[i] = createBioAndroidSegment(i);
        }
    }

    private ModelPart bake(String name, CubeListBuilder builder) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(name, builder, PartPose.ZERO);
        return root.bake(64, 64);
    }

    private ModelPart createSaiyanSegment(int index) {
        float width = Math.max(1.8F, 4.0F - index * 0.35F);
        float depth = Math.max(1.8F, 3.8F - index * 0.32F);
        float length = index == 0 ? 4.5F : 4.0F;

        CubeListBuilder builder = CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(
                        -width / 2.0F,
                        0.0F,
                        0.0F,
                        width,
                        length,
                        depth
                );

        return bake("segment_" + index, builder);
    }

    private ModelPart createArcosianSegment(int index) {
        float width = Math.max(1.5F, 3.5F - index * 0.35F);
        float depth = Math.max(1.5F, 3.2F - index * 0.30F);
        float length = 4.0F;

        CubeListBuilder builder = CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(
                        -width / 2.0F,
                        0.0F,
                        0.0F,
                        width,
                        length,
                        depth
                );

        return bake("segment_" + index, builder);
    }

    private ModelPart createBioAndroidSegment(int index) {
        float width = Math.max(1.2F, 3.8F - index * 0.35F);
        float depth = Math.max(1.2F, 3.5F - index * 0.32F);
        float length = index < 5 ? 4.2F : 3.8F;

        CubeListBuilder builder = CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(
                        -width / 2.0F,
                        0.0F,
                        0.0F,
                        width,
                        length,
                        depth
                );

        if (index >= 5) {
            builder.texOffs(0, 0)
                    .addBox(
                            -width / 2.0F - 0.5F,
                            length - 1.0F,
                            -0.5F,
                            width + 1.0F,
                            3.0F,
                            depth + 1.0F
                    );
        }

        return bake("segment_" + index, builder);
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

        if (race.contains("arcosian")) {
            segments = arcosianTail;
            segmentCount = arcosianTail.length;
        } else if (race.contains("bio_android") || race.contains("cell")) {
            segments = bioTail;
            segmentCount = bioTail.length;
        } else {
            segments = saiyanTail;
            segmentCount = saiyanTail.length;
        }

        RenderType renderType = RenderTypes.entitySolid(WHITE_TEXTURE);
        float age = dbaState.dba$getTailAgeInTicks();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            stack.pushPose();

            body.translateAndRotate(stack);

            // Translate to lower back surface of torso (in meters: y=0.6F down, z=0.15F back)
            stack.translate(0.0F, 0.60F, 0.15F);

            float baseSwing = Mth.sin(age * 0.12F) * 0.12F;
            float sideSwing = Mth.cos(age * 0.09F) * 0.15F;

            for (int i = 0; i < segmentCount; i++) {
                ModelPart segment = segments[i];
                stack.pushPose();

                float progress = i / (float) Math.max(1, segmentCount - 1);
                float yaw;
                float pitch;

                if (race.contains("arcosian")) {
                    yaw = sideSwing * (1.0F - progress) + Mth.sin(age * 0.10F + i * 0.8F) * 0.06F;
                    // Negative X pitch angles BACKWARD out from body!
                    pitch = -0.45F - progress * 0.15F + Mth.cos(age * 0.08F + i * 0.6F) * 0.05F;
                } else if (race.contains("bio_android") || race.contains("cell")) {
                    yaw = baseSwing * (1.0F - progress) + Mth.sin(age * 0.12F + i * 0.45F) * 0.06F;
                    pitch = -0.55F - progress * 0.25F;
                } else { // Saiyan / Half-Saiyan
                    yaw = baseSwing * (1.0F - progress) + Mth.sin(age * 0.10F + i * 0.55F) * 0.08F;
                    pitch = -0.50F - progress * 0.20F + Mth.cos(age * 0.08F + i * 0.4F) * 0.06F;
                }

                stack.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
                stack.mulPose(com.mojang.math.Axis.XP.rotation(pitch));

                int tailColor;
                if (race.contains("arcosian")) {
                    tailColor = 0xFFE5D0FF; // Arcosian lavender/white
                } else if (race.contains("bio_android") || race.contains("cell")) {
                    tailColor = 0xFF4FBC5A; // Bio-Android green
                } else {
                    tailColor = 0xFF8A5A38; // Saiyan brown fur
                }

                segment.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        tailColor
                );

                stack.popPose();

                float length = (race.contains("arcosian") ? 4.0F : (race.contains("bio_android") ? 4.2F : 4.0F));
                // Step outward along tail segment in meters (1 pixel = 1/16 meter)
                stack.translate(0.0F, length / 16.0F, 0.0F);
            }

            stack.popPose();
        });
    }
}
