# Attack Upgrade Pass, Phase 2 Foundation Addendum: rockWall and spikeEruption

Owner-facing contract for the two new ComplexGeometry builders specced
in audits/attack-complexity-audit.md (the "new family" section). Both
builders live in client/render/ki/ComplexGeometry.java, are seeded and
deterministic like every builder in the toolkit (same seed, same
geometry, every client), draw in the caller's local pose space, and
return the number of vertices emitted so owners can check budgets.
They draw BODIES ONLY: palettes and submit types are the caller's
choice, and hot seams or cores are separate calls layered by the
owner, never part of the slab or spike bodies.

## Signatures

```java
public static int rockWall(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                           Vec3 origin, Vec3 facing, float length, float height, float arc,
                           float riseFactor, float sinkFactor,
                           int rootArgb, int tipArgb,
                           float alpha, float envelope, float distScale)

public enum ComplexGeometry.SpikeLayout { LINE, RING, SCATTER, FAN, BODY }

public static int spikeEruption(PoseStack.Pose pose, VertexConsumer buffer, long seed,
                                Vec3 center, Vec3 facing, SpikeLayout layout, int count,
                                float radius, float sizeScale,
                                float riseFactor, float sinkFactor,
                                int rootArgb, int tipArgb,
                                float alpha, float envelope, float distScale)
```

rockWall parameters:

- origin: wall center at ground level (the pose is usually already
  translated to the effect origin; pass Vec3.ZERO then).
- facing: horizontal direction the wall's front faces. The wall line
  runs along facing's right (facing cross up). Only the horizontal
  component is used.
- length: wall length in blocks. Slab count derives from it, about
  one slab per 1.15 blocks, clamped to 6 to 14 slabs; each slab ends
  up 0.7 to 1.6 blocks wide after boundary jitter and insets.
- height: nominal wall height. Individual slab tops follow a grouped
  ridge profile (attack upgrade merge), running about 0.60 to 1.24
  of it in seeded peak clusters, with seeded tilt and lean.
- arc: total bend in radians. 0 is a straight wall. Sign convention
  as shipped in the placement math (merge correction: this paragraph
  originally stated it backwards): the wall's center stays at origin
  and the ends swing, so positive swings the ends toward facing and
  leaves the center recessed (concave from the facing side), and
  negative bows the center toward facing (convex). 0.2 to 0.6 reads
  as a shallow curved front either way. With facing pointed at a
  circle's center and arc set to length over that circle's radius,
  the positive form wraps the wall exactly onto the circle (the War
  Mace rim segments, about 1.2 to 1.8, use this with the wall
  centered on the rim arc).
- rootArgb / tipArgb: base (buried) to top gradient, same convention
  as shard and fracturePlates. Alpha is literal opacity (solid
  discipline, no shader life remap), so fade the SETTLE beat through
  envelope as usual.

spikeEruption parameters:

- center: layout center. Ground layouts want ground level; BODY
  wants the body's center height (about 1.0 above the feet).
- facing: used by LINE (the row marches along it), FAN (the wedge
  opens around it), and BODY (sets the ring's phase). Horizontal
  component only.
- layout: LINE (row along facing, spread over twice radius), RING
  (circle of radius, spikes leaning outward 0.15 to 0.42 radians),
  SCATTER (disc of radius, tilts up to 0.5 radians), FAN (wedge of
  plus or minus 55 degrees around facing, distances 0.35 to 1.0 of
  radius, spikes tilted hard forward: the directional eruption at a
  hit point), BODY (radial ring at radius from the body axis, angled
  outward and up: spikes out of the space around the caster).
- count: spikes requested, clamped to at most 14 per call, then
  distance scaled (minimum 3).
- radius: layout radius as above.
- sizeScale: multiplies the length classes. Lengths roll about 55
  percent at 1.2 to 1.9 blocks, 30 percent at 1.9 to 2.6, 15 percent
  at 2.6 to 3.2, times sizeScale. Girth is 0.15 to 0.23 of length.
  Use sizeScale below 1 for the modest specs (Whis glass ring about
  0.7, Dabura rim about 0.85).

## Animation parameter contract (RISE, HOLD, SLAM or SETTLE)

Both builders take two plain 0 to 1 progress factors and do all
easing and translation internally. Owners never move geometry per
beat; they only move the factors on the host's existing envelope.

- riseFactor: 0 means fully buried (the call emits nothing), 1 means
  fully emerged. Eased ease-out inside the builder, so feeding a
  linear age fraction over the beat's ticks gives fast emergence and
  a soft arrival. Ground spikes carry a seeded per-spike stagger of
  up to 0.35 of the rise, so rings and lines erupt in sequence; the
  wall drops uniformly, so taller slabs break the surface first.
- sinkFactor: 0 means no sink, 1 means fully retracted or buried
  again. Eased ease-in inside (slow start, accelerating drop), which
  reads as a slam when run over 3 to 4 ticks and as a settle when run
  slower under a falling envelope.
- HOLD is riseFactor 1, sinkFactor 0. Keep both factors at HOLD for
  the host's hold ticks; nothing else to do.
- BEAT RECIPES (from the audit): RISE over 6 to 10 ticks for walls
  (spikes 4 to 6, Whis glass ring stretched over the rift's first 20
  ticks), HOLD 2 to 6 ticks, SLAM over 3 to 4 ticks for walls,
  spikes either retract through sinkFactor or hold at sinkFactor 0
  and crack away through the envelope while the host lives.
- BODY spikes emerge along their own axis instead of vertically, so
  the same two factors drive the charge beat: rise as the charge
  builds, hold at full charge, sink on release.

Worked factor math, Ox style (fissure age in ticks, wall rise 8,
hold ends at 12, slam 3):

```java
float rise = FxEnvelope.smoothstep(Math.min(1f, age / 8f));
float sink = age <= 12f ? 0f : FxEnvelope.smoothstep(Math.min(1f, (age - 12f) / 3f));
```

## Measured vertex budgets

Counted from the emission loops (tri emits 6, quad emits 8, both
double-sided), at distance scale 1.0:

| Builder | Per piece | Per call |
|---|---|---|
| rockWall | 72 vertices per slab (8 triangles + 3 quads) | 6 slabs: 432; 10 slabs: 720; 14 slabs (cap): 1008 |
| spikeEruption | 384 vertices per spike (64 triangles: 8 root fan, 48 band, 8 tip fan) | 5 spikes: 1920; 7 spikes: 2688; 11 spikes: 4224; 12 spikes: 4608; 14 spikes (cap): 5376 |

Budgets against the standard: a class L submit holds 6000 piece
vertices total. A full 14 spike eruption (5376) nearly fills a class
L submit on its own, so pair big spike calls with light companions
(a crackFan at 900 or sprite layers) or drop the count; the audit's
spike counts (5 to 14) were written against these numbers. Walls are
cheap: even the 14 slab cap (1008) leaves most of a class L budget
for the plates, cracks, and debris the wall beats already carry.

## Worked example 1: Ox King's Ax fissure wall

From the audit, section 15: a rockWall of 9 to 12 slabs erupting
along the fissure line, RISE staggered over the fissure run, HOLD 4
ticks at 2.2 to 3.0 blocks, then the whole wall slams down flat with
a full-length crackFan and a DebrisSpawner burst (owner-side calls,
unchanged fissure timing and damage underneath). The renderer pose
sits at the fissure origin on the ground; facing is the fissure's
travel direction, so the wall's front faces the travel side and the
wall line runs along the fissure path.

```java
// Inside the fissure renderer's submit, pose at the fissure origin.
float rise = FxEnvelope.smoothstep(Math.min(1f, age / 8f));
float sink = age <= holdEnd ? 0f : FxEnvelope.smoothstep(Math.min(1f, (age - holdEnd) / 3f));
ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
    ComplexGeometry.rockWall(pose, buffer, seed,
        Vec3.ZERO, fissureDir, 12.0f, 2.6f, 0.0f,
        rise, sink,
        groundRootArgb, groundTipArgb,
        alpha, envelope, distScale));
```

Slab count lands at 10 for length 12 (12 / 1.15, inside the audit's
9 to 12). Palette is the ground-sampled stone pair the fissure
already uses for its plates, drawn lit: the wall is physical rock.
No energy insets for Ox; the slam's crackFan and debris burst are
separate calls the owner already makes.

## Worked example 2: Devil Trident brimstone spike ring

From the audit, section 13: a spikeEruption ring of 10 to 12
brimstone spikes between the fracture plates and the crack web on
the slam beat, RISE 6 ticks staggered by ring position (the
builder's built-in per-spike stagger does this), HOLD while the seal
holds, then crack and settle as fading pieces (sinkFactor stays 0;
the envelope fall does the settle while the spikes stand). Ring
radius is 0.90 of the seal radius, matching how the plates are sized
inside the seal on purpose. Palette: blackstone root to basalt tip,
drawn lit. The magma seam is a separate thin energy call, here the
slam's existing crackFan in magma colors between the spikes.

```java
// Inside the slam renderer's submit, pose at the slam center.
float spikeRise = FxEnvelope.smoothstep(Math.min(1f, age / 6f));
ComplexGeometry.submitSolidLit(collector, poseStack, state.lightCoords, (pose, buffer) ->
    ComplexGeometry.spikeEruption(pose, buffer, seed ^ 0x5917E,
        Vec3.ZERO, slamFacing, ComplexGeometry.SpikeLayout.RING, 11, sealRadius * 0.90f,
        1.0f, spikeRise, 0f,
        0xFF1A1216, 0xFF4A4038,
        alpha, envelope, distScale));
// Magma seams stay a separate energy layer, never the spike bodies:
ComplexGeometry.submitEnergy(collector, poseStack, (pose, buffer) ->
    ComplexGeometry.crackFan(pose, buffer, seed ^ 0x5EA3,
        sealRadius * 0.92f, magmaHotArgb, magmaCoolArgb,
        alpha, envelope, distScale));
```

(The seed suffixes above are illustrative placeholders in the doc;
owners use their renderer's existing seed constants.)

## Rules owners inherit

- Physical palettes (stone, basalt, obsidian, iron, crystal, glass)
  go through submitSolidLit with the render state's lightCoords,
  exactly like FragmentRenderer and the Grand Blade Shard after the
  Phase 1 glow fix. A new physical piece on the emissive path is a
  merge-gate failure, same as a damage change.
- The builders are submit-agnostic: the same call works inside
  submitSolid or submitEnergy when a palette is genuinely energy
  (SkyCracks curse glass keeps its emissive read that way, per its
  audit spec). The choice is the palette's, made at the submit call.
- No damage, cooldown, cost, timing, mechanic, or synced field
  changes ride along with these shapes. The wall rises where the
  slam already lands; the spikes erupt where the hit already
  happens. If a spec's beat does not exist yet, the shape does not
  invent one: flag it in the owner report instead.
