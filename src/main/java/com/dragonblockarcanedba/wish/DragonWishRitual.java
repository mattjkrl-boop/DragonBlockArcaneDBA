package com.dragonblockarcanedba.wish;

import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.network.WishMenuOpenPayload;
import com.dragonblockarcanedba.network.WishRitualStartPayload;
import com.dragonblockarcanedba.sound.DbaSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Seven dragon balls within five blocks leave the ground and take a floating
 * heptagon. Earth calls Shenron. Namek calls Porunga. The wish screen opens
 * only after that dragon has been in the world long enough to be seen.
 */
public final class DragonWishRitual {
    public static final int BALLS = 7;
    public static final int SEARCH_RADIUS = 5;
    /** How long the balls take to reach the ring. */
    public static final int FORMATION_TICKS = 56;
    /** Column, storm, and helices after the ring locks. */
    public static final int SUMMON_TICKS = 48;
    /** Dragon is visible before the existing wish screen opens. */
    public static final int REVEAL_TICKS = 24;
    public static final int DRAGON_AT = FORMATION_TICKS + SUMMON_TICKS;
    public static final int SCREEN_AT = DRAGON_AT + REVEAL_TICKS;
    /** Horizontal distance from the cluster center to each ball on the ring. */
    public static final double RING_RADIUS = 3.6;
    /** How far above the cluster the ring floats. */
    public static final double RING_LIFT = 6.0;

    private static final List<Ritual> ACTIVE = new ArrayList<>();
    private static boolean starting;

    private DragonWishRitual() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DragonWishRitual::tick);
    }

    /**
     * Starts a ritual when seven of {@code ball} sit within the usual search.
     * The blocks are consumed immediately; clients render them traveling.
     */
    public static void tryBegin(Level level, BlockPos origin, Block ball, boolean namek) {
        if (starting || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        starting = true;
        try {
            List<BlockPos> found = new ArrayList<>();
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (level.getBlockState(candidate).is(ball)) {
                            found.add(candidate);
                        }
                    }
                }
            }
            if (found.size() < BALLS) {
                return;
            }
            found.sort((left, right) -> Double.compare(left.distSqr(origin), right.distSqr(origin)));
            BlockPos[] chosen = new BlockPos[BALLS];
            for (int i = 0; i < BALLS; i++) {
                chosen[i] = found.get(i);
            }
            Vec3 center = centerOf(chosen);
            if (blocked(serverLevel, center)) {
                return;
            }
            for (BlockPos consumed : chosen) {
                serverLevel.destroyBlock(consumed, false);
            }
            int minY = chosen[0].getY();
            for (BlockPos consumed : chosen) {
                if (consumed.getY() < minY) {
                    minY = consumed.getY();
                }
            }
            BlockPos spawn = new BlockPos(Mth.floor(center.x), minY, Mth.floor(center.z));
            long start = serverLevel.getGameTime();
            ACTIVE.add(new Ritual(serverLevel, center, spawn, namek, start));
            WishRitualStartPayload payload = new WishRitualStartPayload(namek, start, chosen);
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(center) <= 192.0 * 192.0) {
                    ServerPlayNetworking.send(player, payload);
                }
            }
            serverLevel.playSound(null, center.x, center.y, center.z,
                    DbaSounds.CHARGE_RISE, SoundSource.HOSTILE, 1.6f, 0.55f);
        } finally {
            starting = false;
        }
    }

    public static Vec3 centerOf(BlockPos[] balls) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (BlockPos ball : balls) {
            x += ball.getX() + 0.5;
            y += ball.getY() + 0.5;
            z += ball.getZ() + 0.5;
        }
        double count = balls.length;
        return new Vec3(x / count, y / count, z / count);
    }

    /** Angle of ring seat {@code slot}, with seat 0 due north. */
    public static double slotAngle(int slot) {
        return slot * (Math.PI * 2.0 / BALLS) - Math.PI / 2.0;
    }

    public static Vec3 ringPoint(Vec3 center, int slot) {
        double angle = slotAngle(slot);
        return new Vec3(
                center.x + Math.cos(angle) * RING_RADIUS,
                center.y + RING_LIFT,
                center.z + Math.sin(angle) * RING_RADIUS
        );
    }

    /**
     * Matches each ball to a ring seat by compass angle so the paths do not cross.
     */
    public static int[] assignSlots(BlockPos[] balls, Vec3 center) {
        int count = balls.length;
        Integer[] ballOrder = new Integer[count];
        Integer[] slotOrder = new Integer[count];
        for (int i = 0; i < count; i++) {
            ballOrder[i] = i;
            slotOrder[i] = i;
        }
        Arrays.sort(ballOrder, (a, b) -> Double.compare(angleOf(balls[a], center), angleOf(balls[b], center)));
        Arrays.sort(slotOrder, (a, b) -> Double.compare(slotAngle(a), slotAngle(b)));
        int[] slots = new int[count];
        for (int rank = 0; rank < count; rank++) {
            slots[ballOrder[rank]] = slotOrder[rank];
        }
        return slots;
    }

    private static double angleOf(BlockPos ball, Vec3 center) {
        return Math.atan2(ball.getZ() + 0.5 - center.z, ball.getX() + 0.5 - center.x);
    }

    private static boolean blocked(ServerLevel level, Vec3 center) {
        for (Ritual ritual : ACTIVE) {
            if (ritual.level == level && ritual.center.distanceToSqr(center) < 16.0 * 16.0) {
                return true;
            }
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Ritual> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Ritual ritual = it.next();
            if (ritual.level.getServer() != server) {
                it.remove();
                continue;
            }
            long age = ritual.level.getGameTime() - ritual.startGameTime;
            if (!ritual.stormed && age >= FORMATION_TICKS) {
                ritual.stormed = true;
                ritual.level.getServer().setWeatherParameters(0, 12000, true, true);
                ritual.level.playSound(null, ritual.center.x, ritual.center.y, ritual.center.z,
                        DbaSounds.TELEPORT_WHOOSH, SoundSource.HOSTILE, 1.2f, 0.55f);
            }
            if (!ritual.appeared && age >= DRAGON_AT) {
                ritual.appeared = true;
                ritual.dragonId = appear(ritual);
            }
            if (age >= SCREEN_AT) {
                if (ritual.dragonId >= 0) {
                    openWishScreen(ritual);
                }
                it.remove();
            }
        }
    }

    private static int appear(Ritual ritual) {
        ServerLevel level = ritual.level;
        BlockPos pos = ritual.spawnPos;
        LivingEntity dragon;
        if (ritual.namek) {
            dragon = DbaEntities.PORUNGA.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
        } else {
            dragon = DbaEntities.SHENRON.create(level, null, pos, EntitySpawnReason.EVENT, false, false);
        }
        if (dragon == null) {
            return -1;
        }
        Player closest = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64.0, false);
        if (closest != null) {
            double dX = closest.getX() - dragon.getX();
            double dZ = closest.getZ() - dragon.getZ();
            float yaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0F;
            dragon.setYRot(yaw);
            dragon.yRotO = yaw;
            dragon.yBodyRot = yaw;
            dragon.yHeadRot = yaw;
        }
        level.addFreshEntity(dragon);

        LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
        if (lightning != null) {
            lightning.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            level.addFreshEntity(lightning);
        }
        float pitch = ritual.namek ? 0.5f : 0.8f;
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                DbaSounds.DRAGON_GROWL, SoundSource.HOSTILE, 2.5f, pitch);
        String line = ritual.namek
                ? "\u00a76Porunga: \u00a7eI am the Dragon of Dreams. Speak your 3 wishes!"
                : "\u00a76Shenron: \u00a7eI am the Eternal Dragon Shenron. Speak your wish!";
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(line), false);
        return dragon.getId();
    }

    private static void openWishScreen(Ritual ritual) {
        ServerPlayer nearest = null;
        double nearestSqr = 192.0 * 192.0;
        for (ServerPlayer player : ritual.level.players()) {
            double distance = player.distanceToSqr(ritual.center);
            if (distance < nearestSqr) {
                nearestSqr = distance;
                nearest = player;
            }
        }
        if (nearest != null) {
            ServerPlayNetworking.send(nearest, new WishMenuOpenPayload(ritual.dragonId));
        }
    }

    private static final class Ritual {
        final ServerLevel level;
        final Vec3 center;
        final BlockPos spawnPos;
        final boolean namek;
        final long startGameTime;
        boolean stormed;
        boolean appeared;
        int dragonId = -1;

        Ritual(ServerLevel level, Vec3 center, BlockPos spawnPos, boolean namek, long startGameTime) {
            this.level = level;
            this.center = center;
            this.spawnPos = spawnPos;
            this.namek = namek;
            this.startGameTime = startGameTime;
        }
    }
}
