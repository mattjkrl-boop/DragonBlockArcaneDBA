# Attack Upgrade Pass, Phase 2 Owner Report: Devil Trident

Owner slot: Devil Trident (brimstone spike ring). Spec:
audits/attack-complexity-audit.md section 13 and the worked example in
audits/attack-foundation-addendum.md. The spec for this weapon is
ADDITIVE ONLY: the slam is the flagship built from Carter's target
picture and was already rated RICH. HEAD ccfebf2, no commits made.

## What changed

### DevilSlamShockwaveRenderer: brimstone spike ring added

One new submit in the pieces layer, placed between the fracturePlates
call and the crackFan call, exactly where the spec puts the ring in
the composition:

- ComplexGeometry.spikeEruption, SpikeLayout.RING, count 11 (spec
  range 10 to 12), at ring radius shockR * 0.90, the plates' own
  sizing factor, so the ring layers inside the seal with the plates
  instead of crowding past it. The plate fan factor (0.90) and the
  crack web factor (0.95) are untouched, per the spec's explicit
  do-not-resize note.
- Drawn through ComplexGeometry.submitSolidLit with the render
  state's packed lightCoords: the spike bodies are physical
  blackstone and basalt (root 0xFF1A1216 to tip 0xFF4A4038, the
  addendum's palette) and take world lighting like the debris and
  the grand blade shards. No part of a spike body rides the emissive
  path. The magma shows only through the pre-existing crackFan
  submitEnergy web (MAGMA_HOT to HOT), which is the thin energy seam
  layer the spec allows; that call is byte-identical to before.
- Animation on the host's existing envelope, per the addendum's
  recipe: riseFactor is FxEnvelope.smoothstep(min(1, age / 6)), a
  6 tick RISE with the builder's built-in per-spike stagger of up to
  0.35 of the rise, so the ring erupts in sequence around the circle
  on the slam beat. HOLD is rise 1 with sinkFactor held at 0 for as
  long as the seal lives. The SETTLE is the shared pieces envelope
  (the same fade the plates and web use) falling at the end of the
  host's lifetime, so the spikes crack away standing as fading
  pieces while the seal dies, never retracting.
- sizeScale 1.0: spike lengths roll in the builder's classes (about
  55 percent at 1.2 to 1.9 blocks, 30 percent at 1.9 to 2.6, 15
  percent at 2.6 to 3.2), girth 0.15 to 0.23 of length, each spike a
  bent, faceted closed spindle (384 vertices).

Piece counts, before and after (at distance scale 1.0):

- Spike pieces: 0 before, 11 after (4224 vertices).
- fracturePlates: unchanged, about 410 vertices.
- crackFan: unchanged, about 560 vertices.
- flameTongues: unchanged, 432 vertices (9 tongues).
- Pieces total: about 1400 before, about 5630 after, inside the
  class L budget of 6000. Builder types in the pieces layer: 3
  before, 4 after. Submit calls in the pieces layer: 3 before
  (1 solid, 2 energy), 4 after (1 solid, 1 solid-lit, 2 energy).
- Material emission, sprite bursts, radii, growth, pillar and
  follow math: byte-identical. The only other edits in the file are
  the FxEnvelope import, the two palette constants, and comment and
  javadoc text recording the new layer.

### TridentProngRenderer: flameTongues plus 1 at the prongs

The flameTongues cluster at each needle's base goes from 3 tongues
to 4 (the spec's rows plus 1). Radius, height, colors, sway phase,
and per-needle seeds are unchanged, so each needle keeps its exact
formation silhouette with a fuller flame base.

Piece counts, before and after (at distance scale 1.0):

- Flame tongues: 9 before (3 per needle), 12 after (4 per needle),
  about 430 vertices before, about 576 after.
- Shard needle heads: unchanged, 864 vertices (3 at 288).
- Tendril wakes: unchanged, about 770 vertices.
- Pieces total: about 2070 before, about 2210 after, inside the
  class M budget of 2500.

### TridentShardRenderer and the shard swarm: no change

Per the spec, the swarm is lore and already real entities. The file
is untouched (git status shows only the two renderers above as
modified), and no entity, item, or swarm file was edited anywhere in
this slot.

## What did not change

Slam damage and falloff, crater zoning, deploy and recall rules,
shard swarm behavior, Ki costs, cooldowns, and every timing are
untouched: the diff is two client renderer files, and inside them
only additions plus the one tongue count. No synced field, entity,
or item file changed. Foundation files (ComplexGeometry,
ComplexTextures, DbaShaders) were not edited.

## Verification

Targeted javac (JDK 25 at ~/workspace/tools/jdk-25.0.5+8, sourcepath
src/main/java:src/client/java) over the two owned files, full logs
read, never summaries:

- cp262: 4 errors, all standing baseline closure kinds (DbaItems x2,
  CreativeModeTab Output protected access; DbaMenus x2, MenuType
  private access). Zero errors in owned files.
- cp263: 10 errors, exactly the documented baseline set (the 26.2
  four plus DbaHostileEntity x2, PatrolRobotEntity x2,
  FriezaSoldierEntity x1, NamekianWarriorEntity x1). Zero errors in
  owned files.

git diff --check clean. Byte-level em dash and en dash scan over all
added lines, this report included: zero. No Gradle build run or
claimed; the in-game look is unverified until Carter's PC test.

## Change requests

None. The foundation covered the spec as documented: spikeEruption's
RING layout, stagger, and ease curves and the submitSolidLit lit
entry did everything the worked example promised. Nothing was
missing and nothing had to be improvised.

## Honest worse check

- Budget margin: the slam's pieces total is now about 5630 of the
  6000 class L budget at distance scale 1.0, the slimmest margin in
  the kit. The full 11 spike count only emits up close (distance
  scaling cuts the count toward a floor of 3 at range), and up close
  the slam is the center of the screen by design, so this is headroom
  spent as the spec intended, not a regression. Named here so the
  merge gate sees the number with its context.
- Leaning tips: the builder's RING layout leans spikes outward by
  0.15 to 0.42 radians, so a tallest-class spike (3.2 blocks) can
  lean its tip roughly 1.3 blocks out from the ring line. At the
  default seal radius the tip lands near the seal edge, not past
  the plate fan's own reach by much. The 0.90 sizing was the audit's
  choice made knowing the builder's lean, and the spec forbids
  resizing the composition, so I shipped the spec's numbers and flag
  the tip reach as the first thing for the PC test eye, not a thing
  to silently tune.
- Shared grown radius: the ring radius rides shockR, which carries
  the shockwave's own grow factor, so during the 6 tick rise the
  ring also sweeps outward slightly as it erupts. The plates ride
  the identical radius, so spikes and plates stay registered with
  each other through the whole beat; the ring never drifts against
  the composition it belongs to. This matches the slam's existing
  motion language rather than fighting it.
- Prong flame density: 4 tongues per needle reads fuller at the
  needle bases. Cluster radius and height are unchanged, so the
  three-needle formation and its read at range are the same; the
  change is density, not shape.
- Nothing else moved: every other layer of the flagship (plates,
  web, tongues, emission, bursts, debris field) is the code Carter
  already saw, so the only new read on screen is the ring itself
  and the denser prong flames. Nothing was reverted.
