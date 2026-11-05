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
 * Spawns the modeled set of an ability definition's "models" block
 * (Models Pass). Entry points cover cast, fire, step, latch, and
 * cloud finale. Positions come from the formation, the ground from
 * the heightmap, and every behavior clock from the data. Opt-in
 * collide and zone on a set are applied by ModelDisplayEntity;
 * this spawner only places the pieces and binds the ability id and
 * cast power so zone damage can reuse AbilityDamage.
 */
public final class ModelDisplaySpawner {
    private ModelDisplaySpawner() {}

    /** Cast trigger: spawned when a charge or startup begins; orbit pieces end with the cast. */
    public static void spawnForCast(ServerPlayer caster, AbilityDef def, int seed) {
        for (AbilityDef.ModelSpec spec : def.models()) {
            if ("cast".equals(spec.trigger()) && caster.level() instanceof ServerLevel level) {
                spawnSet(level, caster, def, spec, caster.position(), spec.radius(), seed, 1.0f);
            }
        }
    }

    /** Step trigger: one set at the spot a chain move hop leaves, facing the caster's yaw. */
    public static void spawnForStep(ServerPlayer caster, AbilityDef def, Vec3 from, float yaw, int seed) {
        for (AbilityDef.ModelSpec spec : def.models()) {
            if ("step".equals(spec.trigger()) && caster.level() instanceof ServerLevel level) {
                spawnSetRaw(level, def, spec, from, spec.radius(), caster.getUUID(), caster, seed, yaw, 1.0f);
            }
        }
    }

    /** Fire trigger: anchor is the spawned carrier when there is one, else the caster. */
    public static void spawnForFire(ServerPlayer caster, AbilityDef def, Entity carrier, float power, int seed) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        boolean alt = nextAlt(caster.getUUID(), def.id().toString());
        for (AbilityDef.ModelSpec specIn : def.models()) {
            if (!"fire".equals(specIn.trigger()) || power + 1.0e-3f < specIn.minPower()) {
                continue;
            }
            AbilityDef.ModelSpec spec = alt && specIn.animAlt() != null ? withHold(specIn, specIn.animAlt()) : specIn;
            if ("ride".equals(spec.behavior())) {
                // swarm shards host their own riders; any other carrier
                // (a projectile, a wave) carries this piece itself
                if (carrier != null && !(carrier instanceof com.dragonblockarcanedba.entity.SwarmHostEntity)) {
                    spawnRider(level, def, spec, carrier, power);
                }
                continue;
            }
            if ("orbit".equals(spec.behavior()) && carrier != null) {
                // orbit the carrier (a rift, a cyclone, a latched chain
                // cloud) and end with it
                spawnSetRaw(level, def, spec, carrier.position(), spec.radius(), carrier.getUUID(),
                        caster, seed, Float.NaN, power);
                continue;
            }
            if ("burst".equals(spec.behavior()) && "carrier".equals(spec.anchor()) && carrier != null) {
                spawnSetRaw(level, def, spec, carrier.position(), spec.radius(), caster.getUUID(),
                        caster, seed, carrier.getYRot(), power);
                continue;
            }
            // burst sets dress the caster's own body; the rest the carrier
            Vec3 anchor = carrier != null && !"burst".equals(spec.behavior())
                    ? carrier.position() : caster.position();
            float yaw = "burst".equals(spec.behavior()) ? caster.getYRot() : Float.NaN;
            spawnSetRaw(level, def, spec, anchor, spec.radius(), caster.getUUID(), caster, seed, yaw, power);
        }
    }

    /**
     * Latch trigger: a chain cloud latched a new target. Each "latch" set
     * orbits that target (radius 0 binds it) and plays its end clip when
     * the cloud lets it go or ends.
     */
    public static void spawnForLatch(ServerLevel level, AbilityDef def,
                                     com.dragonblockarcanedba.entity.CloudCarrierEntity cloud,
                                     Entity target, int seed) {
        for (AbilityDef.ModelSpec specIn : def.models()) {
            if (!"latch".equals(specIn.trigger())) {
                continue;
            }
            AbilityDef.ModelSpec spec = specIn.lifetimeTicks() > 0 ? specIn
                    : specIn.withLifetime(def.phases().active() + 20);
            RandomSource random = RandomSource.create(seed * 31L + 17L);
            double baseAngle = random.nextDouble() * 360.0;
            for (int i = 0; i < spec.count(); i++) {
                ModelDisplayEntity display = new ModelDisplayEntity(DbaEntities.MODEL_DISPLAY, level);
                display.init(spec, target.getUUID(), i, target.getY(), target.position(), Vec3.ZERO);
                // Latch pieces orbit the target; caster is the cloud's owner.
                display.bindAbility(def.id(), 1.0f, cloud.getOwnerUuid());
                display.bindToLatch(cloud.getUUID());
                display.setYRot((float) baseAngle);
                display.yRotO = display.getYRot();
                level.addFreshEntity(display);
            }
        }
    }

    /** Finale trigger: rains or plants the set across the finale area. */
    public static void spawnForFinale(ServerLevel level, AbilityDef def, Vec3 center,
                                      double areaRadius, UUID ownerUuid, int seed) {
        for (AbilityDef.ModelSpec spec : def.models()) {
            if ("finale".equals(spec.trigger())) {
                spawnFinaleSet(level, def, spec, center, areaRadius, ownerUuid, seed);
            }
        }
    }

    private static final java.util.Map<String, Boolean> ALT = new java.util.concurrent.ConcurrentHashMap<>();

    /** Flips and returns the per caster, per def alternation bit. */
    private static boolean nextAlt(UUID caster, String defId) {
        return ALT.merge(caster + "|" + defId, Boolean.TRUE, (a, b) -> !a) == Boolean.FALSE;
    }

    private static AbilityDef.ModelSpec withHold(AbilityDef.ModelSpec s, String hold) {
        return s.withHold(hold);
    }

    private static void spawnFinaleSet(ServerLevel level, AbilityDef def, AbilityDef.ModelSpec spec,
                                       Vec3 center, double areaRadius, UUID ownerUuid, int seed) {
        double radius = spec.radius() > 0 ? spec.radius() : areaRadius;
        spawnSetRaw(level, def, spec, center, radius, ownerUuid, null, seed, Float.NaN, 1.0f);
    }

    private static void spawnSet(ServerLevel level, ServerPlayer caster, AbilityDef def,
                                 AbilityDef.ModelSpec spec, Vec3 anchor, double radius, int seed,
                                 float power) {
        spawnSetRaw(level, def, spec, anchor, radius, caster.getUUID(), caster, seed, Float.NaN, power);
    }

    private static void spawnSetRaw(ServerLevel level, AbilityDef def, AbilityDef.ModelSpec specIn,
                                    Vec3 anchor, double radius, UUID ownerUuid,
                                    ServerPlayer caster, int seed, float fixedYaw, float power) {
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
            spec = spec.withLifetime(derived);
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
                case "escort", "orbit", "ride" -> {
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
                case "burst" -> {
                    double x = anchor.x;
                    double z = anchor.z;
                    if ("ring".equals(spec.formation()) && count > 1) {
                        double angle = baseAngle + (Math.PI * 2.0 * i) / count;
                        x += Math.cos(angle) * radius;
                        z += Math.sin(angle) * radius;
                        yaw = ModelDisplayEntity.yawFacing(x - anchor.x, z - anchor.z);
                    } else if (!Float.isNaN(fixedYaw)) {
                        yaw = ModelDisplayEntity.yawFacingLook(fixedYaw);
                        // point bursts sit models.forward blocks along the facing
                        double fy = Math.toRadians(fixedYaw);
                        x += -Math.sin(fy) * spec.forward();
                        z += Math.cos(fy) * spec.forward();
                    }
                    // stagger ring pieces up and down the body
                    double lift = count > 1 ? ((i % 3) - 1) * 0.35 : 0.0;
                    targetY = anchor.y + spec.up() + lift;
                    start = new Vec3(x, targetY, z);
                }
                default -> { // descend
                    double x = anchor.x;
                    double z = anchor.z;
                    if ("ring".equals(spec.formation()) && count > 1) {
                        double angle = baseAngle + (Math.PI * 2.0 * i) / count;
                        x += Math.cos(angle) * radius;
                        z += Math.sin(angle) * radius;
                        yaw = ModelDisplayEntity.yawFacing(x - anchor.x, z - anchor.z);
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
            UUID casterId = caster != null ? caster.getUUID() : ownerUuid;
            display.bindAbility(def.id(), power, casterId);
            if ("cast".equals(spec.trigger())) {
                display.bindToCast(def.id());
            }
            display.setYRot(yaw);
            display.yRotO = yaw;
            level.addFreshEntity(display);
        }
    }

    /** ride sets: one piece glued to the given host entity (swarm shards). */
    public static void spawnRider(ServerLevel level, AbilityDef.ModelSpec spec, Entity host) {
        ModelDisplayEntity display = new ModelDisplayEntity(DbaEntities.MODEL_DISPLAY, level);
        display.init(spec, host.getUUID(), 0, host.getY(), host.position(), Vec3.ZERO);
        display.setYRot(ModelDisplayEntity.yawFacingLook(host.getYRot()));
        display.yRotO = display.getYRot();
        level.addFreshEntity(display);
    }

    /** ride set with ability binding so an opted-in zone can resolve damage. */
    public static void spawnRider(ServerLevel level, AbilityDef def, AbilityDef.ModelSpec spec,
                                  Entity host, float power) {
        ModelDisplayEntity display = new ModelDisplayEntity(DbaEntities.MODEL_DISPLAY, level);
        display.init(spec, host.getUUID(), 0, host.getY(), host.position(), Vec3.ZERO);
        Entity owner = host instanceof net.minecraft.world.entity.projectile.Projectile pr ? pr.getOwner() : null;
        display.bindAbility(def.id(), power, owner != null ? owner.getUUID() : null);
        display.setYRot(ModelDisplayEntity.yawFacingLook(host.getYRot()));
        display.yRotO = display.getYRot();
        level.addFreshEntity(display);
    }

    private static double groundY(ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(x), (int) Math.floor(z));
    }
}
