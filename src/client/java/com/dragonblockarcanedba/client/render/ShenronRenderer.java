package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.ShenronEntity;
import com.dragonblockarcanedba.client.model.ShenronModel;
import com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class ShenronRenderer extends MobRenderer<ShenronEntity, ShenronRenderer.ShenronRenderState, ShenronModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/shenron.png");

    public ShenronRenderer(EntityRendererProvider.Context context) {
        super(context, new ShenronModel(context.bakeLayer(DragonBlockArcaneDBAClient.SHENRON_MODEL_LAYER)), 1.5f);
    }

    @Override
    public ShenronRenderState createRenderState() {
        return new ShenronRenderState();
    }

    @Override
    public void extractRenderState(ShenronEntity entity, ShenronRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
        state.ringBufferIndex = entity.ringBufferIndex;
        for (int i = 0; i < 64; i++) {
            state.positions[i][0] = entity.positions[i][0];
            state.positions[i][1] = entity.positions[i][1];
            state.positions[i][2] = entity.positions[i][2];
        }
        state.speed = (float) Math.sqrt((entity.getX() - entity.xo) * (entity.getX() - entity.xo) + (entity.getZ() - entity.zo) * (entity.getZ() - entity.zo));
    }

    @Override
    public Identifier getTextureLocation(ShenronRenderState state) {
        return TEXTURE;
    }

    public static class ShenronRenderState extends LivingEntityRenderState {
        public float ageInTicks;
        public float speed;
        public int ringBufferIndex;
        public final double[][] positions = new double[64][3];

        public double[] getLatencyPos(int bufferOffset, float partialTicks) {
            partialTicks = 1.0F - partialTicks;
            int targetIndex = this.ringBufferIndex - bufferOffset & 63;
            int prevIndex = this.ringBufferIndex - bufferOffset - 1 & 63;
            double[] currentPos = new double[3];
            double rotDiff = this.positions[targetIndex][0] - this.positions[prevIndex][0];

            rotDiff = net.minecraft.util.Mth.wrapDegrees(rotDiff);
            
            currentPos[0] = this.positions[prevIndex][0] + rotDiff * (double)partialTicks;
            currentPos[1] = this.positions[prevIndex][1] + (this.positions[targetIndex][1] - this.positions[prevIndex][1]) * (double)partialTicks;
            currentPos[2] = this.positions[prevIndex][2] + (this.positions[targetIndex][2] - this.positions[prevIndex][2]) * (double)partialTicks;
            
            return currentPos;
        }
    }
}
