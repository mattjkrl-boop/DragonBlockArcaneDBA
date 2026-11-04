# No-Shapes Report: Lava Sword

Owner pass for the Lava Sword effect presentation, converted under
audits/NO-SHAPES-STANDARD.md from the Lava Sword rows of
audits/no-shapes-audit.md. Files edited: only the five Lava renderer
classes under src/client/java/com/dragonblockarcanedba/client/render
(LavaBlobRenderer, LavaCleaveRenderer, LavaEruptionRenderer,
LavaGeyserRenderer, LavaMeteorRenderer). No entity, item, block, block
entity, LavaField, registry, or shared toolkit file was edited. No
magma_crust or mini_volcano blockstate, model, or texture resource file
was edited; those resources belong to the separate Lava Block Art pass.

## Geometry counts: before and after

Counts are BespokeGeometry builder calls (B), LayeredRender geometry
calls (L), and direct vertex draws (V), as inventoried in the audit.

| Effect | File | B before | L before | V before | B after | L after | V after |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| LavaBlob | LavaBlobRenderer.java | 3 | 0 | 0 | 0 | 0 | 0 |
| LavaCleave | LavaCleaveRenderer.java | 4 | 0 | 0 | 0 | 0 | 0 |
| LavaEruption | LavaEruptionRenderer.java | 3 | 1 | 0 | 0 | 0 | 0 |
| LavaGeyser | LavaGeyserRenderer.java | 5 | 0 | 0 | 0 | 0 | 0 |
| LavaMeteor | LavaMeteorRenderer.java | 5 | 0 | 0 | 0 | 0 | 0 |
| Owner total |  | 20 | 1 | 0 | 0 | 0 | 0 |

A closing grep over the five files for BespokeGeometry, LayeredRender,
KiRenderHelper, submitCustomGeometry, and addVertex returns no matches.
The renderers no longer submit custom geometry at all. Each one keeps a
per-entity emission record keyed by entity id and drives the shared
MaterialEmitters.Emitter accumulator with a delta derived from the
entity age, so emission is frame-rate independent. Every stream is
scaled by the existing FxEnvelope lifecycle and by
MaterialEmitters.distanceScale, and every Emitter carries a hard
per-second budget.

## Materials per effect

All five effects use the upgraded FLAME, EMBER, and ASH fixed particle
sets plus the DbaMaterialOptions SMOKE and VAPOR materials, tinted in
the sword's existing hot orange and dark warm brown range. Rates below
are per entity per second before envelope, distance, and density
scaling; budgets are the summed Emitter caps.

- LavaBlob (322 per second, budget 458): FLAME sampled through a small
  volume around the blob for the hot living center; EMBER sampled along
  the segment from the previous position to the current one, so the
  trail follows the real ballistic path; ASH around the body; SMOKE
  material (color 0x2E160D, scale 0.62) and VAPOR material (color
  0xB46226, scale 0.72) drifting off the cluster. The two displaced
  domes and the plume are gone; the blob is the material cluster itself.
- LavaCleave (1099 per second, budget 1430): FLAME and EMBER sampled
  across the swept area behind the current position, using a diameter
  direction derived from the real velocity, so the trail is produced by
  the swing's motion rather than drawn as an arc; ASH sampled along the
  lower half of that swept area as falling drips; SMOKE material (color
  0x35150B, scale 0.84) and VAPOR material (color 0xB46226, scale 0.92)
  across the same swept area. The three bladeArc layers and the plume
  drip are gone.
- LavaEruption (1590 per second, budget 2000): FLAME and EMBER sampled
  throughout the frontal cone volume the mechanics already use, with
  sample velocities forward and upward; ASH sampled across the cone's
  ground footprint; SMOKE material (color 0x2E130B, scale 1.10) sampled
  in the upper cone; VAPOR material (color 0xA34A16, scale 1.00)
  through the cone. The two plumes, the crackStar scorch mark, and the
  LayeredRender anchor glow are gone; the real crust pools and fire
  cells the entity places still carry the ground mark.
- LavaGeyser (1495 per second, budget 1870): FLAME and EMBER sampled
  through a rising column volume whose height follows the same growth
  curve the plume used; ASH sampled across a 2.3 block base area; SMOKE
  material (color 0x2B130B, scale 1.00) in the upper column; VAPOR
  material (color 0x9E4A18, scale 0.92) through the column. The two
  plumes, the broken vortex band, the crackStar, and the glow fan are
  gone.
- LavaMeteor (692 per second, budget 920): FLAME in a dense core volume
  and along a wake sampled over the last 3.2 ticks of the real fall
  path; EMBER and ASH along the wake; BLOCK_CHUNKS carrying the magma
  block state shed from the core, so real material fragments fall with
  it; SMOKE material (color 0x2B130B, scale 1.18) widening through the
  wake; VAPOR material (color 0xA54D18, scale 1.00) around the core.
  The displaced dome rock, the broken vortex band, and both plume
  trails are gone.

Volcano presentation: the audit lists no dedicated volcano renderer,
and none was created. The MiniVolcanoBlock is a physical block model
and stays exactly as it is. What the player sees of an eruption comes
through the converted LavaBlobRenderer for its lobbed blobs, the
converted LavaGeyserRenderer where geysers are raised, and
VolcanoBlockEntity's existing server particle sends (EMBER, ASH,
BLAST, and FLAME), which already use the mod's own particle types.
This pass needed nothing from the block resource files.

## Mechanics preservation

Presentation only. No damage number, damage type, cooldown, Ki cost,
charge time, readiness timer, duration, hitbox, movement value, crust
profile, field cap, or mechanic was changed, because no common-side
file was touched. The entities' synced data is byte-identical; the
render states only additionally read the entity id and, for the blob,
cleave, and meteor, the current velocity, both used solely to place
emission samples along the motion that already exists.

## Verification

Javac audit only, no Gradle claim. Targeted javac with JDK 25, the
five renderer files as roots, and the full sourcepath
(src/main/java:src/client/java):

- 26.2 classpath (cp262.txt): class files emitted for all five
  renderers. Zero errors naming any owned file. The only errors in the
  run are the 4 known baseline errors in DbaItems and DbaMenus.
- 26.3 classpath (cp263.txt): class files emitted for all five
  renderers. Zero errors naming any owned file. The only errors in the
  run are the 10 known baseline errors in DbaItems, DbaMenus,
  DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity, and
  PatrolRobotEntity.
- git diff --check over the five edited files: clean.
- Em dash scan over the five edited files and this report: clean.
- In-game look: unverified from this environment, as with every pass
  in this repo. Carter's PC test is the first visual judgment.

## Change requests

None for the shared toolkit. The MaterialEmitters sources, the Emitter
accumulator, and the FLAME, EMBER, ASH, SMOKE, and VAPOR materials
covered every Lava effect. No block resource change is requested from
this pass.

## Honest worse check

- The blob and the meteor no longer have a solid drawn body. They read
  as dense clusters of flame, embers, smoke, and, for the meteor,
  falling magma chunks. That is the no-shapes rule applied as written,
  but a player who liked the visible tumbling rock will feel the loss,
  most on the meteor. The meteor's chunk shedding and dense core are
  the mitigation available inside this toolkit; a detailed physical
  meteor model would be a separate modeling decision, not a renderer
  swap.
- The cleave no longer has a readable crescent silhouette or a precise
  leading edge. Its swept flame and ember field covers the same travel
  area and dies on the same envelope, but the telegraph edge is softer
  than the old bladeArc. That softness is inherent to removing the
  shape; the damage timing and hit area did not move.
- Eruption and geyser coverage is now particle density. At long range
  or at a low effects density setting, the cone and the column will
  read thinner than the geometry did, while the damage area stays the
  same. The distance scaling and budgets make that trade deliberately,
  for performance.
- Nothing was reverted. No conversion made a number, a timing, or a
  mechanic worse, and the three visual trades above are stated here
  rather than shipped silently.
