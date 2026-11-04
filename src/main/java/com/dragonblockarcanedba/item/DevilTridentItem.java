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

public class DevilTridentItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
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
                        com.dragonblockarcanedba.sound.DbaSounds.MARK_HIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 2.0f);
                }
            } else {
                fireLasers(player);
            }
        }
    }

    /** Damage one shard deals per hit: flat 100 plus half the owner's Strength. */
    public static float shardDamage(Player player) {
        float damage = 100.0f;
        if (player instanceof com.dragonblockarcanedba.attribute.PlayerStatsAccessor accessor) {
            damage += (float) (accessor.dba$getStrength() * 0.5);
        }
        return damage;
    }

    public static void fireLasers(Player player) {
        if (!player.level().isClientSide()) {
            // The lasers were free and spammable at 750 damage a click. They now cost
            // Ki and carry a short cooldown like the other weapons' click abilities.
            // The stack is resolved strictly: main hand if it is the trident, else
            // the offhand if it is, else no shot and no Ki charged. The old code
            // took the offhand unconditionally, so an unusual call path with no
            // trident in either hand drained Ki and silently skipped the cooldown.
            ItemStack tridentStack = player.getMainHandItem().getItem() instanceof DevilTridentItem
                ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof DevilTridentItem
                    ? player.getOffhandItem() : ItemStack.EMPTY;
            if (!(tridentStack.getItem() instanceof DevilTridentItem)) {
                return;
            }
            if (player.getCooldowns().isOnCooldown(tridentStack)) {
                return;
            }
            if (!WeaponDrainHelper.drainKiDiscrete(player, 60.0, 10)) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNot enough Ki!"), true);
                }
                return;
            }
            player.getCooldowns().addCooldown(tridentStack, 10);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.TRIDENT_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.9f);

            // One trident shot: three barbed prongs in formation, 750 together.
            // The old shot was three copies of the shared ki bolt painted red.
            com.dragonblockarcanedba.entity.TridentProngEntity shot =
                new com.dragonblockarcanedba.entity.TridentProngEntity(player.level(), player, 750.0f);
            player.level().addFreshEntity(shot);
        }
    }

    public static void recallShards(ServerLevel serverLevel, Player player, ItemStack stack, net.minecraft.nbt.CompoundTag tag) {
        List<TridentShardEntity> shards = serverLevel.getEntitiesOfClass(TridentShardEntity.class, player.getBoundingBox().inflate(64.0), e -> e.getOwnerId() == player.getId());
        for (TridentShardEntity shard : shards) {
            shard.recall();
        }
        SwarmHelper.recallSwarm(stack);
        clearWipeKeys(stack);
        // Manual recall is a choice, so it costs 30 seconds. The 3-minute cooldown is
        // reserved for the forced recall when the swarm starves on Ki (manageShardSwarm).
        player.getCooldowns().addCooldown(stack, 600);
    }

    /** Removes the starvation wipe bookkeeping keys from a trident stack, if present. */
    private static void clearWipeKeys(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (tag.contains("swarmWipePending") || tag.contains("swarmWipeEmptyTicks")) {
            tag.remove("swarmWipePending");
            tag.remove("swarmWipeEmptyTicks");
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
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
                // Physical 3D Demonic Ground Slam & Shockwave. The slam entity owns
                // the hit now: its damage and launch land at tick 8, when the seal
                // has grown in and the spikes stand, over radius 6.0, the same
                // radius its seal draws. The cast tick itself only telegraphs.
                Vec3 pos = player.position();
                DevilSlamShockwaveEntity slamEntity = new DevilSlamShockwaveEntity(serverLevel, player, pos, 6.0f);
                serverLevel.addFreshEntity(slamEntity);

                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    com.dragonblockarcanedba.sound.DbaSounds.BOOM_BIG,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.7f);

                com.dragonblockarcanedba.fx.DbaImpactFx.heavy(serverLevel, pos.x, pos.y, pos.z);

                // Spawn 10 Shards bursting out of the slam crater in a ring. Each
                // shard flies out to its place over its first ticks (staggered by
                // index, see TridentShardEntity) instead of popping in overhead.
                for (int i = 0; i < 10; i++) {
                    TridentShardEntity shard = new TridentShardEntity(player.level(), player, i);
                    double a = i * (Math.PI * 2.0 / 10.0);
                    shard.setPos(pos.x + Math.cos(a) * 0.9, pos.y + 0.5, pos.z + Math.sin(a) * 0.9);
                    serverLevel.addFreshEntity(shard);
                }

                SwarmHelper.deploySwarm(stack, 10, 15000.0f); // 15000 is default health
                clearWipeKeys(stack);

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

            net.minecraft.nbt.CompoundTag tag = trident.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();

            // Starvation wipe settlement: when Ki starved, the shards were sent home
            // and the 3-minute cooldown normally starts when the last one arrives
            // (onSwarmArrivedHome). A shard discarded by distance, logoff, or chunk
            // unload never arrives, so the cooldown used to be skippable entirely.
            // Now the wipe is a debt on the stack: if every shard is simply gone for
            // 40 straight ticks while a wipe is pending, the swarm is settled here
            // with the same recall and the same 3600 tick cooldown. The 40 tick
            // delay keeps a shard in a briefly unloaded chunk from settling early.
            if (tag.getBoolean("isDeployed").orElse(false) && tag.getBoolean("swarmWipePending").orElse(false)) {
                int totalShards = world.getEntitiesOfClass(TridentShardEntity.class,
                    player.getBoundingBox().inflate(160.0), e -> e.getOwnerId() == player.getId()).size();
                if (totalShards > 0) {
                    if (tag.getInt("swarmWipeEmptyTicks").orElse(0) != 0) {
                        tag.putInt("swarmWipeEmptyTicks", 0);
                        trident.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    }
                } else {
                    int emptyTicks = tag.getInt("swarmWipeEmptyTicks").orElse(0) + 1;
                    if (emptyTicks >= 40) {
                        SwarmHelper.recallSwarm(trident);
                        clearWipeKeys(trident);
                        player.getCooldowns().addCooldown(trident, 3600);
                        return;
                    }
                    tag.putInt("swarmWipeEmptyTicks", emptyTicks);
                    trident.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                }
            }

            if (activeShards.isEmpty()) return;

            // Ki Drain logic: 20% per minute (all shards die when Ki is exhausted)
            if (tag.getBoolean("isDeployed").orElse(false)) {
                if (!WeaponDrainHelper.drainKiPerTick(player, 20.0)) {
                    // Out of Ki. Every shard flies home and the wipe debt is marked on
                    // the stack. The same 3-minute cooldown starts in
                    // onSwarmArrivedHome once the last one is in the trident, or in
                    // the settlement above if none of them ever arrive.
                    tag.putBoolean("swarmWipePending", true);
                    trident.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    for (TridentShardEntity shard : activeShards) {
                        shard.returnHome();
                    }
                    return;
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
            // 2. Offense: Calculate needed damage. Auto-assignment only hunts actual
            // enemies; passive and friendly mobs are never targeted on their own
            // (an explicit swarmTarget set by clicking still hits whatever was clicked).
            List<LivingEntity> enemies = world.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(40.0), e -> e.isAlive() && e != player && e instanceof net.minecraft.world.entity.monster.Enemy);

            float perShard = shardDamage(player);
            for (LivingEntity enemy : enemies) {
                if (activeShards.isEmpty()) break;

                float enemyHealth = enemy.getHealth();
                int shardsNeeded = (int) Math.ceil(enemyHealth / perShard);
                
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

    /**
     * The last shard has reached the trident. This is the same 3-minute wipe
     * cooldown that used to fire the moment the swarm was deleted.
     * Manual recall does not come through here; that one still costs 600 ticks.
     */
    public static void onSwarmArrivedHome(Player player) {
        ItemStack trident = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof DevilTridentItem)) {
                continue;
            }
            boolean deployed = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag()
                .getBoolean("isDeployed")
                .orElse(false);
            if (deployed) {
                trident = stack;
                break;
            }
        }
        if (!(trident.getItem() instanceof DevilTridentItem)) {
            return;
        }
        SwarmHelper.recallSwarm(trident);
        clearWipeKeys(trident);
        player.getCooldowns().addCooldown(trident, 3600);
    }
}
