package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.SpiritCannonBeamEntity;
import com.dragonblockarcanedba.entity.SpiritCleaveEntity;
import com.dragonblockarcanedba.entity.SpiritImpaleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.List;
import java.util.Optional;

/**
 * Spirit Sword: The Annihilator Blade. Endgame legendary weapon.
 * 
 * Ki Drain: 55% per minute (smooth).
 */
public class SpiritSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    private static final double PULSE_RANGE = 32.0;

    public SpiritSwordItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    499.0, // +499 on top of base 1 = 500 total
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -1.0, // Fast legendary blade (effective 3.0 attacks/sec)
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Divine Presence & Physics: Radiant Divine Beacon & Celestial Glide
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.NAME_PLATE_DIST_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("spirit_sword_beacon_nameplate"), 40.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("spirit_sword_air_drag"), -0.55, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- Left Click: Massive damage + Spirit Impale + Spirit Cleave + Disarm ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply Spirit Impale (mid-air suspension + damage weakness + divine Ki radiance)
        target.addEffect(new MobEffectInstance(DbaEffects.SPIRIT_IMPALE_HOLDER, 120, 0, false, true), attacker);
        target.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 120, 0, false, false, false), attacker);

        // MC 26.2 Physics: Spirit blade kinetic radiance bounce
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("spirit_sword_hit_bounce"),
            0.85,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
        );

        // Spirit Cleave: 2% of target's max HP as bonus damage. Bespoke pass:
        // both bonus hits layered on the melee swing (this percent hit and
        // the Strength hit below) use the mod's spirit damage type, dealt
        // direct by the attacker, exactly the shape the old playerAttack
        // source had. The genuine melee hit itself is the item attack
        // pipeline above and keeps the vanilla player attack source.
        if (attacker instanceof ServerPlayer serverPlayer) {
            // Ki is charged once on the swing, in performSpiritSwing.
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
            float spiritCleave = target.getMaxHealth() * 0.02f;
            target.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.spirit(serverPlayer, serverPlayer), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (spiritCleave)));

            // Stat-scaled bonus damage: Strength × 2
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 2.0);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.spirit(serverPlayer, serverPlayer), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (strengthBonus)));
            }

            // 20% chance to Disarm (drop held item). Bosses and other players are immune:
            // stripping a boss's or a player's held weapon outright was unguarded.
            boolean isBossClass = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) >= 0.9
                || target.getMaxHealth() >= 400f; // Same BOSS rule MobPhysics uses
            if (serverLevel.getRandom().nextFloat() < 0.20f && target instanceof LivingEntity livingTarget
                && !(livingTarget instanceof Player)
                && !isBossClass) {
                ItemStack heldItem = livingTarget.getMainHandItem();
                if (!heldItem.isEmpty()) {
                    livingTarget.spawnAtLocation(serverLevel, heldItem.copy());
                    livingTarget.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }

            // Physical 3D Spirit Impale Entity (6 celestial swords, 3 ground seals, ascending
            // divine pillar). At 3 attacks per second a fresh 40-tick entity per hit churns
            // hard, so a target that already has one following it does not get another.
            boolean alreadyImpaled = !serverLevel.getEntitiesOfClass(SpiritImpaleEntity.class,
                target.getBoundingBox().inflate(2.0), e -> e.getTargetId() == target.getId()).isEmpty();
            if (!alreadyImpaled) {
                SpiritImpaleEntity impale = new SpiritImpaleEntity(serverLevel, serverPlayer, target, 1.0f);
                serverLevel.addFreshEntity(impale);
            }

            // High-impact divine sounds, pitch jittered so identical stacks do not grate.
            float pitchJitter = 0.92f + serverLevel.getRandom().nextFloat() * 0.16f;
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.THUNDER_CRACK, SoundSource.PLAYERS, 1.3f, 1.15f * pitchJitter);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.CHARGE_RISE, SoundSource.PLAYERS, 1.1f, 1.35f * pitchJitter);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.ABILITY_CHARGE, SoundSource.PLAYERS, 1.1f, 1.35f * pitchJitter);
        }
    }

    /**
     * The sword's own swing. A miss still throws the divine blade.
     * A body already in melee reach is left to hurtEnemy so the cut is not paid twice.
     */
    public static void performSpiritSwing(ServerPlayer player, ItemStack stack) {
        if (!WeaponDrainHelper.drainKiDiscrete(player, 55.0, 7)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        float damage = 500.0f + (float) (accessor.dba$getStrength() * 2.0);
        Vec3 look = player.getLookAngle();
        SpiritCleaveEntity cleave = new SpiritCleaveEntity(level, player, damage);
        cleave.setDeltaMovement(look.scale(1.7));
        level.addFreshEntity(cleave);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.BLADE_SHING, SoundSource.PLAYERS, 1.1f, 1.45f);
    }

    // --- Right Click: Start holding to fire Spirit Cannon ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!WeaponDrainHelper.hasKi(player)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000; // Can hold essentially forever
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW; // Hold animation
    }

    // --- Continuous Spirit Cannon beam while holding right-click ---
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(livingEntity instanceof ServerPlayer player)) return;

        // Drain Ki smoothly: 55% per minute
        if (!WeaponDrainHelper.drainKiPerTick(player, 55.0)) {
            player.stopUsingItem();
            return;
        }

        int ticksUsed = getUseDuration(stack, livingEntity) - remainingUseDuration;
        ServerLevel serverLevel = (ServerLevel) level;

        // Manage physical 3D geometric Spirit Cannon beam entity
        List<SpiritCannonBeamEntity> existingBeams = serverLevel.getEntitiesOfClass(
            SpiritCannonBeamEntity.class,
            player.getBoundingBox().inflate(6.0),
            b -> b.getCasterId() == player.getId() && b.isAlive()
        );
        SpiritCannonBeamEntity beam;
        if (existingBeams.isEmpty()) {
            beam = new SpiritCannonBeamEntity(serverLevel, player);
            serverLevel.addFreshEntity(beam);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.BEAM_CHARGE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            beam = existingBeams.get(0);
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0f);

        // Fire pulse damage every 5 ticks (0.25 seconds). The pulse used to
        // land every 10 ticks under a beam that looks continuous; halving the
        // interval and the per pulse numbers spreads the same total damage
        // across the beam's life instead of half second steps.
        if (ticksUsed > 0 && ticksUsed % 5 == 0) {
            // Occlusion: the beam visual truncates at the first block, and
            // the damage now stops at the same wall. Before this raycast the
            // pulse tested the full 32 block line and hit targets standing
            // behind walls the picture never reached.
            net.minecraft.world.phys.BlockHitResult blockHit = serverLevel.clip(new net.minecraft.world.level.ClipContext(
                eyePos, eyePos.add(lookVec.scale(PULSE_RANGE)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            double beamDist = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                ? PULSE_RANGE
                : eyePos.distanceTo(blockHit.getLocation());
            Vec3 endPos = eyePos.add(lookVec.scale(beamDist));

            // Calculate stat-scaled beam damage: 100 + (Spirit x 0.75) per
            // pulse at 5 tick intervals, which is the old 200 + Spirit x 1.5
            // per 10 ticks paid out in twice as many, half sized pulses.
            // The beam ramps over the first 2 seconds.
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            float ramp = ticksUsed < 20 ? 0.6f : (ticksUsed < 40 ? 0.85f : 1.0f);
            float beamDamage = (100.0f + (float) (accessor.dba$getSpirit() * 0.75)) * ramp;

            // --- Piercing beam: hit ALL entities in the line ---
            AABB searchArea = new AABB(eyePos, endPos).inflate(1.0);
            List<Entity> entities = level.getEntities(
                player, searchArea, e -> !e.isSpectator() && e.isPickable() && e.isAlive()
            );

            for (Entity entity : entities) {
                AABB entityBB = entity.getBoundingBox().inflate(0.3);
                Optional<Vec3> hitVec = entityBB.clip(eyePos, endPos);
                if (hitVec.isPresent() && entity instanceof LivingEntity livingTarget) {
                    // Main beam damage. Bespoke pass: the mod's spirit
                    // damage type with the beam as the direct entity and the
                    // caster as the attacker, the shape the old playerAttack
                    // source had.
                    livingTarget.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.spirit(beam, player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (beamDamage)));

                    // 1% of target's max HP as bonus spirit damage per pulse
                    // (was 2% per 10 tick pulse; same rate at the new
                    // cadence). Same spirit source as the main hit.
                    float percentDamage = livingTarget.getMaxHealth() * 0.01f;
                    livingTarget.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.spirit(beam, player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (percentDamage)));

                    // Apply Spirit Impale for 5 seconds (100 ticks)
                    livingTarget.addEffect(new MobEffectInstance(DbaEffects.SPIRIT_IMPALE_HOLDER, 100, 0, false, true), player);

                    // MC 26.2 Physics: Spirit Cannon concussive pulse bounce
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        livingTarget,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("spirit_cannon_bounce"),
                        0.80,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );

                    // Physical 3D Spirit Impale impact at hit entity (deduped like melee)
                    boolean targetAlreadyImpaled = !serverLevel.getEntitiesOfClass(SpiritImpaleEntity.class,
                        livingTarget.getBoundingBox().inflate(2.0), e -> e.getTargetId() == livingTarget.getId()).isEmpty();
                    if (!targetAlreadyImpaled) {
                        SpiritImpaleEntity hitImpale = new SpiritImpaleEntity(serverLevel, player, livingTarget, 0.75f);
                        serverLevel.addFreshEntity(hitImpale);
                    }
                    // Terminus feedback: the beam's impact point used to be silent geometry only.
                    serverLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.SPARK,
                        livingTarget.getX(), livingTarget.getY() + livingTarget.getBbHeight() * 0.5, livingTarget.getZ(),
                        4, 0.3, 0.3, 0.3, 0.12);

                    // Impact sound
                    serverLevel.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        com.dragonblockarcanedba.sound.DbaSounds.CHARGE_FULL, SoundSource.PLAYERS, 1.2f, 1.5f);
                }
            }

            // World mark: once the beam is at full ramp, a sustained strike
            // on open ground crystallizes the surface cell it lands on.
            // GroundScar restores it after 120 ticks and refuses protected
            // blocks; re-scarring the same cell just extends the restore.
            if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                && blockHit.getDirection() == net.minecraft.core.Direction.UP
                && ticksUsed >= 40) {
                var crystal = net.minecraft.world.level.block.Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE).defaultBlockState();
                com.dragonblockarcanedba.util.GroundScar.scar(serverLevel, blockHit.getBlockPos(), crystal, 120);
                // Once a second at full ramp the strike point throws crystal
                // shards off the scar it is burning in (point 2).
                if (ticksUsed % 20 == 0) {
                    com.dragonblockarcanedba.util.DebrisSpawner.spawnField(serverLevel, player,
                        blockHit.getLocation(), 8, 1.4, 2.2, 16, List.of(crystal));
                }
            }

            // Beam pulse resonance sound at caster
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.BEAM_FIRE, SoundSource.PLAYERS, 1.0f, 1.6f);
        }
    }

    // --- On release: fade the beam out and apply 1-second cooldown ---
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;

            // Fade the beam out over its dissipation phase instead of
            // discarding it in one tick; the beam entity ends itself.
            List<SpiritCannonBeamEntity> existingBeams = serverLevel.getEntitiesOfClass(
                SpiritCannonBeamEntity.class,
                player.getBoundingBox().inflate(6.0),
                b -> b.getCasterId() == player.getId()
            );
            for (SpiritCannonBeamEntity b : existingBeams) {
                b.beginFadeOut();
            }

            // 1-second cooldown after releasing the cannon (20 ticks)
            player.getCooldowns().addCooldown(stack, 20);
        }
        return true;
    }
}
