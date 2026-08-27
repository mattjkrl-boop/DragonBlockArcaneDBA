package com.dragonblockarcanedba.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Yellow Cloud Block — Celestial Otherworld clouds.
 * Features:
 * - Pass-through: Zero physical collision, entities and players fly/walk freely through.
 * - Dynamic outline: No selection wireframe unless holding the cloud item (doesn't obstruct attacks or views).
 * - Fall cushioning: Cancels fall damage entirely and provides gentle buoyant gliding descent.
 * - Drifting wind: Imparts subtle wind motion in the drifting cloud direction.
 * - Seamless volumetric visuals: Subclasses HalfTransparentBlock to cull faces between adjacent cloud blocks.
 * - Ambient particles: Spawns drifting cloud puffs in the breeze.
 */
public class YellowCloudBlock extends HalfTransparentBlock {
    public static final MapCodec<YellowCloudBlock> CODEC = simpleCodec(YellowCloudBlock::new);

    public YellowCloudBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HalfTransparentBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter world, BlockPos pos, Entity entity) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    @Override
    public net.minecraft.world.item.Item asItem() {
        if (DbaBlocks.YELLOW_CLOUD_BLOCK_ITEM != null) {
            return DbaBlocks.YELLOW_CLOUD_BLOCK_ITEM;
        }
        net.minecraft.world.item.Item item = super.asItem();
        if (item == net.minecraft.world.item.Items.AIR) {
            net.minecraft.world.item.Item byBlock = net.minecraft.world.item.Item.byBlock(this);
            if (byBlock != net.minecraft.world.item.Items.AIR) {
                return byBlock;
            }
        }
        return item;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        net.minecraft.world.item.Item item = this.asItem();
        return (item != null && item != net.minecraft.world.item.Items.AIR && context.isHoldingItem(item)) ? Shapes.block() : Shapes.empty();
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.resetFallDistance();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean bl) {
        entity.resetFallDistance();
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y < -0.08) {
            // Cushion fall into a soft buoyant glide
            entity.setDeltaMovement(motion.x * 0.96, Math.max(motion.y * 0.72, -0.16), motion.z * 0.96);
        }
        // Subtle breeze drift: lower cloud layers have slightly stronger wind drift
        if (entity.isAlive() && !entity.isSpectator()) {
            double altitudeFactor = Math.max(0.4, Math.min(1.5, 1.45 - (pos.getY() - 90) * 0.007));
            entity.push(0.0035 * altitudeFactor, 0.0, 0.0);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            double px = pos.getX() + random.nextDouble();
            double py = pos.getY() + random.nextDouble() * 0.8 + 0.1;
            double pz = pos.getZ() + random.nextDouble();
            double speedFactor = Math.max(0.4, Math.min(1.5, 1.45 - (pos.getY() - 90) * 0.007));
            level.addParticle(
                ParticleTypes.CLOUD,
                px, py, pz,
                0.025 * speedFactor + random.nextDouble() * 0.015, 0.005, 0.0
            );
        }
    }
}
