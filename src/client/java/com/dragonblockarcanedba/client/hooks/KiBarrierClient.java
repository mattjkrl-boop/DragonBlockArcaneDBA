package com.dragonblockarcanedba.client.hooks;

import com.dragonblockarcanedba.client.render.ki.KiBarrierRenderer;
import com.dragonblockarcanedba.ki.KiBarrier;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;

/**
 * Client entry for the Ki Barrier shell. Registers the renderer and nothing else.
 */
public final class KiBarrierClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        init();
    }

    public static void init() {
        EntityRenderers.register(KiBarrier.TYPE, KiBarrierRenderer::new);
    }
}
