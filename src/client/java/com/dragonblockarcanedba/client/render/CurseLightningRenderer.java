package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.ki.LayeredRender;
import com.dragonblockarcanedba.entity.CurseLightningEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Entity Renderer for Curse Lightning Strikes in Minecraft 26.2.
 * The bespoke pass rebuilds the strike entirely on BespokeGeometry: the
 * trunk is the branch builder (a midpoint-displaced jagged ribbon, crimson
 * or violet sheath with a white hot core on the identical seeded path),
 * stabbing down from the cloud as the strike eases in, with a thinner twin
 * bolt beside it in place of the old hand-rolled forks. Where it lands, a
 * crackStar of ground arcs crawls outward over a white glowFan pool. All
 * seeds come from the entity UUID, so every client sees the same bolt.
 * (The Cursed Tempest's live strikes ride BoltEntity; this renderer serves
 * the registered CurseLightningEntity type.)
 */
public class CurseLightningRenderer extends EntityRenderer<CurseLightningEntity, CurseLightningRenderer.CurseLightningRenderState> {

    public CurseLightningRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class CurseLightningRenderState extends EntityRenderState {
        public boolean isRare;
        public float age;
        public long seed;
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(CurseLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(CurseLightningEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public CurseLightningRenderState createRenderState() {
        return new CurseLightningRenderState();
    }

    @Override
    public void extractRenderState(CurseLightningEntity entity, CurseLightningRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.isRare = entity.isRare();
        state.age = (CurseLightningEntity.LIFE_TICKS - entity.life) + partialTicks;
        state.seed = entity.getUUID().getMostSignificantBits();
    }

    @Override
    public void submit(CurseLightningRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(state, poseStack, collector, cameraState);

        float flashTime = state.age;
        // The entity lives exactly CurseLightningEntity.LIFE_TICKS and the
        // fade below reaches zero at tick 9.5, so picture and lifetime agree.
        if (flashTime > CurseLightningEntity.LIFE_TICKS) return;

        float strike = ease(Math.min(1.0f, flashTime / 2.5f));
        float fadeT = flashTime <= 2.5f ? 1.0f : Math.max(0.0f, 1.0f - (flashTime - 2.5f) / 7.0f);
        float flashAlpha = ease(fadeT);
        if (flashAlpha <= 0.02f) return;

        boolean rare = state.isRare;
        float sheathR = rare ? 0.55f : 0.80f;
        float sheathG = 0.04f;
        float sheathB = rare ? 0.95f : 0.40f;
        float ds = LayeredRender.distScale(cameraState, state.x, state.y, state.z);
        RenderType glowType = KiRenderHelper.kiRenderType();
        float cloud = 16.0f;
        long boltSeed = state.seed;

        collector.submitCustomGeometry(poseStack, glowType, (pose, buffer) -> {
            // The trunk stabs down as the strike eases in: its ground end
            // travels from the cloud to the strike point over 2.5 ticks.
            Vec3 top = new Vec3(0, cloud, 0);
            Vec3 ground = new Vec3(0, cloud * (1.0f - strike), 0);
            BespokeGeometry.branch(pose, buffer, boltSeed, top, ground, 0.30f,
                sheathR, sheathG, sheathB, flashAlpha, 1.0f, ds);
            BespokeGeometry.branch(pose, buffer, boltSeed, top, ground, 0.12f,
                1.0f, 0.94f, 0.98f, flashAlpha, 1.0f, ds);

            // Twin bolt: a thinner companion strike on its own seed, landing
            // beside the trunk. Replaces the old per-renderer fork helpers.
            BespokeGeometry.Rng rng = new BespokeGeometry.Rng(boltSeed ^ 0x7F11L);
            Vec3 twinGround = new Vec3(rng.range(-1.6f, 1.6f), cloud * (1.0f - strike), rng.range(-1.6f, 1.6f));
            BespokeGeometry.branch(pose, buffer, boltSeed ^ 0x7A11L, top, twinGround, 0.16f,
                sheathR, sheathG, sheathB, flashAlpha * 0.7f, 1.0f, ds);

            // Ground arcs crawling out from the strike point, over a white
            // hot pool, both fading with the flash.
            float groundRingRadius = 3.2f * ease(Math.min(1.0f, flashTime / 6.0f));
            float ringAlpha = flashAlpha * Math.max(0.0f, 1.0f - flashTime / 9.0f);
            if (ringAlpha > 0.01f && groundRingRadius > 0.1f) {
                BespokeGeometry.crackStar(pose, buffer, boltSeed ^ 0xC1A0L, groundRingRadius,
                    sheathR, sheathG, sheathB, ringAlpha * 0.80f, 1.0f, ds);
                BespokeGeometry.glowFan(pose, buffer, boltSeed ^ 0x610AL, groundRingRadius * 0.42f, 0.05f,
                    1.0f, 0.95f, 0.96f, ringAlpha, ds);
            }
        });

    }

    private static float ease(float t) {
        float c = Math.max(0.0f, Math.min(1.0f, t));
        return c * c * (3.0f - 2.0f * c);
    }
}
