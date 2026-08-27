package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BanshoCycloneEntity;
import com.dragonblockarcanedba.entity.BanshoShockwaveEntity;
import com.dragonblockarcanedba.entity.BanshoWindProjectileEntity;
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
import net.minecraft.world.phys.Vec3;
import com.dragonblockarcanedba.util.WeaponDrainHelper;

import java.util.List;

/**
 * Bansho Fan — The Storm Emperor. Endgame legendary weapon.
 * 
 * Stamina Drain: 45% per minute (smooth).
 * 
 * Left-click: 400 + (Strength × 1.5) damage.
 *   - Gale Force: Launches target 10 blocks with strong upward velocity & physics modifiers
 *   - Applies Bleeding II for 15 seconds (wind cuts)
 *   - Spawns physical 3D emerald impact shockwave
 *   - Cyclone Slash: 30% chance to summon a physical 3D geometric emerald cyclone entity that
 *     swirls, lifts, and shreds nearby enemies for 50% damage
 * 
 * Right-click: "Tempest Barrage"
 *   - Spawns a physical 3D conical launch shockwave
 *   - Fires 5 custom physical 3D BanshoWindProjectile entities in a spread pattern
 *   - Each deals 300 + (Spirit × 1) damage on impact, applies Bleeding III for 20 seconds,
 *     and spawns physical 3D impact shockwaves
 *   - 3× velocity (4.5F), perfect accuracy
 *   - 2-second cooldown
 * 
 * Unbreakable legendary weapon. No durability.
 */
public class BanshoFanItem extends Item {
    public BanshoFanItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    399.0, // +399 on top of base 1 = 400 total
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -2.0, // Standard weapon speed (2.0 attacks/sec)
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Wind glide and zero friction
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("bansho_air_drag"), -0.60, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("bansho_friction"), -0.75, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // --- Left Click: Massive knockback + Bleeding II + Physical 3D Cyclone Slash ---
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Gale Force: Launch target 10 blocks with strong upward velocity & MC 26.2 catapult physics
        Vec3 diff = target.position().subtract(attacker.position()).normalize().scale(5.0);
        target.setDeltaMovement(target.getDeltaMovement().add(diff.x, 1.2, diff.z));
        target.hurtMarked = true; // Force velocity sync to client

        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("bansho_gale_bounce"),
            0.85,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
        );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
            target,
            com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
            com.dragonblockarcanedba.DragonBlockArcaneDBA.id("bansho_gale_drag"),
            -0.60,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );

        // Apply Bleeding II (amplifier 1) for 15 seconds (300 ticks)
        target.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 300, 1, false, true), attacker);

        if (attacker instanceof ServerPlayer serverPlayer) {
            // Drain Stamina: 45% per minute (~10 ticks cadence = 0.375% Max Stamina)
            WeaponDrainHelper.drainStaminaDiscrete(serverPlayer, 45.0, 10);

            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Strength × 1.5
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 1.5);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), strengthBonus);
            }

            // Cyclone Slash: 30% chance to summon a physical 3D emerald cyclone entity
            if (serverLevel.getRandom().nextFloat() < 0.30f) {
                float cycloneDamage = (400.0f + strengthBonus) * 0.5f; // 50% of main hit
                
                // Spawn physical 3D geometric emerald cyclone at target location
                BanshoCycloneEntity cyclone = new BanshoCycloneEntity(
                    serverLevel,
                    serverPlayer,
                    target.position(),
                    cycloneDamage
                );
                serverLevel.addFreshEntity(cyclone);

                AABB cycloneArea = target.getBoundingBox().inflate(3.0);
                List<LivingEntity> nearbyTargets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, cycloneArea,
                    e -> e != attacker && e != target && e.isAlive()
                );

                for (LivingEntity nearby : nearbyTargets) {
                    nearby.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(serverPlayer), cycloneDamage);
                    // Knockback nearby targets too
                    Vec3 knockback = nearby.position().subtract(target.position()).normalize().scale(2.0);
                    nearby.setDeltaMovement(nearby.getDeltaMovement().add(knockback.x, 0.5, knockback.z));
                    nearby.hurtMarked = true;

                    // Apply Bleeding I to cyclone targets
                    nearby.addEffect(new MobEffectInstance(DbaEffects.BLEEDING_HOLDER, 200, 0, false, true), serverPlayer);
                }
            }

            // Physical 3D impact shockwave on every Gale Force strike (0 particles)
            BanshoShockwaveEntity impactWave = new BanshoShockwaveEntity(
                serverLevel,
                serverPlayer,
                target.position().add(0, target.getBbHeight() * 0.5, 0),
                attacker.getYRot(),
                attacker.getXRot(),
                3.2f,
                false // Radial impact shockwave
            );
            serverLevel.addFreshEntity(impactWave);

            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.2f);
        }
    }

    // --- Right Click: Tempest Barrage — Physical 3D launch shockwave + 5 custom wind projectiles ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Drain Stamina: 45% per minute for 40-tick (2-second) cooldown cycle (1.5% Max Stamina)
        if (!WeaponDrainHelper.drainStaminaDiscrete(player, 45.0, 40)) {
            return InteractionResult.FAIL;
        }

        // Play tempest release sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WIND_CHARGE_THROW, SoundSource.NEUTRAL,
            1.2F, 0.9F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS,
            1.0F, 1.5F);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();

            // Spawn physical 3D directional launch shockwave at player nozzle/eye position
            BanshoShockwaveEntity launchWave = new BanshoShockwaveEntity(
                serverLevel,
                player,
                eye.add(look.scale(0.8)),
                player.getYRot(),
                player.getXRot(),
                3.5f,
                true // Directional conical launch shockwave
            );
            serverLevel.addFreshEntity(launchWave);

            // Projectile damage: 300 + (Spirit × 1)
            float projectileDamage = 300.0f;
            if (player instanceof PlayerStatsAccessor accessor) {
                projectileDamage += (float) accessor.dba$getSpirit();
            }

            // Fire 5 physical 3D wind projectiles in a spread pattern
            float[] yawOffsets = { -8.0f, -4.0f, 0.0f, 4.0f, 8.0f };
            float[] pitchOffsets = { 2.0f, -1.0f, 0.0f, -1.0f, 2.0f };

            for (int i = 0; i < 5; i++) {
                BanshoWindProjectileEntity projectile = new BanshoWindProjectileEntity(
                    serverLevel,
                    player,
                    projectileDamage
                );
                projectile.shootFromRotation(
                    player,
                    player.getXRot() + pitchOffsets[i],
                    player.getYRot() + yawOffsets[i],
                    0.0F,
                    4.5F, // 3× velocity
                    0.0F  // Perfect accuracy
                );
                serverLevel.addFreshEntity(projectile);
            }
        }

        // 2-second cooldown (40 ticks)
        player.getCooldowns().addCooldown(itemStack, 40);
        return InteractionResult.SUCCESS;
    }
}
