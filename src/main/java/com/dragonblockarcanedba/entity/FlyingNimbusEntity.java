package com.dragonblockarcanedba.entity;

import com.dragonblockarcanedba.item.DbaItems;
import com.dragonblockarcanedba.util.DbaLivingEntityInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FlyingNimbusEntity extends Mob {
    private boolean droppedItem = false;

    public FlyingNimbusEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        // No default goals needed — completely passenger-controlled
    }

    @Override
    public boolean isNoAi() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 1;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity ? (LivingEntity) passenger : null;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide()) {
            player.startRiding(this);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.WOOL_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.4f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount) {
        if (!this.isRemoved() && !this.droppedItem) {
            this.droppedItem = true;
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.WOOL_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);
            this.discard();
            if (source.getEntity() instanceof Player player && !player.getAbilities().instabuild) {
                this.spawnAtLocation(level, DbaItems.FLYING_NIMBUS);
            }
            return true;
        }
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!this.isAlive()) {
            return;
        }

        if (this.isVehicle() && this.getControllingPassenger() instanceof LivingEntity passenger) {
            // Match the passenger's rotation
            this.setYRot(passenger.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(passenger.getXRot() * 0.5f);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();

            // Disable gravity while ridden so it hovers
            this.setNoGravity(true);

            double speed = 0.6; // Flight speed
            double verticalSpeed = 0.0;

            DbaLivingEntityInput input = (DbaLivingEntityInput) passenger;
            float strafe = input.dba$getXxa(); // A/D
            float forward = input.dba$getZza(); // W/S
            boolean jumping = input.dba$isJumping(); // Space

            // Space to fly up
            if (jumping) {
                verticalSpeed = 0.35;
            } else {
                // Descend when looking down and pressing forward
                Vec3 look = passenger.getLookAngle();
                if (forward > 0 && look.y < -0.2) {
                    verticalSpeed = look.y * speed;
                } else {
                    verticalSpeed = 0.0;
                }
            }

            // WASD horizontal movement math
            double dx = 0;
            double dz = 0;
            Vec3 look = passenger.getLookAngle();
            double horizontalLength = Math.sqrt(look.x * look.x + look.z * look.z);
            if (horizontalLength > 0) {
                double fx = look.x / horizontalLength;
                double fz = look.z / horizontalLength;
                
                dx = (fx * forward + fz * strafe) * speed;
                dz = (fz * forward - fx * strafe) * speed;
            }

            this.setDeltaMovement(new Vec3(dx, verticalSpeed, dz));
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            
            // Apply slight inertia/deceleration
            this.setDeltaMovement(this.getDeltaMovement().scale(0.85));
        } else {
            // Apply normal gravity if not ridden
            this.setNoGravity(false);
            super.travel(travelVector);
        }
    }
}
