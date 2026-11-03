package com.dragonblockarcanedba.client.hooks;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.dragonblockarcanedba.entity.DbaEntities;
import com.dragonblockarcanedba.item.DbaArmorItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.GeoEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Client models for the patrol robot and the worn armor sets. */
public final class GearClient implements ClientModInitializer {
    public GearClient() {}

    @Override
    public void onInitializeClient() {
        init();
    }

    public static void init() {
        registerRobot();
        DbaArmorItem.Hooks.PROVIDER = GearClient::armorProvider;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRobot() {
        EntityRendererRegistry.register(DbaEntities.PATROL_ROBOT, ctx ->
                (EntityRenderer) new GeoEntityRenderer(ctx, new DefaultedEntityGeoModel(DragonBlockArcaneDBA.id("patrol_robot"))));
    }

    private static Object armorProvider(DbaArmorItem item) {
        final String modelName = item.modelName();
        return new GeoRenderProvider() {
            private GeoArmorRenderer<?, ?> renderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
                if (this.renderer == null) {
                    this.renderer = armorRenderer(modelName);
                }
                return this.renderer;
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static GeoArmorRenderer<?, ?> armorRenderer(String modelName) {
        return new GeoArmorRenderer(new DefaultedItemGeoModel(DragonBlockArcaneDBA.id(modelName)));
    }
}
