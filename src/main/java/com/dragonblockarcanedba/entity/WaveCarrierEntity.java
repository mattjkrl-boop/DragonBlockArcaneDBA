package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDamage;
import com.dragonblockarcanedba.ability.AbilityDef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Wave carrier: a ground-anchored band that travels in a
 * straight line from the caster, damaging each living entity once
 * as it reaches them, with linear falloff from its center out to
 * its radius. All numbers come from the ability definition; this
 * class holds no per-weapon behavior. The band's look (an arc of
 * arcDegrees around the travel direction) is the renderer's reading
 * of the same parameters.
 */
public class WaveCarrierEntity extends CarrierEntity {
    private final Set<UUID> hit = new HashSet<>();

    public WaveCarrierEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        AbilityDef def = def();
        if (def == null || def.wave() == null) {
            return;
        }
        AbilityDef.WaveParams wave = def.wave();

        // Travel: straight ahead at the synced yaw, snapped to the ground.
        double yawRad = Math.toRadians(this.getYRot());
        double dx = -Math.sin(yawRad);
        double dz = Math.cos(yawRad);
        double nx = this.getX() + dx * wave.speed();
        double nz = this.getZ() + dz * wave.speed();
        int groundY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(nx), (int) Math.floor(nz));
        this.setPos(nx, groundY, nz);

        // Damage: everyone inside the band volume, once each.
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 center = this.position();
            AABB box = new AABB(
                    center.x - wave.radius(), center.y - 0.5, center.z - wave.radius(),
                    center.x + wave.radius(), center.y + wave.height() + 1.0, center.z + wave.radius());
            Player owner = ownerPlayer();
            for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != owner)) {
                if (this.hit.contains(target.getUUID())) {
                    continue;
                }
                double distX = target.getX() - center.x;
                double distZ = target.getZ() - center.z;
                double dist = Math.sqrt(distX * distX + distZ * distZ);
                if (dist <= wave.radius() + target.getBbWidth() * 0.5) {
                    this.hit.add(target.getUUID());
                    AbilityDamage.apply(serverLevel, def, this, owner, target,
                            getPower(), dist / wave.radius());
                }
            }
        }
    }
}
