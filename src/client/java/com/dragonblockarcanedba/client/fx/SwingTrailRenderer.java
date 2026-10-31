package com.dragonblockarcanedba.client.fx;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.item.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Shared swing trail ribbon for bladed weapons. Each client tick, while a player is mid swing with a blade,
 * the hilt and tip positions of an approximate swing arc are sampled. The ribbon between successive samples
 * is drawn with the emissive ki render type and fades with age. Purely client side, no networking.
 */
public final class SwingTrailRenderer {
    private SwingTrailRenderer() {}

    private static final int MAX_AGE = 6;
    private static final int SUBSTEPS = 4;

    private record Sample(Vec3 hilt, Vec3 tip, int born) {}

    private static final class Trail {
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        int color;
    }

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();
    private static int clock;

    /** Returns the trail color for a blade item, or 0 when the item gets no trail. */
    public static int colorFor(Item item) {
        if (item instanceof ZSwordItem) return 0xBFE6FF;
        if (item instanceof KatanaItem) return 0xFFFFFF;
        if (item instanceof SaberItem) return 0x66CCFF;
        if (item instanceof DaburaSwordItem) return 0xC23BFF;
        if (item instanceof CurseBladeItem) return 0x8A2BE2;
        if (item instanceof HollowsEdgeItem) return 0x5A5A7A;
        if (item instanceof AzureDragonSwordItem) return 0x3FA9FF;
        if (item instanceof BraveSwordItem) return 0xFFD54A;
        if (item instanceof GrandSwordItem) return 0xFFB347;
        if (item instanceof DimensionalSwordItem) return 0x9B6BFF;
        if (item instanceof SpiritSwordItem) return 0x7DF9FF;
        if (item instanceof SickleOfSorrowItem) return 0x8B0000;
        if (item instanceof OxKingsAxeItem) return 0xFF6A2B;
        if (item instanceof EvilSpearItem) return 0xB0003A;
        if (item instanceof DevilTridentItem) return 0xFF2244;
        return 0;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(SwingTrailRenderer::tick);
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            if (TRAILS.isEmpty()) return;
            Vec3 cam = context.levelState().cameraRenderState.pos;
            context.submitNodeCollector().submitCustomGeometry(context.poseStack(), KiRenderHelper.kiRenderType(), (pose, buffer) -> {
                Matrix4f mat = pose.pose();
                for (Trail trail : TRAILS.values()) {
                    float r = KiRenderHelper.red(trail.color), g = KiRenderHelper.green(trail.color), b = KiRenderHelper.blue(trail.color);
                    Sample prev = null;
                    for (Sample s : trail.samples) {
                        if (prev != null) {
                            float a0 = alpha(prev), a1 = alpha(s);
                            quad(buffer, mat, pose, prev, s, cam, r, g, b, a0 * 0.55f, a1 * 0.55f);
                        }
                        prev = s;
                    }
                }
            });
        });
    }

    private static float alpha(Sample s) {
        float t = 1f - (clock - s.born) / (float) MAX_AGE;
        return Math.max(0f, t);
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer buffer, Matrix4f mat, com.mojang.blaze3d.vertex.PoseStack.Pose pose,
                             Sample p, Sample q, Vec3 cam, float r, float g, float b, float a0, float a1) {
        float[] h0 = rel(p.hilt, cam), t0 = rel(p.tip, cam), h1 = rel(q.hilt, cam), t1 = rel(q.tip, cam);
        KiRenderHelper.vertex(buffer, mat, pose, h0[0], h0[1], h0[2], r, g, b, 0f, 0, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t0[0], t0[1], t0[2], r, g, b, a0, 1, 0, 0, 1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t1[0], t1[1], t1[2], r, g, b, a1, 1, 1, 0, 1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, h1[0], h1[1], h1[2], r, g, b, 0f, 0, 1, 0, 1, 0);
        // back face so the ribbon is visible from both sides
        KiRenderHelper.vertex(buffer, mat, pose, h1[0], h1[1], h1[2], r, g, b, 0f, 0, 1, 0, -1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t1[0], t1[1], t1[2], r, g, b, a1, 1, 1, 0, -1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, t0[0], t0[1], t0[2], r, g, b, a0, 1, 0, 0, -1, 0);
        KiRenderHelper.vertex(buffer, mat, pose, h0[0], h0[1], h0[2], r, g, b, 0f, 0, 0, 0, -1, 0);
    }

    private static float[] rel(Vec3 v, Vec3 cam) {
        return new float[] {(float) (v.x - cam.x), (float) (v.y - cam.y), (float) (v.z - cam.z)};
    }

    private static void tick(Minecraft mc) {
        clock++;
        if (mc.level == null || mc.isPaused()) {
            if (mc.level == null) TRAILS.clear();
            return;
        }
        for (Player player : mc.level.players()) {
            int color = colorFor(player.getMainHandItem().getItem());
            if (color == 0) continue;
            float anim = player.getAttackAnim(1.0f);
            float prevAnim = player.getAttackAnim(0.0f);
            if (anim <= 0f && prevAnim <= 0f) continue;
            Trail trail = TRAILS.computeIfAbsent(player.getUUID(), id -> new Trail());
            trail.color = color;
            for (int i = 1; i <= SUBSTEPS; i++) {
                float t = prevAnim + (anim - prevAnim) * (i / (float) SUBSTEPS);
                addSample(trail, player, t);
            }
        }
        Iterator<Map.Entry<UUID, Trail>> it = TRAILS.entrySet().iterator();
        while (it.hasNext()) {
            Trail trail = it.next().getValue();
            while (!trail.samples.isEmpty() && clock - trail.samples.peekFirst().born > MAX_AGE) trail.samples.pollFirst();
            if (trail.samples.isEmpty()) it.remove();
        }
    }

    /** Swing arc: blade sweeps from upper right to lower left in front of the player as anim goes 0 to 1. */
    private static void addSample(Trail trail, Player player, float anim) {
        float yaw = (float) Math.toRadians(player.getYRot());
        float pitch = (float) Math.toRadians(player.getXRot());
        double angle = Math.toRadians(-70 + 140 * anim);
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 up = forward.cross(right).normalize().scale(-1);
        Vec3 dir = forward.scale(Math.cos(angle) * 0.7).add(right.scale(-Math.sin(angle))).add(up.scale(Math.sin(angle) * 0.35)).normalize();
        Vec3 origin = player.getEyePosition().add(0, -0.35, 0);
        Vec3 hilt = origin.add(dir.scale(0.6));
        Vec3 tip = origin.add(dir.scale(2.0));
        trail.samples.addLast(new Sample(hilt, tip, clock));
        if (trail.samples.size() > 64) trail.samples.pollFirst();
    }
}
