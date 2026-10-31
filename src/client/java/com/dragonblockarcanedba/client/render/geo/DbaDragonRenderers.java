package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.client.gui.WishScreen;
import com.dragonblockarcanedba.entity.DbaGeoDragon;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;

/** Client side wiring for the GeckoLib eternal dragons (Shenron and Porunga). */
public final class DbaDragonRenderers {
    /** How long the wish animation keeps playing after the wish menu opens and closes. */
    private static final int WISH_LINGER_TICKS = 80;
    private static int lastWishId = -1;
    private static long lastWishTime;

    private DbaDragonRenderers() {}

    public static void init() {
        DbaGeoDragon.Hooks.WISHING = DbaDragonRenderers::wishing;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Entity> EntityRenderer<T, ?> create(EntityRendererProvider.Context ctx, String name) {
        return (EntityRenderer<T, ?>) (EntityRenderer) new GeoEntityRenderer(ctx, new DefaultedEntityGeoModel(DragonBlockArcaneDBA.id(name)));
    }

    public static void markWish(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        lastWishId = entityId;
        lastWishTime = mc.level != null ? mc.level.getGameTime() : 0;
    }

    private static boolean wishing(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof WishScreen ws && ws.dragonId() == e.getId()) return true;
        return lastWishId == e.getId() && e.level().getGameTime() - lastWishTime < WISH_LINGER_TICKS;
    }
}
