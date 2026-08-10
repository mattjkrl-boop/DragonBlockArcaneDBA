package com.dragonblockarcanedba.effect;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Custom "Melting" status effect with 5 levels.
 * 
 * Damage ticks slow down as the entity's health decreases (they "melt" slower
 * as they weaken, but will still die). Higher levels divide the tick interval,
 * killing much faster.
 * 
 * Formula:
 *   healthRatio = currentHealth / maxHealth  (1.0 at full, 0.0 at dead)
 *   slowFactor = 1.0 + (1.0 - healthRatio)  (1.0 → 2.0 as health drops)
 *   levelDivisor = amplifier + 1
 *   tickInterval = max(4, floor(20 * slowFactor / levelDivisor))
 * 
 * Also applies via attribute modifiers:
 *   - Movement speed: -15% per level
 *   - Attack speed:   -15% per level (mining fatigue feel)
 */
public class MeltingEffect extends MobEffect {
    public MeltingEffect() {
        super(MobEffectCategory.HARMFUL, 0x800080); // Dark purple color

        // Slowness: -15% movement speed per level (amplifier+1 is auto-multiplied)
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.melting.speed"),
            -0.15,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        // Mining Fatigue feel: -15% attack speed per level
        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.melting.attack_speed"),
            -0.15,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        // We handle the timing logic internally in applyEffectTick
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // --- Dynamic damage interval based on health ---
        float healthRatio = entity.getHealth() / entity.getMaxHealth();
        // slowFactor: 1.0 at full health → 2.0 at near-death
        float slowFactor = 1.0f + (1.0f - healthRatio);
        int levelDivisor = amplifier + 1;
        // Higher level = shorter interval = faster kill
        int tickInterval = Math.max(4, (int) (20.0f * slowFactor / levelDivisor));

        if (entity.tickCount % tickInterval == 0) {
            entity.hurtServer(level, level.damageSources().magic(), 1.0F);
        }

        // --- Purple particle "fire" effect (every 3 ticks to avoid spam) ---
        if (entity.tickCount % 3 == 0) {
            float width = entity.getBbWidth();
            float height = entity.getBbHeight();
            level.sendParticles(
                new DustParticleOptions(0x9900CC, 1.5F),
                entity.getX() + (level.getRandom().nextFloat() - 0.5) * width * 1.2,
                entity.getY() + level.getRandom().nextFloat() * height,
                entity.getZ() + (level.getRandom().nextFloat() - 0.5) * width * 1.2,
                2, // count
                0.05, 0.15, 0.05, // delta (slight upward drift like fire)
                0.02 // speed
            );
            // Secondary deeper purple particle for richness
            level.sendParticles(
                new DustParticleOptions(0x660099, 1.0F),
                entity.getX() + (level.getRandom().nextFloat() - 0.5) * width,
                entity.getY() + level.getRandom().nextFloat() * height * 0.6,
                entity.getZ() + (level.getRandom().nextFloat() - 0.5) * width,
                1,
                0.02, 0.08, 0.02,
                0.01
            );
        }

        return true;
    }
}
