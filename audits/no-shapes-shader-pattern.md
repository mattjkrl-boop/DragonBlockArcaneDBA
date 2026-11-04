# No-Shapes Shader Pattern

Owner: shader pattern agent, no-shapes realism pass, on top of Phase 1 (ad22b32).
Status: pattern built, API frozen for merge-time adoption. No effect was
retrofitted; the Phase 2 owners' files were not touched. No commits.

## What was built

Three new files:

- `src/client/java/com/dragonblockarcanedba/client/render/ki/DbaShaders.java`
  The factory. Builds two custom RenderPipelines with the mod's own vertex
  and fragment shaders and exposes them as RenderTypes.
- `src/main/resources/assets/dragonblockarcanedba/shaders/core/no_shapes.vsh`
- `src/main/resources/assets/dragonblockarcanedba/shaders/core/no_shapes.fsh`

The shader delivers, in world space, on the exact vertex contract effect
renderers already write through KiRenderHelper.vertex:

- Animated fractal-noise erosion and dissolve. A two-read fbm value-noise
  field (coarse plus fine octave) is sampled from the surface's
  camera-relative position, so the pattern is glued to the world instead of
  swimming with UVs, and scrolled in opposite directions by the vanilla
  Globals clock (GameTime), the same input that animates
  rendertype_end_portal. As the effect's life envelope falls, a dissolve
  threshold rises through the field and the surface eats away in patches
  with a soft smoothstep band, instead of fading uniformly.
- Fresnel-style edge falloff, computed from the real vertex normal and view
  direction in view space. The additive variant turns it into a hot rim;
  the alpha variant uses facing to soften its own silhouette.
- Soft alpha edges throughout: no hard cutout anywhere; the dissolve band
  is 0.38 wide in noise space and the alpha variant multiplies alpha by a
  facing term so mesh silhouettes melt instead of ending at a polygon edge.
- Internal motion: the fine octave breathes brightness (0.86 plus 0.28 times
  the fine field), so a held effect does not read as a flat lit sheet.
- Vanilla fog applied at the end, same as the entity shader it succeeds.

## The API

```java
RenderType type = DbaShaders.additiveType();   // energy, beams, glows
RenderType type = DbaShaders.alphaType();      // smoke, veils, soft bodies
RenderType type = DbaShaders.additiveType(textureId);  // textured variant
RenderType type = DbaShaders.alphaType(textureId);
```

Called exactly the way renderers call KiRenderHelper.kiRenderType() today
and swapped in at the submit site; nothing else at the call site changes.
Both default factories bind the shared white canvas texture
(dragonblockarcanedba:textures/entity/ki_white.png) as Sampler0, the same
texture kiRenderType() binds. Types are built once and cached.

Inputs (there are no custom uniforms and no new plumbing):

- Vertex color rgb: the tint, as today.
- Vertex color alpha: THE life input. Every call site already drives it
  from FxEnvelope. The shader interprets it as remaining life: alpha 1 is
  intact material, and as alpha falls the dissolve threshold rises. An
  owner adopting the pattern gets erosion for free from the envelope they
  already submit, but see the adoption note under Honest limits: effects
  whose peak envelope alpha is low will read partially eroded at peak.
- Position and Normal: real geometry inputs for the noise anchor and the
  fresnel term. KiRenderHelper already writes true normals everywhere.
- GameTime (Globals uniform, engine-supplied): the animation clock.
- Fog uniforms (engine-supplied): standard distance fog.

Variants differ in pipeline state and one shader define each, in the
vanilla style (ENTITY_TRANSLUCENT_EMISSIVE is itself ENTITY_SNIPPET plus
defines and state):

- Additive: BlendFunction.ADDITIVE, define RIM_GLOW. Javap shows ADDITIVE
  is constructed with factors ONE/ONE, so the alpha channel cannot
  modulate the result; the fragment shader therefore premultiplies color
  by the computed alpha under RIM_GLOW, making brightness the envelope,
  and adds the fresnel rim on top.
- Alpha: BlendFunction.TRANSLUCENT (SRC_ALPHA, ONE_MINUS_SRC_ALPHA),
  define SOFT_SILHOUETTE. Alpha fades toward grazing angles so soft
  bodies have no hard mesh edge.

Shared pipeline state: ENTITY vertex format and QUADS topology inherited
from ENTITY_SNIPPET, cull off (withCull(false), exactly like the type
this pattern succeeds), depth test LESS_THAN_OR_EQUAL with depth write
off (see version findings for why vanilla's emissive depth state was not
copied), Sampler0 only: no lightmap, no overlay. The pattern is emissive
by design, the same FULL_BRIGHT contract ki geometry renders under today.

## Version findings (javap, both audit jars)

All claims below were read out of the actual jars on this machine:
26.2 from minecraft-26.2-client.jar, 26.3 from mc263.jar, JDK 25 javap.

- Pipeline API split, confirmed: on 26.2 the pipeline types live in
  com.mojang.blaze3d.pipeline (RenderPipeline, Builder, Snippet,
  BlendFunction, ColorTargetState, DepthStencilState); on 26.3
  com.mojang.blaze3d.pipeline contains only MainTarget, PipelineCache,
  RenderTarget, TextureTarget, and the API moved to
  com.mojang.renderpearl.api.pipeline. Same class names, same builder
  method shapes, new package. DbaShaders therefore assembles everything
  reflectively, resolving the package at runtime. This extends the
  existing precedent, client/compat/DbaPipelineCompat (audits/
  MC-263-COMPAT-AUDIT.md item 16), which builds the additive particle
  pipeline the same reflective way.
- Snippets: net.minecraft.client.renderer.RenderPipelines holds private
  static final ENTITY_SNIPPET and GLOBALS_SNIPPET fields on BOTH
  versions (renderpearl Snippet type on 26.3). ENTITY_SNIPPET on 26.2 is
  MATRICES_FOG_LIGHT_DIR_SNIPPET plus the core/entity shaders, the
  SAMPLER0_SAMPLER2 layout, a vertex binding of DefaultVertexFormat.ENTITY
  at 0, QUADS, and DepthStencilState.DEFAULT. It does NOT include the
  Globals layout.
- GLOBALS_SNIPPET is BindGroupLayouts.GLOBALS alone. Vanilla composes it
  into other pipelines: END_PORTAL_SNIPPET is GLOBALS_SNIPPET plus
  MATRICES_PROJECTION plus FOG plus SAMPLER0_SAMPLER1, and
  rendertype_end_portal.fsh reads GameTime from <minecraft:globals.glsl>
  in world space. Composing builder(ENTITY_SNIPPET, GLOBALS_SNIPPET) is
  therefore the vanilla composition pattern, and GameTime is a live,
  engine-populated input for world-space draws, not a post-only value.
- The replaced type's recipe, read from the RenderPipelines static
  initializer: ENTITY_TRANSLUCENT_EMISSIVE is builder(ENTITY_SNIPPET),
  location pipeline/entity_translucent_emissive, defines ALPHA_CUTOUT
  0.1 and PER_FACE_LIGHTING, extra SAMPLER1 layout, TRANSLUCENT color
  target, withCull(false), and DepthStencilState(GREATER_THAN_OR_EQUAL,
  writeDepth false). The GREATER_THAN_OR_EQUAL state belongs to vanilla's
  separate emissive target path; DBA effects submit into the main
  translucent pass alongside the existing ki geometry, so DbaShaders uses
  LESS_THAN_OR_EQUAL with write off, which is the exact state the mod's
  own custom additive particle pipeline (DbaPipelineCompat) already ships
  for that same pass. This divergence is deliberate and recorded here.
- BlendFunction constants, from its static initializer: TRANSLUCENT is
  (SRC_ALPHA, ONE_MINUS_SRC_ALPHA, ONE, ONE_MINUS_SRC_ALPHA); ADDITIVE
  is the two-factor constructor with (ONE, ONE). This is why the additive
  fragment path premultiplies (above) instead of trusting alpha.
- RenderSetup and RenderType: RenderSetup exists at
  net.minecraft.client.renderer.rendertype.RenderSetup on both versions,
  with public static builder(...) taking the version's own pipeline type,
  and a builder with withTexture(String, Identifier), useLightmap,
  useOverlay, createRenderSetup. RenderType.create(String, RenderSetup)
  is package-private static and the RenderType constructor is private on
  BOTH versions: there is no public factory for a custom-pipeline render
  type, so DbaShaders invokes create reflectively with setAccessible, in
  classpath mode where that is the same access pattern DbaPipelineCompat
  already uses on this class's sibling (PARTICLE_SNIPPET).
- Registration: RenderPipelines.register(RenderPipeline) is private
  static on both versions, and every vanilla pipeline passes through it
  into a PIPELINES_BY_LOCATION map. DbaShaders calls it reflectively
  after build, so the shader manager precompiles the two pipelines at
  load with everything else. If a future version drops the method, the
  call is skipped (guarded NoSuchMethodException) and the pipelines
  compile lazily on first draw, which is the mode DbaPipelineCompat's
  unregistered particle pipeline already runs in.
- Shader resolution: vanilla core shaders are pure GLSL pairs at
  assets/<namespace>/shaders/core/<name>.vsh and .fsh, with #moj_import
  includes from assets/minecraft/shaders/include (globals, dynamictransforms,
  projection, fog all verified by extraction). DbaShaders points the
  pipelines at Identifier dragonblockarcanedba:core/no_shapes, which
  resolves to the two files listed at the top of this report, under the
  mod's own namespace, following the vanilla layout exactly.
- Vertex format match: the ENTITY format's attributes (Position, Color,
  UV0, UV1, UV2, Normal) are exactly what net.minecraft's entity.vsh
  declares and exactly what KiRenderHelper.vertex writes; no_shapes.vsh
  declares the same inputs.

Failure posture: construction is lazy (first factory call) and wrapped.
If any reflective link is missing on a future version, DbaShaders logs
one error through DragonBlockArcaneDBA.LOGGER and returns
KiRenderHelper.kiRenderType() for the rest of the session. Effects
degrade to the pre-pattern visuals; the client never crashes in a render
loop because of this pattern. The fallback is loud in the log and stated
here, never silent.

## Refraction investigation (Carter's gate: certainty or skip)

Verdict: SKIP. The evidence below shows the load-bearing links are
private internals or absent APIs on both versions, so under the ruling
(refraction only if a complete, supported path is certain on both
versions) it is not implemented, and nothing in the pattern fakes it.
The screen-space post shockwave chain (DbaPostFx) remains the distortion
route, as Phase 1 found.

What a true local refraction needs, link by link, against the jars:

1. A copy of the scene color before the effect draws. The frame graph
   builder is public (com.mojang.blaze3d.framegraph.FrameGraphBuilder:
   addPass, importExternal, createInternal, execute, identical on both
   versions), but no instance is ever exposed. LevelRenderer assembles
   the whole level graph inside private methods on both versions
   (addSkyPass, addMainPass, addCloudsPass, addWeatherPass,
   addAlwaysOnTopPass are all private; addMainPass even gained a boolean
   parameter between 26.2 and 26.3). There is no public or Fabric
   surface to insert a copy pass between opaque and translucent world
   rendering: fabric-rendering-v1 on both versions contains only the
   FabricRenderPipeline GUI-draw-mode interfaces, no frame graph events
   or hooks of any kind. Any insertion would be a mixin into private
   LevelRenderer internals whose shape has already changed once between
   the two supported versions, which is precisely guessing at
   undocumented behavior.
2. A world-space shader sampling that copy. Render pipelines bind
   textures only through RenderSetup texture bindings, which name
   managed textures known at setup time. The complete BindGroupLayouts
   list on both versions (GLOBALS, FOG, LIGHTING, MATRICES_PROJECTION,
   PROJECTION, DYNAMIC_TRANSFORMS, SAMPLER0/1/2 and their combos,
   CHUNK_SECTION, CLOUD_INFO, DISSOLVE_MASK_SAMPLER, IN_SAMPLER,
   LIGHTMAP_INFO, SPRITE families) contains no "current frame color"
   layout for world geometry. IN_SAMPLER exists only in
   POST_PROCESSING_SNIPPET, fed by the frame graph between post passes.
   A world render type has no supported sampler that means "the scene
   behind me".
3. Composite ordering and graphics modes. Post chains prove the only
   supported scene access: PostChainConfig inputs are TextureInput
   (static textures) and TargetInput (named render targets by id, with
   a useDepthBuffer flag). They run after the level is rendered, over
   the finished frame; by then the effect surface is already composited
   into the same target it would need to sample, so a post chain can
   distort the finished image around a point (the shockwave route) but
   cannot make a mid-frame surface refract what is behind it. Doing it
   properly also has to survive the graphics-mode split (fancy and fast
   draw translucency into the main target; fabulous splits entities,
   items, and particles into separate targets and composites them, and
   26.3 adds an OIT pipeline family, OitPipelineSet and the OIT_
   snippets, changing the translucent topology again), each topology
   needing its own copy point and composite step.

What it would risk, if forced through mixins into the private graph:
breakage on every version bump (the graph's method signatures already
differ across the two supported versions), feedback-loop artifacts
where the copy is taken after some translucency has already drawn,
double distortion in fabulous mode, and silent failure modes that only
appear in game. None of that is shippable under the certainty gate.

What remains true and supported: RenderTarget itself is public with
copyDepthFrom and blitAndBlendToTexture, and named-target post inputs
exist. If a future version exposes a public frame graph hook or a
scene-color bind group layout for world pipelines, the missing links
become available and this section can be revisited with the same
evidence method.

## Honest limits

- Not verified in game. This VM cannot run Gradle or the client (the
  standing sandbox restriction), so verification is the javac audit
  below plus javap and shader-source evidence. GLSL was reviewed line
  by line against the extracted vanilla entity and end portal shaders;
  there is no GLSL compiler in this VM, so a driver-side compile of the
  two shader files happens at Carter's first PC run, like every other
  pass. The loud fallback in DbaShaders caps the blast radius if a
  reflective link differs at runtime.
- Adoption note for the merge, the one real look risk: because vertex
  alpha is the dissolve driver, an effect whose envelope peaks low
  (for example a shell authored at 0.34 alpha) will sit at roughly
  two-thirds erosion at its peak under this pattern, where today it
  renders as a uniform faint sheet. For most energy shells that reads
  as material, and it is the intended trade, but any owner adopting the
  pattern at the merge should treat submitted alpha as life, not as
  final opacity: raise the envelope toward 1.0 for the body of the
  effect and let the shader own the fade. If a specific effect reads
  flat or cheap under the pattern on the PC check, that is a merge-time
  tuning call on its envelope numbers, not a pattern change, and it
  should be reported rather than shipped quietly.
- The noise is value noise, not a texture: zero new assets, zero
  sampling cost beyond the fbm reads (8 noise evaluations per fragment
  across the two fields). At full-screen effect sizes this is a real
  but modest fragment cost; it is the standard trade for shader-driven
  erosion and far cheaper than the geometry it replaces per pixel of
  coverage.
- No per-effect uniforms exist in this pattern, deliberately: the only
  supported per-pipeline constants are compile-time shader defines, and
  per-draw state rides the vertex stream (color, alpha, uv, normal).
  An owner who needs a second erosion flavor (faster churn, larger
  scale) gets it from a new define pair at the merge, not from a
  uniform API that the pipeline system does not offer.
- One proof call: none was wired. The work order allowed one minimal
  call into a foundation-owned surface if needed to verify the API
  compiles in use. It was not needed: the factory's own typed return
  (the reflective result is cast to RenderType and returned through
  public methods whose signatures javac checks) is the compile proof,
  and every effect surface belongs to a Phase 2 owner this pass. The
  first real call sites land at the merge.

## Verification

- Targeted javac of DbaShaders.java on both audit classpaths
  (cp262.txt, cp263.txt), sourcepath over src/client/java and
  src/main/java: zero errors in DbaShaders.java on either version, and
  DbaShaders.class was produced. The only errors in those runs are the
  pre-existing baseline kinds in unrelated files (DbaItems, DbaMenus,
  entity swing signatures) that the audit baseline already carries;
  the file set and lines are unchanged by this work.
- Shader resources: both GLSL files follow the vanilla core layout and
  resolve from the registered pipeline shader id
  (dragonblockarcanedba:core/no_shapes maps to
  assets/dragonblockarcanedba/shaders/core/no_shapes.vsh and .fsh, both
  present). No JSON resources are added by this pattern: pipelines in
  this engine generation are code-built, and there is no per-shader
  JSON to parse (verified against the vanilla jar, whose core shaders
  ship as bare GLSL pairs).
- Files changed by this agent: exactly the three new files above plus
  this report. No shared registration file needed an edit: the pattern
  registers itself from inside DbaShaders on first use.
- Javac audit only. No Gradle claim, no in-game claim.
