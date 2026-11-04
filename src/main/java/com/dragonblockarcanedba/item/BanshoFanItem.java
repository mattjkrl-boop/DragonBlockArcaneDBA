package com.dragonblockarcanedba.item;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.effect.DbaEffects;
import com.dragonblockarcanedba.entity.BanshoCycloneEntity;
import com.dragonblockarcanedba.entity.BanshoGaleEntity;
import com.dragonblockarcanedba.entity.BanshoShockwaveEntity;
import com.dragonblockarcanedba.entity.BanshoWindProjectileEntity;
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
import net.minecraft.world.phys.Vec3;
import com.dragonblockarcanedba.util.WeaponDrainHelper;

/**
 * Bansho Fan — The Storm Emperor. Endgame legendary weapon.
 *
 * Stamina cost: 45% per minute charged discretely per action (the swing cost is
 * charged once per swing by the left-click network handler).
 *
 * Left-click: 400 + (Strength × 1.5) damage.
 *   - Gale Force: Launches the target with a strong outward and upward velocity & physics modifiers
 *   - Applies Bleeding II for 15 seconds (wind cuts)
 *   - Spawns physical 3D emerald impact shockwave
 *   - Great Cyclone: every landed hit casts the cyclone at the target while the
 *     storm cooldown (120 ticks) is ready and no cyclone of yours is standing.
 *     The cyclone lives 100 ticks (20 build, peak, 35 dissipate), pulls, spins,
 *     and lifts everything in a 4 block radius, throws a 64 piece debris field,
 *     drives the mod's own rain, scours soft ground, and ejects what it holds at the
 *     end. Its shred figure is 50% of the main hit; it deals 11% of that figure
 *     every 4 ticks inside its active window (16 ticks, 88% of the main hit
 *     over a full stay), plus Bleeding I.
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
public class BanshoFanItem extends Item implements com.dragonblockarcanedba.item.geo.DbaGeoWeapon {
    /** Storm cooldown: 120 ticks, longer than the cyclone's 100 tick life. */
    private static final long CYCLONE_COOLDOWN_TICKS = 120L;
    private static final java.util.Map<java.util.UUID, Long> CYCLONE_READY_AT = new java.util.concurrent.ConcurrentHashMap<>();

    public BanshoFanItem(Properties properties) {
        super(properties.attributes(createModifiers()));
    }

    /**
     * Raise the great cyclone at a point, if this player's storm cooldown has
     * lapsed and none of their cyclones is already standing nearby.
     * Returns true when the cyclone was cast.
     */
    private static boolean castCyclone(ServerLevel level, ServerPlayer player, Vec3 at, float cycloneDamage) {
        long now = level.getGameTime();
        Long readyAt = CYCLONE_READY_AT.get(player.getUUID());
        if (readyAt != null && now < readyAt) {
            return false;
        }
        boolean standing = !level.getEntitiesOfClass(BanshoCycloneEntity.class,
            new net.minecraft.world.phys.AABB(at, at).inflate(24.0),
            e -> e.isAlive() && e.getOwner() == player).isEmpty();
        if (standing) {
            return false;
        }
        BanshoCycloneEntity cyclone = new BanshoCycloneEntity(level, player, at, cycloneDamage);
        level.addFreshEntity(cyclone);
        CYCLONE_READY_AT.put(player.getUUID(), now + CYCLONE_COOLDOWN_TICKS);
        if (CYCLONE_READY_AT.size() > 64) {
            CYCLONE_READY_AT.entrySet().removeIf(e -> now >= e.getValue());
        }
        return true;
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
        // Gale Force: Launch the target with strong upward velocity & MC 26.2 catapult physics
        Vec3 diff = target.position().subtract(attacker.position()).normalize().scale(5.0);
        target.setDeltaMovement(target.getDeltaMovement().add(diff.x, 1.2, diff.z));
        com.dragonblockarcanedba.util.DbaCompat.markHurt(target); // Force velocity sync to client

        // The launch itself had no visual: add a gust burst at the launch moment.
        if (target.level() instanceof ServerLevel gustLevel) {
            gustLevel.sendParticles(com.dragonblockarcanedba.particle.DbaParticles.WIND,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                12, 0.5, 0.6, 0.5, 0.12);
        }

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
            // No stamina drain here: the left-click network handler already charges
            // the swing cost once per swing. Charging again on every landed hit made
            // a connecting swing cost double an air swing.
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();

            // Stat-scaled bonus damage: Strength × 1.5
            PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
            float strengthBonus = (float) (accessor.dba$getStrength() * 1.5);
            if (strengthBonus > 0) {
                target.hurtServer(serverLevel, com.dragonblockarcanedba.util.DbaDamage.tempest(serverPlayer), com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage((float) (strengthBonus)));
            }

            // Great Cyclone: a proper cast, not a proc. A landed hit raises the
            // storm at the target when the per-player storm cooldown is ready;
            // the cooldown (120 ticks) exceeds the cyclone's 100 tick life, and
            // a standing cyclone of yours blocks a second one, so storms never
            // stack at melee cadence.
            castCyclone(serverLevel, serverPlayer, target.position(), (400.0f + strengthBonus) * 0.5f);

            // Physical 3D impact shockwave on every Gale Force strike
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
                com.dragonblockarcanedba.sound.DbaSounds.SWING_FAN, SoundSource.PLAYERS, 1.2f, 1.2f);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.SLASH_WHOOSH, SoundSource.PLAYERS, 1.2f, 1.2f);
        }
    }

    /**
     * The fan's own swing. Opens a gale even when the fan misses.
     * A body already in melee reach is left to hurtEnemy.
     */
    public static void performGaleSwing(ServerPlayer player, ItemStack stack) {
        if (!WeaponDrainHelper.drainStaminaDiscrete(player, 45.0, 10)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        float damage = 400.0f + (float) (accessor.dba$getStrength() * 1.5);
        Vec3 look = player.getLookAngle();
        BanshoGaleEntity gale = new BanshoGaleEntity(level, player, damage);
        gale.setDeltaMovement(look.scale(1.35));
        level.addFreshEntity(gale);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    // --- Right Click: Tempest Barrage — Physical 3D launch shockwave + 5 custom wind projectiles ---
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        // Drain Stamina: 45% per minute for 40-tick (2-second) cooldown cycle (1.5% Max Stamina)
        if (!WeaponDrainHelper.drainStaminaDiscrete(player, 45.0, 40)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // Cast sounds play server side only, in one source category. They used to
            // play on both logical sides with mixed NEUTRAL and PLAYERS categories.
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.SWING_FAN, SoundSource.PLAYERS,
                1.2F, 0.9F);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.dragonblockarcanedba.sound.DbaSounds.WIND_GUST, SoundSource.PLAYERS,
                1.0F, 1.5F);
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
