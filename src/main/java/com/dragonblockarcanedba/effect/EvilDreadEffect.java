package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.entity.DreadGazeEntity;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Evil Dread: the horror layered onto the Evil Spear's mark. Applied by
 * the spear at the exact sites and durations of MARKED_BY_EVIL (melee
 * 120, projectile pierce 160, lunge landing 120), so the dread lives and
 * refreshes with the mark while the mark itself, and the trident that
 * also applies it, stay untouched.
 *
 * The effect deals no damage and changes no numbers. Server side it only
 * maintains one DreadGazeEntity per victim (the eyes in the dark) and
 * sounds a slow heartbeat. The darkness itself is client work: the
 * victim's own screen closes down through the HUD overlay mixin, and
 * the gaze renderer draws the blinking eyes and the smoke shroud for
 * everyone nearby. Ambient void smoke wisps ride the effect particle
 * sync, the same mechanism Dark Faded uses.
 */
public class EvilDreadEffect extends MobEffect {
    public EvilDreadEffect() {
        super(MobEffectCategory.HARMFUL, 0x14000A, DbaParticles.VOID_SMOKE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % 4 == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Exactly one gaze entity per victim: the spear's impale dedup
        // pattern. The entity follows the victim and cleans itself up
        // when the effect ends; this pass only replaces a lost one.
        List<DreadGazeEntity> gazes = level.getEntitiesOfClass(
            DreadGazeEntity.class, entity.getBoundingBox().inflate(4.0),
            g -> g.getVictimId() == entity.getId()
        );
        if (gazes.isEmpty()) {
            level.addFreshEntity(new DreadGazeEntity(level, entity));
            // Onset: the dark closes in with a breath of void ambience
            // and a first wisp of smoke around the victim.
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                DbaSounds.VOID_AMBIENT, SoundSource.PLAYERS, 0.7f, 0.65f);
            level.sendParticles(DbaParticles.VOID_SMOKE,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                10, entity.getBbWidth() * 0.6 + 0.3, entity.getBbHeight() * 0.4,
                entity.getBbWidth() * 0.6 + 0.3, 0.02);
        }

        // A slow heartbeat under the dark, one beat every 28 ticks of
        // remaining duration. Multiples of 28 are multiples of 4, so
        // they land exactly on this effect's every-4-tick cadence.
        var instance = entity.getEffect(DbaEffects.EVIL_DREAD_HOLDER);
        if (instance != null && instance.getDuration() % 28 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                DbaSounds.HEARTBEAT_DARK, SoundSource.PLAYERS, 0.65f, 0.8f);
        }
        return true;
    }
}
