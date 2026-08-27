package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.attribute.PlayerStats;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import net.minecraft.world.entity.player.Player;

/**
 * Standardized Ki and Stamina drain helper for weapons.
 * 
 * Mathematical standard:
 * 1 minute = 60 seconds = 1200 Minecraft ticks.
 * Rate per tick = (MaxStat * (percentPerMinute / 100.0)) / 1200.0;
 * Rate per second = (MaxStat * (percentPerMinute / 100.0)) / 60.0;
 */
public class WeaponDrainHelper {
    public static final double TICKS_PER_MINUTE = 1200.0;

    /**
     * Drains Ki smoothly for a single tick based on the weapon's % per minute.
     * @return true if player had enough Ki; false if Ki reached 0 / insufficient.
     */
    public static boolean drainKiPerTick(Player player, double percentPerMinute) {
        if (percentPerMinute <= 0) return true;
        if (!(player instanceof PlayerStatsAccessor accessor)) return true;

        accessor.dba$pauseKiRecovery(15);

        double maxKi = PlayerStats.getMaxKi(player);
        double drainAmount = (maxKi * (percentPerMinute / 100.0)) / TICKS_PER_MINUTE;
        double currentKi = accessor.dba$getCurrentKi();

        boolean success;
        if (currentKi >= drainAmount) {
            accessor.dba$addKi(-drainAmount);
            success = true;
        } else {
            accessor.dba$setCurrentKi(0.0);
            success = false;
        }

        if (player.tickCount % 3 == 0) {
            accessor.dba$syncStats();
        }
        return success;
    }

    /**
     * Drains Stamina smoothly for a single tick based on the weapon's % per minute.
     * @return true if player had enough Stamina; false if Stamina reached 0 / insufficient.
     */
    public static boolean drainStaminaPerTick(Player player, double percentPerMinute) {
        if (percentPerMinute <= 0) return true;
        if (!(player instanceof PlayerStatsAccessor accessor)) return true;

        accessor.dba$pauseStaminaRecovery(15);

        double maxStamina = PlayerStats.getMaxStamina(player);
        double drainAmount = (maxStamina * (percentPerMinute / 100.0)) / TICKS_PER_MINUTE;
        double currentStamina = accessor.dba$getCurrentStamina();

        boolean success;
        if (currentStamina >= drainAmount) {
            accessor.dba$addStamina(-drainAmount);
            success = true;
        } else {
            accessor.dba$setCurrentStamina(0.0);
            success = false;
        }

        if (player.tickCount % 3 == 0) {
            accessor.dba$syncStats();
        }
        return success;
    }

    /**
     * Drains both Ki and Stamina smoothly for a single tick based on their respective % per minute.
     * @return true if player had enough of both; false if either reached 0 / insufficient.
     */
    public static boolean drainBothPerTick(Player player, double kiPercentPerMinute, double staminaPercentPerMinute) {
        boolean kiOk = drainKiPerTick(player, kiPercentPerMinute);
        boolean stmOk = drainStaminaPerTick(player, staminaPercentPerMinute);
        return kiOk && stmOk;
    }

    /**
     * Drains Ki for a discrete action with a specific duration in ticks (e.g. cooldown / swing time).
     */
    public static boolean drainKiDiscrete(Player player, double percentPerMinute, int durationTicks) {
        if (percentPerMinute <= 0 || durationTicks <= 0) return true;
        if (!(player instanceof PlayerStatsAccessor accessor)) return true;

        accessor.dba$pauseKiRecovery(20);

        double maxKi = PlayerStats.getMaxKi(player);
        double drainAmount = (maxKi * (percentPerMinute / 100.0)) * (durationTicks / TICKS_PER_MINUTE);
        double currentKi = accessor.dba$getCurrentKi();

        boolean success;
        if (currentKi >= drainAmount) {
            accessor.dba$addKi(-drainAmount);
            success = true;
        } else {
            accessor.dba$setCurrentKi(0.0);
            success = false;
        }

        accessor.dba$syncStats();
        return success;
    }

    /**
     * Drains Stamina for a discrete action with a specific duration in ticks (e.g. cooldown / swing time).
     */
    public static boolean drainStaminaDiscrete(Player player, double percentPerMinute, int durationTicks) {
        if (percentPerMinute <= 0 || durationTicks <= 0) return true;
        if (!(player instanceof PlayerStatsAccessor accessor)) return true;

        accessor.dba$pauseStaminaRecovery(20);

        double maxStamina = PlayerStats.getMaxStamina(player);
        double drainAmount = (maxStamina * (percentPerMinute / 100.0)) * (durationTicks / TICKS_PER_MINUTE);
        double currentStamina = accessor.dba$getCurrentStamina();

        boolean success;
        if (currentStamina >= drainAmount) {
            accessor.dba$addStamina(-drainAmount);
            success = true;
        } else {
            accessor.dba$setCurrentStamina(0.0);
            success = false;
        }

        accessor.dba$syncStats();
        return success;
    }

    /**
     * Drains both Ki and Stamina for a discrete action with a specific duration in ticks.
     */
    public static boolean drainBothDiscrete(Player player, double kiPercentPerMinute, double staminaPercentPerMinute, int durationTicks) {
        if (durationTicks <= 0) return true;
        if (!(player instanceof PlayerStatsAccessor accessor)) return true;

        accessor.dba$pauseKiRecovery(20);
        accessor.dba$pauseStaminaRecovery(20);

        boolean kiSuccess = true;
        if (kiPercentPerMinute > 0) {
            double maxKi = PlayerStats.getMaxKi(player);
            double drainKi = (maxKi * (kiPercentPerMinute / 100.0)) * (durationTicks / TICKS_PER_MINUTE);
            double currentKi = accessor.dba$getCurrentKi();
            if (currentKi >= drainKi) {
                accessor.dba$addKi(-drainKi);
            } else {
                accessor.dba$setCurrentKi(0.0);
                kiSuccess = false;
            }
        }

        boolean stmSuccess = true;
        if (staminaPercentPerMinute > 0) {
            double maxStamina = PlayerStats.getMaxStamina(player);
            double drainStm = (maxStamina * (staminaPercentPerMinute / 100.0)) * (durationTicks / TICKS_PER_MINUTE);
            double currentStamina = accessor.dba$getCurrentStamina();
            if (currentStamina >= drainStm) {
                accessor.dba$addStamina(-drainStm);
            } else {
                accessor.dba$setCurrentStamina(0.0);
                stmSuccess = false;
            }
        }

        accessor.dba$syncStats();
        return kiSuccess && stmSuccess;
    }

    /**
     * Checks if player has at least a small threshold of Ki remaining.
     */
    public static boolean hasKi(Player player) {
        if (!(player instanceof PlayerStatsAccessor accessor)) return true;
        return accessor.dba$getCurrentKi() > 0.5;
    }

    /**
     * Checks if player has at least a small threshold of Stamina remaining.
     */
    public static boolean hasStamina(Player player) {
        if (!(player instanceof PlayerStatsAccessor accessor)) return true;
        return accessor.dba$getCurrentStamina() > 0.5;
    }

    private static final java.util.Map<java.util.UUID, java.util.Map<String, Long>> LAST_DRAIN_TICKS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Drains Ki smoothly for a single tick, but guarantees it only drains once per game tick per key
     * to prevent multi-target AOEs from multiplying the drain rate.
     */
    public static boolean drainKiPerTickOnce(Player player, double percentPerMinute, String key) {
        long gameTime = player.level().getGameTime();
        java.util.Map<String, Long> playerMap = LAST_DRAIN_TICKS.computeIfAbsent(player.getUUID(), u -> new java.util.concurrent.ConcurrentHashMap<>());
        Long lastTick = playerMap.get(key);
        if (lastTick != null && lastTick == gameTime) {
            return true;
        }
        playerMap.put(key, gameTime);
        return drainKiPerTick(player, percentPerMinute);
    }

    /**
     * Drains Stamina smoothly for a single tick, capped at once per game tick per key.
     */
    public static boolean drainStaminaPerTickOnce(Player player, double percentPerMinute, String key) {
        long gameTime = player.level().getGameTime();
        java.util.Map<String, Long> playerMap = LAST_DRAIN_TICKS.computeIfAbsent(player.getUUID(), u -> new java.util.concurrent.ConcurrentHashMap<>());
        Long lastTick = playerMap.get(key);
        if (lastTick != null && lastTick == gameTime) {
            return true;
        }
        playerMap.put(key, gameTime);
        return drainStaminaPerTick(player, percentPerMinute);
    }

    /**
     * Drains both Ki and Stamina smoothly for a single tick, capped at once per game tick per key.
     */
    public static boolean drainBothPerTickOnce(Player player, double kiPercent, double staminaPercent, String key) {
        long gameTime = player.level().getGameTime();
        java.util.Map<String, Long> playerMap = LAST_DRAIN_TICKS.computeIfAbsent(player.getUUID(), u -> new java.util.concurrent.ConcurrentHashMap<>());
        Long lastTick = playerMap.get(key);
        if (lastTick != null && lastTick == gameTime) {
            return true;
        }
        playerMap.put(key, gameTime);
        return drainBothPerTick(player, kiPercent, staminaPercent);
    }
}
