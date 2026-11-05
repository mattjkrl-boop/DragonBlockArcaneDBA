package com.dragonblockarcanedba.ability;

import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.ModelDisplayEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Spawns the modeled presentation set of an ability definition's
 * "models" block (Models Pass). Two entry points mirror the two
 * moments a def can ask for models: at cast fire (called from the
 * one carrier spawn path, so casts, summons, moves, and channels
 * all dress the same way) and at a cloud carrier's finale. The
 * set is pure presentation: positions come from the formation,
 * the ground from the heightmap, and every behavior clock from
 * the data. Nothing here deals damage or reads combat state.
 */
public final class ModelDisplaySpawner {
    private ModelDisplaySpawner() {}

    /** Fire trigger: anchor is the spawned carrier when there is one, else the caster. */
    public static void spawnForFire(ServerPlayer caster, AbilityDef def, Entity carrier, int seed) {
        AbilityDef.ModelSpec spec = def.models();
        if (spec == null || !"fire".equals(spec.trigger())) {
            return;
        }
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 anchor = carrier != null ? carrier.position() : caster.position();
        spawnSet(level, caster, def, spec, anchor, spec.radius(), seed);
    }

    /** Finale trigger: rains or plants the set across the finale area. */
    public static void spawnForFinale(ServerLevel level, AbilityDef def, Vec3 center,
                                      double areaRadius, UUID ownerUuid, int seed) {
        AbilityDef.ModelSpec spec = def.models();
        if (spec == null || !"finale".equals(spec.trigger())) {
            return;
        }
        double radius = spec.radius() > 0 ? spec.radius() : areaRadius;
        spawnSetRaw(level, def, spec, center, radius, ownerUuid, null, seed);
    }

    private static void spawnSet(ServerLevel level, ServerPlayer caster, AbilityDef def,
                                 AbilityDef.ModelSpec spec, Vec3 anchor, double radius, int seed) {
        spawnSetRaw(level, def, spec, anchor, radius, caster.getUUID(), caster, seed);
    }

    private static void spawnSetRaw(ServerLevel level, AbilityDef def, AbilityDef.ModelSpec specIn,
                                    Vec3 anchor, double radius, UUID ownerUuid,
                                    ServerPlayer caster, int seed) {
        AbilityDef.ModelSpec spec = specIn;
        if (spec.lifetimeTicks() <= 0) {
            // Derive the lifetime the data left open: the carrier's
            // own active window for fire sets, a fall safety cap for
            // finale rain, hang time plus the drop for hang sets.
            int derived = switch (spec.behavior()) {
                case "hang" -> spec.hangTicks() + 60;
                case "fall" -> 200;
                default -> def.phases().active();
            };
            spec = new AbilityDef.ModelSpec(spec.model(), spec.count(), spec.formation(),
                    spec.behavior(), spec.trigger(), spec.radius(), spec.height(), spec.scale(),
                    derived, spec.descendTicks(), spec.fallSpeed(), spec.spin(), spec.hangTicks(),
                    spec.speed(), spec.forward(), spec.up(),
                    spec.animSpawn(), spec.animHold(), spec.animEnd());
        }
        RandomSource random = RandomSource.create(seed * 31L + 17L);
        int count = spec.count();
        double baseAngle = random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < count; i++) {
            ModelDisplayEntity display = new ModelDisplayEntity(DbaEntities.MODEL_DISPLAY, level);
            Vec3 start;
            Vec3 boltVelocity = Vec3.ZERO;
            double targetY;
            float yaw = (float) (baseAngle * (180.0 / Math.PI));
            switch (spec.behavior()) {
                case "escort" -> {
                    start = caster != null ? caster.position() : anchor;
                    targetY = start.y;
                }
                case "bolt" -> {
                    Vec3 look = caster != null ? caster.getLookAngle() : new Vec3(0, 0, 1);
                    double speed = spec.speed() > 0 ? spec.speed()
                            : def.projectile() != null ? def.projectile().speed() : 2.4;
                    boltVelocity = look.scale(speed);
                    start = caster != null ? caster.getEyePosition() : anchor;
                    targetY = start.y;
                }
                case "fall" -> {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double r = radius * Math.sqrt(random.nextDouble());
                    double x = anchor.x + Math.cos(angle) * r;
                    double z = anchor.z + Math.sin(angle) * r;
                    targetY = groundY(level, x, z);
                    double stagger = random.nextDouble() * 6.0;
                    start = new Vec3(x, targetY + spec.height() + stagger, z);
                    yaw = (float) (random.nextDouble() * 360.0);
                }
                case "hang" -> {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double r = radius * Math.sqrt(random.nextDouble());
                    double x = anchor.x + Math.cos(angle) * r;
                    double z = anchor.z + Math.sin(angle) * r;
                    targetY = groundY(level, x, z);
                    start = new Vec3(x, anchor.y + spec.height(), z);
                    yaw = (float) (random.nextDouble() * 360.0);
                }
                default -> { // descend
                    double x = anchor.x;
                    double z = anchor.z;
                    if ("ring".equals(spec.formation()) && count > 1) {
                        double angle = baseAngle + (Math.PI * 2.0 * i) / count;
                        x += Math.cos(angle) * radius;
                        z += Math.sin(angle) * radius;
                        yaw = (float) (Math.atan2(x - anchor.x, z - anchor.z) * (180.0 / Math.PI));
                    } else if (radius > 0) {
                        double angle = random.nextDouble() * Math.PI * 2.0;
                        double r = radius * Math.sqrt(random.nextDouble());
                        x += Math.cos(angle) * r;
                        z += Math.sin(angle) * r;
                    }
                    targetY = groundY(level, x, z);
                    start = new Vec3(x, targetY + spec.height(), z);
                }
            }
            display.init(spec, ownerUuid, i, targetY, start, boltVelocity);
            display.setYRot(yaw);
            display.yRotO = yaw;
            level.addFreshEntity(display);
        }
    }

    private static double groundY(ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(x), (int) Math.floor(z));
    }
}
