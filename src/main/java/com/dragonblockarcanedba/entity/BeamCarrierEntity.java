package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.ability.AbilityDamage;
import com.dragonblockarcanedba.ability.AbilityDef;
import com.dragonblockarcanedba.ability.AbilityFeedbackService;
import com.dragonblockarcanedba.ability.AbilityFxService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Beam carrier (Phase 6): rides the owner's eye and look for its
 * active lifetime and, on the def's damage interval, applies the
 * ability damage to every living entity whose bounding box comes
 * within the beam radius of the clipped eye-to-end segment. All
 * numbers come from the def's beam block; this class holds no
 * per-weapon behavior.
 */
public class BeamCarrierEntity extends CarrierEntity {

    public BeamCarrierEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AbilityDef def = def();
        if (def == null || def.beam() == null || this.isRemoved()) {
            return;
        }
        Player owner = ownerPlayer();
        if (owner == null) {
            this.discard();
            return;
        }
        this.setPos(owner.getEyePosition());
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());

        AbilityDef.BeamParams beam = def.beam();
        if (this.tickCount <= 0 || this.tickCount % beam.damageInterval() != 0) {
            return;
        }
        Vec3 from = owner.getEyePosition();
        Vec3 dir = owner.getLookAngle();
        Vec3 fullEnd = from.add(dir.scale(beam.length()));
        BlockHitResult clip = serverLevel.clip(new ClipContext(from, fullEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 end = clip.getType() == HitResult.Type.BLOCK ? clip.getLocation() : fullEnd;
        double segLen = from.distanceTo(end);
        AABB box = new AABB(
                Math.min(from.x, end.x) - beam.radius() - 1, Math.min(from.y, end.y) - beam.radius() - 1,
                Math.min(from.z, end.z) - beam.radius() - 1,
                Math.max(from.x, end.x) + beam.radius() + 1, Math.max(from.y, end.y) + beam.radius() + 1,
                Math.max(from.z, end.z) + beam.radius() + 1);
        int hits = 0;
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner)) {
            Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
            double t = segLen <= 0 ? 0 : Math.max(0, Math.min(segLen, center.subtract(from).dot(dir)));
            Vec3 closest = from.add(dir.scale(t));
            double distToBeam = center.distanceTo(closest);
            if (distToBeam <= beam.radius() + target.getBbWidth() * 0.5) {
                AbilityDamage.applyHit(serverLevel, def, this, owner, target,
                        getPower(), segLen <= 0 ? 0 : t / beam.length(), false);
                hits++;
            }
        }
        if (beam.endBurstRadius() > 0) {
            AbilityDamage.applyAreaHit(serverLevel, def, this, owner, end,
                    beam.endBurstRadius(), getPower(), owner);
        }
        if (hits > 0 || beam.endBurstRadius() > 0) {
            AbilityFxService.send(this, def, AbilityDef.FxEvent.CARRIER_HIT,
                    end, dir, getSeed() ^ this.tickCount, getPower(), 1);
            AbilityFeedbackService.carrier(this, def, AbilityDef.FxEvent.CARRIER_HIT, end);
        }
    }
}
