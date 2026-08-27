package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.entity.SpiritCannonBeamEntity;
import com.dragonblockarcanedba.entity.SpiritImpaleEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * Spirit Sword — The Annihilator Blade. Endgame legendary weapon.
 * 
 * Ki Drain: 55% per minute (smooth).
 */
public class SpiritSwordItem extends Item {
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

        // Spirit Cleave: 2% of target's max HP as bonus magic damage
        if (attacker instanceof ServerPlayer serverPlayer) {
            // Drain discrete Ki for swing duration (~7 ticks at 3.0 attacks/sec)
            WeaponDrainHelper.drainKiDiscrete(serverPlayer, 55.0, 7);

            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
            float spiritCleave = target.getMaxHealth() * 0.02f;
            target.hurtServer(serverLevel, serverLevel.damageSources().magic(), spiritCleave);

            // Stat-scaled bonus damage: Strength × 2
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 2.0);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), strengthBonus);
            }

            // 20% chance to Disarm (drop held item)
            if (serverLevel.getRandom().nextFloat() < 0.20f && target instanceof LivingEntity livingTarget) {
                ItemStack heldItem = livingTarget.getMainHandItem();
                if (!heldItem.isEmpty()) {
                    livingTarget.spawnAtLocation(serverLevel, heldItem.copy());
                    livingTarget.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
            }

            // Physical 3D Spirit Impale Entity (6 celestial swords, 3 ground seals, ascending divine pillar)
            SpiritImpaleEntity impale = new SpiritImpaleEntity(serverLevel, serverPlayer, target, 1.0f);
            serverLevel.addFreshEntity(impale);

            // High-impact divine sounds
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.3f, 1.15f);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.1f, 1.35f);
        }
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
        } else {
            beam = existingBeams.get(0);
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0f);

        // Fire pulse damage every 10 ticks (0.5 seconds)
        if (ticksUsed > 0 && ticksUsed % 10 == 0) {
            Vec3 endPos = eyePos.add(lookVec.scale(PULSE_RANGE));

            // Calculate stat-scaled beam damage: 200 + (Spirit × 1.5)
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
            float beamDamage = 200.0f + (float) (accessor.dba$getSpirit() * 1.5);

            // --- Piercing beam: hit ALL entities in the line ---
            AABB searchArea = new AABB(eyePos, endPos).inflate(1.0);
            List<Entity> entities = level.getEntities(
                player, searchArea, e -> !e.isSpectator() && e.isPickable() && e.isAlive()
            );

            for (Entity entity : entities) {
                AABB entityBB = entity.getBoundingBox().inflate(0.3);
                Optional<Vec3> hitVec = entityBB.clip(eyePos, endPos);
                if (hitVec.isPresent() && entity instanceof LivingEntity livingTarget) {
                    // Main beam damage
                    livingTarget.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), beamDamage);

                    // 2% of target's max HP as bonus magic damage
                    float percentDamage = livingTarget.getMaxHealth() * 0.02f;
                    livingTarget.hurtServer(serverLevel, serverLevel.damageSources().magic(), percentDamage);

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

                    // Physical 3D Spirit Impale impact at hit entity
                    SpiritImpaleEntity hitImpale = new SpiritImpaleEntity(serverLevel, player, livingTarget, 0.75f);
                    serverLevel.addFreshEntity(hitImpale);

                    // Impact sound
                    serverLevel.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.5f);
                }
            }

            // Beam pulse resonance sound at caster
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        }
    }

    // --- On release: discard beam entity and apply 1-second cooldown ---
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            ServerLevel serverLevel = (ServerLevel) level;

            // Discard active beam entity
            List<SpiritCannonBeamEntity> existingBeams = serverLevel.getEntitiesOfClass(
                SpiritCannonBeamEntity.class,
                player.getBoundingBox().inflate(6.0),
                b -> b.getCasterId() == player.getId()
            );
            for (SpiritCannonBeamEntity b : existingBeams) {
                b.discard();
            }

            // 1-second cooldown after releasing the cannon (20 ticks)
            player.getCooldowns().addCooldown(stack, 20);
        }
        return true;
    }
}
