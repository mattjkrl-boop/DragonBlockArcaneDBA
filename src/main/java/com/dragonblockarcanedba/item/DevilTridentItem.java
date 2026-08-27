package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.DevilSlamShockwaveEntity;
import com.dragonblockarcanedba.entity.TridentShardEntity;
import net.minecraft.server.level.ServerLevel;
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
import java.util.Map;
import com.dragonblockarcanedba.util.SwarmHelper;
import com.dragonblockarcanedba.util.WeaponDrainHelper;

public class DevilTridentItem extends Item {
    public DevilTridentItem(Properties properties) {
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
                    -2.2, // Slightly faster
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            );

        // MC 26.2 Physics: Demonic Overlord Stance & Hellish Grip
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("devil_trident_friction"), 0.60, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );

        return builder.build();
    }

    // Left Click: Three-Pronged Lasers or Targeting
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            // Let the network payload / air swing handler do the targeting so it works consistently.
            // If they actually physically hit a target in melee, we can also target them here as a fallback.
            performLeftClickTargeting(player, stack, target);
        }
    }

    public static void performLeftClickTargeting(Player player, ItemStack stack, LivingEntity fallbackTarget) {
        if (!player.level().isClientSide() && player.level() instanceof ServerLevel serverLevel) {
            net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if (tag.getBoolean("isDeployed").orElse(false)) {
                LivingEntity target = fallbackTarget;
                
                // Raytrace to find target if fallback is null
                if (target == null) {
                    Vec3 eyePos = player.getEyePosition();
                    Vec3 look = player.getLookAngle();
                    
                    AABB searchBox = player.getBoundingBox().inflate(64.0);
                    List<LivingEntity> potentialTargets = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, searchBox,
                        e -> e.isAlive() && e != player && !e.isSpectator()
                    );

                    double bestScore = Double.MAX_VALUE;
                    for (LivingEntity e : potentialTargets) {
                        Vec3 toTarget = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eyePos);
                        double distance = toTarget.length();
                        double dot = look.dot(toTarget.normalize());

                        if (dot > 0.90) { // Forward cone
                            double score = distance * (2.0 - dot);
                            if (score < bestScore) {
                                bestScore = score;
                                target = e;
                            }
                        }
                    }
                }
                
                if (target != null) {
                    tag.putString("swarmTarget", target.getUUID().toString());
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    target.addEffect(new MobEffectInstance(DbaEffects.MARKED_BY_EVIL_HOLDER, 60, 0, false, false));
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CONVERTED, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 2.0f);
                }
            } else {
                fireLasers(player);
            }
        }
    }

    public static void fireLasers(Player player) {
        if (!player.level().isClientSide()) {
            Vec3 look = player.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            Vec3 up = right.cross(look).normalize();

            boolean isRightHanded = (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            boolean isOffhand = (player.getOffhandItem().getItem() instanceof DevilTridentItem && 
                !(player.getMainHandItem().getItem() instanceof DevilTridentItem));
            boolean onRight = isRightHanded ? !isOffhand : isOffhand;
            float sideSign = onRight ? 1.0f : -1.0f;

            Vec3 handOrigin = player.getEyePosition().add(look.scale(0.8)).add(right.scale(sideSign * 0.35)).add(up.scale(-0.25));

            for (int i = -1; i <= 1; i++) {
                com.dragonblockarcanedba.entity.KiBlastEntity laser = new com.dragonblockarcanedba.entity.KiBlastEntity(com.dragonblockarcanedba.entity.DbaEntities.KI_BLAST, player.level());
                laser.setOwner(player);
                laser.setDamage(250.0f); // 3 * 250 = 750
                laser.setColor(0xFF0000);
                
                Vec3 offset = right.scale(i * 0.25);
                Vec3 start = handOrigin.add(offset);
                
                laser.setPos(start.x, start.y, start.z);
                laser.shoot(look.x, look.y, look.z, 2.5f, 0.0f);
                player.level().addFreshEntity(laser);
            }
        }
    }

    public static void recallShards(ServerLevel serverLevel, Player player, ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        List<TridentShardEntity> shards = serverLevel.getEntitiesOfClass(TridentShardEntity.class, player.getBoundingBox().inflate(64.0), e -> e.getOwnerId() == player.getId());
        for (TridentShardEntity shard : shards) {
            shard.recall();
        }
        SwarmHelper.recallSwarm(stack);
        player.getCooldowns().addCooldown(stack, 3600); // 3-minute cooldown (3600 ticks)
    }

    // Right Click: Deployment / Recall
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            boolean isDeployed = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("isDeployed").orElse(false);

            if (isDeployed) {
                // Recall Shards
                recallShards(serverLevel, player, stack, stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag());
            } else {
                // Physical 3D Demonic Ground Slam & Shockwave
                Vec3 pos = player.position();
                DevilSlamShockwaveEntity slamEntity = new DevilSlamShockwaveEntity(serverLevel, player, pos, 5.0f);
                serverLevel.addFreshEntity(slamEntity);

                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.7f);

                AABB aoe = player.getBoundingBox().inflate(5.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, aoe, e -> e != player && e.isAlive());
                for (LivingEntity t : targets) {
                    t.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(player), 750.0f);
                    t.addEffect(new MobEffectInstance(DbaEffects.DEVILS_HANDS_HOLDER, 300, 2, false, true), player);
                    t.addEffect(new MobEffectInstance(DbaEffects.CINEMATIC_TRACKING_HOLDER, 300, 0, false, false, false), player);
                    
                    // MC 26.2 Physics: Demonic ground slam kinetic bounce
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        t,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.BOUNCINESS_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("devil_slam_bounce"),
                        0.85,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                    );
                    com.dragonblockarcanedba.util.DbaPhysicsAttributes.applyModifier(
                        t,
                        com.dragonblockarcanedba.util.DbaPhysicsAttributes.AIR_DRAG_ID,
                        com.dragonblockarcanedba.DragonBlockArcaneDBA.id("devil_slam_drag"),
                        -0.40,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    );

                    // Ground pull
                    t.setDeltaMovement(t.getDeltaMovement().add(0, -2.0, 0));
                    t.hurtMarked = true;
                }

                // Spawn 10 Shards
                for (int i = 0; i < 10; i++) {
                    TridentShardEntity shard = new TridentShardEntity(player.level(), player, i);
                    shard.setPos(pos.x, pos.y + 2.0, pos.z);
                    serverLevel.addFreshEntity(shard);
                }

                SwarmHelper.deploySwarm(stack, 10, 15000.0f); // 15000 is default health
                
                player.getCooldowns().addCooldown(stack, 20); // 1 second buffer before recall
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static void manageShardSwarm(Player player, ServerLevel world) {
        // Quick check: only execute swarm management if player is holding a Devil Trident
        ItemStack trident = player.getMainHandItem();
        if (!(trident.getItem() instanceof DevilTridentItem)) {
            trident = player.getOffhandItem();
        }
        if (!(trident.getItem() instanceof DevilTridentItem)) {
            return;
        }

        List<TridentShardEntity> activeShards = new java.util.ArrayList<>(world.getEntitiesOfClass(TridentShardEntity.class, player.getBoundingBox().inflate(64.0), e -> e.getOwnerId() == player.getId() && !e.isRecalling()));

        // 1. Prioritize Protection: Find incoming projectiles
        List<net.minecraft.world.entity.projectile.Projectile> projectiles = world.getEntitiesOfClass(
            net.minecraft.world.entity.projectile.Projectile.class, 
            player.getBoundingBox().inflate(12.0), // Increased radius from 6 to 12
            p -> p.getOwner() != player && !(p instanceof TridentShardEntity)
        );
        
        for (net.minecraft.world.entity.projectile.Projectile proj : projectiles) {
            if (!activeShards.isEmpty()) {
                TridentShardEntity protector = activeShards.remove(0);
                protector.intercept(proj);
            }
        }

        LivingEntity priorityTarget = null;
        if (trident.getItem() instanceof DevilTridentItem) {
            Map<Integer, Float> missing = SwarmHelper.getMissingEntities(trident, activeShards);
            if (!missing.isEmpty()) {
                for (Map.Entry<Integer, Float> entry : missing.entrySet()) {
                    TridentShardEntity shard = new TridentShardEntity(player.level(), player, entry.getKey());
                    shard.setSwarmHealth(entry.getValue());
                    shard.setPos(player.getX(), player.getY() + 2.0, player.getZ());
                    world.addFreshEntity(shard);
                    activeShards.add(shard);
                }
            }

            if (activeShards.isEmpty()) return;

            net.minecraft.nbt.CompoundTag tag = trident.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            
            // Ki Drain logic: 20% per minute (all shards die when Ki is exhausted)
            if (tag.getBoolean("isDeployed").orElse(false)) {
                if (!WeaponDrainHelper.drainKiPerTick(player, 20.0)) {
                    // Out of Ki! All shards die immediately and swarm is recalled
                    for (TridentShardEntity shard : activeShards) {
                        shard.discard();
                    }
                    SwarmHelper.recallSwarm(trident);
                    player.getCooldowns().addCooldown(trident, 3600); // 3-minute cooldown (3600 ticks)
                    return; // Stop processing swarm
                }
            }

            if (tag.contains("swarmTarget")) {
                String targetUuidStr = tag.getString("swarmTarget").orElse("");
                if (!targetUuidStr.isEmpty()) {
                    java.util.UUID targetUuid = java.util.UUID.fromString(targetUuidStr);
                    net.minecraft.world.entity.Entity e = world.getEntity(targetUuid);
                    if (e instanceof LivingEntity le && le.isAlive()) {
                        priorityTarget = le;
                    } else {
                        // Target died or lost, clear it
                        tag.remove("swarmTarget");
                        trident.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    }
                }
            }
        }

        if (priorityTarget != null) {
            // Swarm the priority target with all remaining shards!
            for (TridentShardEntity attacker : activeShards) {
                attacker.setTarget(priorityTarget);
            }
            activeShards.clear();
        } else {
            // 2. Offense: Calculate needed damage
            List<LivingEntity> enemies = world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(40.0), e -> e.isAlive() && e != player);
            
            for (LivingEntity enemy : enemies) {
                if (activeShards.isEmpty()) break;
                
                float enemyHealth = enemy.getHealth();
                int shardsNeeded = (int) Math.ceil(enemyHealth / 100.0f); // 100 is shard damage
                
                // Send only the exact number of shards needed to kill
                for (int i = 0; i < shardsNeeded && !activeShards.isEmpty(); i++) {
                    TridentShardEntity attacker = activeShards.remove(0);
                    attacker.setTarget(enemy);
                }
            }
        }
        
        // Remaining shards just orbit
        for (TridentShardEntity idle : activeShards) {
            idle.setTarget(null);
        }
    }
}
