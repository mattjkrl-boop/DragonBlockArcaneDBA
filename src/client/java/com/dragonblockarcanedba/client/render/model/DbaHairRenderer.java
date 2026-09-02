package com.dragonblockarcanedba.client.render.model;

import com.dragonblockarcanedba.client.render.layer.DbaPlayerState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal High-Fidelity 3D Dynamic Animated Hair Renderer for Dragon Block Arcane.
 * - Procedural 3D DBZ polygonal hair spikes attached directly to the humanoid skull.
 * - Dynamic head movement physics: head turning (yaw) and nodding/pitching (pitch) drive natural inertia and springy flex.
 * - Stride/locomotion harmonic bouncing, high-speed slipstream compression, and vertical jumping/falling inertia.
 * - Active transformation aura fluttering (SSJ, SSG, SSB, UI) and idle atmospheric anime wind breathing.
 * - Live dynamic hair color response for character customization and transformation overrides.
 */
public class DbaHairRenderer {

    private static final Identifier WHITE_TEX =
            Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/ki_white.png");

    private static final Map<Integer, Float> SMOOTHED_YAW_DRAG = new ConcurrentHashMap<>();
    private static final Map<Integer, Float> SMOOTHED_PITCH_DRAG = new ConcurrentHashMap<>();

    public static class HairSpike {
        public final float b0x, b0y, b0z;
        public final float b1x, b1y, b1z;
        public final float b2x, b2y, b2z;
        public final float b3x, b3y, b3z;
        public final float tx, ty, tz;

        public final float pivotX, pivotY, pivotZ;
        public final float inertiaWeight;
        public final float phaseOffset;

        public HairSpike(float b0x, float b0y, float b0z,
                         float b1x, float b1y, float b1z,
                         float b2x, float b2y, float b2z,
                         float b3x, float b3y, float b3z,
                         float tx, float ty, float tz,
                         float inertiaWeight, float phaseOffset) {
            this.b0x = b0x; this.b0y = b0y; this.b0z = b0z;
            this.b1x = b1x; this.b1y = b1y; this.b1z = b1z;
            this.b2x = b2x; this.b2y = b2y; this.b2z = b2z;
            this.b3x = b3x; this.b3y = b3y; this.b3z = b3z;
            this.tx = tx; this.ty = ty; this.tz = tz;

            this.pivotX = (b0x + b1x + b2x + b3x) / 4.0F;
            this.pivotY = (b0y + b1y + b2y + b3y) / 4.0F;
            this.pivotZ = (b0z + b1z + b2z + b3z) / 4.0F;
            this.inertiaWeight = inertiaWeight;
            this.phaseOffset = phaseOffset;
        }

        public void render(Matrix4f matrix, VertexConsumer consumer, int light, int overlay,
                           float r, float g, float b, float a,
                           float totalYaw, float totalPitch, float totalRoll, float totalScale) {

            // Calculate dynamic animated tip position rotated around base pivot
            float tipDynX = tx - pivotX;
            float tipDynY = (ty - pivotY) * totalScale;
            float tipDynZ = tz - pivotZ;

            // 1. Yaw (Y-axis rotation)
            float cosY = Mth.cos(totalYaw), sinY = Mth.sin(totalYaw);
            float x1 = tipDynX * cosY + tipDynZ * sinY;
            float z1 = -tipDynX * sinY + tipDynZ * cosY;

            // 2. Pitch (X-axis rotation)
            float cosP = Mth.cos(totalPitch), sinP = Mth.sin(totalPitch);
            float y2 = tipDynY * cosP - z1 * sinP;
            float z2 = tipDynY * sinP + z1 * cosP;

            // 3. Roll (Z-axis rotation)
            float cosR = Mth.cos(totalRoll), sinR = Mth.sin(totalRoll);
            float x3 = x1 * cosR - y2 * sinR;
            float y3 = x1 * sinR + y2 * cosR;

            float finalTipX = x3 + pivotX;
            float finalTipY = y3 + pivotY;
            float finalTipZ = z2 + pivotZ;

            // Render 4 side triangular faces + base quad
            addTriangle(matrix, consumer, light, overlay, r, g, b, a, b0x, b0y, b0z, b1x, b1y, b1z, finalTipX, finalTipY, finalTipZ);
            addTriangle(matrix, consumer, light, overlay, r, g, b, a, b1x, b1y, b1z, b2x, b2y, b2z, finalTipX, finalTipY, finalTipZ);
            addTriangle(matrix, consumer, light, overlay, r, g, b, a, b2x, b2y, b2z, b3x, b3y, b3z, finalTipX, finalTipY, finalTipZ);
            addTriangle(matrix, consumer, light, overlay, r, g, b, a, b3x, b3y, b3z, b0x, b0y, b0z, finalTipX, finalTipY, finalTipZ);

            // Base quad
            addTriangle(matrix, consumer, light, overlay, r, g, b, a, b0x, b0y, b0z, b2x, b2y, b2z, b1x, b1y, b1z);
            addTriangle(matrix, consumer, light, overlay, r, g, b, a, b0x, b0y, b0z, b3x, b3y, b3z, b2x, b2y, b2z);
        }

        private static void addTriangle(Matrix4f matrix, VertexConsumer consumer, int light, int overlay,
                                        float r, float g, float b, float a,
                                        float x0, float y0, float z0,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2) {
            float ax = x1 - x0, ay = y1 - y0, az = z1 - z0;
            float bx = x2 - x0, by = y2 - y0, bz = z2 - z0;
            float cx = ay * bz - az * by;
            float cy = az * bx - ax * bz;
            float cz = ax * by - ay * bx;
            float len = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            float nx = 0.0F, ny = 1.0F, nz = 0.0F;
            if (len > 0.0001F) {
                nx = cx / len; ny = cy / len; nz = cz / len;
            }

            consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
            consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(0.5F, 1.0F).setOverlay(overlay).setLight(light).setNormal(nx, ny, nz);
        }
    }

    private static final List<HairSpike> SAIYAN_SPIKES = new ArrayList<>();
    private static final List<HairSpike> HALF_SAIYAN_SPIKES = new ArrayList<>();
    private static final List<HairSpike> SSJ3_SPIKES_EXTRA = new ArrayList<>();

    static {
        buildSaiyanSpikes();
        buildHalfSaiyanSpikes();
        buildSSJ3SpikesExtra();
    }

    private static void buildSaiyanSpikes() {
        // 1. Central Massive Soaring Crown Spike (Goku/Vegeta crest)
        SAIYAN_SPIKES.add(new HairSpike(
                -0.12F, -0.48F, -0.10F,
                 0.12F, -0.48F, -0.10F,
                 0.10F, -0.50F,  0.12F,
                -0.10F, -0.50F,  0.12F,
                 0.00F, -0.84F,  0.10F,
                 1.20F, 0.0F
        ));

        // 2. Left-Upper Crown Spike (Swept up-left-back)
        SAIYAN_SPIKES.add(new HairSpike(
                -0.20F, -0.48F, -0.05F,
                -0.08F, -0.49F, -0.05F,
                -0.06F, -0.50F,  0.15F,
                -0.18F, -0.49F,  0.15F,
                -0.28F, -0.76F,  0.18F,
                 1.05F, 0.8F
        ));

        // 3. Right-Upper Crown Spike (Swept up-right-back)
        SAIYAN_SPIKES.add(new HairSpike(
                 0.08F, -0.49F, -0.05F,
                 0.20F, -0.48F, -0.05F,
                 0.18F, -0.49F,  0.15F,
                 0.06F, -0.50F,  0.15F,
                 0.28F, -0.76F,  0.18F,
                 1.05F, 1.6F
        ));

        // 4. Far Left Outer Flaring Spike
        SAIYAN_SPIKES.add(new HairSpike(
                -0.25F, -0.38F, -0.12F,
                -0.22F, -0.48F, -0.05F,
                -0.22F, -0.48F,  0.12F,
                -0.25F, -0.38F,  0.10F,
                -0.42F, -0.58F,  0.08F,
                 0.90F, 2.4F
        ));

        // 5. Far Right Outer Flaring Spike
        SAIYAN_SPIKES.add(new HairSpike(
                 0.22F, -0.48F, -0.05F,
                 0.25F, -0.38F, -0.12F,
                 0.25F, -0.38F,  0.10F,
                 0.22F, -0.48F,  0.12F,
                 0.42F, -0.58F,  0.08F,
                 0.90F, 3.2F
        ));

        // 6. Rear Top Occipital Spike (Swept back)
        SAIYAN_SPIKES.add(new HairSpike(
                -0.12F, -0.48F, 0.12F,
                 0.12F, -0.48F, 0.12F,
                 0.10F, -0.36F, 0.25F,
                -0.10F, -0.36F, 0.25F,
                 0.00F, -0.65F, 0.40F,
                 0.85F, 4.0F
        ));

        // 7. Left-Back Lower Flare
        SAIYAN_SPIKES.add(new HairSpike(
                -0.24F, -0.44F, 0.10F,
                -0.10F, -0.44F, 0.18F,
                -0.08F, -0.28F, 0.26F,
                -0.22F, -0.28F, 0.22F,
                -0.26F, -0.48F, 0.38F,
                 0.70F, 4.8F
        ));

        // 8. Right-Back Lower Flare
        SAIYAN_SPIKES.add(new HairSpike(
                 0.10F, -0.44F, 0.18F,
                 0.24F, -0.44F, 0.10F,
                 0.22F, -0.28F, 0.22F,
                 0.08F, -0.28F, 0.26F,
                 0.26F, -0.48F, 0.38F,
                 0.70F, 5.6F
        ));

        // 9. Forehead Hairline Fringe / Brow Tufts
        SAIYAN_SPIKES.add(new HairSpike(
                -0.15F, -0.44F, -0.24F,
                 0.15F, -0.44F, -0.24F,
                 0.12F, -0.48F, -0.16F,
                -0.12F, -0.48F, -0.16F,
                 0.00F, -0.42F, -0.28F,
                 0.40F, 0.5F
        ));

        // 10. Left Front Bang Tip
        SAIYAN_SPIKES.add(new HairSpike(
                -0.22F, -0.42F, -0.22F,
                -0.12F, -0.44F, -0.24F,
                -0.14F, -0.48F, -0.18F,
                -0.22F, -0.46F, -0.18F,
                -0.16F, -0.36F, -0.29F,
                 0.65F, 1.2F
        ));
    }

    private static void buildHalfSaiyanSpikes() {
        // 1. Prominent Sweeping Front-Left Bang (Teen Gohan / Trunks style)
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                -0.18F, -0.46F, -0.22F,
                -0.04F, -0.46F, -0.24F,
                -0.06F, -0.49F, -0.12F,
                -0.16F, -0.49F, -0.12F,
                -0.10F, -0.28F, -0.36F,
                 1.35F, 0.2F
        ));

        // 2. Right Front Bang
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                 0.04F, -0.46F, -0.24F,
                 0.18F, -0.46F, -0.22F,
                 0.16F, -0.49F, -0.12F,
                 0.06F, -0.49F, -0.12F,
                 0.14F, -0.34F, -0.34F,
                 1.15F, 1.0F
        ));

        // 3. Sharp Elevated Crown Spike
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                -0.10F, -0.48F, -0.12F,
                 0.10F, -0.48F, -0.12F,
                 0.12F, -0.50F,  0.08F,
                -0.12F, -0.50F,  0.08F,
                 0.04F, -0.80F, -0.04F,
                 1.20F, 1.8F
        ));

        // 4. Left Flare Spike
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                -0.24F, -0.46F, -0.10F,
                -0.12F, -0.48F, -0.05F,
                -0.10F, -0.48F,  0.12F,
                -0.22F, -0.46F,  0.10F,
                -0.34F, -0.68F,  0.06F,
                 0.95F, 2.6F
        ));

        // 5. Right Flare Spike
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                 0.12F, -0.48F, -0.05F,
                 0.24F, -0.46F, -0.10F,
                 0.22F, -0.46F,  0.10F,
                 0.10F, -0.48F,  0.12F,
                 0.34F, -0.66F,  0.06F,
                 0.95F, 3.4F
        ));

        // 6. Back Occipital Layer
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                -0.14F, -0.46F,  0.10F,
                 0.14F, -0.46F,  0.10F,
                 0.12F, -0.34F,  0.25F,
                -0.12F, -0.34F,  0.25F,
                 0.00F, -0.60F,  0.36F,
                 0.80F, 4.2F
        ));

        // 7. Left Sidehair Lock
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                -0.25F, -0.42F, -0.12F,
                -0.22F, -0.44F,  0.05F,
                -0.22F, -0.30F,  0.05F,
                -0.25F, -0.30F, -0.10F,
                -0.28F, -0.32F, -0.02F,
                 0.60F, 5.0F
        ));

        // 8. Right Sidehair Lock
        HALF_SAIYAN_SPIKES.add(new HairSpike(
                 0.22F, -0.44F,  0.05F,
                 0.25F, -0.42F, -0.12F,
                 0.25F, -0.30F, -0.10F,
                 0.22F, -0.30F,  0.05F,
                 0.28F, -0.32F, -0.02F,
                 0.60F, 5.8F
        ));
    }

    private static void buildSSJ3SpikesExtra() {
        // Cascading massive Super Saiyan 3 mane trailing down the back
        SSJ3_SPIKES_EXTRA.add(new HairSpike(
                -0.16F, -0.38F, 0.22F,
                 0.16F, -0.38F, 0.22F,
                 0.14F, -0.15F, 0.32F,
                -0.14F, -0.15F, 0.32F,
                 0.00F,  0.05F, 0.44F,
                 1.40F, 0.4F
        ));

        SSJ3_SPIKES_EXTRA.add(new HairSpike(
                -0.14F, -0.15F, 0.30F,
                 0.14F, -0.15F, 0.30F,
                 0.10F,  0.15F, 0.36F,
                -0.10F,  0.15F, 0.36F,
                 0.00F,  0.55F, 0.46F,
                 1.65F, 1.2F
        ));

        SSJ3_SPIKES_EXTRA.add(new HairSpike(
                -0.22F, -0.25F, 0.20F,
                -0.12F, -0.25F, 0.28F,
                -0.08F,  0.08F, 0.32F,
                -0.18F,  0.08F, 0.24F,
                -0.20F,  0.42F, 0.38F,
                 1.50F, 2.0F
        ));

        SSJ3_SPIKES_EXTRA.add(new HairSpike(
                 0.12F, -0.25F, 0.28F,
                 0.22F, -0.25F, 0.20F,
                 0.18F,  0.08F, 0.24F,
                 0.08F,  0.08F, 0.32F,
                 0.20F,  0.42F, 0.38F,
                 1.50F, 2.8F
        ));
    }

    /**
     * Determines whether the specified race renders coded-in 3D hair.
     */
    public static boolean hasCodedHair(String race) {
        if (race == null) return false;
        String r = race.toLowerCase();
        return r.contains("saiyan") || r.contains("half_saiyan") || r.equals("human");
    }

    /**
     * Dynamically renders animated 3D DBZ hair geometry responding to head turning, nodding,
     * locomotion bounce, vertical inertia, and transformation aura flutter.
     */
    public static void renderHair(Matrix4f matrix, VertexConsumer buffer, int packedLight, int overlay,
                                  String race, Identifier activeFormId, int hairColor,
                                  AvatarRenderState state, DbaPlayerState dbaState) {
        float r = ((hairColor >> 16) & 0xFF) / 255.0f;
        float g = ((hairColor >> 8) & 0xFF) / 255.0f;
        float b = (hairColor & 0xFF) / 255.0f;
        float a = 1.0f;

        if (hairColor == 0) {
            r = 0.12f; g = 0.12f; b = 0.12f;
        }

        // Clean up entity caches periodically
        if (SMOOTHED_YAW_DRAG.size() > 64) {
            SMOOTHED_YAW_DRAG.clear();
            SMOOTHED_PITCH_DRAG.clear();
        }

        int entityId = state.id;

        // 1. Head Turning Yaw Inertia (Looking left/right wiggles hair in opposite direction)
        float headYawVel = dbaState != null ? dbaState.dba$getYawVelocity() : 0.0F;
        float targetYawDrag = -Mth.clamp(headYawVel * 0.0075F, -0.14F, 0.14F);
        float prevYawDrag = SMOOTHED_YAW_DRAG.getOrDefault(entityId, 0.0F);
        float smoothedYawDrag = Mth.lerp(0.18F, prevYawDrag, targetYawDrag);
        SMOOTHED_YAW_DRAG.put(entityId, smoothedYawDrag);

        // 2. Head Pitch Nodding Inertia (Looking up/down flexes spikes vertically)
        float headPitchVel = dbaState != null ? dbaState.dba$getPitchVelocity() : 0.0F;
        float targetPitchDrag = -Mth.clamp(headPitchVel * 0.0065F, -0.12F, 0.12F);
        float prevPitchDrag = SMOOTHED_PITCH_DRAG.getOrDefault(entityId, 0.0F);
        float smoothedPitchDrag = Mth.lerp(0.18F, prevPitchDrag, targetPitchDrag);
        SMOOTHED_PITCH_DRAG.put(entityId, smoothedPitchDrag);

        // 3. Stride / Locomotion Harmonic Bounce
        float walkPos = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;
        float walkBounce = Mth.sin(walkPos * 1.2F) * (walkSpeed * 0.045F);
        float walkLateralSway = Mth.cos(walkPos * 0.6F) * (walkSpeed * 0.025F);

        // 4. Vertical Velocity Inertia (Jumping / Falling)
        float vertVel = dbaState != null ? dbaState.dba$getLocalVelocityY() : 0.0F;
        float vertInertia = -Mth.clamp(vertVel * 0.05F, -0.10F, 0.10F);

        // 5. Slipstream Drag (Sprinting pushes hair back)
        boolean isSprinting = dbaState != null && dbaState.dba$isSprinting();
        float speedDrag = isSprinting ? -0.04F : 0.0F;

        // 6. Ki Aura / Idle Wind Wave
        float age = dbaState != null ? dbaState.dba$getTailAgeInTicks() : state.ageInTicks;
        boolean isTransformed = activeFormId != null;
        float kiFreq = isTransformed ? 0.24F : 0.075F;
        float kiAmp = isTransformed ? 0.038F : 0.016F;
        float flareScale = isTransformed ? (1.04F + Mth.sin(age * 0.35F) * 0.03F) : 1.0F;

        String raceKey = race != null ? race.toLowerCase() : "saiyan";
        List<HairSpike> spikeList = raceKey.contains("half_saiyan") ? HALF_SAIYAN_SPIKES : SAIYAN_SPIKES;

        for (HairSpike spike : spikeList) {
            float weight = spike.inertiaWeight;
            float phase = spike.phaseOffset;

            // Compute dynamic compound flex angles for this individual spike
            float dynYaw = (smoothedYawDrag + walkLateralSway + Mth.sin(age * kiFreq + phase) * kiAmp) * weight;
            float dynPitch = (smoothedPitchDrag + vertInertia + walkBounce + speedDrag + Mth.cos(age * kiFreq * 0.85F + phase) * kiAmp) * weight;
            float dynRoll = (smoothedYawDrag * 0.45F + Mth.sin(age * kiFreq * 1.1F + phase) * kiAmp * 0.7F) * weight;

            spike.render(matrix, buffer, packedLight, overlay, r, g, b, a, dynYaw, dynPitch, dynRoll, flareScale);
        }

        // Render extended animated SSJ3 mane if active form is Super Saiyan 3
        if (activeFormId != null && activeFormId.getPath().contains("super_saiyan_3")) {
            for (HairSpike spike : SSJ3_SPIKES_EXTRA) {
                float weight = spike.inertiaWeight;
                float phase = spike.phaseOffset;
                float dynYaw = (smoothedYawDrag * 1.3F + walkLateralSway * 1.5F + Mth.sin(age * kiFreq * 0.8F + phase) * (kiAmp * 1.4F)) * weight;
                float dynPitch = (smoothedPitchDrag * 1.2F + vertInertia * 1.5F + walkBounce * 1.3F + speedDrag * 1.8F + Mth.cos(age * kiFreq * 0.7F + phase) * (kiAmp * 1.2F)) * weight;
                float dynRoll = (smoothedYawDrag * 0.6F + Mth.sin(age * kiFreq + phase) * (kiAmp * 1.1F)) * weight;

                spike.render(matrix, buffer, packedLight, overlay, r, g, b, a, dynYaw, dynPitch, dynRoll, flareScale);
            }
        }
    }

    public static RenderType getHairRenderType() {
        return RenderTypes.entitySolid(WHITE_TEX);
    }
}
