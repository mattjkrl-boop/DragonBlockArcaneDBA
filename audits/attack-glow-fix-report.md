# Attack Upgrade Pass, Phase 1: Launched-Block Glow Fix Report

Carter's playtest correction (Nov 4, 2026): "I dont want the glowing
applied to the blocks launching and that happens a bit too much."
Base: HEAD 8594bf8. This report covers the code fix only; the attack
complexity audit is audits/attack-complexity-audit.md. No damage,
cooldown, cost, timing, mechanic, or synced field changed anywhere in
this fix. Presentation only. No Gradle build exists in this VM; none
was run or is claimed. The in-game look is unverified until Carter's
PC test.

## Root cause

The launched blocks themselves were being drawn on the wrong render
path, and the physical shard entities were drawn on an emissive path.
Three findings, in the order the brief named the suspects:

### 1. DebrisEntity: the item/display path stamps one flat light on the whole block (CONFIRMED, the main cause)

client/render/DebrisRenderer.java at 8594bf8 submitted the real block
model through BlockModelRenderState.submit with the entity render
state's single packed lightCoords sample (the path vanilla uses for a
carried block or a block display). Verified against the 26.2 client
jar: BlockModelFeatureRenderer, which consumes that submit, calls
QuadInstance.setColor and QuadInstance.setLightCoords with the one
submitted value for every quad. There is no light-engine sampling, no
ambient occlusion, and no cardinal (per-face directional) shading on
that path. A placed block gets all three from the terrain renderer.

So a tumbling chunk in flight took one light sample at the entity
position (outdoors, full sky brightness) and wore it uniformly on
every face: top, sides, and underside all equally bright, no edge
darkening, no face shading. A uniformly lit solid with no shading
structure reads as lit from within, which is exactly what Carter saw.
It "happens a bit too much" because DebrisSpawner.spawnField has more
than 20 call sites (storms, slams, rifts, meteors, whirlwinds): the
mod launches block chunks constantly.

History: the renderer originally used the terrain path (vanilla
FallingBlockRenderer style). Bespoke pass Phase 1 (commit 2ef870b)
replaced it with the display path and documented the trade in the
class comment ("per-quad light engine sampling at the block position
is replaced by the entity's packed light coords"). That trade is the
bug. The FallingBlock path was correct.

### 2. FragmentEntity and the Grand Blade shard: physical shards drawn full bright (CONFIRMED, second cause)

client/render/FragmentRenderer.java drew every fragment through
ComplexGeometry.submitSolid, which submits on
KiRenderHelper.kiRenderType() = RenderTypes.entityTranslucentEmissive,
and KiRenderHelper.vertex stamps every vertex with
.setLight(FULL_BRIGHT) (0xF000F0). Emissive type plus full-bright
vertex light means world lighting is ignored completely: a steel,
crystal, or glass fragment reads at maximum brightness in daylight,
at night, and in a cave, and keeps "glowing" after it lands and rests
on the ground. GrandBladeShardRenderer had the same defect on its
steel shard body (both the flying and the embedded submitSolid calls).

### 3. Glow accompaniment at launch sites (CHECKED, not a cause)

- fx/SlamDebris.java throws BLOCK_CHUNKS particles, which
  DbaParticleProviders registers as vanilla TerrainParticle: textured
  from the real BlockState and lit by the world. Not a cause.
- SlamDebris's DUST_PUFF cloud is a non-additive DbaGlowParticle style
  with world light. Not a cause. Its EMBER and SPARK accents are the
  slam families' own energy, allowed to stay by the pass rules.
- DebrisEntity's landing puff is DUST_PUFF, world-lit. Not a cause.
- Host compositions (slam and storm renderers) do draw plasma and
  additive energy at the same centers where debris fields launch.
  That is the weapon's own burst, not light applied to the blocks,
  and it stays. Once the blocks themselves are world-lit, the burst
  reads as a burst again instead of dyeing the chunks.
- LavaField crust launches (entity/LavaField.java lines 496 and 777)
  use BLOCK_CHUNKS TerrainParticles: world-lit. Not a cause. Crater
  rims and settled crust are real placed blocks in the world: world
  lighting by construction. Not a cause.

## The fix

One rule, applied everywhere a launched physical piece is drawn:
physical matter uses world lighting.

### DebrisRenderer: back on the terrain path

client/render/DebrisRenderer.java now renders through
MovingBlockRenderState and SubmitNodeCollector.submitMovingBlock,
the vanilla FallingBlockRenderer path, identical in shape to the
pre-bespoke renderer and to the in-tree FlyingNimbusRenderer:
extractRenderState fills blockPos and randomSeedPos from the piece's
position, blockState from the synced block, and biome,
cardinalLighting, and lightEngine from the ClientLevel. The terrain
block renderer (MovingBlockFeatureRenderer with ModelBlockRenderer,
verified in the 26.2 jar) then samples the world light engine per
vertex at that position, with ambient occlusion and cardinal shading.
The API is byte-identical on the 26.3 jar (MovingBlockRenderState
field set verified by javap on mc263.jar). Scale, tumble, grow-in,
and landed-fade behavior are unchanged from 8594bf8; only the submit
path and the state plumbing changed. The class comment records the
history and the rule so the trade is not reintroduced.

What the blocks look like now, per the code path: exactly like the
same block placed in the world at that spot. Bright on top in
daylight, shaded on the sides, dark underneath, dark at night, warm
near lava and torches, biome tint applied. Nothing about them can
read as self-lit, because nothing in the path can produce light the
world did not already have there.

### A world-lit solid path for physical shards

- client/render/ki/KiRenderHelper.java: new kiLitType() returning
  RenderTypes.entityTranslucent(WHITE_TEXTURE) (the lightmapped
  translucent entity pipeline; the emissive kiRenderType is untouched
  for energy). vertex() now stamps a draw light that defaults to
  FULL_BRIGHT, and a new withLight(lightCoords, draw) swaps in a
  caller-supplied packed light for exactly one draw call. The swap
  happens inside the wrapped draw callback, which the feature
  renderer runs synchronously when it consumes the submit, so the
  hand-off is correct even though submits are deferred.
- client/render/ki/ComplexGeometry.java: new submitSolidLit(collector,
  poseStack, lightCoords, piece). Same builders, same faceted vertex
  shading as submitSolid; only the render type and the vertex light
  differ. submitSolid itself is unchanged, so every energy piece in
  the game is byte-identical.
- client/render/FragmentRenderer.java: the fragment shard now goes
  through submitSolidLit with the render state's packed lightCoords.
  All seven hosts' fragments (SkyCracks rain, Temporal Rift, Dimensional
  Rift, Void Rift ring, Warp Rift, TimeShatter fan, Valor Field dome
  and markers) keep their exact theme colors, silhouettes, growth,
  and fades; the world now decides how bright they read. A fragment
  resting on the ground at night is dark. A glass fragment in
  daylight still reads as bright glass, because the world is bright.
- client/render/GrandBladeShardRenderer.java: both steel shard body
  calls (flying body and embedded body) switched to submitSolidLit
  with the state lightCoords. The gold tendril wake stays on
  submitEnergy: the wake is the sword's valor energy, the steel is
  not.

## What deliberately did NOT change

- TridentShardRenderer: the trident shard body stays on submitSolid.
  Its authored identity is a hellfire fragment, dark shell to hot
  core, with a flame wake; the heat is the material, not world light
  being ignored by accident. Flagged here so the call is visible: if
  Carter wants the metal shell world-lit with only the core hot, that
  is a Phase 2 composition change, not a bug fix.
- LavaBlobRenderer, LavaMeteorRenderer, LavaGeyserRenderer: molten
  rock and magma. Lava is emissive in Minecraft itself; a meteor's
  crust going through submitSolid over a flame tongue body is the
  material being lava. Unchanged.
- Renderer-drawn fracture plates and crack fans in slam compositions
  (DevilSlamShockwaveRenderer, KingsSlamRenderer, WarMaceSmashRenderer,
  and kin) still draw through submitSolid and submitEnergy. They are
  transient impact composition attached to the slam, not launched
  blocks, and several are magma or brimstone by theme. They are named
  here as the honest remainder: if Carter still sees ground plates
  reading too bright in the PC test, the same submitSolidLit path is
  ready for them, but re-lighting every slam composition is a Phase 2
  call, not something to slip into a bug fix.
- Every other submitSolid caller is unchanged. Energy and magic
  pieces keep glowing; that was never the complaint.

## Verification

- Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8, sourcepath
  src/main/java:src/client/java) over the five edited files
  (DebrisRenderer, FragmentRenderer, GrandBladeShardRenderer,
  KiRenderHelper, ComplexGeometry) on both classpaths
  (~/workspace/dba-audit-libs/cp262.txt and cp263.txt): zero errors in
  the edited files on both. The only errors in the compile closure are
  the standing baseline kinds (DbaItems, DbaMenus, and on 26.3 the
  hostile mob set), identical to the 8594bf8 baseline. No full sweep
  was run; the merge runs it.
- API evidence: MovingBlockRenderState fields and
  RenderTypes.entityTranslucent(Identifier) verified present with
  identical shapes in the 26.2 client jar and mc263.jar via javap.
  FlyingNimbusRenderer already used submitMovingBlock at HEAD, so the
  collector method is proven in-tree on both versions.
- git diff --check clean. Added-lines em dash scan: zero.
- No Gradle build run or claimed. In-game look unverified until
  Carter's PC test; the specific thing to look at is a debris field
  at night and a landed fragment resting in shadow.
