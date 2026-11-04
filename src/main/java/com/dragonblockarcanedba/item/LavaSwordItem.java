package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.LavaCleaveEntity;
import com.dragonblockarcanedba.entity.LavaEruptionEntity;
import com.dragonblockarcanedba.entity.LavaField;
import com.dragonblockarcanedba.entity.LavaMeteorEntity;
import com.dragonblockarcanedba.entity.LavaSwordGround;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.WeaponDrainHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lava sword. Ki, matching Grand Sword's 90% per minute and Spirit Sword's discrete swing.
 * The eruption cooldown is Grand Sword's finisher length. The cleave cooldown only
 * swallows the swing packet that follows the same press.
 */
public class LavaSwordItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    private static final double KI_PERCENT = 90.0;
    private static final Map<UUID, Long> LAST_CLEAVE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ERUPT_TICK = new ConcurrentHashMap<>();
    /** Meteor finisher (redesign idea 5): hold sneak and right click for this many ticks. */
    private static final int METEOR_CHARGE_TICKS = 50;
    private static final int METEOR_COUNT = 7;
    /** Finisher and ultimate readiness, kept off the item cooldown so they never block cleave or eruption. */
    private static final int METEOR_COOLDOWN_TICKS = 400;
    private static final int VOLCANO_COOLDOWN_TICKS = 1200;
    private static final Set<UUID> CHARGING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> METEOR_READY_AT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> VOLCANO_READY_AT = new ConcurrentHashMap<>();

    static {
        // Item registration loads this class at startup, so this is where the
        // ground burn's restore ticker hooks in: crash recovery then runs from
        // the first server tick instead of waiting for the first swing.
        LavaSwordGround.register();
        // The redesign's field engine hooks in the same way, for the same reason.
        LavaField.register();
    }

    public LavaSwordItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 859.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.1, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The blade is living lava, so a plain hit burns too: the same 60
        // ticks the cleave applies, plus a spit of flame off the target. The
        // abilities still pay their own way on top of the melee hit.
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
        if (target.level() instanceof ServerLevel server) {
            server.sendParticles(DbaParticles.FLAME,
                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                6, 0.25, 0.3, 0.25, 0.02);
        }
    }

    public static void performCleave(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = LAST_CLEAVE_TICK.get(id);
        if (last != null && last == now) {
            return;
        }
        LAST_CLEAVE_TICK.put(id, now);
        if (player.isShiftKeyDown()) {
            // Sneak and left click is the volcano ultimate (redesign idea
            // 6), not a cleave. It has its own readiness clock, so it never
            // touches the cleave's Ki drain or item cooldown.
            performVolcano(player, now);
            return;
        }
        if (!WeaponDrainHelper.drainKiDiscrete(player, KI_PERCENT, 14)) {
            return;
        }
        player.getCooldowns().addCooldown(stack, 8);
        ServerLevel level = (ServerLevel) player.level();
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 740.0f + strength * 2.0f;
        Vec3 look = player.getLookAngle();
        LavaCleaveEntity cleave = new LavaCleaveEntity(level, player, damage);
        cleave.setDeltaMovement(look.scale(1.35));
        level.addFreshEntity(cleave);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.FIRE_SHOT, SoundSource.PLAYERS, 0.85f, 0.7f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    /**
     * The volcano ultimate (redesign idea 6): sneak and left click plants
     * a mini volcano on the ground block in reach. It stands for 30
     * seconds, erupting every 36 to 54 ticks, then crumbles and the cell
     * restores (LavaField holds the site entry). One active volcano per
     * player, a 60 second readiness, and the heaviest Ki drain the sword
     * has: it is a zoning ultimate, not a swing.
     */
    public static void performVolcano(ServerPlayer player, long now) {
        UUID id = player.getUUID();
        if (now < VOLCANO_READY_AT.getOrDefault(id, 0L)) {
            player.sendSystemMessage(Component.literal("§cThe volcano is not ready yet"), true);
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(7.0));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            player.sendSystemMessage(Component.literal("§cNo ground in reach for a volcano"), true);
            return;
        }
        BlockPos groundPos = hit.getBlockPos();
        if (!LavaField.canPlantVolcano(level, groundPos, id)) {
            String reason = LavaField.activeVolcanoes(level, id) >= 1
                ? "§cYou already have a volcano standing"
                : "§cA volcano cannot rise there";
            player.sendSystemMessage(Component.literal(reason), true);
            return;
        }
        if (!WeaponDrainHelper.drainKiDiscrete(player, KI_PERCENT, 200)) {
            return;
        }
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float blobDamage = 100.0f + strength * 0.6f;
        if (!LavaField.plantVolcano(level, groundPos, id, blobDamage)) {
            player.sendSystemMessage(Component.literal("§cA volcano cannot rise there"), true);
            return;
        }
        VOLCANO_READY_AT.put(id, now + VOLCANO_COOLDOWN_TICKS);
        level.playSound(null, groundPos.getX() + 0.5, groundPos.getY() + 1.0, groundPos.getZ() + 0.5,
            DbaSounds.GROUND_CRACK, SoundSource.PLAYERS, 1.1f, 0.7f);
        level.playSound(null, groundPos.getX() + 0.5, groundPos.getY() + 1.0, groundPos.getZ() + 0.5,
            DbaSounds.LAVA_BUBBLE, SoundSource.PLAYERS, 1.1f, 0.55f);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack) || !WeaponDrainHelper.hasKi(player)) {
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            // Sneak and hold right click charges the meteor finisher
            // (redesign idea 5). Plain right click stays the eruption.
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                long now = level.getGameTime();
                if (now < METEOR_READY_AT.getOrDefault(player.getUUID(), 0L)) {
                    serverPlayer.sendSystemMessage(Component.literal("§cThe meteor finisher is not ready yet"), true);
                    return InteractionResult.FAIL;
                }
                CHARGING.add(player.getUUID());
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            performEruption(serverPlayer, stack);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (level.isClientSide() || !(living instanceof ServerPlayer player)) {
            return;
        }
        if (!CHARGING.contains(player.getUUID())) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        int heldTicks = getUseDuration(stack, living) - remainingTicks;
        // The sword heats up in the hand: flame licks off the blade and
        // the charge hum climbs until the full-charge chime at 50 ticks.
        Vec3 handPos = player.getEyePosition().add(player.getLookAngle().scale(0.6));
        server.sendParticles(DbaParticles.FLAME, handPos.x, handPos.y - 0.2, handPos.z,
            2, 0.15, 0.15, 0.15, 0.02);
        if (heldTicks == METEOR_CHARGE_TICKS) {
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                DbaSounds.CHARGE_FULL, SoundSource.PLAYERS, 1.0f, 0.9f);
        } else if (heldTicks > 0 && heldTicks % 20 == 0) {
            float ratio = Math.min(1.0f, heldTicks / (float) METEOR_CHARGE_TICKS);
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                DbaSounds.CHARGE_HUM, SoundSource.PLAYERS, 0.8f, 0.6f + ratio * 0.6f);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide() || !(living instanceof ServerPlayer player)) {
            return false;
        }
        if (!CHARGING.remove(player.getUUID())) {
            // Not a meteor charge (the set is the guard, the Blaster Gun
            // pattern): some other use of the item ended.
            return false;
        }
        ServerLevel server = (ServerLevel) level;
        int heldTicks = getUseDuration(stack, living) - timeLeft;
        if (heldTicks < METEOR_CHARGE_TICKS
                || !WeaponDrainHelper.drainKiDiscrete(player, KI_PERCENT, 80)) {
            // Released early or out of Ki: the charge dies in the hand.
            // No meteors, no cost beyond the failed drain, no readiness.
            Vec3 fizzlePos = player.getEyePosition().add(player.getLookAngle().scale(0.6));
            server.sendParticles(DbaParticles.EMBER, fizzlePos.x, fizzlePos.y, fizzlePos.z,
                6, 0.2, 0.2, 0.2, 0.03);
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                DbaSounds.FIZZLE, SoundSource.PLAYERS, 1.0f, 1.1f);
            if (heldTicks < METEOR_CHARGE_TICKS) {
                player.sendSystemMessage(Component.literal("§cMeteor finisher fizzled: hold the charge longer"), true);
            }
            return true;
        }
        long now = level.getGameTime();
        METEOR_READY_AT.put(player.getUUID(), now + METEOR_COOLDOWN_TICKS);
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 520.0f + strength * 1.6f;
        // The volley falls on the point the player is looking at, out to
        // 64 blocks: one meteor on the point, six staggered around it,
        // each spawning 4 ticks of fall above the last.
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(64.0));
        BlockHitResult hit = server.clip(new ClipContext(eye, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : end;
        for (int i = 0; i < METEOR_COUNT; i++) {
            double angle = i * 2.4;
            double radius = i == 0 ? 0.0 : 1.5 + (i % 3);
            Vec3 spawn = new Vec3(
                target.x + Math.cos(angle) * radius,
                target.y + 26 + i * 4,
                target.z + Math.sin(angle) * radius);
            server.addFreshEntity(new LavaMeteorEntity(server, player, spawn, damage));
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.RELEASE_BLAST, SoundSource.PLAYERS, 1.1f, 0.7f);
        server.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.FIRE_SHOT, SoundSource.PLAYERS, 1.0f, 0.5f);
        return true;
    }

    public static void performEruption(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = LAST_ERUPT_TICK.get(id);
        if (last != null && last == now) {
            return;
        }
        LAST_ERUPT_TICK.put(id, now);
        if (!WeaponDrainHelper.drainKiDiscrete(player, KI_PERCENT, 40)) {
            return;
        }
        player.getCooldowns().addCooldown(stack, 40);
        ServerLevel level = (ServerLevel) player.level();
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 880.0f + strength * 2.2f;
        level.addFreshEntity(new LavaEruptionEntity(level, player, damage));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.LAVA_BUBBLE, SoundSource.PLAYERS, 1.1f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.FIRE_SHOT, SoundSource.PLAYERS, 1.0f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            DbaSounds.FIRE_IGNITE, SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        LAST_CLEAVE_TICK.remove(playerUuid);
        LAST_ERUPT_TICK.remove(playerUuid);
        CHARGING.remove(playerUuid);
        METEOR_READY_AT.remove(playerUuid);
        VOLCANO_READY_AT.remove(playerUuid);
    }
}
