package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.entity.WarMaceRiseEntity;
import com.dragonblockarcanedba.entity.WarMaceSmashEntity;
import com.dragonblockarcanedba.movement.MovementPhysics;
import com.dragonblockarcanedba.util.WeaponDrainHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * War Mace. Heavier than the Z sword, and it spends stamina the way Ox King's axe
 * spends it on a landing: 125% of max stamina per minute, paid once per blow.
 * A rising blow and a falling smash do not share a lock, so the launch can be
 * followed. The short item cooldown only swallows a second packet from the same press.
 */
public class WarMaceItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    private static final double STAMINA_PERCENT = 125.0;
    /** Same upward speed the right-click rise adds from a standstill. */
    private static final double RISE_VERTICAL = 1.08;
    /** Same look nudge the right-click rise adds. */
    private static final double RISE_LOOK = 0.18;
    private static final Map<UUID, Long> LAST_SMASH_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_RISE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_GROUND = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_HEAVY = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_RISE = new ConcurrentHashMap<>();

    public WarMaceItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    private static ItemAttributeModifiers createModifiers() {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 1299.0, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.4, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            );
        com.dragonblockarcanedba.util.DbaPhysicsAttributes.getAttributeHolder(com.dragonblockarcanedba.util.DbaPhysicsAttributes.FRICTION_ID).ifPresent(h ->
            builder.add(h, new AttributeModifier(com.dragonblockarcanedba.DragonBlockArcaneDBA.id("war_mace_weight"), 0.9, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
        );
        return builder.build();
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // The smash and the rising blow already pay. The melee attribute is the hit itself.
    }

    public static void performSmash(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        Long last = LAST_SMASH_TICK.get(id);
        if (last != null && last == now) {
            return;
        }
        LAST_SMASH_TICK.put(id, now);
        boolean heavy = !player.onGround() && player.getDeltaMovement().y < -0.08;
        if (heavy) {
            if (now < NEXT_HEAVY.getOrDefault(id, 0L)) {
                return;
            }
        } else if (now < NEXT_GROUND.getOrDefault(id, 0L)) {
            return;
        }
        int payTicks = heavy ? 48 : 28;
        if (!WeaponDrainHelper.drainStaminaDiscrete(player, STAMINA_PERCENT, payTicks)) {
            return;
        }
        if (heavy) {
            player.resetFallDistance();
            NEXT_HEAVY.put(id, now + 16L);
        } else {
            NEXT_GROUND.put(id, now + 10L);
        }
        player.getCooldowns().addCooldown(stack, 5);
        ServerLevel level = (ServerLevel) player.level();
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = heavy ? 1600.0f + strength * 4.0f : 900.0f + strength * 2.5f;
        WarMaceSmashEntity smash = new WarMaceSmashEntity(level, player, damage, heavy);
        level.addFreshEntity(smash);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, heavy ? 1.6f : 1.0f, heavy ? 0.55f : 0.75f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, heavy ? 1.1f : 0.55f, heavy ? 0.7f : 0.95f);
        com.dragonblockarcanedba.fx.DbaImpactFx.heavy(level, player.getX(), player.getY(), player.getZ());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack) || !WeaponDrainHelper.hasStamina(player)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            performRise(serverPlayer, stack);
        }
        return InteractionResult.CONSUME;
    }

    public static void performRise(ServerPlayer player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        long now = player.level().getGameTime();
        UUID id = player.getUUID();
        if (now < NEXT_RISE.getOrDefault(id, 0L)) {
            return;
        }
        Long last = LAST_RISE_TICK.get(id);
        if (last != null && last == now) {
            return;
        }
        LAST_RISE_TICK.put(id, now);
        if (!WeaponDrainHelper.drainStaminaDiscrete(player, STAMINA_PERCENT, 22)) {
            return;
        }
        NEXT_RISE.put(id, now + 18L);
        player.getCooldowns().addCooldown(stack, 6);
        player.resetFallDistance();
        Vec3 look = player.getLookAngle();
        MovementPhysics.addVelocity(player, new Vec3(look.x * RISE_LOOK, RISE_VERTICAL, look.z * RISE_LOOK));
        ServerLevel level = (ServerLevel) player.level();
        float strength = player instanceof PlayerStatsAccessor acc ? acc.dba$getStrength() : 0f;
        float damage = 480.0f + strength * 1.5f;
        level.addFreshEntity(new WarMaceRiseEntity(level, player, damage));
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1f, 0.55f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    /**
     * A smash that connected launches the player again at the rise's upward speed.
     * The vertical component is set, not added: the smash connects while falling,
     * and adding the rise onto that downward speed would not clear the ground.
     * The heavy and ground locks are dropped so the next falling smash is not
     * held back. The short item cooldown stays, so one press cannot pay twice.
     */
    public static void relaunchFromSmash(ServerPlayer player) {
        UUID id = player.getUUID();
        NEXT_HEAVY.remove(id);
        NEXT_GROUND.remove(id);
        Vec3 look = player.getLookAngle();
        Vec3 current = player.getDeltaMovement();
        MovementPhysics.setVelocity(player, new Vec3(
            current.x + look.x * RISE_LOOK,
            RISE_VERTICAL,
            current.z + look.z * RISE_LOOK
        ));
        player.resetFallDistance();
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        LAST_SMASH_TICK.remove(playerUuid);
        LAST_RISE_TICK.remove(playerUuid);
        NEXT_GROUND.remove(playerUuid);
        NEXT_HEAVY.remove(playerUuid);
        NEXT_RISE.remove(playerUuid);
    }
}
