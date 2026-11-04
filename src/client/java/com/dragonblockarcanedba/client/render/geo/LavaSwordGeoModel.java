package com.dragonblockarcanedba.client.render.geo;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * GeckoLib already swings and charges the item bones. The blade core is a
 * separate scrolling texture so the molten rock moves while the edge glow stays put.
 */
public class LavaSwordGeoModel<T extends Item & GeoAnimatable> extends DefaultedItemGeoModel<T> {
    private static final int FRAMES = 6;
    private static final Identifier[] TEXTURES = new Identifier[FRAMES];

    static {
        for (int i = 0; i < FRAMES; i++) {
            TEXTURES[i] = DragonBlockArcaneDBA.id("textures/item/lava_sword_geo_f" + i + ".png");
        }
    }

    public LavaSwordGeoModel() {
        super(DragonBlockArcaneDBA.id("lava_sword"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        long ticks = 0L;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            ticks = minecraft.level.getGameTime();
        }
        int frame = (int) ((ticks / 3L) % FRAMES);
        return TEXTURES[frame];
    }
}
