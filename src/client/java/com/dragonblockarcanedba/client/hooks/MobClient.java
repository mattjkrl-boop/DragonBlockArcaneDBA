package com.dragonblockarcanedba.client.hooks;

import com.dragonblockarcanedba.client.render.geo.DbaDragonRenderers;
import com.dragonblockarcanedba.entity.DbaEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Client renderers for the hostile roster. Models use the same GeckoLib entity path as the dragons. */
public final class MobClient implements ClientModInitializer {
    public MobClient() {}

    @Override
    public void onInitializeClient() {
        init();
    }

    public static void init() {
        EntityRendererRegistry.register(DbaEntities.SAIBAMAN, ctx -> DbaDragonRenderers.create(ctx, "saibaman"));
        EntityRendererRegistry.register(DbaEntities.FRIEZA_SOLDIER, ctx -> DbaDragonRenderers.create(ctx, "frieza_soldier"));
        EntityRendererRegistry.register(DbaEntities.NAMEKIAN_WARRIOR, ctx -> DbaDragonRenderers.create(ctx, "namekian_warrior"));
    }
}
