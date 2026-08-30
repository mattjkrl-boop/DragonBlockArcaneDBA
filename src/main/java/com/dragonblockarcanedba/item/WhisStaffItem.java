package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.TemporalRiftEntity;
import com.dragonblockarcanedba.entity.TimeShatterEntity;
import com.dragonblockarcanedba.util.TimeTracker;
import com.dragonblockarcanedba.util.WeaponDrainHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Whis Staff — The Temporal God Rod. Endgame legendary weapon.
 * 
 * Left-click: 350 + (Dexterity × 2) damage.
 *   - Time reversal for 10 seconds (200 ticks)
 *   - Temporal Shatter: 1.5% of target's max HP as bonus magic damage
 *   - Slowness III for 5 seconds (time distortion)
 *   - Physical 3D Time Shatter entity (prismatic tumbling glass shards & fractured chrono-mirror)
 * 
 * Right-click: "Temporal Rift" — 12-block radius AOE.
 *   - All entities: Temporal Freeze (Slowness 127 = frozen) for 3 seconds, then 15-second reversal
 *   - Instant 400 + (Spirit × 1.5) magic damage to all entities in range
 *   - Self-Buff: Celestial Grace for 10 seconds
 *   - Physical 3D Translucent Celestial Dome & 3D Rotating Clock Astrolabe Entity
 *   - 5-second cooldown
 * 
 * Passive: Auto-Dodge — 50% chance to negate incoming damage while held in mainhand.
 *   (Implemented via LivingEntityMixin)
 * 
 * Unbreakable legendary weapon. No durability.
 * Ki Drain: 100% per minute (smooth).
 */
public class WhisStaffItem extends Item {
    private static final double AOE_RADIUS = 12.0;

    public WhisStaffItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    349.0, // +349 on top of base 1 = 350 total
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -2.4, // Standard weapon speed (1.6 attacks/sec)
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Angelic effortless motion
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("whis_angel_friction"), -0.80, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("whis_angel_drag"), -0.80, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- Left Click: Damage + Time Reversal + Temporal Shatter + Slowness ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Time reversal for 10 seconds (200 ticks)
        if (target instanceof TimeTracker timeTracker) {
            timeTracker.dba$startReversing(200);
        }

        // Temporal Stasis — time distortion freeze & extreme temporal drag
        target.addEffect(new MobEffectInstance(DbaEffects.TEMPORAL_STASIS_HOLDER, 100, 0, false, true), attacker);

        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("whis_stasis_drag"),
            4.0,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("whis_stasis_friction"),
            3.0,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        if (attacker instanceof ServerPlayer serverPlayer) {
            // Drain Ki: 100% per minute (~13 ticks cadence = 1.08% Max Ki)
            WeaponDrainHelper.drainKiDiscrete(serverPlayer, 100.0, 13);

            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Dexterity × 2
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float dexBonus = (float) (accessor.dba$getDexterity() * 2.0);
            if (dexBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), dexBonus);
            }

            // Temporal Shatter: 1.5% of target's max HP as bonus magic damage
            float temporalDamage = target.getMaxHealth() * 0.015f;
            target.hurtServer(serverLevel, serverLevel.damageSources().magic(), temporalDamage);

            // Physical 3D Time Shatter Entity — Prismatic Glass & Chrono-Mirror Burst
            TimeShatterEntity shatter = new TimeShatterEntity(
                serverLevel,
                serverPlayer,
                target.position().add(0, target.getBbHeight() * 0.5, 0),
                1.2f
            );
            serverLevel.addFreshEntity(shatter);

            // Crisp glass & temporal distortion sound
            serverLevel.playSound(
                null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                1.2f, 1.6f
            );
            serverLevel.playSound(
                null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                0.9f, 1.9f
            );
        }
    }

    // --- Right Click: Temporal Rift AOE ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Drain Ki: 100% per minute for 100-tick (5-second) cooldown cycle (8.33% Max Ki)
        if (!WeaponDrainHelper.drainKiDiscrete(player, 100.0, 100)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // Calculate AOE burst damage: 400 + (Spirit × 1.5)
            float burstDamage = 400.0f + (float) (accessor.dba$getSpirit() * 1.5);

            // 12-block radius AOE — reverse all living entities
            AABB aoe = player.getBoundingBox().inflate(AOE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, aoe, e -> e != player && e.isAlive()
            );

            for (LivingEntity target : targets) {
                if (player.distanceTo(target) <= AOE_RADIUS) {
                    // Instant burst magic damage
                    target.hurtServer(serverLevel, serverLevel.damageSources().magic(), burstDamage);

                    // Temporal Stasis: Custom time paralysis for 3 seconds (60 ticks)
                    target.addEffect(new MobEffectInstance(DbaEffects.TEMPORAL_STASIS_HOLDER, 60, 0, false, true), player);

                    // Time reversal for 15 seconds (300 ticks) — starts after freeze ends
                    if (target instanceof TimeTracker tracker) {
                        tracker.dba$startReversing(300);
                    }
                }
            }

            // --- Self-Buff: Celestial Grace (Speed + Jump Boost + Resistance + Safe Fall) for 10 seconds (200 ticks) ---
            player.addEffect(new MobEffectInstance(DbaEffects.CELESTIAL_GRACE_HOLDER, 200, 0, false, true));

            // Physical 3D Geometric Temporal Rift Entity (Volumetric Celestial Dome + 3D Rotating Clock Astrolabe)
            TemporalRiftEntity rift = new TemporalRiftEntity(
                serverLevel,
                player,
                player.position(),
                (float) AOE_RADIUS,
                60
            );
            serverLevel.addFreshEntity(rift);

            // Resonant celestial time activation audio
            serverLevel.playSound(
                null, player.getX(), player.getY() + 1.0, player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                1.4f, 1.2f
            );
            serverLevel.playSound(
                null, player.getX(), player.getY() + 1.0, player.getZ(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS,
                1.0f, 1.6f
            );
        }

        // 5-second cooldown (100 ticks)
        player.getCooldowns().addCooldown(stack, 100);
        return InteractionResult.SUCCESS;
    }
}
