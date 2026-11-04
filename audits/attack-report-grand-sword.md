# Attack Upgrade Pass, Phase 2 Owner Report: Grand Sword

Owner slot: Grand Sword (worst offender 2). Spec:
audits/attack-complexity-audit.md section 5, builder contract
audits/attack-foundation-addendum.md. Base HEAD ccfebf2, no commits
made. Valor Field is out of scope and untouched: no changes to
ValorFieldEntity, its renderer, or the projectile stasis system
anywhere in this report's diff. Files changed: exactly three client
renderers.

## GrandCrescentWaveRenderer: the rebuild

Before: the wave's whole pieces layer was two tendril ribbons (about
770 piece vertices, class M) wrapped in plasma and vapor filler, 17
emit calls total. Nothing solid for the eye to hold.

After, in submit order:

- Pushing front (new): one rockWall call through submitSolidLit.
  Length is the wave span (4.8 at full open, so the builder's minimum
  count for the span: 6 slabs), height 0.55 times the chord (about
  0.85, a low wall), palette dark stone root 0x3F3A33 to steel tip
  0x8B939C, alpha 0.97, envelope the wave fade. The wall rides the
  existing wave entity and its exact path and speed: it is drawn in
  the same submit, on the same pose, as the skin has always been. Bow
  follows the arc parameter: the skin's spine bows 0.475 ahead of its
  chord line (bezier control at forward 0.95), so the wall origin
  sits at forward 0.475, and the wall arc is -0.8 (see change request
  1: the shipped builder's positive arc swings the slab ends toward
  facing, so the convex crescent bow is the negative value; the
  sagitta at span 4.8 and arc 0.8 is 0.47, matching the skin's 0.475).
  Animation rides existing beats only: riseFactor is the wave's own
  open factor (slabs emerge as the wave opens over its first 12
  ticks), sinkFactor runs on the existing hit beat in step with the
  hit fade. The builder's seeded height (0.70 to 1.16), tilt, lean,
  face offset, and bulge variance give the slabs their uneven,
  heaving read; nothing about the wall is a neat row, and per
  Carter's steering the count is the builder's fewest and biggest
  for the span.
- Gold insets (new): two thin tendrils through submitEnergy on the
  tendril canvas, the only energy on the front, as specced. One seam
  runs the arc at mid slab height; the second is a short offset chunk
  higher up (arc angles -0.17 to 0.27, not the full span), so the
  insets read as set into the stone. Both float 0.24 ahead of the
  slab center plane, just proud of the proudest faces (builder face
  offset plus bulge tops out at 0.21). Gold 0xFFD833 to 0xFFF3B0.
- Escort (new): six valor shards orbiting the front. A single
  shardBurst is a cone from one origin and cannot orbit, so the
  escort is six single-shard shardBurst calls through submitSolidLit
  at orbiting anchors on an ellipse in the wave's plane (radii 0.34
  of span by 0.50 of chord, centered at forward 0.35), each shard
  directed along its orbit tangent blended with a forward lean, unit
  1.15 (large shards: rolled lengths 0.35 to 1.84), steel root
  0x767F8C to gold tip 0xFFD833, the Valor Field shard palette.
  Per-shard seeds (seed xor 0xE5C0 plus i times 131) keep each
  silhouette stable while its anchor travels. Anchor angles are
  clustered, not even: offsets 0.0, 0.75, 2.35, 3.05, 4.35, 5.45
  radians (two close pairs, two singles), per Carter's steering.
  Orbit rate 0.21 radians per tick (about a 30 tick circuit). The
  escort envelope gathers with the open factor.
- Flame row (new): five flameTongues clusters through submitEnergy
  on the flame canvas along the front's top edge, stations on the
  wall arc at its top lip (up 0.30 of chord, forward 0.10), three
  tongues each, valor gold (hot 0xFFF3B0 to cool 0xFFD833). Station
  angles (-0.33, -0.15, 0.02, 0.17, 0.33) and heights (0.66, 0.46,
  0.74, 0.52, 0.60) jitter per station, so the burning lip is jagged.
- Shed (new): two material streams off the wall face, sampled at the
  wall's depth (front source with a 0.35 back offset): SHARD_CHIP at
  85 per second (budget 120) drifting back off the face, and stone
  dust (0x8A7D68) at 65 per second (budget 100).
- Unchanged: both skin tendrils byte-for-byte as the complex redo
  left them (they are the energy skin over the wall now), all prior
  emission streams and rates, the first-person opening scale, the
  envelope and hit fade math.

Piece counts: before 2 geometry calls, about 770 vertices (class M).
After 2 skin tendrils (770) plus wall 6 slabs (432) plus 2 seam
tendrils (about 440) plus 6 escort shards (1728) plus 15 flame
tongues (720): about 4090 vertices at distance scale 1.0, class L.
The class change is documented in the renderer javadoc.

## GrandClashSparkRenderer

Before: tendrilFan of 3 strands (about 1150 vertices), no solid
pieces, class M.
After: tendrilFan strands doubled 3 to 6 (about 2300 vertices), same
seeded fan axis, length, width, and colors; plus a shardBurst of 5
valor shards at the clash point through submitSolidLit (1440
vertices), axis the same fan axis, spread 55 degrees, unit 0.55
times the clash scale, steel root 0x767F8C to pale gold tip
0xFFF3B0, envelope the clash fade. Total about 3750 vertices, class
L, documented in the javadoc. All emission streams, the guarded
SPARK_STREAK opening burst, and the 24 tick lifecycle are unchanged.

## GrandBladeShardRenderer

Before (flight): one steel shard mesh through submitSolidLit (288
vertices) plus one tendril wake (about 384), about 670 total.
After (flight): the shard body is unchanged and still on the
world-lit path from the glow fix; the wake is now two braided
strands. The original strand is byte-for-byte unchanged; the second
is slimmer (width 0.19 against 0.34), counter-swayed (its control
and end sway against the first strand's phase), paler at the tip
(0xFFE9A8), alpha 0.80, drawn at 0.7 distance scale (about 270
vertices). Flight total about 940 vertices, still class M. The
embedded state (body set back in the ground plus its leak tendril)
is untouched, as are all emission streams and both burst guards.
One implementation note: the second strand's lambda needs the side
vector, which the existing code reassigns once, so a final copy
(braidSide) was introduced for capture; no behavior change.

## Rules compliance

- No damage, cooldown, cost, timing, mechanic, or synced field
  changes: the diff is three client renderers only. Wave speed,
  width, damage, and lifetime ride the untouched entity and the
  untouched pre-existing renderer math (span, chord, envelope, hit
  fade are the same expressions as before; new pieces consume them).
  Blade shard counts and swarm behavior are entity-side and
  untouched.
- Every new physical piece (wall slabs, escort shards, clash shards)
  draws through submitSolidLit only. The only submitEnergy additions
  are the gold seam tendrils and flame tongues, which are energy and
  fire, never slab or shard bodies.
- Foundation files untouched; gaps filed below as change requests.

## Verification

- Targeted javac, JDK 25 at ~/workspace/tools/jdk-25.0.5+8,
  sourcepath src/main/java:src/client/java, over the three owned
  files, full logs read line by line (no summaries):
  - cp262: 4 errors, all the documented baseline closure kinds
    (DbaItems x2, DbaMenus x2). Zero in owned files.
  - cp263: 10 errors, exactly the documented baseline set (the 26.2
    four plus DbaHostileEntity x2, PatrolRobotEntity x2,
    FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero in owned
    files.
- git diff --check on the three owned files: clean.
- Byte-level added-lines scan on the three owned files: zero em
  dash bytes, zero en dash bytes.
- No Gradle build run or claimed; the in-game look is unverified
  until Carter's PC test.
- Note for the merge: the working tree also holds the other Phase 2
  owners' in-flight changes; this report covers only the three
  Grand Sword renderers, and nothing outside them was edited here.

## Change requests (foundation, filed not fixed)

1. rockWall arc sign contradicts its own documentation. The
   addendum and the builder javadoc say positive arc bows the wall's
   center toward facing; the shipped placement math does the
   opposite (positive arc puts the ends toward facing and recesses
   the center; the convex bow is negative). Owners have to derive
   the sign from the code, as this one did. Reconcile doc or math at
   the foundation level before the next wall owner trips on it.
2. rockWall slabs stand world-vertical with no tilt toward a host's
   up vector. Fine for ground walls and level shots; a pitched host
   cannot carry its wall with it. An optional facing-basis tilt
   would close this if a future spec needs it.
3. There is no escort or orbit helper in the foundation; orbiting
   pieces are hand-rolled per-anchor burst calls. If escorts spread
   to other weapons, a small helper would standardize anchor motion.

## Honest worse check

- Pitched shots: the skin tilts with the shot's pitch; the wall,
  built on the ground builder's contract, stays world-upright and
  runs on the horizontal facing. On steep up or down shots the skin
  can peel away from the wall's top edge and the composition reads
  as two layers instead of one body. On level shots, the common
  case, they track exactly (same bow, same apex, matched vertical
  extents). This is the builder's limit, named here rather than
  papered over; change request 2 is the fix if Carter dislikes it.
- Ground clipping: slab bottoms ride about 0.75 below the wave
  center (0.45 of builder skirt under a wall origin 0.31 below
  center), matched to the skin's lower lip at about 0.72, but the
  wall has no ground awareness. A wave fired low over terrain will
  plow its slabs through the ground line visually. For a pushing
  front that mostly reads as intent, but the old wave had nothing
  there to clip, and it is a real difference to judge in game.
- Escort crossings: the chunk-class escort shards roll up to 1.84
  blocks long, and their anchors ride at forward 0.35, inside the
  skin's band, so a big shard swinging through the orbit will cross
  the energy skin plane twice per circuit. They are lit physical
  steel, so crossings read as shards swinging through energy; if it
  reads messy in the PC test, the knob is unit 1.15 down to 1.0,
  not removal.
- Clash density: the doubled fan plus five solid shards is the
  busiest the 24 tick clash flash has ever been. It rides the same
  fade and does not linger, and a parry flash wants density, but it
  is flagged for the PC test rather than assumed good.
- Blade shard braid: both wakes anchor in the same region behind
  the shard, and at extreme sway phases the second strand can cross
  the shard body's silhouette. It is offset 0.16 to the side to keep
  this rare, and it reads as braid crossing when it happens.
- Nothing was reverted. The one deviation from a naive reading of
  the spec's docs (negative arc) is forced by the shipped builder
  math, is documented in the code comments and in change request 1,
  and produces exactly the bow the spec asked for: slabs on the
  skin's own arc, center ahead, pushing outward.
