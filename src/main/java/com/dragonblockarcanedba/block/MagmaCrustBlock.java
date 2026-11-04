package com.dragonblockarcanedba.block;

import com.dragonblockarcanedba.entity.LavaField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cooling lava crust, the Lava Sword redesign's ground material. One block,
 * three heat stages driven by LavaField's schedule:
 * stage 0 molten (bright, no collision, burns hard), stage 1 cooling
 * (dimmer, no collision, burns lightly), stage 2 dark crust (a low solid
 * slab an entity can stand on, no burn). The field owns every placed cell:
 * it advances the stages, then restores the cell's original block, and its
 * SavedData record survives a crash, so a crust cell is never permanent.
 *
 * The block itself holds no schedule; it only reports contact. Standing in
 * a hot cell (entityInside) and standing on the cooled slab (stepOn) both
 * route to LavaField.onCrustTouch, which looks up the field entry for the
 * cell and applies the stage's burn through the lava damage family.
 */
public class MagmaCrustBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    /** A raised puddle: 6/16 high, matching the model in every stage. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public MagmaCrustBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Molten and cooling crust is waded through (entityInside does the
        // burning). Only the dark crust has cooled enough to stand on.
        return state.getValue(STAGE) >= 2 ? SHAPE : Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean past) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living) {
            LavaField.onCrustTouch(server, pos, living, state.getValue(STAGE));
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // Standing on top of a hot cell: the puddle's rim is under the
        // entity's feet even when its body clears the cell. Stage 2 is
        // cold rock and does nothing, handled inside onCrustTouch.
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living) {
            LavaField.onCrustTouch(server, pos, living, state.getValue(STAGE));
        }
        super.stepOn(level, pos, state, entity);
    }
}
