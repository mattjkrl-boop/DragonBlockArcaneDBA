package com.dragonblockarcanedba.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;

public class AlphaModulatingVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alphaMultiplier;

    public AlphaModulatingVertexConsumer(VertexConsumer delegate, float alphaMultiplier) {
        this.delegate = delegate;
        this.alphaMultiplier = alphaMultiplier;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        int modulatedAlpha = Math.max(0, Math.min(255, Math.round(alpha * alphaMultiplier)));
        delegate.setColor(red, green, blue, modulatedAlpha);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        int a = ARGB.alpha(color);
        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        int modulatedAlpha = Math.max(0, Math.min(255, Math.round(a * alphaMultiplier)));
        delegate.setColor(ARGB.color(modulatedAlpha, r, g, b));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    // No @Override: VertexConsumer.setUv3 only exists on 26.3 and up, where this satisfies the
    // interface. The delegate call is reflective because the method does not exist on 26.2.
    public VertexConsumer setUv3(float u, float v) {
        try {
            java.lang.reflect.Method m = delegate.getClass().getMethod("setUv3", float.class, float.class);
            m.invoke(delegate, u, v);
        } catch (ReflectiveOperationException ignored) {
        }
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }
}
