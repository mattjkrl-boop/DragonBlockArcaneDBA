package com.dragonblockarcanedba.client.render.ki;

import com.dragonblockarcanedba.DragonBlockArcaneDBA;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * The mod's world-space shader pattern: custom RenderPipelines running the
 * no_shapes core shader (animated fractal-noise erosion and dissolve, fresnel
 * rim, soft alpha edges) for effect geometry, built for the no-shapes realism
 * pass. See audits/no-shapes-shader-pattern.md for the full API contract, the
 * javap evidence behind every choice, and the honest limits.
 *
 * Call it exactly like KiRenderHelper.kiRenderType():
 *
 *   RenderType type = DbaShaders.additiveType();  // energy, beams, glows
 *   RenderType type = DbaShaders.alphaType();     // smoke, veils, soft bodies
 *
 * The vertex contract is unchanged from KiRenderHelper.vertex: position,
 * color, uv, overlay, light, normal in the ENTITY format. Vertex color rgb is
 * the tint. Vertex color ALPHA is the life input: callers already drive it
 * from FxEnvelope, and the shader turns that envelope into a material
 * dissolve (the surface eats away through a noise field as alpha falls)
 * instead of a uniform fade. Animation time comes from the vanilla Globals
 * uniform (GameTime), the same clock rendertype_end_portal animates from.
 *
 * Version shape: the pipeline classes live in com.mojang.blaze3d.pipeline on
 * 26.2 and in com.mojang.renderpearl.api.pipeline on 26.3, and
 * RenderType.create is package-private on both, so the pipelines and render
 * types are assembled reflectively from whichever package the runtime has.
 * This is the same approach as client/compat/DbaPipelineCompat (the additive
 * particle pipeline, audits/MC-263-COMPAT-AUDIT.md item 16), extended from a
 * snippet-only build to a full custom-shader pipeline: ENTITY_SNIPPET for the
 * format and matrices, GLOBALS_SNIPPET for the GameTime clock, and our own
 * vertex and fragment shaders on top.
 *
 * Failure posture: if any link of the reflective chain is missing on a future
 * version, the factory logs one error and returns KiRenderHelper.kiRenderType()
 * for the rest of the session, so effects degrade to the pre-pattern visuals
 * instead of crashing the render loop. The fallback is deliberate and logged,
 * never silent.
 */
public final class DbaShaders {

    private DbaShaders() {}

    private static final Identifier SHADER_ID = DragonBlockArcaneDBA.id("core/no_shapes");
    private static final Identifier ADDITIVE_PIPELINE_ID = DragonBlockArcaneDBA.id("pipeline/no_shapes_additive");
    private static final Identifier ALPHA_PIPELINE_ID = DragonBlockArcaneDBA.id("pipeline/no_shapes_alpha");
    private static final String ADDITIVE_NAME = "dba_no_shapes_additive";
    private static final String ALPHA_NAME = "dba_no_shapes_alpha";

    private static final Map<Identifier, RenderType> ADDITIVE_TYPES = new HashMap<>();
    private static final Map<Identifier, RenderType> ALPHA_TYPES = new HashMap<>();
    private static boolean failureLogged;

    /** Additive variant on the shared white canvas texture. Energy, beams, glows. */
    public static synchronized RenderType additiveType() {
        return additiveType(KiRenderHelper.WHITE_TEXTURE);
    }

    /** Additive variant sampling the given texture as Sampler0. */
    public static synchronized RenderType additiveType(Identifier texture) {
        return ADDITIVE_TYPES.computeIfAbsent(texture, tex -> build(tex, true));
    }

    /** Alpha variant on the shared white canvas texture. Smoke, veils, soft bodies. */
    public static synchronized RenderType alphaType() {
        return alphaType(KiRenderHelper.WHITE_TEXTURE);
    }

    /** Alpha variant sampling the given texture as Sampler0. */
    public static synchronized RenderType alphaType(Identifier texture) {
        return ALPHA_TYPES.computeIfAbsent(texture, tex -> build(tex, false));
    }

    private static RenderType build(Identifier texture, boolean additive) {
        try {
            return createType(texture, additive);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!failureLogged) {
                failureLogged = true;
                DragonBlockArcaneDBA.LOGGER.error(
                    "DbaShaders: custom shader pipeline unavailable on this version; "
                        + "falling back to the vanilla ki render type for shader effects.", e);
            }
            return KiRenderHelper.kiRenderType();
        }
    }

    private static RenderType createType(Identifier texture, boolean additive) throws ReflectiveOperationException {
        // Pipeline classes: blaze3d on 26.2, renderpearl on 26.3. Identical
        // builder API, different package (DbaPipelineCompat precedent).
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

        Class<?> pipelinesClass = Class.forName("net.minecraft.client.renderer.RenderPipelines");
        Object entitySnippet = snippet(pipelinesClass, "ENTITY_SNIPPET");
        Object globalsSnippet = snippet(pipelinesClass, "GLOBALS_SNIPPET");

        Object snippetArray = Array.newInstance(snippetClass, 2);
        Array.set(snippetArray, 0, entitySnippet);
        Array.set(snippetArray, 1, globalsSnippet);
        Method builderMethod = pipelineClass.getMethod("builder", snippetArray.getClass());
        Object builder = builderMethod.invoke(null, snippetArray);
        Class<?> builderClass = builder.getClass();

        builder = call(builder, builderClass, "withLocation", Identifier.class,
            additive ? ADDITIVE_PIPELINE_ID : ALPHA_PIPELINE_ID);
        builder = call(builder, builderClass, "withVertexShader", Identifier.class, SHADER_ID);
        builder = call(builder, builderClass, "withFragmentShader", Identifier.class, SHADER_ID);
        // Variant flavor, in the vanilla style (ENTITY_TRANSLUCENT_EMISSIVE is
        // itself just ENTITY_SNIPPET plus defines and state). ADDITIVE blends
        // with factors ONE/ONE, so its brightness is premultiplied in-shader
        // under RIM_GLOW; the alpha variant softens its own silhouette under
        // SOFT_SILHOUETTE.
        builder = call(builder, builderClass, "withShaderDefine", String.class,
            additive ? "RIM_GLOW" : "SOFT_SILHOUETTE");
        Object blend = blendClass.getField(additive ? "ADDITIVE" : "TRANSLUCENT").get(null);
        Object colorTarget = colorTargetClass.getConstructor(blendClass).newInstance(blend);
        builder = call(builder, builderClass, "withColorTargetState", colorTargetClass, colorTarget);
        // Depth: test on, write off, LESS_THAN_OR_EQUAL. This matches the mod's
        // existing custom additive particle pipeline (DbaPipelineCompat), which
        // submits into the same translucent pass these effects draw in. It
        // deliberately does not copy ENTITY_TRANSLUCENT_EMISSIVE's
        // GREATER_THAN_OR_EQUAL state, which belongs to vanilla's separate
        // emissive target path.
        Object lessOrEqual = compareOpClass.getField("LESS_THAN_OR_EQUAL").get(null);
        Object depthStencil = depthStencilClass.getConstructor(compareOpClass, boolean.class)
            .newInstance(lessOrEqual, false);
        builder = call(builder, builderClass, "withDepthStencilState", depthStencilClass, depthStencil);
        // Cull off, exactly like the type this pattern succeeds
        // (ENTITY_TRANSLUCENT_EMISSIVE builds with withCull(false)): effect
        // geometry is authored to be read from both sides.
        builder = call(builder, builderClass, "withCull", boolean.class, false);
        Object pipeline = builderClass.getMethod("build").invoke(builder);

        // Vanilla registers every pipeline it builds into RenderPipelines,
        // keyed by location, so the shader manager precompiles them with the
        // rest at load. Do the same when the hook exists; a version without it
        // still compiles the pipeline lazily on first draw.
        try {
            Method register = pipelinesClass.getDeclaredMethod("register", pipelineClass);
            register.setAccessible(true);
            register.invoke(null, pipeline);
        } catch (NoSuchMethodException ignored) {
        }

        // RenderSetup.builder(pipeline) exists on both versions with the
        // runtime pipeline class as its parameter; resolve it by shape.
        Class<?> setupClass = Class.forName("net.minecraft.client.renderer.rendertype.RenderSetup");
        Method setupBuilder = null;
        for (Method method : setupClass.getDeclaredMethods()) {
            if (method.getName().equals("builder") && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isAssignableFrom(pipelineClass)) {
                setupBuilder = method;
                break;
            }
        }
        if (setupBuilder == null) {
            throw new NoSuchMethodException("RenderSetup.builder(" + pipelineClass.getName() + ")");
        }
        Object setup = setupBuilder.invoke(null, pipeline);
        Class<?> setupBuilderClass = setup.getClass();
        // Sampler0 only. No lightmap and no overlay flags: the pattern is
        // emissive by design, the same FULL_BRIGHT contract KiRenderHelper
        // geometry already renders under.
        setup = setupBuilderClass.getMethod("withTexture", String.class, Identifier.class)
            .invoke(setup, "Sampler0", texture);
        Object renderSetup = setupBuilderClass.getMethod("createRenderSetup").invoke(setup);

        // RenderType.create(String, RenderSetup) is package-private on both
        // versions; there is no public factory for a custom pipeline.
        Class<?> renderTypeClass = Class.forName("net.minecraft.client.renderer.rendertype.RenderType");
        Method create = renderTypeClass.getDeclaredMethod("create", String.class, setupClass);
        create.setAccessible(true);
        return (RenderType) create.invoke(null, additive ? ADDITIVE_NAME : ALPHA_NAME, renderSetup);
    }

    private static Object snippet(Class<?> pipelinesClass, String fieldName) throws ReflectiveOperationException {
        Field field = pipelinesClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object call(Object target, Class<?> type, String method, Class<?> paramType, Object arg)
        throws ReflectiveOperationException {
        return type.getMethod(method, paramType).invoke(target, arg);
    }
}
