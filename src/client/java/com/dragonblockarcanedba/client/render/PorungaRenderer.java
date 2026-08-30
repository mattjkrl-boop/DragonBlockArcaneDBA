package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.PorungaEntity;
import com.dragonblockarcanedba.client.model.PorungaModel;
import com.dragonblockarcanedba.client.DragonBlockArcaneDBAClient;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class PorungaRenderer extends MobRenderer<PorungaEntity, PorungaRenderer.PorungaRenderState, PorungaModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("dragonblockarcanedba", "textures/entity/porunga.png");

    public PorungaRenderer(EntityRendererProvider.Context context) {
        super(context, new PorungaModel(context.bakeLayer(DragonBlockArcaneDBAClient.PORUNGA_MODEL_LAYER)), 7.5f);
    }

    @Override
    public PorungaRenderState createRenderState() {
        return new PorungaRenderState();
    }

    @Override
    public void extractRenderState(PorungaEntity entity, PorungaRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ageInTicks = entity.tickCount + partialTicks;
    }

    @Override
    public Identifier getTextureLocation(PorungaRenderState state) {
        return TEXTURE;
    }

    public static class PorungaRenderState extends LivingEntityRenderState {
        public float ageInTicks;
    }
}
