package com.dragonblockarcanedba.block.entity;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.inventory.GravityTrainingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class GravityTrainingBlockEntity extends BlockEntity implements MenuProvider {

    private int gravity = 0; // 0 to 1000
    private int fuel = 0; // in ticks

    public final SimpleContainer inventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            GravityTrainingBlockEntity.this.setChanged();
        }
    };

    public GravityTrainingBlockEntity(BlockPos pos, BlockState state) {
        super(DbaBlockEntities.GRAVITY_TRAINING_BLOCK_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GravityTrainingBlockEntity entity) {
        if (level == null || level.isClientSide()) return;

        // Check if block is still valid. If not, drop contents to prevent ghost items.
        if (!state.is(com.dragonblockarcanedba.block.DbaBlocks.GRAVITY_TRAINING_BLOCK) && !entity.inventory.isEmpty()) {
            Containers.dropContents(level, pos, entity.inventory);
            entity.inventory.clearContent();
        }

        // Consume fuel from slot if we are out of fuel
        if (entity.fuel <= 0 && entity.gravity > 0) {
            ItemStack fuelStack = entity.inventory.getItem(0);
            if (!fuelStack.isEmpty()) {
                // Check if it's coal or lava.
                // Normally we'd use FuelRegistry, but we'll hardcode coal (1600) and lava (20000) for simplicity
                int fuelValue = 0;
                if (fuelStack.is(net.minecraft.world.item.Items.COAL) || fuelStack.is(net.minecraft.world.item.Items.CHARCOAL)) {
                    fuelValue = 1600;
                } else if (fuelStack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
                    fuelValue = 20000;
                    entity.inventory.setItem(0, new ItemStack(net.minecraft.world.item.Items.BUCKET)); // return bucket
                } else if (fuelStack.is(net.minecraft.world.item.Items.COAL_BLOCK)) {
                    fuelValue = 16000;
                }

                if (fuelValue > 0) {
                    entity.fuel = fuelValue;
                    if (!fuelStack.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
                        fuelStack.shrink(1);
                    }
                    entity.setChanged();
                }
            }
        }

        if (entity.fuel > 0 && entity.gravity > 0) {
            // Drain fuel based on gravity. 1 fuel tick per tick at gravity 10.
            // So at 1000 gravity, it consumes 100 fuel per tick.
            int consumption = Math.max(1, entity.gravity / 10);
            entity.fuel = Math.max(0, entity.fuel - consumption);
            entity.setChanged();

            // Find players in 10x10x10 radius (5 blocks each direction)
            AABB area = new AABB(pos).inflate(5);
            List<Player> players = level.getEntitiesOfClass(Player.class, area);

            for (Player player : players) {
                if (player.isSpectator()) continue;

                // Dynamic Downward Pull based on Gravity (0 to 0.12 force)
                double pull = (entity.gravity / 1000.0) * 0.12; 
                player.setDeltaMovement(player.getDeltaMovement().add(0, -pull, 0));
                player.hurtMarked = true; // Tell client to update motion

                // Dynamic Slowness Effect: Amplifier scales with gravity (0 to 6)
                int slownessAmp = Math.min(6, entity.gravity / 150);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SLOWNESS,
                    25,
                    slownessAmp,
                    false,
                    false
                ));

                // Dynamic Damage and XP every 20 ticks (1 second)
                if (level.getGameTime() % 20 == 0) {
                    // Dynamic scaling damage based on gravity (Up to 20 damage at 1000G)
                    if (!player.isCreative()) {
                        float damage = (entity.gravity / 1000.0f) * 20.0f;
                        if (damage > 0.1f) {
                            player.invulnerableTime = 0;
                            player.hurt(level.damageSources().generic(), damage);
                        }
                    }

                    // Dynamic scaling DBA XP based on gravity (Up to 250 XP/sec at 1000G)
                    int xp = Math.max(1, (int)(entity.gravity * 0.25f));
                    if (xp > 0 && player instanceof PlayerStatsAccessor accessor) {
                        accessor.dba$addXp(xp);
                    }
                }
            }
        }
    }

    public int getGravity() {
        return gravity;
    }

    public void setGravity(int gravity) {
        this.gravity = Math.max(0, Math.min(1000, gravity));
        this.setChanged();
    }

    public int getFuel() {
        return fuel;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Gravity", gravity);
        output.putInt("Fuel", fuel);
        
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        items.add(inventory.getItem(0));
        output.store("Items", ItemStack.CODEC.listOf(), items);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        gravity = input.getIntOr("Gravity", 0);
        fuel = input.getIntOr("Fuel", 0);
        
        input.read("Items", ItemStack.CODEC.listOf()).ifPresent(items -> {
            if (!items.isEmpty()) inventory.setItem(0, items.get(0));
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.dragonblockarcanedba.gravity_training_block");
    }

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> gravity;
                case 1 -> fuel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> setGravity(value);
                case 1 -> fuel = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new GravityTrainingMenu(syncId, playerInventory, this.inventory, this.data);
    }
}
