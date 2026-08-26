package com.dragonblockarcanedba.tail;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.network.DbaNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;

import java.util.Set;

/**
 * Manages tailed race logic, health-scaled tail detachment calculations,
 * servering events, regrowing, and extensible hooks.
 */
public class TailHelper {

    private static final Set<String> TAILED_RACES = Set.of(
            "saiyan",
            "half_saiyan",
            "arcosian",
            "bio_android"
    );

    /**
     * Checks whether the given race identifier has a physical tail.
     */
    public static boolean isTailedRace(Identifier raceId) {
        if (raceId == null) {
            return false;
        }
        String path = raceId.getPath().toLowerCase();
        return TAILED_RACES.contains(path);
    }

    /**
     * Calculates the probability of a tail being severed upon receiving damage.
     * - Health >= 75%: 0.0 (0% chance)
     * - Health < 75%: Rapidly rising power curve up to a maximum cap of 75% near death.
     * Strictly never reaches 1.0 (100%).
     */
    public static float calculateDetachmentChance(float health, float maxHealth) {
        if (maxHealth <= 0.0f) {
            return 0.0f;
        }

        float healthRatio = Math.max(0.0f, Math.min(1.0f, health / maxHealth));

        // Safe threshold: tails never fall off when healthy (> 75% max health)
        if (healthRatio >= 0.75f) {
            return 0.0f;
        }

        // x goes from 0.0 (at 75% health) to 1.0 (at 0% health)
        float x = (0.75f - healthRatio) / 0.75f;

        // Rapid non-linear rise with a strict maximum cap of 75% (0.75f)
        float maxCeiling = 0.75f;
        float chance = maxCeiling * (float) Math.pow(x, 2.2);

        return Math.min(maxCeiling, chance);
    }

    /**
     * Attempts to sever the player's tail when struck in combat.
     *
     * @return true if the tail was severed, false otherwise.
     */
    public static boolean trySeverTailOnHit(ServerPlayer player, DamageSource source, float incomingDamage) {
        if (!(player instanceof PlayerStatsAccessor accessor)) {
            return false;
        }

        if (!accessor.dba$hasTail()) {
            return false;
        }

        float postDamageHealth = Math.max(0.0f, player.getHealth() - incomingDamage);
        float chance = calculateDetachmentChance(postDamageHealth, player.getMaxHealth());

        if (chance <= 0.0f) {
            return false;
        }

        if (player.getRandom().nextFloat() < chance) {
            return severTail(player, source);
        }

        return false;
    }

    /**
     * Forces severing of a player's tail with audio-visual FX and state synchronization.
     */
    public static boolean severTail(ServerPlayer player, DamageSource source) {
        if (!(player instanceof PlayerStatsAccessor accessor)) {
            return false;
        }

        if (!accessor.dba$hasTail()) {
            return false;
        }

        accessor.dba$setTailSevered(true);
        accessor.dba$syncStats();
        DbaNetwork.broadcastTransformState(player);

        ServerLevel level = (ServerLevel) player.level();
        double x = player.getX();
        double y = player.getY() + 0.6;
        double z = player.getZ();

        // Visceral particle burst at lower back (spine / tail origin)
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 25, 0.25, 0.3, 0.25, 0.2);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 10, 0.2, 0.2, 0.2, 0.1);
        level.sendParticles(ParticleTypes.SMOKE, x, y, z, 8, 0.15, 0.15, 0.15, 0.05);

        // Visceral audio: snap / break sound + crit slice impact
        level.playSound(null, x, y, z, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.3F, 0.75F);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1F, 0.65F);

        // Notify the player
        player.sendSystemMessage(Component.literal("§c§l[!] Your tail was severed in combat!").withStyle(ChatFormatting.RED));

        // Hook for future tail loss penalties / transformations / debuffs
        onTailSevered(player, source);

        return true;
    }

    /**
     * Regrows a player's severed tail.
     */
    public static boolean regrowTail(ServerPlayer player) {
        if (!(player instanceof PlayerStatsAccessor accessor)) {
            return false;
        }

        Identifier raceId = accessor.dba$getRaceId();
        if (!isTailedRace(raceId)) {
            return false;
        }

        if (!accessor.dba$isTailSevered()) {
            return false;
        }

        accessor.dba$setTailSevered(false);
        accessor.dba$syncStats();
        DbaNetwork.broadcastTransformState(player);

        ServerLevel level = (ServerLevel) player.level();
        double x = player.getX();
        double y = player.getY() + 0.6;
        double z = player.getZ();

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 15, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);

        player.sendSystemMessage(Component.literal("§a§l[!] Your tail has regrown!").withStyle(ChatFormatting.GREEN));
        return true;
    }

    /**
     * Extensible hook called when a tail is severed.
     * Future mechanics (such as preventing Great Ape / Oozaru transformation,
     * stamina loss, or balance stun) can be implemented here.
     */
    public static void onTailSevered(ServerPlayer player, DamageSource source) {
        // Reserved for future mechanics
    }
}
