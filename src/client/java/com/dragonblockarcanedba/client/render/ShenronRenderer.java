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
    }

    @Override
    public Identifier getTextureLocation(ShenronRenderState state) {
        return TEXTURE;
    }

    public static class ShenronRenderState extends LivingEntityRenderState {
        public float ageInTicks;
    }
}
