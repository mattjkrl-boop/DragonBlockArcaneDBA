package com.dragonblockarcanedba.ki;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    /**
     * Fires the Ki technique in the given slot for a player.
     */
    public static void fire(ServerPlayer player, int slot) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        KiTechnique tech = accessor.dba$getKiTechniqueSlot(slot);
        if (tech.isEmpty) {
            player.sendSystemMessage(Component.literal("§cNo Ki Technique in slot " + (slot + 1)), true);
            return;
        }

        double currentKi = accessor.dba$getCurrentKi();
        if (currentKi <= 0) {
            player.sendSystemMessage(Component.literal("§cNot enough Ki!"), true);
            return;
        }

        int willpower = accessor.dba$getWillpower();
        ServerLevel level = (ServerLevel) player.level();

        if (tech.type == KiTechniqueType.EXPLOSION) {
            // Ki Explosion: always uses 100% Ki
            fireExplosion(player, accessor, level, currentKi, willpower, tech.color);
        } else {
            double usedKi = currentKi * (tech.usedPercent / 100.0);
            double damage = usedKi * (1.0 + willpower * 0.002);

            // Drain Ki
            accessor.dba$addKi(-usedKi);

            switch (tech.type) {
                case BLAST -> {
                    if (tech.isBarrage) {
                        fireBarrage(player, level, damage, tech.color);
                    } else {
                        fireBlast(player, level, damage, tech.color);
                    }
                }
                case SPIRAL_BEAM, BEAM -> fireBeam(player, level, damage, tech.color, tech.type == KiTechniqueType.SPIRAL_BEAM);
                case DISK -> fireDisk(player, level, damage, tech.color);
                case LASER -> fireLaser(player, level, damage, tech.color);
                case EXPLOSION -> {}
            }

            player.sendSystemMessage(Component.literal("§b" + tech.type.displayName() + " §7— §a" + (int) damage + " dmg §7(used " + (int) usedKi + " Ki)"), true);
        }

        accessor.dba$syncStats();
    }

    // =========== BLAST: single projectile along look direction ===========
    private static void fireBlast(ServerPlayer player, ServerLevel level, double damage, int color) {
        com.dragonblockarcanedba.entity.KiBlastEntity blast = new com.dragonblockarcanedba.entity.KiBlastEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_BLAST, level);
        blast.setOwner(player);
        blast.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        blast.setColor(color);
        blast.setDamage((float) damage);
        
        Vec3 look = player.getLookAngle();
        blast.shoot(look.x, look.y, look.z, 2.0f, 0.0f);
        level.addFreshEntity(blast);
    }

    // =========== BARRAGE: 8 smaller blasts with spread ===========
    private static void fireBarrage(ServerPlayer player, ServerLevel level, double damage, int color) {
        double perShot = damage / 8.0;
        Vec3 look = player.getLookAngle();

        for (int s = 0; s < 8; s++) {
            com.dragonblockarcanedba.entity.KiBlastEntity blast = new com.dragonblockarcanedba.entity.KiBlastEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_BLAST, level);
            blast.setOwner(player);
            blast.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            blast.setColor(color);
            blast.setDamage((float) perShot);
            
            // Add slight random spread
            double spreadX = (level.getRandom().nextDouble() - 0.5) * 0.15;
            double spreadY = (level.getRandom().nextDouble() - 0.5) * 0.15;
            Vec3 dir = look.add(spreadX, spreadY, 0).normalize();
            
            blast.shoot(dir.x, dir.y, dir.z, 2.0f, 0.0f);
            level.addFreshEntity(blast);
        }
    }

    // =========== BEAM: continuous line of damage along look direction ===========
    private static void fireBeam(ServerPlayer player, ServerLevel level, double damage, int color, boolean spiral) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();
        double range = 60;
        
        // Raycast to find true length
        double hitLength = range;
        for (int i = 1; i <= (int) range; i++) {
            Vec3 pos = start.add(look.scale(i));
            List<LivingEntity> hit = getEntitiesAt(level, player, pos, 1.5);
            if (!hit.isEmpty()) {
                hitLength = i;
                for (LivingEntity target : hit) {
                    target.hurtServer(level, player.damageSources().indirectMagic(player, player), (float) damage);
                }
                break;
            }
        }

        if (spiral) {
            com.dragonblockarcanedba.entity.KiSpiralBeamEntity beam = new com.dragonblockarcanedba.entity.KiSpiralBeamEntity(level, player, (float) hitLength, color);
            level.addFreshEntity(beam);
        } else {
            com.dragonblockarcanedba.entity.KiBeamEntity beam = new com.dragonblockarcanedba.entity.KiBeamEntity(level, player, (float) hitLength, color);
            level.addFreshEntity(beam);
        }
    }

    // =========== DISK: horizontal disk that travels forward ===========
    private static void fireDisk(ServerPlayer player, ServerLevel level, double damage, int color) {
        com.dragonblockarcanedba.entity.KiDiskEntity disk = new com.dragonblockarcanedba.entity.KiDiskEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_DISK, level);
        disk.setOwner(player);
        // Start above head
        disk.setPos(player.getX(), player.getY() + player.getBbHeight() + 1.0, player.getZ());
        disk.setColor(color);
        disk.setDamage((float) damage);
        
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize(); // Horizontal only
        if (look.length() < 0.1) look = new Vec3(1, 0, 0);
        
        disk.shoot(look.x, look.y, look.z, 1.5f, 0.0f);
        level.addFreshEntity(disk);
    }

    // =========== LASER: twin rapid eye beams ===========
    private static void fireLaser(ServerPlayer player, ServerLevel level, double damage, int color) {
        Vec3 look = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();
        // Two parallel beams offset slightly from the eyes
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize().scale(0.15);

        double perBeam = damage / 2.0;
        double range = 45;
        
        for (Vec3 offset : new Vec3[]{right, right.scale(-1)}) {
            Vec3 start = eyePos.add(offset);
            
            // Raycast for length
            double hitLength = range;
            for (int i = 1; i <= (int) range; i++) {
                Vec3 pos = start.add(look.scale(i));
                List<LivingEntity> hit = getEntitiesAt(level, player, pos, 1.0);
                if (!hit.isEmpty()) {
                    hitLength = i;
                    for (LivingEntity target : hit) {
                        target.hurtServer(level, player.damageSources().indirectMagic(player, player), (float) perBeam);
                    }
                    break;
                }
            }
            
            com.dragonblockarcanedba.entity.KiLaserEntity laser = new com.dragonblockarcanedba.entity.KiLaserEntity(level, player, (float) hitLength, color);
            // Override position since it comes from the eyes offset
            laser.setPos(start.x, start.y, start.z);
            level.addFreshEntity(laser);
        }
    }

    // =========== EXPLOSION: AoE last resort ===========
    private static void fireExplosion(ServerPlayer player, PlayerStatsAccessor accessor,
                                       ServerLevel level, double currentKi, int willpower, int color) {
        double kiPower = currentKi; // 100% Ki forced
        double totalDamage = kiPower * 1.5 * (1.0 + willpower * 0.002);
        double selfDamage = kiPower * 0.95 * (1.0 + willpower * 0.002);
        double radius = Math.max(3.0, kiPower / 100.0);
        if (radius > 50) radius = 50; // Safety cap

        // Drain ALL Ki
        accessor.dba$setCurrentKi(0);

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
                    target.hurtServer(level, player.damageSources().indirectMagic(player, player), (float) entityDmg);
                }
            }
        }

        // Self damage
        player.hurtServer(level, player.damageSources().magic(), (float) selfDamage);

        // Spawn visual explosion entity
        com.dragonblockarcanedba.entity.KiExplosionEntity explosion = new com.dragonblockarcanedba.entity.KiExplosionEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_EXPLOSION, level);
        explosion.setPos(player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ());
        explosion.setColor(color);
        explosion.setRadius((float) radius);
        level.addFreshEntity(explosion);

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
}
