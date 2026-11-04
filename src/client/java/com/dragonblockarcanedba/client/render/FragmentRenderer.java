package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.ComplexGeometry;
import com.dragonblockarcanedba.entity.FragmentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws one FragmentEntity as a single ComplexGeometry shard through
 * submitSolidLit, seeded from the synced seed with the host's exact theme
 * gradient, so the entity piece is the same silhouette the renderer
 * pieces layer drew (ENTITY-STANDARD renderer rules). Fragments are
 * physical matter: they fly, land, and rest in the world, so they are
 * drawn with the entity's packed world light, never full bright. Their
 * theme colors stay; the world decides how bright they read. Alpha is 0.95 at
 * peak, matching the converted hosts; the fade envelope comes from the
 * synced FADE_START; FROZEN fragments grow on the TimeShatter travel
 * curve while they hang, keyed to the growth window preset at spawn
 * (synced GROWTH_WINDOW, never a release() stamp), on length and girth
 * alike, and the growth freezes at its reached value once the piece is
 * released. Held modes (FROZEN, FIELD_HELD) skip the spawn scale-in:
 * they appear at full size on their hang point or anchor.
 */
public class FragmentRenderer extends EntityRenderer<FragmentEntity, FragmentRenderer.FragmentState> {

    public static class FragmentState extends EntityRenderState {
        public int seed;
        public int rootArgb;
        public int tipArgb;
        public float length;
        public float girth;
        public int mode;
        public int fadeStart;
        public int releaseAt;
        public int growthWindow;
        public int age;
        public float xRot;
        public float yRot;
    }

    public FragmentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public FragmentState createRenderState() {
        return new FragmentState();
    }

    @Override
    public void extractRenderState(FragmentEntity entity, FragmentState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.seed = entity.getSeed();
        state.rootArgb = entity.getRootArgb();
        state.tipArgb = entity.getTipArgb();
        state.length = entity.getLength();
        state.girth = entity.getGirth();
        state.mode = entity.getMode();
        state.fadeStart = entity.getFadeStart();
        state.releaseAt = entity.getReleaseAt();
        state.growthWindow = entity.getGrowthWindow();
        state.age = entity.tickCount;
        state.xRot = entity.getXRot();
        state.yRot = entity.getYRot();
    }

    @Override
    public void submit(FragmentState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);

        float envelope = 1f;
        if (state.fadeStart >= 0) {
            envelope = 1f - Mth.clamp((state.age - state.fadeStart) / (float) FragmentEntity.FADE_TICKS, 0f, 1f);
        }
        if (envelope <= 0f) return;

        // Spawn scale-in, the same five tick read as TridentShardRenderer,
        // for the flying modes only. Held modes (FROZEN, FIELD_HELD) skip
        // it and appear at full size where they hang (ENTITY-STANDARD
        // describes the scale-in as except-FROZEN; the merge extends the
        // same read to FIELD_HELD, whose pieces materialize on an anchor
        // rather than flying in).
        float scaleIn = (state.mode == FragmentEntity.MODE_FROZEN
            || state.mode == FragmentEntity.MODE_FIELD_HELD)
            ? 1f
            : 0.5f + 0.5f * Math.min(1f, state.age / 5f);
        float drawLength = state.length * scaleIn;
        float drawGirth = state.girth * scaleIn;

        // FROZEN growth: while the piece hangs, its drawn size follows
        // the TimeShatter travel curve (0.55 to 1.40, cubic-out) over
        // the growth window preset at spawn, on length and girth alike
        // (the drawn fan this replaced rolled girth from the grown
        // length every frame). The window is the spawn preset, never
        // the current RELEASE_AT: release() restamps that field, so
        // keying growth to it flipped a host-released piece straight to
        // 1.40 on the release frame. Here the effective age is capped
        // at the release age, so once a piece drops, its growth freezes
        // at the value it had reached and can never step. For the
        // TimeShatter preset (window and RELEASE_AT both 36) this is
        // the original curve exactly: growth saturates at 1.40 at the
        // window end and holds there through the fall.
        if (state.mode == FragmentEntity.MODE_FROZEN && state.growthWindow > 0) {
            float effAge = state.releaseAt >= 0
                ? Math.min(state.age, state.releaseAt)
                : state.age;
            float t = Mth.clamp(effAge / (float) state.growthWindow, 0f, 1f);
            float inv = 1f - t;
            float growth = 0.55f + 0.85f * (1f - inv * inv * inv);
            drawLength *= growth;
            drawGirth *= growth;
        }

        Vec3 dir = Vec3.directionFromRotation(state.xRot, state.yRot);
        // Origin pulled back along the axis so the shard body centers on
        // the entity position, the TridentShardRenderer centering choice.
        final Vec3 origin = dir.scale(-drawLength * 0.35);
        final long seed = state.seed;
        final int root = state.rootArgb;
        final int tip = state.tipArgb;
        final float len = drawLength;
        final float girth = drawGirth;
        final float env = envelope;
        final int lightCoords = state.lightCoords;
        ComplexGeometry.submitSolidLit(collector, poseStack, lightCoords, (pose, buffer) ->
            ComplexGeometry.shard(pose, buffer, seed, origin, dir, len, girth,
                root, tip, 0.95f, env));
    }
}
