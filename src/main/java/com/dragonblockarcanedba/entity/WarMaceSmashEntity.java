package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.fx.DbaImpactFx;
import com.dragonblockarcanedba.item.WarMaceItem;
import com.dragonblockarcanedba.util.DebrisSpawner;
import com.dragonblockarcanedba.util.GroundScar;
import com.dragonblockarcanedba.util.LayeredParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Overhead smash. A falling mace writes a wider, taller shell. The first two
 * ticks are a core blast around the mace; after that the hits travel with the
 * lip of the expanding shell, and every hit falls off with distance from the
 * center (full at the core, 35 percent at the rim).
 *
 * A heavy smash is cast mid fall, so the volume follows the falling owner
 * until the owner lands or the shell first connects; only then does it plant.
 * On impact the ground answers: a dust burst, a fracture disk scarred into
 * the surface through GroundScar (restored later), and a field of real ground
 * debris thrown up by DebrisSpawner.
 */
public class WarMaceSmashEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> HEAVY = SynchedEntityData.defineId(WarMaceSmashEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(WarMaceSmashEntity.class, EntityDataSerializers.FLOAT);

    /** Bronze theme color for dust, matching the renderer shell (0.85, 0.62, 0.28). */
    private static final int THEME_ARGB = 0xD99E47;
    /** Damage kept at the rim of the shell, as a fraction of the core damage. */
    private static final float RIM_KEEP = 0.35f;

    private final Set<Integer> hitIds = new HashSet<>();
    /** One connected smash launches the owner once, no matter how many mobs it caught. */
    private boolean relaunched;
    /** True while the volume still rides the falling owner. Plants on landing or first connect. */
    private boolean following = true;
    /** The ground burst, scar, and debris fire exactly once, at the impact moment. */
    private boolean impacted;

    public WarMaceSmashEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public WarMaceSmashEntity(Level level, LivingEntity owner, float damage, boolean heavy) {
        super(DbaEntities.WAR_MACE_SMASH, level);
        this.setOwner(owner);
        this.noPhysics = true;
        if (owner != null) {
            this.setPos(owner.getX(), owner.getY() + 0.05, owner.getZ());
            this.setYRot(owner.getYRot());
        }
        this.entityData.set(HEAVY, heavy);
        this.entityData.set(DAMAGE, damage);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HEAVY, false);
        builder.define(DAMAGE, 900.0f);
    }

    public boolean isHeavy() {
        return this.entityData.get(HEAVY);
    }

    public int life() {
        return this.isHeavy() ? 14 : 10;
    }

    public float maxRadius() {
        return this.isHeavy() ? 7.6f : 4.5f;
    }

    public float radiusAt(float age) {
        float max = this.maxRadius();
        float linear = Math.max(0.0f, Math.min(1.0f, age / this.life()));
        float eased = 1.0f - (1.0f - linear) * (1.0f - linear) * (1.0f - linear);
        return 0.7f + eased * (max - 0.7f);
    }

    /** Distance falloff: 1.0 at the center, easing to RIM_KEEP at the shell rim. */
    private float falloffAt(double dist) {
        double t = Math.max(0.0, Math.min(1.0, dist / this.maxRadius()));
        return (float) (1.0 - (1.0 - RIM_KEEP) * t);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > this.life()) {
            this.discard();
            return;
        }
        if (this.level().isClientSide() || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        Entity owner = this.getOwner();
        boolean heavy = this.isHeavy();

        // A heavy smash is cast mid fall. The volume rides the falling owner
        // down instead of freezing at the cast height, and plants the moment
        // the owner lands.
        boolean landedNow = false;
        if (this.following) {
            if (owner instanceof LivingEntity living && living.isAlive()) {
                this.setPos(living.getX(), living.getY() + 0.05, living.getZ());
                if (living.onGround()) {
                    this.following = false;
                    landedNow = true;
                }
            } else {
                this.following = false;
            }
        }

        // Impact moment: the ground smash lands on its first tick; the heavy
        // smash lands with the player, or on its first connect, or (if the
        // fall never ends inside its life) at its final tick.
        if (!this.impacted) {
            if (!heavy && this.tickCount >= 1) {
                this.doImpact(server, this.getX(), this.getY(), this.getZ(), true);
            } else if (heavy && landedNow) {
                this.doImpact(server, this.getX(), this.getY(), this.getZ(), true);
            } else if (heavy && this.tickCount >= this.life()) {
                int gy = server.getHeight(Heightmap.Types.MOTION_BLOCKING,
                    (int) Math.floor(this.getX()), (int) Math.floor(this.getZ()));
                if (this.getY() - gy <= 24.0) {
                    this.doImpact(server, this.getX(), gy, this.getZ(), true);
                } else {
                    this.doImpact(server, this.getX(), this.getY(), this.getZ(), false);
                }
            }
        }

        float radius = this.radiusAt(this.tickCount);
        float previous = this.radiusAt(this.tickCount - 1);
        AABB box = new AABB(
            this.getX() - radius, this.getY() - 0.4, this.getZ() - radius,
            this.getX() + radius, this.getY() + 2.4, this.getZ() + radius);
        List<LivingEntity> hits = server.getEntitiesOfClass(LivingEntity.class, box,
            e -> e.isAlive() && e != owner && !this.hitIds.contains(e.getId()));
        float damage = this.entityData.get(DAMAGE);
        boolean hadHits = !this.hitIds.isEmpty();
        for (LivingEntity living : hits) {
            double dx = living.getX() - this.getX();
            double dz = living.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            boolean inLip = dist <= radius + 0.6 && dist >= previous - 0.8;
            if (this.tickCount > 2 && !inLip) {
                continue;
            }
            this.hitIds.add(living.getId());
            var source = owner instanceof net.minecraft.world.entity.player.Player player
                ? server.damageSources().playerAttack(player)
                : server.damageSources().magic();
            living.hurtServer(server, source, com.dragonblockarcanedba.config.DbaEffectsConfig.capDamage(damage * this.falloffAt(dist)));
            // A small nudge only. The player is the one who goes up, and the mob has to
            // still be under the next falling smash.
            double nx = 0.0;
            double nz = 0.0;
            if (dist >= 0.15) {
                nx = dx / dist;
                nz = dz / dist;
            }
            living.setDeltaMovement(living.getDeltaMovement().add(nx * 0.25, 0.08, nz * 0.25));
            com.dragonblockarcanedba.util.DbaCompat.markHurt(living);
            if (!this.relaunched && owner instanceof ServerPlayer smasher) {
                this.relaunched = true;
                WarMaceItem.relaunchFromSmash(smasher);
            }
        }
        // First connect plants a still falling heavy smash and fires its
        // impact at the ground under the connect point.
        if (!hadHits && !this.hitIds.isEmpty()) {
            this.following = false;
            if (heavy && !this.impacted) {
                int gy = server.getHeight(Heightmap.Types.MOTION_BLOCKING,
                    (int) Math.floor(this.getX()), (int) Math.floor(this.getZ()));
                if (this.getY() - gy <= 24.0) {
                    this.doImpact(server, this.getX(), gy, this.getZ(), true);
                } else {
                    this.doImpact(server, this.getX(), this.getY(), this.getZ(), false);
                }
            }
        }

        // Layered particles for the shell's whole life: a low dust vortex in
        // three layers while it expands, then settling drizzle as it dies.
        Vec3 center = new Vec3(this.getX(), this.getY() + 0.1, this.getZ());
        LayeredParticles.emitVortex(server, center, radius, heavy ? 1.6f : 1.0f,
            this.tickCount, this.life(), THEME_ARGB, ParticleTypes.POOF);
        if (this.tickCount / (float) this.life() > 0.65f) {
            LayeredParticles.emitRain(server, center, radius, heavy ? 1.6f : 1.0f,
                this.tickCount, this.life(), LayeredParticles.dust(THEME_ARGB, 1.1f));
        }
    }

    /**
     * The impact beat: base dust burst always; when the smash truly reached
     * the ground, also a fracture scar disk and a debris field of the real
     * ground, plus (heavy only) a landing shake, because the cast time shake
     * fired up at the cast height where nobody felt the landing.
     */
    private void doImpact(ServerLevel server, double x, double y, double z, boolean grounded) {
        this.impacted = true;
        boolean heavy = this.isHeavy();
        LayeredParticles.emitGroundBurst(server, new Vec3(x, y, z), heavy ? 3.4f : 2.2f, THEME_ARGB);
        if (!grounded) {
            return;
        }
        if (heavy) {
            DbaImpactFx.shake(server, x, y, z, 2.4f, 14, 3);
        }
        this.scarFractureDisk(server, x, y, z, heavy ? 5 : 3, heavy ? 300 : 200);
        Entity owner = this.getOwner();
        DebrisSpawner.spawnField(server, owner instanceof LivingEntity living ? living : null,
            new Vec3(x, y + 0.25, z),
            heavy ? 36 : 24, heavy ? 5.5 : 3.4, heavy ? 3.2 : 2.2, heavy ? 34 : 26);
    }

    /**
     * Fractures the surface inside a disk: dirt family tears up into coarse
     * dirt, stone family crushes to gravel, brick and deepslate variants swap
     * to their cracked forms. GroundScar restores every cell afterwards and
     * refuses protected blocks. Columns scan a few blocks up and down so the
     * disk follows sloped ground instead of floating at one height.
     */
    private void scarFractureDisk(ServerLevel server, double x, double y, double z, int radius, int restoreTicks) {
        int cx = (int) Math.floor(x);
        int cz = (int) Math.floor(z);
        int baseY = (int) Math.floor(y);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) {
                    continue;
                }
                float chance = 0.9f - 0.45f * (float) (dist / radius);
                if (server.getRandom().nextFloat() > chance) {
                    continue;
                }
                for (int dy = 2; dy >= -6; dy--) {
                    m.set(cx + dx, baseY + dy, cz + dz);
                    BlockState state = server.getBlockState(m);
                    if (state.isAir() || state.getCollisionShape(server, m).isEmpty()) {
                        continue;
                    }
                    BlockState fracture = fractureFor(state);
                    if (fracture != null) {
                        GroundScar.scar(server, m, fracture, restoreTicks);
                    }
                    break;
                }
            }
        }
    }

    /** The crushed form of a surface block, or null when the block does not fracture. */
    private static BlockState fractureFor(BlockState state) {
        Block b = state.getBlock();
        if (b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.PODZOL || b == Blocks.MYCELIUM
            || b == Blocks.ROOTED_DIRT || b == Blocks.MUD || b == Blocks.DIRT_PATH || b == Blocks.FARMLAND) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (b == Blocks.STONE_BRICKS) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (b == Blocks.POLISHED_BLACKSTONE_BRICKS) {
            return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        }
        if (b == Blocks.DEEPSLATE_BRICKS) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (b == Blocks.DEEPSLATE_TILES) {
            return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
        }
        if (b == Blocks.DEEPSLATE) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (b == Blocks.STONE || b == Blocks.COBBLESTONE || b == Blocks.MOSSY_COBBLESTONE
            || b == Blocks.ANDESITE || b == Blocks.DIORITE || b == Blocks.GRANITE
            || b == Blocks.TUFF || b == Blocks.COBBLED_DEEPSLATE) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return null;
    }
}
