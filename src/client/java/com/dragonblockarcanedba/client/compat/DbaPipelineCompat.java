package com.dragonblockarcanedba.client.compat;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Builds the additive particle pipeline and its render Layer.
 *
 * The pipeline classes moved in 26.3 from com.mojang.blaze3d.pipeline (and
 * com.mojang.blaze3d.platform.CompareOp) to com.mojang.renderpearl.api.pipeline,
 * with an identical builder API. Because one compiled jar serves both versions,
 * the pipeline is assembled reflectively from whichever package the runtime has.
 * See audits/MC-263-COMPAT-AUDIT.md item 16.
 */
public final class DbaPipelineCompat {
    private DbaPipelineCompat() {}

    public static SingleQuadParticle.Layer buildAdditiveLayer() {
        try {
            boolean renderpearl = true;
            Class<?> pipelineClass;
            try {
                pipelineClass = Class.forName("com.mojang.renderpearl.api.pipeline.RenderPipeline");
            } catch (ClassNotFoundException e) {
                renderpearl = false;
                pipelineClass = Class.forName("com.mojang.blaze3d.pipeline.RenderPipeline");
            }
            String pkg = renderpearl ? "com.mojang.renderpearl.api.pipeline" : "com.mojang.blaze3d.pipeline";
            Class<?> snippetClass = Class.forName(pkg + ".RenderPipeline$Snippet");
            Class<?> blendClass = Class.forName(pkg + ".BlendFunction");
            Class<?> colorTargetClass = Class.forName(pkg + ".ColorTargetState");
            Class<?> depthStencilClass = Class.forName(pkg + ".DepthStencilState");
            Class<?> compareOpClass = Class.forName(renderpearl ? pkg + ".CompareOp" : "com.mojang.blaze3d.platform.CompareOp");

            Field snippetField = Class.forName("net.minecraft.client.renderer.RenderPipelines")
                .getDeclaredField("PARTICLE_SNIPPET");
            snippetField.setAccessible(true);
            Object snippet = snippetField.get(null);

            Object snippetArray = Array.newInstance(snippetClass, 1);
            Array.set(snippetArray, 0, snippet);
            Method builderMethod = pipelineClass.getMethod("builder", snippetArray.getClass());
            Object builder = builderMethod.invoke(null, snippetArray);

            Object additive = blendClass.getField("ADDITIVE").get(null);
            Object colorTarget = colorTargetClass.getConstructor(blendClass).newInstance(additive);
            Object lessOrEqual = compareOpClass.getField("LESS_THAN_OR_EQUAL").get(null);
            Object depthStencil = depthStencilClass.getConstructor(compareOpClass, boolean.class)
                .newInstance(lessOrEqual, false);

            Class<?> builderClass = builder.getClass();
            builder = builderClass.getMethod("withLocation", Identifier.class)
                .invoke(builder, Identifier.fromNamespaceAndPath("dragonblockarcanedba", "pipeline/additive_particle"));
            builder = builderClass.getMethod("withColorTargetState", colorTargetClass).invoke(builder, colorTarget);
            builder = builderClass.getMethod("withDepthStencilState", depthStencilClass).invoke(builder, depthStencil);
            Object pipeline = builderClass.getMethod("build").invoke(builder);

            return buildLayer(pipelineClass, pipeline);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to build the additive particle pipeline", e);
        }
    }

    private static SingleQuadParticle.Layer buildLayer(Class<?> pipelineClass, Object pipeline)
        throws ReflectiveOperationException {
        Class<?> layerClass = SingleQuadParticle.Layer.class;
        // 26.2: Layer(boolean, Identifier, RenderPipeline). 26.3 adds an OitPipelineSet component.
        try {
            Constructor<?> three = layerClass.getDeclaredConstructor(boolean.class, Identifier.class, pipelineClass);
            three.setAccessible(true);
            return (SingleQuadParticle.Layer) three.newInstance(true, TextureAtlas.LOCATION_PARTICLES, pipeline);
        } catch (NoSuchMethodException ignored) {
            Class<?> oitClass = Class.forName("net.minecraft.client.renderer.oit.OitPipelineSet");
            Constructor<?> four = layerClass.getDeclaredConstructor(
                boolean.class, Identifier.class, pipelineClass, oitClass);
            four.setAccessible(true);
            return (SingleQuadParticle.Layer) four.newInstance(true, TextureAtlas.LOCATION_PARTICLES, pipeline, null);
        }
    }
}
