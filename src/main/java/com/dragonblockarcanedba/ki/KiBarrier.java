package com.dragonblockarcanedba.ki;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.config.DbaEffectsConfig;
import com.dragonblockarcanedba.entity.KiBarrierEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raised Ki Barrier. Selecting the technique toggles a shell that spends the
 * same ki pool as blasts: {@code currentKi * usedPercent / 100}, drained across
 * {@link #HOLD_TICKS} instead of all at once. Hits cost extra ki and are capped
 * with {@link DbaEffectsConfig#capDamage}. The shell drops when that budget or
 * the live pool runs out. Firing the technique again lowers it.
 */
public final class KiBarrier {
    /** Six seconds. The committed ki is spread across this window. */
    public static final int HOLD_TICKS = 120;

    public static final EntityType<KiBarrierEntity> TYPE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DragonBlockArcaneDBA.id("ki_barrier"),
            EntityType.Builder.<KiBarrierEntity>of(KiBarrierEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, DragonBlockArcaneDBA.id("ki_barrier")))
    );

    private static final Map<UUID, KiBarrierEntity> ACTIVE = new ConcurrentHashMap<>();

    /**
     * Set by the combo-damage mixin only while it reapplies a stored player hit.
     * The preview call is cancelled, so the barrier must price that hit once, here.
     */
    public static boolean deliveringDeferredDamage;

    private KiBarrier() {}

    /** Forces entity-type registration during mod init, before the registry freezes. */
    public static void register() {
        DragonBlockArcaneDBA.LOGGER.info("Registered Ki Barrier ({})", TYPE);
    }

    /**
     * Toggles the barrier for this technique. The first press raises it. A second
     * press drops it. Nothing is spent up front; {@link #serverTick} drains ki.
     */
    public static void start(ServerPlayer player, KiTechnique technique) {
        if (player == null || technique == null || technique.isEmpty || technique.type != KiTechniqueType.BARRIER) {
            return;
        }
        KiBarrierEntity existing = ACTIVE.get(player.getUUID());
        if (existing != null && !existing.isRemoved()) {
            drop(player, true);
            return;
        }
        ACTIVE.remove(player.getUUID());

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        int percent = technique.usedPercent;
        if (percent < 1) {
            percent = 1;
        }
        if (percent > 100) {
            percent = 100;
        }
        double budget = accessor.dba$getCurrentKi() * (percent / 100.0);
        if (budget < 0.5) {
            player.sendSystemMessage(Component.literal("§cNot enough Ki!"), true);
            return;
        }

        int color = technique.color == 0 ? 0xFF55DDFF : technique.color;
        ServerLevel level = (ServerLevel) player.level();
        KiBarrierEntity shell = new KiBarrierEntity(level, player, color, percent);
        shell.setBudget(budget);
        level.addFreshEntity(shell);
        ACTIVE.put(player.getUUID(), shell);
        accessor.dba$pauseKiRecovery(20);
        accessor.dba$syncStats();

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.7f);
        player.sendSystemMessage(Component.translatable("message.dragonblockarcanedba.ki_barrier_up", Integer.toString(percent)), true);

        float density = DbaEffectsConfig.effectDensity();
        if (density > 0f) {
            int count = Math.max(4, Math.round(10f * density));
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color & 0xFFFFFF, 1.3f),
                    player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                    count, 0.55, 0.75, 0.55, 0.01);
        }
    }

    /**
     * Prices one hit against the raised shell.
     * Returns the damage that still has to land. A miss, a dodge that already
     * returned, or an empty shell leaves {@code amount} unchanged.
     * Void, starvation, drowning, and damage that bypasses invulnerability are not absorbed.
     */
    public static float absorb(Player player, ServerLevel level, DamageSource source, float amount) {
        if (!(player instanceof ServerPlayer serverPlayer) || amount <= 0f || source == null || level == null) {
            return amount;
        }
        if (player.isSpectator() || ignores(source)) {
            return amount;
        }
        KiBarrierEntity shell = ACTIVE.get(player.getUUID());
        if (shell == null || shell.isRemoved() || shell.level() != player.level()) {
            if (shell != null) {
                drop(serverPlayer, false);
            }
            return amount;
        }

        int percent = shell.getUsedPercent();
        if (percent < 1) {
            percent = 1;
        }
        if (percent > 100) {
            percent = 100;
        }
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) serverPlayer;
        double kiPerDamage = 100.0 / percent;
        double ki = accessor.dba$getCurrentKi();
        double spendable = Math.min(ki, Math.max(0.0, shell.getBudgetRemaining()));
        if (spendable <= 0.05) {
            drop(serverPlayer, true);
            return amount;
        }

        float capped = DbaEffectsConfig.capDamage(amount);
        float want = Math.min(amount, capped);
        float absorbed = (float) Math.min(want, spendable / kiPerDamage);
        if (absorbed > 0f) {
            double cost = absorbed * kiPerDamage;
            accessor.dba$addKi(-cost);
            shell.spendBudget(cost);
            accessor.dba$pauseKiRecovery(20);
            if (shell.claimAbsorbSound(level.getGameTime())) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.45f, 1.8f);
            }
            float density = DbaEffectsConfig.effectDensity();
            if (density > 0f) {
                int count = Math.max(1, Math.round(4f * density));
                int dust = shell.getColor() & 0xFFFFFF;
                level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(dust, 1.1f),
                        player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                        count, 0.45, 0.6, 0.45, 0.02);
            }
        }

        if (accessor.dba$getCurrentKi() <= 0.05 || shell.getBudgetRemaining() <= 0.05) {
            drop(serverPlayer, true);
        } else {
            accessor.dba$syncStats();
        }
        return amount - absorbed;
    }

    /**
     * Drains the committed ki. Called from the shell each server tick.
     * The slice stays {@code budget / HOLD_TICKS} so regen cannot stretch it,
     * and recovery stays paused so the pool cannot refill under the shell.
     */
    public static void serverTick(KiBarrierEntity shell, ServerPlayer player) {
        if (shell == null || player == null || shell.isRemoved()) {
            return;
        }
        if (ACTIVE.get(player.getUUID()) != shell) {
            forget(shell);
            return;
        }
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        double perTick = shell.getBudgetTotal() / (double) HOLD_TICKS;
        if (perTick <= 0.0 || shell.getBudgetRemaining() <= 0.05) {
            drop(player, true);
            return;
        }
        double pay = Math.min(perTick, Math.min(shell.getBudgetRemaining(), accessor.dba$getCurrentKi()));
        accessor.dba$addKi(-pay);
        shell.spendBudget(pay);
        accessor.dba$pauseKiRecovery(10);
        if (accessor.dba$getCurrentKi() <= 0.05 || shell.getBudgetRemaining() <= 0.05) {
            drop(player, true);
            return;
        }
        if (shell.tickCount % 10 == 0) {
            accessor.dba$syncStats();
        }
    }

    /** Drops a shell that no longer has a living caster, without a chat line. */
    public static void forget(KiBarrierEntity shell) {
        if (shell == null) {
            return;
        }
        ACTIVE.entrySet().removeIf(entry -> entry.getValue() == shell);
        if (!shell.isRemoved()) {
            shell.discard();
        }
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        KiBarrierEntity shell = ACTIVE.remove(playerUuid);
        if (shell != null && !shell.isRemoved()) {
            shell.discard();
        }
    }

    private static void drop(ServerPlayer player, boolean tell) {
        KiBarrierEntity shell = ACTIVE.remove(player.getUUID());
        if (shell != null && !shell.isRemoved()) {
            shell.discard();
        }
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        accessor.dba$syncStats();
        if (!tell) {
            return;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 1.2f);
        player.sendSystemMessage(Component.translatable("message.dragonblockarcanedba.ki_barrier_down"), true);
    }

    private static boolean ignores(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.DROWN);
    }
}
