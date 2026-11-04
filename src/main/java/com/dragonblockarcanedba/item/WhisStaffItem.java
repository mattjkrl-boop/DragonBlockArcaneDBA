package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.FragmentEntity;
import com.dragonblockarcanedba.entity.TemporalRiftEntity;
import com.dragonblockarcanedba.entity.TimeShatterEntity;
import com.dragonblockarcanedba.util.DbaDamage;
import com.dragonblockarcanedba.util.TimeTracker;
import com.dragonblockarcanedba.util.WeaponDrainHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Whis Staff, the Temporal God Rod. Endgame legendary weapon.
 * 
 * Left-click: 350 + (Dexterity × 2) damage.
 *   - Time reversal for 5 seconds (100 ticks, the position history buffer maximum)
 *   - Temporal Shatter: 1.5% of target's max HP as bonus temporal damage
 *   - Temporal Stasis amplifier 0 for 100 ticks (full time freeze with temporal drag)
 *   - Physical 3D Time Shatter entity (prismatic tumbling glass shards & fractured chrono-mirror)
 *
 * Right-click: "Temporal Rift", 12-block radius AOE.
 *   - All entities in range: Temporal Stasis for 3 seconds (60 ticks), then a
 *     5-second (100 tick) reversal that starts when the freeze ends, triggered
 *     by the rift entity at tick 60
 *   - 400 + (Spirit × 1.5) temporal damage to all entities in range, split in two
 *     halves: the first at cast, the second at the rift's envelope peak (tick 15)
 *   - Self-Buff: Celestial Grace for 10 seconds
 *   - Material based Temporal Rift Entity (inward streaming temporal
 *     plasma, soul material, glass dust, mist, and a dark core)
 *     that lives 5 seconds (100 ticks)
 *   - 7-second cooldown (140 ticks), longer than the rift's own life
 *
 * Passive: Auto-Dodge: 50% chance to negate incoming damage while held in mainhand.
 *   (Implemented via LivingEntityMixin)
 *
 * Unbreakable legendary weapon. No durability.
 * Ki cost: the swing cost (100% per minute over 13 ticks) is charged once per swing
 * by the left-click network handler; the rift costs 100% per minute over 100 ticks.
 */
public class WhisStaffItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    private static final double AOE_RADIUS = 12.0;
    private static final int RIFT_LIFETIME_TICKS = 100;
    private static final int RIFT_COOLDOWN_TICKS = 140;
    private static final int RIFT_STASIS_TICKS = 60;

    // TimeShatter fan salts, re-homed from TimeShatterRenderer when the
    // frozen fan became real FragmentEntity pieces (ENTITY-STANDARD,
    // audits/entity-audit.md section 6): the angle salt seeds the
    // java.util.Random that rolls the five spoke angles, the seed salt
    // derives each spoke's fragment mesh seed from the host seed.
    private static final long SHATTER_FAN_ANGLE_SALT = 0xFA17L;
    private static final long SHATTER_FAN_SEED_SALT = 0x5A1DL;
    private static final int SHATTER_FAN_SPOKES = 5;
    private static final int SHATTER_FRAGMENT_REST_TICKS = 10;

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

    // --- Left Click: Damage + Time Reversal + Temporal Shatter + Temporal Stasis ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Player attackers only: the control package used to apply for any attacker,
        // so a mob somehow swinging the staff froze and reversed targets for free
        // while every cost and bonus damage stayed player-only.
        if (!(attacker instanceof Player)) {
            return;
        }

        // Time reversal for 5 seconds (100 ticks). The position history buffer holds
        // at most 100 entries, so the old 200-tick request ran out of recorded
        // positions halfway through the rewind.
        if (target instanceof TimeTracker timeTracker) {
            timeTracker.dba$startReversing(100);
        }

        // Temporal Stasis: time distortion freeze & extreme temporal drag
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
            // No Ki drain here: the left-click network handler already charges the
            // swing cost once per swing. Charging again on every landed hit made a
            // connecting swing cost double an air swing.
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Dexterity × 2
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float dexBonus = (float) (accessor.dba$getDexterity() * 2.0);
            if (dexBonus > 0) {
                // Bonus hit layered on the melee swing: the mod's temporal type,
                // direct and attacker both the player, the same source shape the
                // vanilla player attack had. The swing itself stays player attack.
                target.hurtServer(serverLevel, DbaDamage.temporal(serverPlayer, serverPlayer), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (dexBonus)));
            }

            // Temporal Shatter: 1.5% of target's max HP as bonus temporal damage
            float temporalDamage = target.getMaxHealth() * 0.015f;
            target.hurtServer(serverLevel, DbaDamage.temporal(serverPlayer), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (temporalDamage)));

            // Physical 3D Time Shatter Entity: Prismatic Glass & Chrono-Mirror Burst
            TimeShatterEntity shatter = new TimeShatterEntity(
                serverLevel,
                serverPlayer,
                target.position().add(0, target.getBbHeight() * 0.5, 0),
                1.2f
            );
            serverLevel.addFreshEntity(shatter);
            spawnShatterFan(serverLevel, serverPlayer, shatter);

            // Crisp glass & temporal distortion sound
            serverLevel.playSound(
                null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.CRYSTAL_CHIME, SoundSource.PLAYERS,
                1.2f, 1.6f
            );
            serverLevel.playSound(
                null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.CHARGE_RISE, SoundSource.PLAYERS,
                0.9f, 1.9f
            );
            serverLevel.playSound(
                null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS,
                0.9f, 1.9f
            );
        }
    }

    /**
     * The staff's own strike. Glass cracks along the rod even on a miss.
     * Someone already in melee reach is left to hurtEnemy.
     */
    public static void performTemporalStrike(ServerPlayer player, ItemStack stack) {
        if (!WeaponDrainHelper.drainKiDiscrete(player, 100.0, 13)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        // The crack escalates toward the target instead of ticking at even
        // steps: each shatter lands further out and lands bigger.
        double[] shatterDistances = {1.2, 2.6, 4.2};
        float[] shatterScales = {0.8f, 1.05f, 1.4f};
        for (int step = 0; step < shatterDistances.length; step++) {
            Vec3 pos = eye.add(look.scale(shatterDistances[step]));
            TimeShatterEntity shatter = new TimeShatterEntity(level, player, pos, shatterScales[step]);
            level.addFreshEntity(shatter);
            spawnShatterFan(level, player, shatter);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.CRYSTAL_CHIME, SoundSource.PLAYERS, 1.0f, 1.7f);

        AABB reach = player.getBoundingBox().expandTowards(look.scale(6.5)).inflate(1.15);
        LivingEntity struck = null;
        double best = 6.5;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, reach, e -> e != player && e.isAlive())) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist < 3.0 || dist > 6.5 || to.lengthSqr() < 1.0E-4) {
                continue;
            }
            if (look.dot(to.normalize()) < 0.84) {
                continue;
            }
            if (dist < best) {
                best = dist;
                struck = candidate;
            }
        }
        if (struck != null) {
            applyStaffHit(player, level, struck, true);
        }
    }

    /**
     * The TimeShatter frozen fan as real pieces (ENTITY-STANDARD,
     * audits/entity-audit.md section 6). The five solid glass shards the
     * renderer used to draw on its per-client camera plane now hang in
     * the world as FROZEN FragmentEntity instances on the plane
     * perpendicular to the caster's look direction at the cast moment,
     * which the item owns server side. Everything the drawn fan read
     * from is replicated exactly: the spoke angles roll from
     * java.util.Random over the host seed with the same salt and are
     * consumed in the same order, each spoke's mesh seed is the host
     * seed with the same per-spoke salt (the old burst call's per-index
     * seed salt adds zero at index zero, so nothing else shifts), the size
     * unit is the host scale times 0.90, sizes roll over the shardBurst
     * classes, and the gradient is the same near-white root to ghost
     * cyan tip. RELEASE_AT is the host's own lifetime, so at the tick
     * the shatter ends every fragment drops under gravity in the same
     * tick, lands, rests, and fades instead of vanishing with the host.
     * The fragments deal no damage; the host's damage, costs, timing,
     * and synced seed are untouched.
     */
    private static void spawnShatterFan(ServerLevel level, Player caster, TimeShatterEntity host) {
        long hostSeed = host.getSeed();
        float fanUnit = host.getShatterScale() * 0.90f;
        Vec3 center = host.position();

        Vec3 forward = caster.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 planeUp = right.cross(forward).normalize();

        Random fanRandom = new Random(hostSeed ^ SHATTER_FAN_ANGLE_SALT);
        for (int i = 0; i < SHATTER_FAN_SPOKES; i++) {
            double angle = (i / (double) SHATTER_FAN_SPOKES) * Math.PI * 2.0
                + (fanRandom.nextFloat() - 0.5f) * 0.55f;
            Vec3 axis = right.scale(Math.cos(angle)).add(planeUp.scale(Math.sin(angle))).normalize();
            int fragSeed = (int) (hostSeed ^ (SHATTER_FAN_SEED_SALT + i * 131L));
            Random sizeRandom = new Random(fragSeed);
            float roll = sizeRandom.nextFloat();
            float length = fanUnit * (roll < 0.55f ? 0.30f + 0.25f * sizeRandom.nextFloat()
                : roll < 0.85f ? 0.55f + 0.50f * sizeRandom.nextFloat()
                : 1.05f + 0.55f * sizeRandom.nextFloat());
            float girth = length * (0.30f + 0.14f * sizeRandom.nextFloat());
            FragmentEntity.hang(level, caster, center, axis, length, girth,
                fragSeed, 0xFFFFFF, 0x65D8FF, host.getMaxLifetime(), SHATTER_FRAGMENT_REST_TICKS);
        }
    }

    /**
     * @param includeBase true when vanilla melee did not already apply the staff's 350
     */
    private static void applyStaffHit(ServerPlayer player, ServerLevel level, LivingEntity target, boolean includeBase) {
        if (target instanceof TimeTracker timeTracker) {
            timeTracker.dba$startReversing(100);
        }
        target.addEffect(new MobEffectInstance(DbaEffects.TEMPORAL_STASIS_HOLDER, 100, 0, false, true), player);
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
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        float damage = (float) (accessor.dba$getDexterity() * 2.0) + target.getMaxHealth() * 0.015f;
        if (includeBase) {
            damage += 350.0f;
        }
        if (damage > 0.0f) {
            // The extended strike is an ability hit, not the plain melee swing:
            // the mod's temporal type, direct and attacker both the player. The
            // percent-health bonus is folded into this one hit in the code, so
            // the whole hit converts together; the total is unchanged.
            target.hurtServer(level, DbaDamage.temporal(player, player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage));
        }
        TimeShatterEntity shatter = new TimeShatterEntity(level, player, target.position().add(0, target.getBbHeight() * 0.5, 0), 1.2f);
        level.addFreshEntity(shatter);
        spawnShatterFan(level, player, shatter);
        level.playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.CRYSTAL_CHIME, SoundSource.PLAYERS, 1.2f, 1.6f);
        level.playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS, 0.9f, 1.9f);
    }

    // --- Right Click: Temporal Rift AOE ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Drain Ki: one charge of 100% per minute prorated over 100 ticks
        // (8.33% of max Ki), checked before anything fires, so a blocked cast
        // never charges.
        if (!WeaponDrainHelper.drainKiDiscrete(player, 100.0, 100)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;

            // AOE burst damage: 400 + (Spirit × 1.5), dealt in two halves.
            // The first half lands at cast; the rift entity deals the second
            // half at its envelope peak (tick 15), inside the visual's active
            // window, instead of the whole burst landing on tick 1 of a
            // 100 tick visual.
            float burstDamage = 400.0f + (float) (accessor.dba$getSpirit() * 1.5);
            float castDamage = burstDamage * 0.5f;

            // 12-block radius AOE: freeze all living entities
            AABB aoe = player.getBoundingBox().inflate(AOE_RADIUS);
            List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, aoe, e -> e != player && e.isAlive()
            );

            for (LivingEntity target : targets) {
                if (player.distanceTo(target) <= AOE_RADIUS) {
                    // First half of the burst damage, the mod's temporal type
                    target.hurtServer(serverLevel, DbaDamage.temporal(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (castDamage)));

                    // Temporal Stasis: Custom time paralysis for 3 seconds (60 ticks)
                    target.addEffect(new MobEffectInstance(DbaEffects.TEMPORAL_STASIS_HOLDER, RIFT_STASIS_TICKS, 0, false, true), player);

                    // The time reversal starts after the freeze ends: the rift
                    // entity triggers dba$startReversing(100) at its tick 60,
                    // the moment this stasis expires. It used to be called
                    // here, immediately, while the comment claimed it waited.
                }
            }

            // --- Self-Buff: Celestial Grace (Speed + Jump Boost + Resistance + Safe Fall) for 10 seconds (200 ticks) ---
            player.addEffect(new MobEffectInstance(DbaEffects.CELESTIAL_GRACE_HOLDER, 200, 0, false, true));

            // Temporal Rift Entity: the material based time suspension field
            TemporalRiftEntity rift = new TemporalRiftEntity(
                serverLevel,
                player,
                player.position(),
                (float) AOE_RADIUS,
                RIFT_LIFETIME_TICKS,
                burstDamage - castDamage
            );
            serverLevel.addFreshEntity(rift);

            // Impact feedback for a 12-block time freeze: it used to land with no
            // shake, no hitstop, and no DBA sound at all.
            com.dragonblockarcanedba.fx.DbaImpactFx.medium(serverLevel, player.getX(), player.getY(), player.getZ());

            // Resonant celestial time activation audio
            serverLevel.playSound(
                null, player.getX(), player.getY() + 1.0, player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS,
                1.2f, 1.4f
            );
            serverLevel.playSound(
                null, player.getX(), player.getY() + 1.0, player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.CHARGE_FULL, SoundSource.PLAYERS,
                1.4f, 1.2f
            );
            serverLevel.playSound(
                null, player.getX(), player.getY() + 1.0, player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.CAST_MAGIC, SoundSource.PLAYERS,
                1.0f, 1.6f
            );
        }

        // 7-second cooldown (140 ticks): longer than the rift's 100 tick
        // life, so two suspension fields can never stack.
        player.getCooldowns().addCooldown(stack, RIFT_COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
