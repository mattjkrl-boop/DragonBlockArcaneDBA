package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.LavaGeyserEntity;
import com.dragonblockarcanedba.util.FxEnvelope;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * The geyser pillar (redesign idea 2): a bright bespoke plume core inside
 * a darker, wider plume, wrapped in a rising broken vortex band, over a
 * cracked and glowing base. All geometry comes from BespokeGeometry off
 * the entity's synced seed, so the pillar is the same for every client.
 */
public class LavaGeyserRenderer extends EntityRenderer<LavaGeyserEntity, LavaGeyserRenderer.GeyserState> {
    public LavaGeyserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class GeyserState extends EntityRenderState {
        public float age;
        public int seed;
        public double posX;
        public double posY;
        public double posZ;
    }

    public boolean shouldRender(LavaGeyserEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    public boolean shouldRender(LavaGeyserEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public GeyserState createRenderState() {
        return new GeyserState();
    }

    @Override
    public void extractRenderState(LavaGeyserEntity entity, GeyserState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.seed = entity.getSeed();
        state.posX = entity.getX();
        state.posY = entity.getY();
        state.posZ = entity.getZ();
    }

    @Override
    public void submit(GeyserState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        float scale = LayeredRender.distScale(cameraState, state.posX, state.posY, state.posZ);
        float env = FxEnvelope.envelope(state.age, LavaGeyserEntity.LIFE_TICKS);
        // The pillar is at full height almost at once, then burns down.
        float height = 4.4f * Math.min(1.0f, state.age / 2.5f);
        long seed = state.seed;
        RenderType type = KiRenderHelper.kiRenderType();
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            BespokeGeometry.plume(pose, buffer, seed ^ 0x6A11L, 1.18f, height * 0.82f,
                0.85f, 0.22f, 0.02f, LayeredRender.ALPHA_MID, env, scale);
            BespokeGeometry.plume(pose, buffer, seed, 0.8f, height,
                1.0f, 0.44f, 0.05f, LayeredRender.ALPHA_CORE, env, scale);
            BespokeGeometry.brokenVortexBand(pose, buffer, seed ^ 0xBEEFL, 1.3f, 0.1f, height * 0.85f,
                state.age * 0.22f, 1.0f, 0.5f, 0.08f, LayeredRender.ALPHA_OUTER * env, scale);
            BespokeGeometry.crackStar(pose, buffer, seed ^ 0xC4ACL, 2.3f,
                1.0f, 0.5f, 0.1f, LayeredRender.ALPHA_MID, env, scale);
            BespokeGeometry.glowFan(pose, buffer, seed ^ 0x610FL, 2.1f, 0.07f,
                1.0f, 0.45f, 0.05f, LayeredRender.ALPHA_MID * env, scale);
        });
    }
}
