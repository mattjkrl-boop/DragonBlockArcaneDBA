package com.dragonblockarcanedba.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shared vortex forces (SPECTACLE-STANDARD point 3): the pull in, lift, and
 * fling out that the reference storm applies to everything inside it.
 * Weight classes mirror movement/MobPhysics: BOSS class (knockback resistance
 * at least 0.9 or max health at least 400) barely moves, heavy bodies move at
 * 0.45, light bodies at full strength. Players are pulled at 0.6 so a vortex
 * threatens without deleting player control.
 */
public final class VortexPhysics {
    private VortexPhysics() {}

    public static float weightScale(LivingEntity e) {
        var kb = e.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        double resistance = kb != null ? kb.getValue() : 0.0;
        if (resistance >= 0.9 || e.getMaxHealth() >= 400f) return 0.12f;
        var size = e.getBoundingBox();
        double volume = (size.maxX - size.minX) * (size.maxY - size.minY) * (size.maxZ - size.minZ);
        if (volume >= 4.0) return 0.45f;
        return 1.0f;
    }

    /**
     * One tick of vortex force on every living entity inside the cylinder.
     * strength is inward pull speed added per tick at the rim; swirl is the
     * tangential fraction of pull; lift is upward speed added near the core.
     * The owner and their allies-by-identity (same entity) are skipped; pass a
     * null owner to affect everything.
     */
    public static void applyVortex(ServerLevel level, Vec3 center, double radius, double height,
                                   LivingEntity owner, double strength, double swirl, double lift,
                                   float envelope) {
        if (envelope <= 0f || radius <= 0) return;
        AABB box = new AABB(center.x - radius, center.y - 1.0, center.z - radius,
                            center.x + radius, center.y + height, center.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner);
        for (LivingEntity target : targets) {
            Vec3 toCenter = new Vec3(center.x - target.getX(), 0, center.z - target.getZ());
            double dist = toCenter.length();
            if (dist > radius || dist < 0.001) continue;
            double falloff = 1.0 - (dist / radius) * 0.6; // stronger deeper in
            float w = weightScale(target) * (target instanceof net.minecraft.world.entity.player.Player ? 0.6f : 1.0f);
            Vec3 inward = toCenter.normalize();
            Vec3 tangent = new Vec3(-inward.z, 0, inward.x);
            Vec3 add = inward.scale(strength * falloff)
                .add(tangent.scale(strength * swirl * falloff));
            // Lift lives near the core, like the funnel's updraft.
            double coreFrac = 1.0 - dist / radius;
            double vy = lift * coreFrac * coreFrac;
            Vec3 vel = target.getDeltaMovement();
            target.setDeltaMovement(
                (vel.x + add.x * w * envelope),
                Math.min(vel.y + vy * w * envelope, 0.9),
                (vel.z + add.z * w * envelope));
            DbaCompat.markHurt(target);
        }
    }

    /** Ejection beat: throw one entity outward and up, weight scaled. */
    public static void fling(LivingEntity target, Vec3 from, double power, double up) {
        Vec3 dir = new Vec3(target.getX() - from.x, 0, target.getZ() - from.z);
        if (dir.lengthSqr() < 0.001) dir = new Vec3(1, 0, 0);
        dir = dir.normalize();
        float w = weightScale(target);
        Vec3 vel = target.getDeltaMovement();
        target.setDeltaMovement(
            vel.x + dir.x * power * w,
            Math.max(vel.y, up * w),
            vel.z + dir.z * power * w);
        DbaCompat.markHurt(target);
    }
}
