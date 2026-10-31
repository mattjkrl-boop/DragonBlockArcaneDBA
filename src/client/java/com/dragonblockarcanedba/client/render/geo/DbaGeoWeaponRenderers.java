package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.item.geo.DbaGeoWeapon;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/** Client side wiring for {@link DbaGeoWeapon}: geo renderer with glow layer and hand state. */
public final class DbaGeoWeaponRenderers {
    private DbaGeoWeaponRenderers() {}

    public static void init() {
        DbaGeoWeapon.Hooks.PROVIDER = DbaGeoWeaponRenderers::provider;
        DbaGeoWeapon.Hooks.STATE = DbaGeoWeaponRenderers::state;
    }

    private static GeoRenderProvider provider(Item item) {
        return new GeoRenderProvider() {
            private GeoItemRenderer<?> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = build(item);
                return renderer;
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends Item & GeoAnimatable> GeoItemRenderer<T> build(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        DefaultedItemGeoModel<T> model = new DefaultedItemGeoModel<T>(DragonBlockArcaneDBA.id(path))
            .withAltAnimations(DragonBlockArcaneDBA.id("dba_weapon"));
        GeoItemRenderer<T> r = new GeoItemRenderer<>(model);
        r.withRenderLayer(new AutoGlowingGeoLayer(r));
        return r;
    }

    private static int state(Item item) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return 0;
        if (p.isUsingItem() && p.getUseItem().is(item)) return 2;
        if (p.swinging && p.getMainHandItem().is(item)) return 1;
        return 0;
    }
}
