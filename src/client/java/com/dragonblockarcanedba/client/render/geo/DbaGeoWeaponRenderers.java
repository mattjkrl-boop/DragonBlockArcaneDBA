package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.item.geo.DbaGeoWeapon;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Set;

/**
 * Client side wiring for {@link DbaGeoWeapon}: builds the DbaWeaponRenderer with its glow
 * layer for each weapon item. Animation state no longer comes from here; DbaWeaponRenderer
 * stamps it per holder at render time (see DbaWeaponTickets).
 *
 * Weapons listed in MIGRATED have been rebuilt to the Phase 1 standard: they use their own
 * texture atlas (path + "_geo", with a matching "_geo_glowmask") and their own animation
 * file (geckolib/animations/item/<path>.animation.json) instead of the shared dba_weapon
 * animations and the flat sprite texture. Add a weapon's registry path here as its rebuild
 * lands; every other weapon keeps the legacy shared setup until its turn.
 */
public final class DbaGeoWeaponRenderers {
    private DbaGeoWeaponRenderers() {}

    private static final Set<String> MIGRATED = Set.of(
        "katana", "z_sword", "brave_sword", "grand_sword", "azure_dragon_sword",
        "dabura_sword", "cursed_blade", "hollows_edge");

    public static void init() {
        DbaGeoWeapon.Hooks.PROVIDER = DbaGeoWeaponRenderers::provider;
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
        DefaultedItemGeoModel<T> model = new DefaultedItemGeoModel<T>(DragonBlockArcaneDBA.id(path));
        if (MIGRATED.contains(path)) {
            model = model.withAltTexture(DragonBlockArcaneDBA.id(path + "_geo"));
        } else {
            model = model.withAltAnimations(DragonBlockArcaneDBA.id("dba_weapon"));
        }
        GeoItemRenderer<T> r = new DbaWeaponRenderer<>(model);
        r.withRenderLayer(new AutoGlowingGeoLayer(r));
        return r;
    }
}
