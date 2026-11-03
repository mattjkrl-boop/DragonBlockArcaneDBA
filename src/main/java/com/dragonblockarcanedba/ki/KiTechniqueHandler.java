package com.dragonblockarcanedba.ki;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server-side handler for executing Ki Technique attacks.
 *
 * Damage formula:
 *   kiPower  = currentKi × (usedPercent / 100.0)
 *   damage   = kiPower × (1.0 + willpower × 0.002)
 *
 * Ki Explosion:
 *   Forced 100% Ki. Damage = kiPower × 1.5, self = kiPower × 0.95
 *   Radius = kiPower / 100, min 3 blocks
 *   Damage falloff: linear from center
 */
public class KiTechniqueHandler {
    private static final java.util.Map<java.util.UUID, Long> LAST_FIRE_TIME = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Fires the Ki technique in the given slot for a player.
     * A charge of 0 releases on the press. A saved charge waits until the key has been held that long.
     */
    public static void fire(ServerPlayer player, int slot) {
        fire(player, slot, 0);
    }

    public static void fire(ServerPlayer player, int slot, int heldTicks) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        KiTechnique tech = accessor.dba$getKiTechniqueSlot(slot);
        if (tech != null && !tech.isEmpty) {
            int need = tech.getChargeTicks();
            if (need > 0 && heldTicks < need) {
                return;
            }
        }

        long now = player.level().getGameTime();
        long last = LAST_FIRE_TIME.getOrDefault(player.getUUID(), 0L);
        if (now - last < 4) {
            return; // Rate-limited: 4 ticks (0.2s) minimum between Ki fires to prevent macro packet flooding
        }
        LAST_FIRE_TIME.put(player.getUUID(), now);

        if (tech == null || tech.isEmpty) {
            player.sendSystemMessage(Component.literal("§cNo Ki Technique in slot " + (slot + 1)), true);
            return;
        }

        // Barrier is not a blast. The barrier move owns the shell.
        if (tech.type == KiTechniqueType.BARRIER) {
            KiBarrier.start(player, tech);
            return;
        }

        double currentKi = accessor.dba$getCurrentKi();
        if (currentKi <= 0) {
            player.sendSystemMessage(Component.literal("§cNot enough Ki!"), true);
            return;
        }

        int willpower = accessor.dba$getWillpower();
        ServerLevel level = (ServerLevel) player.level();
        float scale = visualScale(tech);

        if (tech.type == KiTechniqueType.EXPLOSION) {
            // Ki Explosion: always uses 100% Ki. Size changes the radius, not the damage.
            fireExplosion(player, accessor, level, currentKi, willpower, tech.color, scale);
        } else {
            double usedKi = currentKi * (tech.usedPercent / 100.0);
            double damage = usedKi * (1.0 + willpower * 0.002);

            // Drain Ki & Pause Ki Recovery (like weapons do)
            accessor.dba$addKi(-usedKi);
            accessor.dba$pauseKiRecovery(25);

            switch (tech.type) {
                case BLAST -> {
                    if (tech.isBarrage) {
                        fireBarrage(player, level, damage, tech.color, scale);
                    } else {
                        fireBlast(player, level, damage, tech.color, scale);
                    }
                }
                case SPIRAL_BEAM, BEAM -> fireBeam(player, level, damage, tech.color, tech.type == KiTechniqueType.SPIRAL_BEAM, scale);
                case DISK -> fireDisk(player, level, damage, tech.color, scale);
                case LASER -> fireLaser(player, level, damage, tech.color, scale);
                case EXPLOSION, BARRIER -> {}
            }

            player.sendSystemMessage(Component.literal("§b" + tech.type.displayName() + " §7— §a" + (int) damage + " dmg §7(used " + (int) usedKi + " Ki)"), true);
        }

        accessor.dba$syncStats();
    }

    /** 1.0 reproduces every current visual radius and hit radius. */
    private static float visualScale(KiTechnique tech) {
        double size = tech.getSize();
        if (Double.isNaN(size) || size <= 0.0) {
            return 1.0f;
        }
        return (float) size;
    }

    // =========== BLAST: single projectile along look direction ===========
    private static void fireBlast(ServerPlayer player, ServerLevel level, double damage, int color, float scale) {
        com.dragonblockarcanedba.entity.KiBlastEntity blast = new com.dragonblockarcanedba.entity.KiBlastEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_BLAST, level);
        blast.setOwner(player);
        blast.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        blast.setColor(color);
        blast.setDamage((float) damage);
        blast.setScale(scale);
        
        Vec3 look = player.getLookAngle();
        blast.shoot(look.x, look.y, look.z, 2.0f, 0.0f);
        level.addFreshEntity(blast);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 1.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.BLASTER_SHOT, SoundSource.PLAYERS, 1.2f, 1.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    // =========== BARRAGE: 8 smaller blasts with spread ===========
    private static void fireBarrage(ServerPlayer player, ServerLevel level, double damage, int color, float scale) {
        double perShot = damage / 8.0;
        Vec3 look = player.getLookAngle();

        for (int s = 0; s < 8; s++) {
            com.dragonblockarcanedba.entity.KiBlastEntity blast = new com.dragonblockarcanedba.entity.KiBlastEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_BLAST, level);
            blast.setOwner(player);
            blast.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            blast.setColor(color);
            blast.setDamage((float) perShot);
            blast.setScale(scale);
            
            // Add slight random spread
            double spreadX = (level.getRandom().nextDouble() - 0.5) * 0.15;
            double spreadY = (level.getRandom().nextDouble() - 0.5) * 0.15;
            Vec3 dir = look.add(spreadX, spreadY, 0).normalize();
            
            blast.shoot(dir.x, dir.y, dir.z, 2.0f, 0.0f);
            level.addFreshEntity(blast);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.6f + (float) (level.getRandom().nextFloat() * 0.4));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.BLASTER_SHOT, SoundSource.PLAYERS, 0.7f, 1.6f + (float) (level.getRandom().nextFloat() * 0.4));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2f, 1.7f);
    }

    // =========== BEAM: continuous line of damage along look direction ===========
    private static void fireBeam(ServerPlayer player, ServerLevel level, double damage, int color, boolean spiral, float scale) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();
        double range = 60;
        double hitRadius = 1.5 * scale;
        
        // Raycast to find true length. The hit radius scales with the beam the player sees.
        double hitLength = range;
        for (int i = 1; i <= (int) range; i++) {
            Vec3 pos = start.add(look.scale(i));
            List<LivingEntity> hit = getEntitiesAt(level, player, pos, hitRadius);
            if (!hit.isEmpty()) {
                hitLength = i;
                for (LivingEntity target : hit) {
                    target.hurtServer(level, player.damageSources().indirectMagic(player, player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) ((float) damage)));
                }
                break;
            }
        }

        if (spiral) {
            com.dragonblockarcanedba.entity.KiSpiralBeamEntity beam = new com.dragonblockarcanedba.entity.KiSpiralBeamEntity(level, player, (float) hitLength, color);
            beam.setScale(scale);
            level.addFreshEntity(beam);
        } else {
            com.dragonblockarcanedba.entity.KiBeamEntity beam = new com.dragonblockarcanedba.entity.KiBeamEntity(level, player, (float) hitLength, color);
            beam.setScale(scale);
            level.addFreshEntity(beam);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.8f, 1.4f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS, 1.8f, 1.4f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS, 1.5f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    // =========== DISK: horizontal disk that travels forward ===========
    private static void fireDisk(ServerPlayer player, ServerLevel level, double damage, int color, float scale) {
        com.dragonblockarcanedba.entity.KiDiskEntity disk = new com.dragonblockarcanedba.entity.KiDiskEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_DISK, level);
        disk.setOwner(player);
        // Start above head
        disk.setPos(player.getX(), player.getY() + player.getBbHeight() + 1.0, player.getZ());
        disk.setColor(color);
        disk.setDamage((float) damage);
        disk.setScale(scale);
        
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize(); // Horizontal only
        if (look.length() < 0.1) look = new Vec3(1, 0, 0);
        
        disk.shoot(look.x, look.y, look.z, 1.5f, 0.0f);
        level.addFreshEntity(disk);

        level.playSound(null, player.getX(), player.getY() + player.getBbHeight(), player.getZ(),
            SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.5f, 1.6f);
        level.playSound(null, player.getX(), player.getY() + player.getBbHeight(), player.getZ(),
            SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.2f, 1.8f);
    }

    // =========== LASER: twin rapid eye beams ===========
    private static void fireLaser(ServerPlayer player, ServerLevel level, double damage, int color, float scale) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        // Two parallel beams offset slightly from the eyes. Scale 1 keeps the current 0.15 gap.
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize().scale(0.15 * scale);

        double perBeam = damage / 2.0;
        double range = 45;
        double hitRadius = 1.0 * scale;
        
        for (Vec3 offset : new Vec3[]{right, right.scale(-1)}) {
            Vec3 start = eyePos.add(offset);
            
            // Raycast for length
            double hitLength = range;
            for (int i = 1; i <= (int) range; i++) {
                Vec3 pos = start.add(look.scale(i));
                List<LivingEntity> hit = getEntitiesAt(level, player, pos, hitRadius);
                if (!hit.isEmpty()) {
                    hitLength = i;
                    for (LivingEntity target : hit) {
                        target.hurtServer(level, player.damageSources().indirectMagic(player, player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) ((float) perBeam)));
                    }
                    break;
                }
            }
            
            com.dragonblockarcanedba.entity.KiLaserEntity laser = new com.dragonblockarcanedba.entity.KiLaserEntity(level, player, (float) hitLength, color);
            laser.setScale(scale);
            // Override position since it comes from the eyes offset
            laser.setPos(start.x, start.y, start.z);
            level.addFreshEntity(laser);
        }

        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4f, 2.0f);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(),
            SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.2f, 1.8f);
    }

    // =========== EXPLOSION: AoE last resort ===========
    private static void fireExplosion(ServerPlayer player, PlayerStatsAccessor accessor,
                                       ServerLevel level, double currentKi, int willpower, int color, float scale) {
        double kiPower = currentKi; // 100% Ki forced
        double totalDamage = kiPower * 1.5 * (1.0 + willpower * 0.002);
        double selfDamage = kiPower * 0.95 * (1.0 + willpower * 0.002);
        double radius = Math.max(3.0, kiPower / 100.0);
        if (radius > 50) radius = 50; // Safety cap, applied before size so scale 1 stays exactly here
        radius *= scale;

        // Drain ALL Ki & Pause Ki Recovery (like weapons do)
        accessor.dba$setCurrentKi(0);
        accessor.dba$pauseKiRecovery(60);

        // Find all entities in radius
        AABB box = new AABB(
            player.getX() - radius, player.getY() - radius, player.getZ() - radius,
            player.getX() + radius, player.getY() + radius, player.getZ() + radius
        );
        List<Entity> entities = level.getEntities(player, box);

        // Damage falloff: 150% at center → 0% at edge
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity target && target != player) {
                double dist = target.position().distanceTo(player.position());
                if (dist <= radius) {
                    double falloff = 1.0 - (dist / radius);
                    double entityDmg = totalDamage * falloff;
                    target.hurtServer(level, player.damageSources().indirectMagic(player, player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) ((float) entityDmg)));
                }
            }
        }

        // Self damage
        player.hurtServer(level, player.damageSources().magic(), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) ((float) selfDamage)));

        // Spawn visual explosion entity
        com.dragonblockarcanedba.entity.KiExplosionEntity explosion = new com.dragonblockarcanedba.entity.KiExplosionEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_EXPLOSION, level);
        explosion.setPos(player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ());
        explosion.setColor(color);
        explosion.setRadius((float) radius);
        level.addFreshEntity(explosion);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS, 2.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 2.0f, 0.6f);

        player.sendSystemMessage(
            Component.literal("§4§lKI EXPLOSION! §7— §c" + (int) totalDamage + " dmg §7(radius " + (int) radius + ") §4Self: " + (int) selfDamage),
            true
        );
    }

    // =========== Helpers ===========

    private static List<LivingEntity> getEntitiesAt(ServerLevel level, ServerPlayer caster, Vec3 pos, double radius) {
        AABB box = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                            pos.x + radius, pos.y + radius, pos.z + radius);
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> e != caster && e.isAlive());
    }

    public static void onPlayerDisconnect(java.util.UUID playerUuid) {
        LAST_FIRE_TIME.remove(playerUuid);
    }
}
