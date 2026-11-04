package com.dragonblockarcanedba.block.entity;

import com.dragonblockarcanedba.entity.LavaBlobEntity;
import com.dragonblockarcanedba.entity.LavaField;
import com.dragonblockarcanedba.particle.DbaParticles;
import com.dragonblockarcanedba.sound.DbaSounds;
import com.dragonblockarcanedba.util.DbaDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Drives one planted mini volcano (Lava Sword redesign, idea 6). All state
 * is saved with the chunk (owner, absolute expiry, next eruption, blob
 * damage), so a crash or a chunk unload pauses the volcano exactly where
 * it was and it resumes on load; LavaField's site entry mirrors the cell
 * so the site's record closes on schedule even if this entity is lost.
 *
 * Eruptions lob LavaBlobEntity arcs onto a ring around the cone (the
 * blobs place the cooling crust pools that zone the area) and push a
 * small heat pulse onto anything standing against the cone, credited to
 * the planter when they are in the level. At expiry the volcano goes
 * dormant instead of ending: eruptions and breathing stop, and the block
 * stands on as a permanent dormant volcano. LavaField's site entry
 * closes its record a little later; LavaField.endVolcano remains the
 * manual removal path.
 */
public class VolcanoBlockEntity extends BlockEntity {
    /** Eruption cadence: every 36 to 54 ticks, randomized per eruption. */
    private static final int ERUPT_MIN = 36;
    private static final int ERUPT_SPREAD = 19;

    private long ownerMost;
    private long ownerLeast;
    private long expireAt;
    private long nextEruptAt;
    private float blobDamage;
    private boolean initialized;

    public VolcanoBlockEntity(BlockPos pos, BlockState state) {
        super(DbaBlockEntities.VOLCANO_BLOCK_ENTITY, pos, state);
    }

    /** Called by LavaField right after the block is placed. */
    public void initialize(UUID owner, long expireAt, long firstEruptAt, float blobDamage) {
        this.ownerMost = owner.getMostSignificantBits();
        this.ownerLeast = owner.getLeastSignificantBits();
        this.expireAt = expireAt;
        this.nextEruptAt = firstEruptAt;
        this.blobDamage = blobDamage;
        this.initialized = true;
        this.setChanged();
    }

    public UUID ownerId() {
        return new UUID(ownerMost, ownerLeast);
    }

    public long expireAt() {
        return expireAt;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("OwnerMost", ownerMost);
        output.putLong("OwnerLeast", ownerLeast);
        output.putLong("ExpireAt", expireAt);
        output.putLong("NextEruptAt", nextEruptAt);
        output.putFloat("BlobDamage", blobDamage);
        output.putBoolean("Initialized", initialized);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ownerMost = input.getLongOr("OwnerMost", 0L);
        ownerLeast = input.getLongOr("OwnerLeast", 0L);
        expireAt = input.getLongOr("ExpireAt", 0L);
        nextEruptAt = input.getLongOr("NextEruptAt", 0L);
        blobDamage = input.getFloatOr("BlobDamage", 0.0f);
        initialized = input.getBooleanOr("Initialized", false);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcanoBlockEntity entity) {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!state.is(com.dragonblockarcanedba.block.DbaBlocks.MINI_VOLCANO)) {
            return;
        }
        if (!entity.initialized) {
            // Placed but never initialized (a half-finished plant). The
            // field's site entry is the backstop; end it through there.
            LavaField.endVolcano(server, pos);
            return;
        }
        long now = level.getGameTime();
        if (now >= entity.expireAt) {
            // Dormant: the volcano is done erupting but the block stands
            // on permanently. Nothing ticks, nothing breathes.
            return;
        }
        RandomSource rand = level.getRandom();
        Vec3 crater = new Vec3(pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5);
        if (now % 10 == 0) {
            // Idle breathing: the crater smokes and spits embers so the
            // construct reads as alive between eruptions.
            server.sendParticles(DbaParticles.EMBER, crater.x, crater.y, crater.z,
                2, 0.25, 0.10, 0.25, 0.03);
            server.sendParticles(DbaParticles.ASH, crater.x, crater.y + 0.3, crater.z,
                1, 0.3, 0.15, 0.3, 0.02);
        }
        if (now < entity.nextEruptAt) {
            return;
        }
        entity.nextEruptAt = now + ERUPT_MIN + rand.nextInt(ERUPT_SPREAD);
        entity.setChanged();
        entity.erupt(server, pos, crater, rand, now);
    }

    private void erupt(ServerLevel server, BlockPos pos, Vec3 crater, RandomSource rand, long now) {
        ServerPlayer owner = server.getServer().getPlayerList().getPlayer(ownerId());
        int blobs = 3 + rand.nextInt(3);
        for (int i = 0; i < blobs; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double dist = 3.5 + rand.nextDouble() * 4.0;
            double tx = pos.getX() + 0.5 + Math.cos(angle) * dist;
            double tz = pos.getZ() + 0.5 + Math.sin(angle) * dist;
            BlockPos ground = LavaField.findSurface(server, (int) Math.floor(tx), (int) Math.floor(tz), pos.getY() + 2);
            double ty = ground != null ? ground.getY() : pos.getY();
            int flight = 13 + rand.nextInt(7);
            LavaBlobEntity.launch(server, owner, crater, new Vec3(tx, ty, tz), flight, blobDamage);
        }
        // Heat pulse against the cone: standing next to an erupting
        // volcano is the zone it controls most tightly.
        if (owner != null) {
            AABB box = new AABB(pos.getX() - 1.5, pos.getY() - 0.5, pos.getZ() - 1.5,
                pos.getX() + 2.5, pos.getY() + 2.0, pos.getZ() + 2.5);
            List<LivingEntity> near = server.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner);
            for (LivingEntity living : near) {
                living.hurtServer(server, DbaDamage.lava(owner),
                    com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(blobDamage * 0.5f));
                living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), 40));
                Vec3 push = living.position().subtract(crater).multiply(1, 0, 1);
                if (push.lengthSqr() > 0.001) {
                    push = push.normalize().scale(0.45);
                    living.setDeltaMovement(living.getDeltaMovement().add(push.x, 0.25, push.z));
                }
            }
        }
        // One eruption in three, the cone also sheds a crust pool at its
        // base, so a long-lived volcano slowly paves its own field.
        if (rand.nextInt(3) == 0) {
            int dx = rand.nextInt(5) - 2;
            int dz = rand.nextInt(5) - 2;
            if (dx != 0 || dz != 0) {
                LavaField.crustColumn(server, pos.getX() + dx, pos.getZ() + dz, pos.getY() + 1,
                    LavaField.PROFILE_POOL, owner, blobDamage * 0.8f);
            }
        }
        server.playSound(null, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            DbaSounds.LAVA_BUBBLE, SoundSource.BLOCKS, 1.1f, 0.45f);
        server.playSound(null, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
            DbaSounds.RELEASE_BLAST, SoundSource.BLOCKS, 0.4f, 0.65f);
        server.sendParticles(DbaParticles.BLAST, crater.x, crater.y + 0.2, crater.z,
            3, 0.3, 0.2, 0.3, 0.05);
        server.sendParticles(DbaParticles.FLAME, crater.x, crater.y, crater.z,
            10, 0.35, 0.25, 0.35, 0.04);
    }
}
