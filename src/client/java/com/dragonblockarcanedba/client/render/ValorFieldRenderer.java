package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.client.render.ki.BespokeGeometry;
import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.entity.ValorFieldEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Renderer for Grand Sword's Valor Field in Minecraft 26.2.
 * Bespoke pass: every shape comes from BespokeGeometry builders or the
 * shared LayeredRender blocks, with fixed theme seeds (the field's radius
 * never changes, so its silhouette is identical on every client, the same
 * convention LayeredRender uses).
 * - displacedDome shell in valor gold with a pale inner shell standing in
 *   for the old three-stop gradient, squashed by the spawn rise factor
 * - LayeredRender.drawVortexBands for the interior energy banding
 * - brokenVortexBand ground mandala rings, lip band, and tilted orbitals
 * - crackStar for the eight ground rays
 * - Crossed shardFans as stasis cages around suspended projectiles
 */
public class ValorFieldRenderer extends EntityRenderer<ValorFieldEntity, ValorFieldRenderer.ValorFieldRenderState> {

    // Fixed theme seeds: a Valor Field always has the same radius, so a
    // constant seed keeps the dome silhouette stable and client-identical.
    private static final long SEED_DOME = 0xD09E5L;
    private static final long SEED_RING_OUTER = 0xB111L;
    private static final long SEED_RING_INNER = 0xB222L;
    private static final long SEED_RAYS = 0xBA75L;
    private static final long SEED_LIP = 0x1115L;

    public ValorFieldRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public static class ValorFieldRenderState extends EntityRenderState {
        public float radius = 9.0f;
        public float ageInTicks = 0;
        public float fadeStart = -1;
        public final List<Vec3> suspendedRelPositions = new ArrayList<>();
    }

    // 26.2 signature. No @Override: 26.3 added a float parameter to the vanilla method.
    public boolean shouldRender(ValorFieldEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    // 26.3 signature: vanilla calls this overload on 26.3 and up.
    public boolean shouldRender(ValorFieldEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
        return true;
    }

    @Override
    public ValorFieldRenderState createRenderState() {
        return new ValorFieldRenderState();
    }

    @Override
    public void extractRenderState(ValorFieldEntity entity, ValorFieldRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.radius = ValorFieldEntity.FIELD_RADIUS;
        state.ageInTicks = entity.tickCount + partialTicks;
        state.fadeStart = entity.getFadeStart();

        state.suspendedRelPositions.clear();
        // Cages only around projectiles the field actually suspended: the ids
        // come from the entity's own suspended map, synced to the client.
        // This used to cage any projectile moving slowly inside the radius,
        // including ones the field never touched.
        java.util.Set<Integer> suspendedIds = entity.getSuspendedIds();
        if (suspendedIds.isEmpty()) return;
        Vec3 fieldPos = entity.position();
        float r = state.radius;
        AABB box = new AABB(
            fieldPos.x - r, fieldPos.y - r, fieldPos.z - r,
            fieldPos.x + r, fieldPos.y + r, fieldPos.z + r
        );

        List<Projectile> projectiles = entity.level().getEntitiesOfClass(
            Projectile.class, box,
            p -> p.isAlive() && p != entity && suspendedIds.contains(p.getId())
                && p.distanceToSqr(fieldPos) <= r * r
        );

        for (Projectile p : projectiles) {
            Vec3 pPos = p.position();
            state.suspendedRelPositions.add(pPos.subtract(fieldPos));
        }
    }

    @Override
    public void submit(ValorFieldRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        RenderType renderType = KiRenderHelper.kiRenderType();

        float radius = state.radius;
        float age = state.ageInTicks;
        // Spawn fade-in over the first 10 ticks: the 9 block dome used to appear
        // at full strength in a single tick when cast.
        float spawnIn = Math.min(1.0f, age / 10.0f);
        float rise = smooth(spawnIn);
        // Fade out over FADE_OUT_TICKS once the entity starts dissipating, so
        // the dome leaves the same way it arrived instead of vanishing in one
        // tick on release, Ki exhaustion, death, or disconnect.
        float fadeOut = 1.0f;
        if (state.fadeStart >= 0) {
            fadeOut = 1.0f - smooth(Math.min(1.0f,
                (age - state.fadeStart) / (float) ValorFieldEntity.FADE_OUT_TICKS));
        }
        float pulse = (0.88f + 0.12f * (float) Math.sin(age * 0.18f)) * rise * fadeOut;
        float distScale = com.dragonblockarcanedba.client.render.ki.LayeredRender.distScale(
            cameraState, state.x, state.y, state.z);
        // Same dome height formula as the spectacle pass: the dome grows out
        // of the ground over the spawn rise instead of snapping to full size.
        float domeHeight = radius * 1.05f * (0.22f + 0.78f * rise);
        float heightScale = 1.05f * (0.22f + 0.78f * rise);

        poseStack.pushPose();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            // 0. Ground anchor glow (shared LayeredRender block): a soft gold
            // disc grounding the dome to the world at its base.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawAnchorGlow(pose, buffer,
                radius * 0.95f, 0.06f, 1.0f, 0.85f, 0.20f, 0.35f * pulse, distScale);

            // 1. Dome shell: displacedDome squashed by the rise factor, plus
            // a paler inner shell so the shell still reads two-tone.
            PoseStack domeStack = new PoseStack();
            domeStack.last().pose().set(pose.pose());
            domeStack.last().normal().set(pose.normal());
            domeStack.translate(0.0f, 0.1f, 0.0f);
            domeStack.scale(1.0f, heightScale, 1.0f);
            BespokeGeometry.displacedDome(domeStack.last(), buffer, SEED_DOME, radius,
                1.0f, 0.88f, 0.25f, 0.30f, pulse, distScale);
            BespokeGeometry.displacedDome(domeStack.last(), buffer, SEED_DOME ^ 0x1L, radius * 0.97f,
                1.0f, 0.97f, 0.70f, 0.13f, pulse, distScale);

            // 2. Interior energy banding (shared LayeredRender block): three
            // stacked broken bands twisting up the inside of the shell,
            // standing in for the old eight meridian ribs.
            com.dragonblockarcanedba.client.render.ki.LayeredRender.drawVortexBands(pose, buffer,
                radius * 0.92f, domeHeight * 0.92f, age,
                1.0f, 0.88f, 0.25f, pulse, distScale);

            // 3. Ground sanctum mandala: two counter-twisting broken rings
            // (twist rates match the old 6 and -12 degrees per tick rings).
            BespokeGeometry.brokenVortexBand(pose, buffer, SEED_RING_OUTER,
                radius * 0.98f, 0.05f, 0.30f, age * 0.10f,
                1.0f, 0.85f, 0.20f, 0.75f * pulse, distScale);
            BespokeGeometry.brokenVortexBand(pose, buffer, SEED_RING_INNER,
                radius * 0.62f, 0.08f, 0.26f, -age * 0.20f,
                1.0f, 0.95f, 0.40f, 0.82f * pulse, distScale);

            // Eight ground rays: one seeded crack star at field radius.
            BespokeGeometry.crackStar(pose, buffer, SEED_RAYS, radius,
                1.0f, 0.90f, 0.30f, 0.62f, pulse, distScale);

            // Lip band at 45 percent of the dome height, as before.
            float lipY = 0.1f + domeHeight * 0.45f;
            BespokeGeometry.brokenVortexBand(pose, buffer, SEED_LIP,
                radius * 0.88f, lipY - 0.10f, lipY + 0.10f, age * 0.06f,
                1.0f, 0.96f, 0.62f, 0.80f * pulse, distScale);

            // 4. Dual tilted orbital rings, plus and minus 35 degrees,
            // precessing in opposite directions as before.
            drawOrbital(pose, buffer, radius, 35.0f, age * 0.31f,
                1.0f, 0.90f, 0.30f, pulse, distScale);
            drawOrbital(pose, buffer, radius, -35.0f, -age * 0.31f,
                1.0f, 0.95f, 0.40f, pulse, distScale);

            // 5. Stasis cages: crossed shard fans around each suspended
            // projectile, seeded from its synced relative position so every
            // client draws the same cage.
            for (Vec3 relPos : state.suspendedRelPositions) {
                float sx = (float) relPos.x;
                float sy = (float) relPos.y;
                float sz = (float) relPos.z;
                long cageSeed = ((long) (sx * 1000.0f) * 73856093L)
                    ^ ((long) (sy * 1000.0f) * 19349663L)
                    ^ ((long) (sz * 1000.0f) * 83492791L);
                PoseStack cageStack = new PoseStack();
                cageStack.last().pose().set(pose.pose());
                cageStack.last().normal().set(pose.normal());
                cageStack.translate(sx, sy, sz);
                BespokeGeometry.shardFan(cageStack.last(), buffer, cageSeed, 0.70f,
                    1.0f, 0.88f, 0.20f, 0.85f * pulse, 1.0f, distScale);
                cageStack.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(180.0f)));
                BespokeGeometry.shardFan(cageStack.last(), buffer, cageSeed, 0.70f,
                    1.0f, 0.88f, 0.20f, 0.85f * pulse, 1.0f, distScale);
            }
        });

        poseStack.popPose();
        super.submit(state, poseStack, collector, cameraState);
    }

    /**
     * Pose glue only: tilts one brokenVortexBand into an orbital ring at
     * head height, then calls the builder. Emits no vertices of its own.
     */
    private static void drawOrbital(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                    float radius, float tiltDeg, float twist,
                                    float r, float g, float b, float envelope, float distScale) {
        PoseStack orbital = new PoseStack();
        orbital.last().pose().set(pose.pose());
        orbital.last().normal().set(pose.normal());
        orbital.translate(0.0f, 1.0f, 0.0f);
        orbital.mulPose(new org.joml.Matrix4f().rotation(Axis.XP.rotationDegrees(tiltDeg)));
        BespokeGeometry.brokenVortexBand(orbital.last(), buffer, SEED_RING_OUTER ^ (long) tiltDeg,
            radius * 0.96f, -0.12f, 0.12f, twist,
            r, g, b, 0.68f * envelope, distScale);
    }

    private static float smooth(float t) {
        float x = Math.max(0.0f, Math.min(1.0f, t));
        return x * x * (3.0f - 2.0f * x);
    }
}
