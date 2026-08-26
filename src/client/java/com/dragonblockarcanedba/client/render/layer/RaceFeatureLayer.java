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
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class RaceFeatureLayer extends RenderLayer<AvatarRenderState, EntityModel<AvatarRenderState>> {
    private static final Identifier WHITE_TEXTURE =
            Identifier.parse("dragonblockarcanedba:textures/entity/ki_white.png");

    private final ModelPart spikyHair;
    private final ModelPart longManeHair;
    private final ModelPart arcosianFeatures;
    private final ModelPart namekianFeatures;
    private final ModelPart majinFeatures;
    private final ModelPart majinTorso;
    private final ModelPart cellFeatures;
    private final ModelPart saiyanArmor;
    private final ModelPart alienEars;
    private final ModelPart skinHeadOverlay;
    private final ModelPart halo;

    public RaceFeatureLayer(RenderLayerParent<AvatarRenderState, EntityModel<AvatarRenderState>> renderer) {
        super(renderer);

        this.spikyHair = createSpikyHair();
        this.longManeHair = createLongManeHair();
        this.arcosianFeatures = createArcosianFeatures();
        this.namekianFeatures = createNamekianFeatures();
        this.majinFeatures = createMajinFeatures();
        this.majinTorso = createMajinTorso();
        this.cellFeatures = createCellFeatures();
        this.saiyanArmor = createSaiyanArmor();
        this.alienEars = createAlienEars();
        this.skinHeadOverlay = createSkinHeadOverlay();
        this.halo = createHalo();
    }

    private ModelPart createHalo() {
        CubeListBuilder builder = CubeListBuilder.create();
        // Golden ring floating above the head at Y = -12.0F
        builder.texOffs(0, 0).addBox(-3.5F, -12.0F, -3.5F, 7.0F, 0.8F, 1.0F);
        builder.texOffs(0, 0).addBox(-3.5F, -12.0F, 2.5F, 7.0F, 0.8F, 1.0F);
        builder.texOffs(0, 0).addBox(-3.5F, -12.0F, -2.5F, 1.0F, 0.8F, 5.0F);
        builder.texOffs(0, 0).addBox(2.5F, -12.0F, -2.5F, 1.0F, 0.8F, 5.0F);
        return bake(builder, "halo");
    }

    private ModelPart createSkinHeadOverlay() {
        CubeListBuilder builder = CubeListBuilder.create();
        // Subtle skin overlay around head for live skin tinting
        builder.texOffs(0, 0).addBox(-4.02F, -8.02F, -4.02F, 8.04F, 8.04F, 8.04F);
        return bake(builder, "head_skin");
    }

    private ModelPart bake(CubeListBuilder builder, String name) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(name, builder, PartPose.ZERO);
        return root.bake(64, 64);
    }

    private ModelPart createSpikyHair() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(-4.5F, -10.0F, -4.5F, 9.0F, 3.0F, 9.0F);

        builder.texOffs(0, 12)
                .addBox(-4.0F, -14.0F, -2.5F, 3.0F, 5.0F, 4.0F);

        builder.texOffs(14, 12)
                .addBox(1.0F, -15.0F, -2.5F, 3.0F, 6.0F, 4.0F);

        builder.texOffs(28, 12)
                .addBox(-6.0F, -11.0F, -1.5F, 3.0F, 4.0F, 4.0F);

        builder.texOffs(42, 12)
                .addBox(3.0F, -11.5F, -1.5F, 3.0F, 4.0F, 4.0F);

        builder.texOffs(0, 22)
                .addBox(-2.5F, -13.0F, 1.5F, 5.0F, 5.0F, 4.0F);

        builder.texOffs(18, 22)
                .addBox(-2.0F, -10.0F, -6.0F, 2.0F, 4.0F, 2.0F);

        builder.texOffs(26, 22)
                .addBox(0.0F, -10.5F, -6.0F, 2.0F, 4.0F, 2.0F);

        return bake(builder, "hair");
    }

    private ModelPart createLongManeHair() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(-4.5F, -10.0F, -4.5F, 9.0F, 4.0F, 9.0F);

        builder.texOffs(0, 14)
                .addBox(-5.0F, -9.0F, 2.0F, 10.0F, 12.0F, 3.0F);

        builder.texOffs(26, 14)
                .addBox(-3.5F, 1.0F, 2.5F, 7.0F, 8.0F, 2.5F);

        builder.texOffs(44, 14)
                .addBox(-5.5F, -8.0F, -4.5F, 3.0F, 7.0F, 2.0F);

        builder.texOffs(54, 14)
                .addBox(2.5F, -8.0F, -4.5F, 3.0F, 7.0F, 2.0F);

        builder.texOffs(0, 29)
                .addBox(-6.0F, -6.0F, 0.5F, 2.0F, 9.0F, 3.0F);

        builder.texOffs(10, 29)
                .addBox(4.0F, -6.0F, 0.5F, 2.0F, 9.0F, 3.0F);

        return bake(builder, "mane");
    }

    private ModelPart createArcosianFeatures() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(4.0F, -8.0F, -2.0F, 3.0F, 2.0F, 2.0F);

        builder.texOffs(10, 0)
                .addBox(5.0F, -13.0F, -2.5F, 2.0F, 5.0F, 2.0F);

        builder.texOffs(18, 0)
                .addBox(-7.0F, -8.0F, -2.0F, 3.0F, 2.0F, 2.0F);

        builder.texOffs(28, 0)
                .addBox(-7.0F, -13.0F, -2.5F, 2.0F, 5.0F, 2.0F);

        builder.texOffs(36, 0)
                .addBox(-1.5F, -7.5F, -4.5F, 3.0F, 3.0F, 1.0F);

        builder.texOffs(44, 0)
                .addBox(4.5F, 1.0F, -2.5F, 3.0F, 2.0F, 5.0F);

        builder.texOffs(54, 0)
                .addBox(-7.5F, 1.0F, -2.5F, 3.0F, 2.0F, 5.0F);

        return bake(builder, "arcosian");
    }

    private ModelPart createNamekianFeatures() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(4.0F, -6.0F, -1.0F, 4.0F, 2.0F, 1.0F);

        builder.texOffs(10, 0)
                .addBox(-8.0F, -6.0F, -1.0F, 4.0F, 2.0F, 1.0F);

        builder.texOffs(20, 0)
                .addBox(0.5F, -12.0F, -1.0F, 1.0F, 5.0F, 1.0F);

        builder.texOffs(26, 0)
                .addBox(-1.5F, -12.0F, -1.0F, 1.0F, 5.0F, 1.0F);

        builder.texOffs(32, 0)
                .addBox(4.2F, 2.5F, -1.5F, 1.0F, 4.0F, 3.0F);

        builder.texOffs(40, 0)
                .addBox(-5.2F, 2.5F, -1.5F, 1.0F, 4.0F, 3.0F);

        return bake(builder, "namekian");
    }

    private ModelPart createMajinFeatures() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(-1.5F, -10.0F, 1.0F, 3.0F, 3.0F, 4.0F);

        builder.texOffs(12, 0)
                .addBox(-1.0F, -9.0F, 4.0F, 2.0F, 3.0F, 5.0F);

        builder.texOffs(20, 0)
                .addBox(-1.0F, -7.0F, 8.0F, 2.0F, 4.0F, 2.0F);

        builder.texOffs(28, 0)
                .addBox(-5.0F, 0.0F, 2.2F, 10.0F, 14.0F, 1.0F);

        return bake(builder, "majin");
    }

    private ModelPart createMajinTorso() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(-6.0F, 0.0F, -4.0F, 12.0F, 12.0F, 8.0F);

        return bake(builder, "fat_torso");
    }

    private ModelPart createCellFeatures() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(2.0F, -13.0F, -3.0F, 3.0F, 6.0F, 2.0F);

        builder.texOffs(10, 0)
                .addBox(-5.0F, -13.0F, -3.0F, 3.0F, 6.0F, 2.0F);

        builder.texOffs(20, 0)
                .addBox(1.0F, 1.0F, 2.5F, 5.0F, 12.0F, 2.0F);

        builder.texOffs(32, 0)
                .addBox(-6.0F, 1.0F, 2.5F, 5.0F, 12.0F, 2.0F);

        return bake(builder, "cell");
    }

    private ModelPart createSaiyanArmor() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(4.5F, -1.0F, -3.0F, 3.0F, 3.0F, 6.0F);

        builder.texOffs(20, 0)
                .addBox(-7.5F, -1.0F, -3.0F, 3.0F, 3.0F, 6.0F);

        builder.texOffs(40, 0)
                .addBox(-4.5F, 0.5F, -2.5F, 9.0F, 10.0F, 1.0F);

        return bake(builder, "armor");
    }

    private ModelPart createAlienEars() {
        CubeListBuilder builder = CubeListBuilder.create();

        builder.texOffs(0, 0)
                .addBox(4.0F, -7.0F, -1.0F, 5.0F, 3.0F, 1.0F);

        builder.texOffs(12, 0)
                .addBox(-9.0F, -7.0F, -1.0F, 5.0F, 3.0F, 1.0F);

        return bake(builder, "alien");
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

        Identifier raceId = dbaState.dba$getRaceId();
        String path = raceId != null ? raceId.getPath().toLowerCase() : "human";

        if (!(this.getParentModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        int skinColor = dbaState.dba$getSkinColor();
        int hairColor = dbaState.dba$getHairColor();

        ModelPart head = humanoidModel.head;
        ModelPart body = humanoidModel.body;

        RenderType renderType = RenderTypes.entitySolid(WHITE_TEXTURE);

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().pose().set(pose.pose());
            stack.last().normal().set(pose.normal());

            // Always render head skin overlay tinted by skinColor for real-time customization!
            stack.pushPose();
            head.translateAndRotate(stack);
            this.skinHeadOverlay.render(
                    stack,
                    buffer,
                    packedLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    skinColor
            );
            stack.popPose();

            // Render DBZ Golden Angel Halo when deceased in Otherworld
            if (dbaState.dba$isInOtherworld()) {
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
            }

            if (path.contains("saiyan")
                    || path.contains("human")
                    || path.contains("tuffle")
                    || path.contains("half_saiyan")) {

                stack.pushPose();
                head.translateAndRotate(stack);
                this.spikyHair.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        hairColor
                );
                stack.popPose();

                stack.pushPose();
                body.translateAndRotate(stack);
                this.saiyanArmor.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        0xFFE0E0E0
                );
                stack.popPose();

            } else if (path.contains("arcosian")) {
                stack.pushPose();
                head.translateAndRotate(stack);
                this.arcosianFeatures.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        skinColor
                );
                stack.popPose();

            } else if (path.contains("namekian")) {
                stack.pushPose();
                head.translateAndRotate(stack);
                this.namekianFeatures.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        skinColor
                );
                stack.popPose();

            } else if (path.contains("majin")) {
                stack.pushPose();
                head.translateAndRotate(stack);
                this.majinFeatures.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        skinColor
                );
                stack.popPose();

                stack.pushPose();
                body.translateAndRotate(stack);
                this.majinTorso.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        skinColor
                );
                stack.popPose();

            } else if (path.contains("bio_android") || path.contains("cell")) {
                stack.pushPose();
                head.translateAndRotate(stack);
                this.cellFeatures.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        0xFF50C850
                );
                stack.popPose();

            } else if (path.contains("yardrat") || path.contains("android")) {
                stack.pushPose();
                head.translateAndRotate(stack);
                this.alienEars.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        skinColor
                );
                stack.popPose();
            }

            if (path.contains("ssj4")) {
                stack.pushPose();
                head.translateAndRotate(stack);
                this.longManeHair.render(
                        stack,
                        buffer,
                        packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        hairColor
                );
                stack.popPose();
            }
        });
    }
}
