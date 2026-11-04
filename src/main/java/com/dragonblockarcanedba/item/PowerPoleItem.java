package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.PowerPoleExtensionEntity;
import com.dragonblockarcanedba.entity.PowerPoleImpactEntity;
import com.dragonblockarcanedba.entity.PowerPoleWhirlwindEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import com.dragonblockarcanedba.util.WeaponDrainHelper;

public class PowerPoleItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {

    public PowerPoleItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    749.0, // Late game damage (1 + 749 = 750)
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -2.0, // Faster than normal sword
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Aerodynamic Glide
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("power_pole_air_drag"), -0.40, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The whirlwind and the extended poke already pay. A vanilla connect used to
        // spend stamina again and show nothing of its own.
    }

    // Per-target whirlwind hit gate: the spin is called every 2 ticks while held, and
    // used to re-damage everything in the cone on every call. A target now takes a
    // spin hit at most once per 10 ticks. The key packs the SPINNING PLAYER's id
    // with the target's id: keyed by target alone, two players spinning near the
    // same mob shared one gate and one player's spin suppressed the other's hit.
    private static final java.util.Map<Long, Long> SPIN_HIT_TIMES = new java.util.concurrent.ConcurrentHashMap<>();

    private static long spinGateKey(Player player, LivingEntity target) {
        return (((long) player.getId()) << 32) | (target.getId() & 0xFFFFFFFFL);
    }

    public static void performWindSpin(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) {
            // Drain Stamina: 20% per minute (~10 ticks duration = ~0.167% Max Stamina)
            if (!WeaponDrainHelper.drainStaminaDiscrete(player, 20.0, 10)) {
                return;
            }

            ServerLevel serverLevel = (ServerLevel) player.level();

            double maxRange = 25.0; // 25 blocks max range
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookDir = player.getLookAngle();

            // ONE sustained whirlwind for the whole spin: spawn it on the first
            // call, refresh it on every later call. It builds over its first 10
            // ticks, sustains while refreshed, and dissipates over 24 ticks once
            // calls stop. Damage and deflection only run inside its active
            // window, so the funnel visibly builds before it can hurt anything.
            PowerPoleWhirlwindEntity whirlwind = PowerPoleWhirlwindEntity.findActive(player);
            if (whirlwind != null && whirlwind.getDecayStart() >= 0) {
                // Re-pressing mid-dissipation starts a fresh funnel instead of
                // refreshing one that is already blowing out.
                whirlwind.discard();
                whirlwind = null;
            }
            if (whirlwind == null) {
                whirlwind = new PowerPoleWhirlwindEntity(
                    serverLevel, player, eyePos, player.getYRot(), player.getXRot(), (float) maxRange, 35.0f
                );
                serverLevel.addFreshEntity(whirlwind);

                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.FLIGHT_WIND, SoundSource.PLAYERS, 1.4f, 1.3f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS, 1.0f, 1.1f);
            } else {
                whirlwind.refreshSpin();
            }
            boolean activeWindow = whirlwind.isInActiveWindow();

            // Inflate AABB to cover the max range
            AABB aoe = player.getBoundingBox().inflate(maxRange);
            List<LivingEntity> targets = activeWindow
                ? serverLevel.getEntitiesOfClass(LivingEntity.class, aoe, e -> e != player && e.isAlive())
                : List.of();

            for (LivingEntity t : targets) {
                Vec3 targetPos = t.position().add(0, t.getBbHeight() / 2, 0);
                Vec3 toTarget = targetPos.subtract(eyePos);
                double distance = toTarget.length();

                Vec3 toTargetDir = toTarget.normalize();
                double dotProduct = Math.max(-1.0, Math.min(1.0, lookDir.dot(toTargetDir)));
                double angleToTarget = Math.toDegrees(Math.acos(dotProduct));

                boolean inCone = angleToTarget <= 35.0;
                boolean inClosePerimeter = distance <= 4.5; // Radial barrier around the spinner

                if (inCone || inClosePerimeter) {
                    long now = serverLevel.getGameTime();
                    long gateKey = spinGateKey(player, t);
                    Long lastHit = SPIN_HIT_TIMES.get(gateKey);
                    if (lastHit != null && now - lastHit < 10) {
                        continue;
                    }
                    SPIN_HIT_TIMES.put(gateKey, now);
                    if (SPIN_HIT_TIMES.size() > 512) {
                        SPIN_HIT_TIMES.entrySet().removeIf(e -> now - e.getValue() >= 10);
                    }

                    // Inverse area damage scaling
                    float damage = 750.0f / (float) Math.max(1.0, distance / 3.0);
                    t.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.tempest(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (damage)));
                    
                    // Counter incoming rush velocity and apply overwhelming gale repulsion
                    com.dragonblockarcanedba.util.MovementLimiterHelper.applyPowerPoleGaleForce(t, eyePos, distance);

                    // MC 26.2 Physics: Apply bounciness so enemies ricochet off terrain
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        t,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("pole_spin_bounce"),
                        0.90,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                }
            }

            // Deflect incoming projectiles & Ki Blasts, inside the same active
            // window as the damage: a funnel still building does not bat
            // projectiles back yet.
            List<net.minecraft.world.entity.projectile.Projectile> projectiles = activeWindow
                ? serverLevel.getEntitiesOfClass(
                    net.minecraft.world.entity.projectile.Projectile.class, aoe, p -> p.isAlive() && p.getOwner() != player)
                : List.of();
            for (net.minecraft.world.entity.projectile.Projectile p : projectiles) {
                Vec3 pPos = p.position();
                Vec3 toProj = pPos.subtract(eyePos);
                double pDist = toProj.length();
                if (pDist <= maxRange && (pDist <= 4.5 || lookDir.dot(toProj.normalize()) > 0.75)) {
                    p.setOwner(player);
                    p.setDeltaMovement(toProj.normalize().scale(2.2));
                    com.dragonblockarcanedba.util.DbaCompat.markHurt(p);
                }
            }
        }
    }

    // Right Click: Extended Reach
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Drain Stamina: 20% per minute for 40-tick (2-second) cooldown cycle (0.667% Max Stamina)
        if (!WeaponDrainHelper.drainStaminaDiscrete(player, 20.0, 40)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            double reach = 30.0;
            
            Vec3 eyePos = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 endPos = eyePos.add(look.scale(reach));
            
            AABB hitBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePos, endPos, hitBox, e -> !e.isSpectator() && e.isPickable(), reach * reach);

            // Blocks clip over the FULL reach and the nearer of block or entity
            // wins: the pole used to check blocks only inside the player's short
            // POV reach, so past a few blocks it visually passed through walls
            // its own length said it should have stopped at.
            net.minecraft.world.phys.BlockHitResult blockHit = serverLevel.clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            boolean blockHitSomething = blockHit.getType() != HitResult.Type.MISS;
            double blockDist = blockHitSomething ? eyePos.distanceTo(blockHit.getLocation()) : Double.MAX_VALUE;
            double entityDist = entityHit != null ? eyePos.distanceTo(entityHit.getLocation()) : Double.MAX_VALUE;

            Vec3 hitVec = endPos;
            boolean hitSomething = false;
            net.minecraft.world.phys.BlockHitResult landedBlock = null;
            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living && entityDist <= blockDist) {
                hitVec = entityHit.getLocation();
                hitSomething = true;

                // Damage
                living.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.crush(player), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (750.0f)));
                
                // Pole Stun
                living.addEffect(new MobEffectInstance(DbaEffects.POLE_STUN_HOLDER, 100, 0, false, false));

                // MC 26.2 Physics: Apply high bounciness upon heavy impact
                com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                    living,
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                    com.dragonblockarcanedba.DragonBlockArcaneDBA.id("pole_poke_bounce"),
                    0.95,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                );
                
                // 15% Heavy Concussion Stun: amplifier 1 and always at least as long
                // as the guaranteed amplifier 0 application (was 60-100 ticks, which
                // could end sooner than the base stun it was meant to upgrade).
                if (serverLevel.getRandom().nextFloat() < 0.15f) {
                    int stunDuration = 100 + serverLevel.getRandom().nextInt(41); // 5-7 seconds
                    living.addEffect(new MobEffectInstance(DbaEffects.POLE_STUN_HOLDER, stunDuration, 1, false, false));
                }
            } else if (blockHitSomething) {
                hitVec = blockHit.getLocation();
                hitSomething = true;
                landedBlock = blockHit;
            }

            double dist = eyePos.distanceTo(hitVec);

            // Spawn Physical 3D Stretching Power Pole Model
            PowerPoleExtensionEntity extension = new PowerPoleExtensionEntity(
                serverLevel, player, eyePos, player.getYRot(), player.getXRot(), (float) dist
            );
            serverLevel.addFreshEntity(extension);

            // The impact entity and heavy impact sounds only play when the poke
            // actually connected; a total miss used to detonate them in mid-air.
            if (hitSomething) {
                PowerPoleImpactEntity impact = new PowerPoleImpactEntity(
                    serverLevel, player, hitVec, player.getYRot(), player.getXRot(), 2.4f
                );
                serverLevel.addFreshEntity(impact);

                // World interaction at the strike point (SPECTACLE-STANDARD
                // points 2 and 3): a dust burst where the pole lands, real
                // debris pieces torn from the ground at the hit that orbit,
                // fall, and rain back down, and torn turf when the pole comes
                // down on grass from above (temporary, restored by GroundScar).
                com.dragonblockarcanedba.util.LayeredParticles.emitGroundBurst(
                    serverLevel, hitVec, 1.6f, 0xF2C14E);
                com.dragonblockarcanedba.util.DebrisSpawner.spawnField(
                    serverLevel, player, hitVec, 24, 2.2, 2.5, 18);
                if (landedBlock != null
                    && landedBlock.getDirection() == net.minecraft.core.Direction.UP) {
                    net.minecraft.core.BlockPos groundPos = landedBlock.getBlockPos();
                    var rand = serverLevel.getRandom();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            net.minecraft.core.BlockPos cell = groundPos.offset(dx, 0, dz);
                            if (serverLevel.getBlockState(cell).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                                && rand.nextFloat() < 0.7f) {
                                com.dragonblockarcanedba.util.GroundScar.scar(serverLevel, cell,
                                    net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 120);
                            }
                        }
                    }
                }

                serverLevel.playSound(null, hitVec.x, hitVec.y, hitVec.z,
                    com.dragonblockarcanedba.sound.DbaSounds.SLAM_THUD, SoundSource.PLAYERS, 1.2f, 1.6f);
                serverLevel.playSound(null, hitVec.x, hitVec.y, hitVec.z,
                    com.dragonblockarcanedba.sound.DbaSounds.HEAVY_IMPACT, SoundSource.PLAYERS, 1.3f, 1.2f);
            }

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.TRIDENT_THROW, SoundSource.PLAYERS, 1.4f, 1.4f);
            
            player.getCooldowns().addCooldown(stack, 40); // 2 second cooldown
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
