package com.dragonblockarcanedba.client.render.item;

import net.minecraft.client.renderer.special.SpecialModelRenderer;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ProceduralWeaponUnbakedModel implements SpecialModelRenderer.Unbaked<Object> {
    public static final MapCodec<ProceduralWeaponUnbakedModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("weapon").forGetter(model -> model.weaponName)
    ).apply(instance, ProceduralWeaponUnbakedModel::new));

    private final String weaponName;

    public ProceduralWeaponUnbakedModel(String weaponName) {
        this.weaponName = weaponName;
    }

    @Override
    public MapCodec<? extends SpecialModelRenderer.Unbaked<Object>> type() {
        return CODEC;
    }

    @Override
    public SpecialModelRenderer<Object> bake(SpecialModelRenderer.BakingContext context) {
        return new ProceduralWeaponRenderer(weaponName);
    }
}
