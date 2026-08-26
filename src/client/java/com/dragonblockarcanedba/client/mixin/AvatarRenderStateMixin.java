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
    private int dba$skinColor = 0xFF8CC8FF;

    @Unique
    private int dba$hairColor = 0xFF1EB4FF;

    @Unique
    private boolean dba$isInOtherworld = false;

    @Override
    public void dba$extractFromPlayer(AbstractClientPlayer player, float partialTicks) {
        if (player instanceof PlayerStatsAccessor accessor) {
            this.dba$raceId = accessor.dba$getRaceId();
            if (this.dba$raceId != null) {
                String path = this.dba$raceId.getPath();
                this.dba$hasTail = path.equals("saiyan") || path.equals("arcosian") || path.equals("half_saiyan") || path.equals("bio_android");
            } else {
                this.dba$hasTail = false;
            }

            this.dba$skinColor = parseHexColor(accessor.dba$getSkinColor(), 0xFF8CC8FF);
            this.dba$hairColor = parseHexColor(accessor.dba$getHairColor(), 0xFF1EB4FF);
        } else {
            this.dba$hasTail = false;
            this.dba$raceId = null;
            this.dba$skinColor = 0xFF8CC8FF;
            this.dba$hairColor = 0xFF1EB4FF;
        }

        this.dba$isInOtherworld = player.level() != null && player.level().dimension().identifier().getPath().contains("otherworld");
        
        this.dba$tailAgeInTicks = player.tickCount + partialTicks;
        
        this.dba$tailRingBufferIndex = (this.dba$tailRingBufferIndex + 1) % 64;
        this.dba$tailPositions[this.dba$tailRingBufferIndex][0] = player.getYRot();
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
    public Identifier dba$getRaceId() {
        return this.dba$raceId;
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
}
