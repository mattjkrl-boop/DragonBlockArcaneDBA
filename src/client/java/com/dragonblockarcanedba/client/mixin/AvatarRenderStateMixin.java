package com.dragonblockarcanedba.client.mixin;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.client.render.layer.DbaPlayerState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements DbaPlayerState {

    @Unique
    private boolean dba$hasTail = false;
    
    @Unique
    private float dba$tailAgeInTicks = 0;

    @Unique
    private final double[][] dba$tailPositions = new double[64][3];
    
    @Unique
    private int dba$tailRingBufferIndex = 0;

    @Unique
    private Identifier dba$raceId = null;

    @Unique
    private Identifier dba$activeFormId = null;

    @Unique
    private int dba$skinColor = 0xFF8CC8FF;

    @Unique
    private int dba$hairColor = 0xFF1EB4FF;

    @Unique
    private String dba$activeEmote = "";

    @Unique
    private boolean dba$isInOtherworld = false;

    @Unique
    private boolean dba$isSprinting = false;

    @Unique
    private boolean dba$isCrouching = false;

    @Unique
    private boolean dba$isSwimming = false;

    @Unique
    private boolean dba$isFlying = false;

    @Unique
    private float dba$horizontalSpeed = 0.0F;

    @Unique
    private float dba$yawVelocity = 0.0F;

    @Unique
    private float dba$bodyYawVelocity = 0.0F;

    @Unique
    private float dba$localVelocityX = 0.0F;

    @Unique
    private float dba$localVelocityZ = 0.0F;

    @Unique
    private float dba$localVelocityY = 0.0F;

    @Unique
    private float dba$headYawRel = 0.0F;

    @Override
    public void dba$extractFromPlayer(AbstractClientPlayer player, float partialTicks) {
        if (player instanceof PlayerStatsAccessor accessor) {
            this.dba$raceId = accessor.dba$getRaceId();
            this.dba$hasTail = accessor.dba$hasTail();
            this.dba$activeFormId = accessor.dba$getActiveFormId();
            this.dba$skinColor = parseHexColor(accessor.dba$getSkinColor(), 0xFF8CC8FF);
            this.dba$hairColor = parseHexColor(accessor.dba$getHairColor(), 0xFF1EB4FF);
            this.dba$activeEmote = accessor.dba$getActiveEmote();
        } else {
            this.dba$hasTail = false;
            this.dba$raceId = null;
            this.dba$activeFormId = null;
            this.dba$skinColor = 0xFF8CC8FF;
            this.dba$hairColor = 0xFF1EB4FF;
            this.dba$activeEmote = "";
        }

        this.dba$isInOtherworld = player.level() != null && player.level().dimension().identifier().getPath().contains("otherworld");
        this.dba$tailAgeInTicks = player.tickCount + partialTicks;

        this.dba$isSprinting = player.isSprinting();
        this.dba$isCrouching = player.isCrouching();
        this.dba$isSwimming = player.isSwimming() || player.isInWater();
        this.dba$isFlying = player.getAbilities().flying || player.isFallFlying();

        var delta = player.getDeltaMovement();
        double dx = delta.x;
        double dy = delta.y;
        double dz = delta.z;
        this.dba$horizontalSpeed = (float) Math.sqrt(dx * dx + dz * dz);
        this.dba$localVelocityY = (float) dy;

        // Accurate body yaw and angular velocity
        float bodyYaw = Mth.rotLerp(partialTicks, player.yBodyRotO, player.yBodyRot);
        this.dba$bodyYawVelocity = Mth.wrapDegrees(player.yBodyRot - player.yBodyRotO);

        // Head yaw relative to body
        float headYaw = Mth.rotLerp(partialTicks, player.yRotO, player.getYRot());
        this.dba$headYawRel = Mth.wrapDegrees(headYaw - bodyYaw);

        // Overall look yaw turning rate
        this.dba$yawVelocity = Mth.wrapDegrees(player.getYRot() - player.yRotO);

        // Transform world velocity vector into local torso coordinate space
        // Facing bodyYaw: 0 = south (+Z), 90 = west (-X), 180 = north (-Z), 270 = east (+X)
        float bodyRad = bodyYaw * ((float) Math.PI / 180.0F);
        float sinB = Mth.sin(bodyRad);
        float cosB = Mth.cos(bodyRad);

        // Local X (strafe): positive = strafe right, negative = strafe left
        this.dba$localVelocityX = (float) (-dx * cosB - dz * sinB);
        // Local Z (forward/backward): positive = forward, negative = backward
        this.dba$localVelocityZ = (float) (-dx * sinB + dz * cosB);

        this.dba$tailRingBufferIndex = (this.dba$tailRingBufferIndex + 1) % 64;
        this.dba$tailPositions[this.dba$tailRingBufferIndex][0] = bodyYaw;
        this.dba$tailPositions[this.dba$tailRingBufferIndex][1] = player.getY();
        this.dba$tailPositions[this.dba$tailRingBufferIndex][2] = player.getXRot();
    }

    @Unique
    private static int parseHexColor(String hex, int defaultColor) {
        if (hex == null || hex.isEmpty()) return defaultColor;
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            int rgb = Integer.parseInt(hex, 16);
            return 0xFF000000 | rgb;
        } catch (Exception e) {
            return defaultColor;
        }
    }

    @Override
    public boolean dba$hasTail() {
        return this.dba$hasTail;
    }

    @Override
    public float dba$getTailAgeInTicks() {
        return this.dba$tailAgeInTicks;
    }

    @Override
    public double[] dba$getTailLatencyPos(int bufferOffset, float partialTicks) {
        partialTicks = 1.0F - partialTicks;
        int targetIndex = this.dba$tailRingBufferIndex - bufferOffset & 63;
        int prevIndex = this.dba$tailRingBufferIndex - bufferOffset - 1 & 63;
        double[] currentPos = new double[3];
        double rotDiff = this.dba$tailPositions[targetIndex][0] - this.dba$tailPositions[prevIndex][0];

        rotDiff = Mth.wrapDegrees(rotDiff);
        
        currentPos[0] = this.dba$tailPositions[prevIndex][0] + rotDiff * (double)partialTicks;
        currentPos[1] = this.dba$tailPositions[prevIndex][1] + (this.dba$tailPositions[targetIndex][1] - this.dba$tailPositions[prevIndex][1]) * (double)partialTicks;
        currentPos[2] = this.dba$tailPositions[prevIndex][2] + (this.dba$tailPositions[targetIndex][2] - this.dba$tailPositions[prevIndex][2]) * (double)partialTicks;
        
        return currentPos;
    }

    @Override
    public boolean dba$isSprinting() {
        return this.dba$isSprinting;
    }

    @Override
    public boolean dba$isCrouching() {
        return this.dba$isCrouching;
    }

    @Override
    public boolean dba$isSwimming() {
        return this.dba$isSwimming;
    }

    @Override
    public boolean dba$isFlying() {
        return this.dba$isFlying;
    }

    @Override
    public float dba$getHorizontalSpeed() {
        return this.dba$horizontalSpeed;
    }

    @Override
    public float dba$getYawVelocity() {
        return this.dba$yawVelocity;
    }

    @Override
    public float dba$getBodyYawVelocity() {
        return this.dba$bodyYawVelocity;
    }

    @Override
    public float dba$getLocalVelocityX() {
        return this.dba$localVelocityX;
    }

    @Override
    public float dba$getLocalVelocityZ() {
        return this.dba$localVelocityZ;
    }

    @Override
    public float dba$getLocalVelocityY() {
        return this.dba$localVelocityY;
    }

    @Override
    public float dba$getHeadYawRel() {
        return this.dba$headYawRel;
    }

    @Override
    public Identifier dba$getRaceId() {
        return this.dba$raceId;
    }

    @Override
    public Identifier dba$getActiveFormId() {
        return this.dba$activeFormId;
    }

    @Override
    public int dba$getSkinColor() {
        return this.dba$skinColor;
    }

    @Override
    public int dba$getHairColor() {
        return this.dba$hairColor;
    }

    @Override
    public boolean dba$isInOtherworld() {
        return this.dba$isInOtherworld;
    }

    @Override
    public String dba$getActiveEmote() {
        return this.dba$activeEmote != null ? this.dba$activeEmote : "";
    }
}

