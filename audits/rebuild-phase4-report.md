# Rebuild Phase 4 report: the primitive geometry kit

Status: landed and pushed. HEAD before this phase: be51845
(Phase 3). Verification in this report is the dual javac sweep
plus a runtime harness that executes the real builders; there is
no Gradle build in this VM (waived) and the in-game look is
unverified until Carter's PC test, same as every phase.

## What Phase 4 built

One kit package, client/render/kit/, holding eight seeded,
deterministic, parameterized builders plus the shared emission
helper. Renderers no longer hand-roll ability geometry: they
read the definition's new geometry block and call the kit.
Everything is presentation. No damage, cost, cooldown, timing,
or carrier behavior changed anywhere in this phase; the only
main-source edit is the schema gaining the geometry block.

### Kit inventory (documented budget, and what the harness measured)

All builders emit through KitEmit over KiRenderHelper.vertex,
so the world-lit hand-off behind submitSolidLit applies to kit
pieces exactly as to the older ComplexGeometry builders.
Physical variants are world-lit only; energy variants are
additive with white-hot core options. Counts below were measured
live in the harness by counting vertices at the consumer, not
just read off the builders' return values (the two agree).

- KitEmit: shared vertex emission (rgb/mix/shade helpers, a
  double-sided quad and a single-sided quad). No budget of its
  own.
- CrescentBand: the wave band. The Phase 1 three-strip profile
  lives in tables (rim 0.75 to mid 0.90 over the bottom 35
  percent, mid 0.90 to core 1.00 to 68 percent, core hard edge
  0.55 dissolving to a transparent rim top), and turbulence
  displaces each column's radius (plus or minus 5 percent) and
  top edge (plus or minus 16 percent, scaled up the column so
  lower strips lean with the tear). At turbulence 0 the emitted
  vertices are the Phase 1 band's, position for position and
  color for color. Budget 24 per segment: measured 720 at the
  wave's 30 full-detail segments, at turbulence 0 and at 0.45;
  ceiling 1152 at 48 segments.
- VortexBand: a raised ring of torn energy slabs, the Saturn
  ring form. Per slab: seeded gap test (about a sixth of slots
  stay empty), span, radius, and height jitter, tangential
  top-edge tilt, facet shade, and a core-tipped minority. Budget
  24 per slab: measured 552 for the charge ring's 26 slabs at
  the harness seed (three slabs fell in gaps); ceiling 624 at
  26 slabs, 1152 at the 48 slab caller cap.
- DomeShell: hemisphere layers only; the old cloud shells were
  full spheres whose lower half sat under the ground plane and
  never read, so the above-ground picture is unchanged while
  emitting roughly half the vertices. Layers nest at fixed
  scales with fixed base alphas (mid 0.10 at full radius, rim
  0.07 at 0.8, core 0.05 at 0.62). swirlSpeed turns on scrolling
  alpha bands that rotate with age (the second layer
  counter-rotates), rimGlow mixes vertex color toward the core
  and lifts brightness toward the equator band. At swirl 0 the
  modulation factor is exactly 1 and at rimGlow 0 the color is
  the flat layer color, so the defaults are the Phase 1 to 3
  shell. Budget 4 per grid cell per layer: measured 1344 for two
  layers at the cloud's 28 slices by 6 rings, 672 for one layer;
  ceiling 2016 at three layers.
- TornWall: clustered rock slabs on a bowed line, grouped ridge
  profile from low-frequency noise (peaks cluster in groups of
  two to four slabs instead of alternating), per-slab ground
  following through a caller-supplied height sampler, crest
  jitter, facet shades per face. World-lit through submitSolidLit
  only; it is physical matter. Budget 32 per slab: measured 384
  at 12 slabs; ceiling 512 at the 16 slab caller cap. No consumer
  yet (Phase 6 walls).
- ChainRun: instanced flat-ring links along a catenary between
  two anchors, link count from span and link size, alternating
  90 degree roll per link with seeded jitter, per-sector facet
  shade. Budget 64 per link: measured 1472 (23 links) on a
  10-block span at link size 0.55; ceiling 3072 at 48 links. No
  consumer yet (Phase 6 chains).
- CrackDecal: emissive crack fan flat on the ground with the
  crack count as an explicit parameter, the direct answer to the
  old crackFan gap (its branch count was never exposed). Each
  crack walks five segments with seeded angle wander and a width
  that pinches at seeded points; about a third throw one fork
  from the second joint. Budget 40 per crack plus 16 for a fork:
  measured 456 at 11 cracks; ceiling 896 at 16 cracks. No
  consumer yet (Phase 6 ground scars). Physical fracture stays
  ComplexGeometry.fracturePlates on the lit path.
- BeamDrawer: an entity-less beam between two points: a seeded
  wiggle chain (two sine fields animated by age, pinned to zero
  at both ends), drawn as crossed-ribbon core and halo shells
  with an end taper. Budget 32 per path segment: measured 448 at
  14 segments on a 12-block beam; ceiling 512 at 16 segments. No
  consumer yet (Phase 6 beams).
- PlateCasing: four flat sabot plates around a projectile's
  flight axis in two length classes with a shingle stagger, the
  direct answer to the flat plate builder gap. Budget 16 per
  plate, 64 total: measured 64, against the shard body's 288.

Determinism is harness-proven: CrescentBand, VortexBand, and
ChainRun emitted into a position-hashing consumer produce
identical streams for identical seeds, and CrescentBand's stream
differs under a different seed.

### Schema additions

AbilityDef gains an optional top-level "geometry" block, parsed
into a GeometrySpec record with one sub-record per surface:

- wave: turbulence (0 to 1, default 0), baseRing (boolean,
  default true).
- cloud: shellLayers (1 to 3, default 2), swirlSpeed (0 to 4,
  default 0), shellAlpha (0 to 2, default 1), rimGlow (0 to 2,
  default 0), strikeHueSplit (boolean, default false).
- charge: raisedRing (boolean, default false), ringHeight
  (0.2 to 4, default 1.1), ringWidth (0.05 to 1, default 0.22).
- projectile: casingForm ("shard" or "plate", default "shard").

Validation follows the house pattern: unknown sub-blocks,
unknown fields inside a sub-block, wrong types, and
out-of-range values are all rejected with an
IllegalArgumentException naming the exact field
(geometry.wave.turbulence, geometry.charge.raisedRing, and so
on), which skips that one ability at load. GeometrySpec.DEFAULT
is the all-defaults record, and the defaults reproduce the
Phase 1 to 3 rendering exactly, so every other ability,
present or future, and any datapack ability without the block,
renders as it always has.

### Integration points

- WaveCarrierRenderer.drawBand: the three inline arcBand strips
  are replaced by one CrescentBand.draw call fed from the
  definition (seed, facing, radius, height, arc, segment count
  through the same FxEnvelope discipline, turbulence from data).
  The flat ground ring at the band's base now draws only when
  wave.baseRing is set (default set).
- CloudCarrierRenderer.drawDomain: the two drawSphere veil calls
  are replaced by one DomeShell.draw call (layers, swirl, alpha
  scale, rim glow from data; slice count still distance-scaled).
  Rim rings, ambient lightning, telegraph, and the shed are
  untouched. In drawTelegraphAndStrike, when strikeHueSplit is
  set, the synced strike bolt's glow color takes the rim hue on
  odd strike seeds and the mid hue on even ones; the seed is the
  strike's already-synced seed, so all observers agree.
- ChargeRingRenderer.submitRing: when charge.raisedRing is set
  and the viewer is inside full-detail distance, a VortexBand of
  26 slabs draws at radius 1.02 times the ground ring's, lifting
  from 20 percent to 100 percent of ringHeight and brightening
  as the charge fraction grows, turning with the vortex spin.
  Its seed is the caster's UUID hash, shared by every observer.
  The ground ring, dashed arcs, and full-power pulse are
  untouched, and abilities without the option draw exactly the
  Phase 2 ring.
- ProjectileCarrierRenderer.drawBody: when casingForm is
  "plate", the casing body swaps from ComplexGeometry.shard to
  PlateCasing on the same material path the casing names (lit
  through submitSolidLit, energy through submitEnergy). The
  inset hot core shard and the wake are unchanged.

### The burst() decision

Phase 3 left open whether the projectile carrier's server-side
burst() impact flash should fold into the recipe/kit
presentation. The code settles it: there is no such flash.
burst() is a damage-only method (AoE application on hit) in the
current tree and at the Phase 1 and Phase 2 heads, and no
sendParticles call exists anywhere in the projectile hit path;
MaterialEmitters has no callers in the ability sources. The
visual impact flash already rides the Phase 3 route: the
recipe-driven carrier_hit seeded packet fires on the same tick
as the hit. Decision: leave burst() exactly as it is. The
recipe system owns the projectile's impact presentation, and
there was nothing to migrate. The Phase 3 wording is corrected
here so Phase 5 and 6 do not chase it again.

## Proof attack upgrades

Both upgrades are data plus kit. No weapon-specific code exists
or was added.

### Z Shockwave (z-sword.png): PARTIAL-STRONG becomes STRONG

Data: geometry.wave.turbulence 0.45; geometry.charge
raisedRing true, ringHeight 1.15, ringWidth 0.26.

What changed against the picture set: the charge, which was the
named remaining gap, now carries the picture's Saturn ring for
real: a raised ring of torn gold slabs at waist height coiling
around the caster, lifting and brightening as the charge grows,
over the existing flat vortex rings. The wave wall itself now
tears: its silhouette breathes and its top edge rips by up to 16
percent of its height instead of ending in a clean gradient
sheet, which was the card's turbulent-wall read. Color language
(white core, gold body, orange rim), scale, ground anchoring,
and the charge-release choreography were already strong and are
unchanged.

Honest remainder: the wave still leaves no scorched glowing
path behind it and throws no enemies; those are carrier
behaviors (a ground decal on the wave's path, knockback), not
renderer options, and CrackDecal now exists as the decal
primitive if Phase 6 wants the path. The band's surface is
still a vertex-color gradient, not a scrolling texture.

### Cursed Tempest (cursed-blade.png): PARTIAL becomes PARTIAL-STRONG

Data: geometry.cloud shellLayers 2, swirlSpeed 1.2, rimGlow
0.7, strikeHueSplit true.

What changed against the picture set: the dome is now a
swirling read instead of two static translucent spheres:
scrolling alpha bands rotate through the shell (the inner layer
counter-rotates), and the shell brightens toward its equator
rim in the core color, which gives the domain a lit edge
against the world. The card's signature two-hue lightning is
now in the bolts themselves: strikes alternate violet and
crimson by their synced seed, where before crimson showed only
in the rim ring, dome tint, and telegraph. Domain scale,
telegraphed strikes, forked interior lightning, and the dark
violet palette were already the strong parts and are unchanged.

Honest remainder: the storm still does not lift or drag enemies
(a carrier movement behavior, Phase 6's call), the dome is a
shell with scrolling alpha rather than a volumetric cloud mass
(true volume is beyond a shell primitive), and the card's rain
still reads here as drifting dark motes from the recipes.

## Vertex budgets at full tilt (measured where the harness could)

- Cursed Tempest, domain at full radius, full detail: DomeShell
  1344, two ground rim rings 640 (two 40-segment flat rings at 8
  vertices per segment), for 1984 vertices of steady geometry;
  about 2240 while a telegraph is up (ring and pillar, Phase 3
  accounting), plus one branchedLightning call for an ambient or
  strike bolt on the frames one fires, inside that builder's
  existing budget. Particles unchanged from Phase 3: 230 per
  second of shed inside the 320 data cap, about 564 alive worst
  case.
- Z Shockwave at max charge: ground ring 320, two dashed arcs
  320, full-power pulse ring 224, raised VortexBand 552 measured
  (624 ceiling), for 1416 vertices of charge presentation
  (1488 ceiling). The fired wave is 720 of band plus 240 of base
  ring, 960 total. Charge and wave never overlap in time.
  Particles unchanged from Phase 3: about 50 alive while
  charging, about 222 alive worst case for the wave.

## Gates

- Dual javac sweeps: 120 error kinds on the 26.2 classpath and
  126 on the 26.3 classpath, exactly the strip-base rebuild
  baselines; comm -13 against kinds-rebuild-base262.txt and
  kinds-rebuild-base263.txt prints nothing on both.
- Harness (sources in /tmp/KitHarness.java, run against the
  sweep-compiled tree): 39 checks, all passed. Twelve parse the
  two proof files' geometry blocks and the defaults; two prove
  an absent or partial block fills from GeometrySpec.DEFAULT;
  eleven malformed variants are rejected naming the exact field;
  ten measure builder vertex counts at the consumer; four prove
  seed determinism.
- All 571 resource JSONs parse (the two ability files carry the
  new block; no JSON files were added).
- git diff --check clean. Byte-level em dash and en dash scan
  over every changed and new file: zero hits (the two legacy
  hits in DbaHudOverlay.java are untouched, per the standing
  rule).
- Behavior audit: the full diff was read. Server-side, only
  AbilityDef changed, and only by gaining the parsed geometry
  block; AbilityService, the three carriers, the damage path,
  and the network payloads are byte-identical to be51845. The
  two ability JSON diffs are pure geometry insertions.

A note on provenance: the kit sources were already in the
working tree when this verification pass began (written before
the session compaction), and two builders' real signatures
differ from the earlier plan text (CrackDecal draws around the
local origin with an explicit crack count and a height;
TornWall takes its arc before its height). This pass verified
the kit by executing it, and the harness is written against the
real code.

## What Phase 5 inherits

- The feedback service (synced screen shake, fog shifts, narrow
  hit-stop) is orthogonal to the kit: nothing here blocks it,
  and the cast store plus CastSyncPayload remain the natural
  hang points for cast-side feedback. Impact feedback can key
  off the same events the fx recipes already use (fire,
  carrier_hit, carrier_end), whose seeds are already synced or
  derivable on every client.
- The burst() question is closed (see above); Phase 5 should
  not spend time on a projectile impact flash migration.

## What Phase 6 inherits

- All 22 attacks become data on the carriers, the recipes, and
  now the kit. The geometry block is the per-attack shape
  language: a Phase 6 author writes turbulence, shells, rings,
  and casing forms in JSON and never touches Java.
- The primitives Phase 6 attacks will reach for most, by the
  design record: TornWall (Ox King's Ax fissure wall, War Mace
  crater rim, Z Sword gravity curtain, Lava Sword field wall,
  Grand Sword's pushing front on the Crescent Wave, Bansho Fan's
  push wall), ChainRun (Cursed Blade and Evil Spear chain
  attacks), BeamDrawer (Spirit Sword's beam, the Blaster's
  cannon beam, dimensional slash beams), CrackDecal (ground
  scars under slams and fissures, including a possible decal
  path under the Z wave), DomeShell (Dabura's eclipse domain,
  Grand Sword's Valor Field, Whis temporal shells), VortexBand
  (charge rings beyond Z, storm and domain rims), CrescentBand
  (every wave and slash), PlateCasing (Blaster bolt jackets and
  any physical projectile).
- Two behavior-side gaps the pictures ask for are Phase 6
  design calls, not presentation: wave knockback and a decal
  path (carrier additions), and cloud movement control (lift and
  drag inside a domain). The kit and schema do not need changes
  for either; the carriers would gain parameters.
- Picture verdicts to beat: Z Shockwave STRONG, Cursed Tempest
  PARTIAL-STRONG, under the relaxed guidance (signature
  silhouette, scale, color language, choreography; best effort
  with honest gaps).
