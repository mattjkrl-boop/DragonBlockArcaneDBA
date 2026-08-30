package com.dragonblockarcanedba.client.render.item;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

import com.dragonblockarcanedba.client.render.ki.KiRenderHelper;
import com.dragonblockarcanedba.client.render.weapon.WeaponModelRegistry;
import com.dragonblockarcanedba.client.render.weapon.model.ModelPart;

import java.util.function.Consumer;

public class ProceduralWeaponRenderer implements SpecialModelRenderer<Object> {
    private final String weaponName;

    public ProceduralWeaponRenderer(String weaponName) {
        this.weaponName = weaponName;
    }

    @Override
    public Object extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> minMax) {
        minMax.accept(new org.joml.Vector3f(-2, -2, -2));
        minMax.accept(new org.joml.Vector3f(2, 2, 2));
    }

    @Override
    public void submit(Object arg, com.mojang.blaze3d.vertex.PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int otherInt) {
        ModelPart root = WeaponModelRegistry.getModel(weaponName);
        if (root != null) {
            // Using standard solid render type with the plain white texture
            RenderType renderType = RenderTypes.entitySolid(KiRenderHelper.WHITE_TEXTURE);

            // Use submitCustomGeometry for procedural mathematically generated primitives (Cylinders, Rings, etc.)
            collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                com.mojang.blaze3d.vertex.PoseStack localPose = new com.mojang.blaze3d.vertex.PoseStack();
                localPose.last().pose().set(pose.pose());
                localPose.last().normal().set(pose.normal());
                root.render(localPose, buffer, light, overlay, null);
            });
        }
    }
}
