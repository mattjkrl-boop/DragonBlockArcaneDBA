package com.dragonblockarcanedba.client.render;

import com.dragonblockarcanedba.entity.AzureStormEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class AzureStormRenderer extends EntityRenderer<AzureStormEntity, EntityRenderState> {
    public AzureStormRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
