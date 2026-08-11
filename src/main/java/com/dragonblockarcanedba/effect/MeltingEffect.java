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
 * Custom "Melting" status effect — Late-game percentage-based DoT.
 * 
 * Damage scales with the target's max health (1% per tick at dynamic intervals),
 * making it devastating against high-HP bosses and players.
 * 
 * Formula:
 *   healthRatio = currentHealth / maxHealth  (1.0 at full, 0.0 at dead)
 *   slowFactor = 1.0 + (1.0 - healthRatio)  (1.0 → 2.0 as health drops)
 *   levelDivisor = amplifier + 1
 *   tickInterval = max(4, floor(20 * slowFactor / levelDivisor))
 * 
 * Attribute modifiers per level:
 *   - Movement speed: -20% per level
 *   - Attack speed:   -20% per level
 *   - Max health:     -5% per level (targets weaken as they melt)
 */
public class MeltingEffect extends MobEffect {
    public MeltingEffect() {
        super(MobEffectCategory.HARMFUL, 0x800080); // Dark purple color

        // Slowness: -20% movement speed per level (amplifier+1 is auto-multiplied)
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            DragonBlockArcaneDBA.id("effect.melting.speed"),
            -0.20,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        // Mining Fatigue feel: -20% attack speed per level
        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            DragonBlockArcaneDBA.id("effect.melting.attack_speed"),
            -0.20,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        // Max health reduction: -5% per level (targets get weaker the longer they melt)
        this.addAttributeModifier(
            Attributes.MAX_HEALTH,
            DragonBlockArcaneDBA.id("effect.melting.max_health"),
            -0.05,
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
            // Deal 1% of target's max health as magic damage per tick
            // This ensures Melting is devastating against high-HP targets (500k HP = 5000 dmg/tick)
            float percentDamage = entity.getMaxHealth() * 0.01f;
            // Minimum 2.0 damage so it still works on low-HP entities
            float damage = Math.max(2.0f, percentDamage);
            entity.hurtServer(level, level.damageSources().magic(), damage);
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
