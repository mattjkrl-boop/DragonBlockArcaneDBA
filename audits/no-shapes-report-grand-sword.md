# No-Shapes Realism Pass Phase 2 Report: Grand Sword

Scope: the four Grand Sword renderers named in audits/no-shapes-audit.md
(GrandBladeShardRenderer, GrandClashSparkRenderer,
GrandCrescentWaveRenderer, ValorFieldRenderer). Converted at working
tree state on top of commit ad22b32. Not committed, per instructions.

Replacement-agent note: the previous Grand Sword agent died to an
infrastructure error mid-work. Its partial diff covered all four
renderers above (ValorFieldRenderer included, even though its name has
no "Grand" in it). I audited that diff file by file against HEAD, the
binding standard, and the audit assignments before finishing it. The
conversion in all four files was complete and coherent: no
half-converted file, no leftover geometry call, no broken reference was
found, and no part of it had to be reverted or rewritten. What I did on
top of the partial diff: verified every entity API and MaterialEmitters
signature it calls exists at HEAD with matching shape, verified every
lifecycle number against the HEAD renderers, ran the geometry greps,
and ran the dual javac verification below. GrandSwordItem and the four
Grand/Valor entities are unmodified in the tree, so mechanics are
untouched by construction.

## Before and after geometry counts (per audit row)

| Effect | BespokeGeometry before | LayeredRender before | Vertex before | After (all three) |
| --- | ---: | ---: | ---: | ---: |
| GrandBladeShard | 8 | 0 | 0 | 0 |
| GrandClashSpark | 8 | 0 | 0 | 0 |
| GrandCrescentWave | 4 | 0 | 0 | 0 |
| ValorField | 9 | 2 | 0 | 0 |
| Owner total | 29 | 2 | 0 | 0 |

After-counts verified by grep over the four files: BespokeGeometry 0,
LayeredRender 0, addVertex 0, KiRenderHelper 0, .vertex( 0. The
renderers draw no geometry at all; they only emit material particles
through the shared client/fx/MaterialEmitters toolkit (Preset, Source,
Emitter with per-second budgets, distance scaling via
MaterialEmitters.distanceScale, and the Emitter accumulator pattern).
Each renderer keeps a small stream table keyed by entity id so a moving
entity's emitter follows it; that table wraps the shared Emitter and is
bookkeeping, not a private particle system. No new texture, particle
type, or geometry helper was created.

## Materials per effect (theme: valor plasma, steel sparks, stone dust)

GrandBladeShardRenderer:
- Flying: dense valor plasma core (0xFFD833) emitted at the shard
  position with a slight counter-velocity so the cluster trails the real
  flight path, steel SPARK particles in a small volume around it, and
  stone dust (0x8A7D68) shed in the same volume.
- Embedded: ground dust across a 0.95 radius ground area, valor plasma
  rising through a volume above the impact point, and sparse pale vapor
  (0xFFE9A8) as the hazard shimmer that replaces the old beacon plume.

GrandClashSparkRenderer:
- White-gold valor plasma (0xFFF3B0) burst in a growing sphere volume,
  steel SPARK particles thrown outward, pale vapor (0xFFE9A8), and a
  little stone dust. The burst volume grows with the same scale curve
  the old spike star used.

GrandCrescentWaveRenderer:
- A moving front of valor plasma (0xFFD833) sampled across the wave's
  real span and chord (span 4.8, chord 1.55, carried verbatim from the
  old crescent), steel SPARK particles inside the front, pale vapor
  trailing 1.05 behind the front, and a white plasma surge stream that
  only runs during the opening strike window. Sampling positions are
  data from a Source; no arc, ribbon, or crescent mesh is drawn.

ValorFieldRenderer:
- A living volume of valor plasma motes (0xFFD833) filling the field's
  sphere volume on its old height profile, sparse large pale vapor
  sprites sampled on the boundary surface (positions only, no shell),
  stone ground dust across the base disc area, and sparse steel sparks
  through the volume. Suspended projectiles are marked by dense
  white-gold plasma motes sampled around their actual positions, using
  the entity's own synced suspended id set (getSuspendedIds), exactly
  as the old cage placement did.

## Identity preserved (verified against HEAD by diff and grep)

- Shard: build-in over 3 ticks, flight fade over the last 10 ticks,
  embedded fade over the last 30 ticks, embedded pulse
  0.85 + 0.15 sin(age x 0.25). All formulas carried verbatim.
- Clash: lifecycle envelope fractions 0.20 / 0.45 and the growth curve
  scale = clashScale x (0.55 + grow x 1.25) carried verbatim.
- Wave: envelope fractions 0.04 / 0.35, the 12 tick hit dissipate fade
  (DISSIPATE_TICKS), the opening strike window (first 16 percent of
  life, sine profile), span 4.8 and chord 1.55 with the same
  0.80 + 0.20 open factor, and the first-person early scale-in
  (0.55 rising to 1.0 over the first 6 ticks).
- Field: 10 tick spawn rise with the dome height profile
  radius x 1.05 x (0.22 + 0.78 x rise), fade out over FADE_OUT_TICKS,
  pulse 0.88 + 0.12 sin(age x 0.18), radius from FIELD_RADIUS (9.0).
- Colors: valor gold 0xFFD833, white-gold 0xFFF3B0, pale vapor 0xFFE9A8,
  stone dust 0x8A7D68 throughout, matching the bespoke palette.
- No damage number, damage type, cooldown, Ki cost, timing, hitbox,
  synced field, or mechanic changed: the item and entity files are
  untouched, and the render states carry the same fields plus entityId
  for stream keying.

## Verification

Targeted javac (JDK 25, sourcepath over src/main/java and
src/client/java, the four owned renderers) on both audit classpaths:
- cp262: 0 errors in owned files. 4 errors total, all in non-owned
  DbaItems.java and DbaMenus.java pulled in as dependencies (the known
  pre-existing artifacts).
- cp263: 0 errors in owned files. 10 errors total, all in non-owned
  files (DbaItems, DbaMenus, DbaHostileEntity, FriezaSoldierEntity,
  NamekianWarriorEntity, PatrolRobotEntity).
No error references a Grand Sword or Valor Field symbol. This is a
javac audit only; no Gradle build was run or claimed.

## Toolkit gaps (shared change requests)

None new. The bespoke pass Grand Sword change requests (containment
cage builder, displacedDome zenith color, glyph builder, faceted blade
builder) are moot under this standard: the cage, dome, glyphs, and
blade mesh are gone by design rather than approximated.

## Honest worse check

- Blade shard body: the shard no longer has any object read at all. It
  is a plasma, spark, and dust cluster in flight and a dust and plasma
  hazard patch when embedded. That is what the audit row orders (no
  shard mesh; the shard was a glowing mesh, not a detailed model), but
  anyone expecting to see a sword fragment will not. If Carter wants a
  physical fragment back, it needs a detailed model, which is a
  separate asset job, not a renderer change.
- Clash burst reach: the old spike star reached radius 3.5 x scale at
  full growth. The new burst volume tops out near 0.86 x scale at the
  source, with spark and plasma velocities carrying material outward
  from there. The burst reads denser and more physical, but its
  widest silhouette is smaller than the old star's.
- Wave silhouette: the old crescent had a crisp curved leading edge.
  The new front is a noisy plasma band across the same span and chord;
  it reads as energy in motion rather than a blade edge. That is the
  standard's intent for slashes and waves, but the clean arc read is
  gone.
- Valor Field boundary: the vapor boundary sprites suggest the field's
  edge statistically rather than drawing it, so at a glance the exact
  9 block radius is less legible than the old dome. The mote volume and
  ground dust still fill and mark the same footprint.
- Nothing else regressed: every lifecycle timing, envelope, radius,
  color, and mechanic is identical, and emission is budgeted per
  stream (72 to 520 per second caps) with distance scaling, so the
  conversions do not depend on unlimited particle counts.
